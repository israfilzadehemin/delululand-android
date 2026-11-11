package com.delulutopia.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delulutopia.core.data.Moods
import com.delulutopia.core.designsystem.component.DlBottomSheet
import com.delulutopia.core.designsystem.component.DlDialog
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlTextButton
import com.delulutopia.core.designsystem.component.DlTextField
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.motion.Enter
import com.delulutopia.core.designsystem.motion.enter
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.network.model.Mood
import com.delulutopia.core.ui.EmojiPickerSheet
import kotlinx.coroutines.launch

private sealed interface MoodView {
    data object Grid : MoodView

    data object New : MoodView

    data class Edit(val mood: Mood) : MoodView
}

/**
 * S3 (web home/MoodSheet.tsx; canvas M-Sheet): the universe's moods — the 32 defaults, then its own —
 * in pages of 8 (4 × 2) with "+ New mood" last (disabled at the limit). Swipe for the next page;
 * dots and ‹ › show where you are. Opens on the page holding the current mood, which is marked. One
 * tap sets it and closes. Each custom mood has a ✎ that opens its form, with Delete inside.
 */
@Composable
internal fun MoodSheet(
    current: String?,
    friend: String?,
    friendMood: String?,
    moods: List<Mood>,
    limit: Int,
    canAdd: Boolean,
    vm: HomeActions,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var view by remember { mutableStateOf<MoodView>(MoodView.Grid) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val full = moods.size >= limit

    val entries: List<Mood?> = if (canAdd) moods + null else moods
    val pages = entries.chunked(Moods.PER_PAGE)
    val currentIndex = moods.indexOfFirst { it.key == current }
    val startPage = if (currentIndex < 0) 0 else currentIndex / Moods.PER_PAGE
    val pager = rememberPagerState(initialPage = startPage) { pages.size }

    DlBottomSheet(onDismissRequest = onClose) {
        when (val v = view) {
            MoodView.Grid -> Column(Modifier.padding(bottom = 16.dp)) {
                Text(
                    "How do you feel?",
                    Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.headlineSmall,
                )
                HorizontalPager(pager, Modifier.fillMaxWidth()) { p ->
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        pages[p].chunked(4).forEachIndexed { r, row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEachIndexed { k, m ->
                                    // The grid pops in tile by tile (web .dl-mood-grid: 80 ms, 20 ms apart, capped at 220 ms).
                                    Box(Modifier.weight(1f).enter(Enter.PopIn, (80 + 20 * (r * 4 + k)).coerceAtMost(220), 260)) {
                                        if (m == null) {
                                            NewMoodTile(enabled = !busy && !full) { view = MoodView.New }
                                        } else {
                                            MoodTile(m, on = m.key == current, enabled = !busy) {
                                                busy = true
                                                error = false
                                                scope.launch {
                                                    if (vm.setMood(m.key, moods)) {
                                                        onClose()
                                                    } else {
                                                        error = true
                                                        busy = false
                                                    }
                                                }
                                            }
                                            if (m.custom) {
                                                Box(
                                                    Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(4.dp)
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.surface)
                                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                                        .clickable(enabled = !busy, role = Role.Button) { view = MoodView.Edit(m) }
                                                        .semantics { contentDescription = "Edit ${m.label}" },
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Icon(WebIcons.Pencil, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                        // Keep every page two rows tall so the sheet does not jump.
                        if (pages[p].size <= 4) Spacer(Modifier.height(92.dp))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }, enabled = pager.currentPage > 0) {
                        Icon(WebIcons.Back, "Previous moods", Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(pages.size) { p ->
                            val on = p == pager.currentPage
                            Box(
                                Modifier
                                    .size(width = if (on) 18.dp else 6.dp, height = 6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                                    .clickable { scope.launch { pager.animateScrollToPage(p) } }
                                    .semantics { contentDescription = "Mood page ${p + 1}" },
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    IconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }, enabled = pager.currentPage < pages.size - 1) {
                        Icon(WebIcons.Forward, "More moods", Modifier.size(18.dp))
                    }
                }
                val foot = when {
                    error -> "Not saved. Try again."
                    canAdd && full -> "$limit of $limit — delete one to add another"
                    friend != null -> "One tap sets it. $friend sees it right away, not after an hour."
                    else -> "One tap sets it."
                }
                Text(
                    foot,
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            MoodView.New, is MoodView.Edit -> MoodForm(
                editing = (v as? MoodView.Edit)?.mood,
                catalogue = moods,
                current = current,
                friend = friend,
                friendMood = friendMood,
                vm = vm,
                onBack = { view = MoodView.Grid },
                onDone = { page ->
                    view = MoodView.Grid
                    if (page != null) scope.launch { pager.scrollToPage(page) }
                },
            )
        }
    }
}

@Composable
private fun MoodTile(mood: Mood, on: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(shape)
            .background(if (on) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
            .border(if (on) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { selected = on }
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Text(mood.emoji, fontSize = 26.sp)
        Text(
            mood.label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun NewMoodTile(enabled: Boolean, onClick: () -> Unit) {
    val line = DlTheme.colors.inkDisabled
    Column(
        Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(12.dp))
            .drawBehind {
                drawRoundRect(
                    line,
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))),
                )
            }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Text("+", fontSize = 26.sp, color = if (enabled) MaterialTheme.colorScheme.primary else line)
        Text(
            "New mood",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The add / edit form (web home/MoodForm.tsx; canvas M-New, M-Edit): one emoji from the full picker,
 * a label of up to 16 characters checked as you type, Save; editing adds Delete with a confirm that
 * says who is wearing it. ‹ goes back to the grid.
 */
@Composable
private fun MoodForm(
    editing: Mood?,
    catalogue: List<Mood>,
    current: String?,
    friend: String?,
    friendMood: String?,
    vm: HomeActions,
    onBack: () -> Unit,
    onDone: (page: Int?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var emoji by remember { mutableStateOf(editing?.emoji ?: "") }
    var label by remember { mutableStateOf(editing?.label ?: "") }
    var picker by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val problem = Moods.labelProblem(label, catalogue, editing?.key)
    val count = Moods.labelLength(label)
    val unchanged = editing != null && emoji == editing.emoji && Moods.normalise(label) == editing.label
    val canSave = emoji.isNotEmpty() && count > 0 && problem == null && !busy && !unchanged

    Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = !busy) { Icon(WebIcons.Back, "Back to moods", Modifier.size(20.dp)) }
            Text(if (editing != null) "Edit mood" else "New mood", style = MaterialTheme.typography.headlineSmall)
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .clickable(enabled = !busy, role = Role.Button) { picker = true }
                    .semantics { contentDescription = if (emoji.isNotEmpty()) "Emoji $emoji. Change it" else "Choose an emoji" },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    emoji.ifEmpty {
                        "+"
                    },
                    fontSize = 36.sp,
                    color = if (emoji.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
            if (emoji.isEmpty()) Text("Choose an emoji", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DlTextField(
            value = label,
            onValueChange = {
                if (it.length <= 40) label = it
                error = null
            },
            label = "Label",
            supportingText = Moods.LABEL_RULE,
            errorText = problem,
            counter = "$count/${Moods.LABEL_MAX}",
            enabled = !busy,
        )
        error?.let {
            Text(
                it,
                Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        DlFilledButton(
            if (busy && !confirm) "Saving…" else "Save",
            {
                busy = true
                error = null
                scope.launch {
                    vm.saveMood(editing, emoji, label)
                        .onSuccess { onDone(if (editing == null) catalogue.size / Moods.PER_PAGE else null) }
                        .onFailure {
                            error = it.message
                            busy = false
                        }
                }
            },
            Modifier.fillMaxWidth(),
            enabled = canSave,
        )
        if (editing != null) {
            DlTextButton("Delete mood", { confirm = true }, Modifier.align(Alignment.CenterHorizontally), destructive = true, enabled = !busy)
        }
    }

    if (picker) {
        EmojiPickerSheet(
            title = "Choose an emoji",
            onPick = {
                emoji = it
                error = null
                picker = false
            },
            onDismiss = { picker = false },
        )
    }

    if (confirm && editing != null) {
        val meWears = current == editing.key
        val friendWears = friend != null && friendMood == editing.key
        val wearing = when {
            meWears && friendWears -> "You and $friend are feeling this right now. Deleting it clears your moods."
            friendWears -> "$friend is feeling this right now. Deleting it clears their mood."
            meWears -> "You're feeling this right now. Deleting it clears your mood."
            else -> null
        }
        DlDialog(
            title = "Delete this mood?",
            text = "“${editing.emoji} ${editing.label}” goes from the sheet${if (friend != null) " for both of you" else ""}. Letters already sent keep it." +
                (wearing?.let { "\n\n$it" } ?: ""),
            confirmLabel = "Yes, delete",
            onConfirm = {
                busy = true
                error = null
                scope.launch {
                    val failed = vm.deleteMood(editing.key)
                    confirm = false
                    if (failed == null) {
                        onDone(null)
                    } else {
                        error = failed
                        busy = false
                    }
                }
            },
            dismissLabel = "No",
            onDismiss = { confirm = false },
            busy = busy,
        )
    }
}
