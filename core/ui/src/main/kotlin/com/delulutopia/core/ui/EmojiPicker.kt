package com.delulutopia.core.ui

import android.content.Context
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.children
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.delulutopia.core.designsystem.component.DlBottomSheet

/** The picker's height in the sheet. */
private val PickerHeight = 380.dp

/** Keyboard cells (Gboard): about 43 dp wide, so 9 columns on most phones and 8 on a 360 dp one. */
private val CellWidth = 43.dp
private const val MIN_COLUMNS = 8

/** The emoji itself (its glyph about 28 dp), centred in its cell. */
private val EmojiSize = 32.dp

/** emoji2-emojipicker: the 2 dp it takes off each emoji, and its header (42) and category-title allowance (2 × 24 + 4). */
private val EmojiPadding = 2.dp
private val PickerChrome = 94.dp

/**
 * The full emoji picker in its own sheet (web components/EmojiPicker.tsx, emoji-picker-element):
 * the platform's emoji picker, with its recents, categories and skin tones. Used by the editor's
 * emoji button, reactions' "all emojis" and the custom mood form.
 *
 * The platform picker draws each emoji as big as its cell (≈ 43 dp glyphs at 8 columns), so the
 * cells are keyboard-sized here with a 32 dp emoji centred in each ([CentredEmojis]). Its grid is
 * a RecyclerView inside a Compose sheet, whose drag would take every vertical swipe on it first;
 * [GestureClaimingFrame] gives the whole swipe to the grid instead (pulled down at the top, the
 * grid still hands the rest to the sheet through nested scrolling).
 */
@Composable
fun EmojiPickerSheet(title: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val pick by rememberUpdatedState(onPick)
    DlBottomSheet(onDismissRequest = onDismiss) {
        Column {
            Text(title, Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp), style = MaterialTheme.typography.titleLarge)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = (maxWidth / CellWidth).toInt().coerceAtLeast(MIN_COLUMNS)
                // emoji2 sizes a cell's height as (picker - chrome) / rows; the emoji is that minus 2 dp.
                val rows = (PickerHeight - PickerChrome) / (EmojiSize + EmojiPadding)
                AndroidView(
                    factory = { context ->
                        GestureClaimingFrame(context).apply {
                            addView(
                                EmojiPickerView(context).apply {
                                    emojiGridColumns = columns
                                    emojiGridRows = rows
                                    setOnHierarchyChangeListener(CentredEmojis)
                                    setOnEmojiPickedListener { item -> pick(item.emoji) }
                                },
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(PickerHeight),
                )
            }
        }
    }
}

/**
 * Asks the sheet not to intercept the moment a finger lands on the picker, so a vertical swipe
 * anywhere on the grid scrolls it (Compose's AndroidView turns this into consuming the gesture
 * before the sheet's drag sees it).
 */
private class GestureClaimingFrame(context: Context) : FrameLayout(context) {
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) parent?.requestDisallowInterceptTouchEvent(true)
        return super.dispatchTouchEvent(ev)
    }
}

/**
 * The picker builds its views once its emoji data has loaded (and again if rebuilt); each time,
 * its grid (the second RecyclerView, after the category header) gets [CentredEmojiCell].
 */
private object CentredEmojis : ViewGroup.OnHierarchyChangeListener {
    override fun onChildViewAdded(parent: View?, child: View?) {
        val grid = (child as? ViewGroup)?.children?.filterIsInstance<RecyclerView>()?.lastOrNull() ?: return
        grid.addItemDecoration(CentredEmojiCell)
    }

    override fun onChildViewRemoved(parent: View?, child: View?) = Unit
}

/**
 * The picker places an emoji smaller than its cell at the cell's top-left; this centres it and
 * makes each row as tall as a cell is wide. Category titles (full-row items) are left alone.
 */
private object CentredEmojiCell : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val grid = parent.layoutManager as? GridLayoutManager ?: return
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION || grid.spanSizeLookup.getSpanSize(position) != 1) return
        val cell = view.layoutParams ?: return
        if (cell.width <= 0 || cell.height <= 0) return
        val padding = (EmojiPadding.value * view.resources.displayMetrics.density).toInt()
        val gap = ((cell.width - (minOf(cell.width, cell.height) - padding)) / 2).coerceAtLeast(0)
        outRect.set(gap, gap, 0, gap)
    }
}
