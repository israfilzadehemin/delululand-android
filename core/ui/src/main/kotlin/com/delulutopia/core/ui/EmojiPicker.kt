package com.delulutopia.core.ui

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
import androidx.emoji2.emojipicker.EmojiPickerView
import com.delulutopia.core.designsystem.component.DlBottomSheet

/**
 * The full emoji picker in its own sheet (web components/EmojiPicker.tsx, emoji-picker-element):
 * the platform's emoji picker, with its recents, categories and skin tones. Used by the editor's
 * emoji button, reactions' "all emojis" and the custom mood form.
 */
@Composable
fun EmojiPickerSheet(title: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val pick by rememberUpdatedState(onPick)
    DlBottomSheet(onDismissRequest = onDismiss) {
        Column {
            Text(title, Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp), style = MaterialTheme.typography.titleLarge)
            AndroidView(
                factory = { context ->
                    EmojiPickerView(context).apply {
                        emojiGridColumns = 8
                        setOnEmojiPickedListener { item -> pick(item.emoji) }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(380.dp),
            )
        }
    }
}
