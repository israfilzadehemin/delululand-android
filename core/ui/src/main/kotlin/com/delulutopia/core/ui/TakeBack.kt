package com.delulutopia.core.ui

import androidx.compose.runtime.Composable
import com.delulutopia.core.designsystem.component.DlDialog

/** "Take it back?" (web letters/useCancel.tsx), with the draft warning when a draft exists. */
@Composable
fun TakeBackDialog(friend: String, draftEmpty: Boolean, busy: Boolean, onYes: () -> Unit, onNo: () -> Unit) {
    DlDialog(
        title = "Take it back?",
        text = "The letter leaves $friend's Home right away and its text goes back into your draft.",
        warning = if (draftEmpty) null else "This replaces your current draft.",
        confirmLabel = "Yes, take it back",
        onConfirm = onYes,
        dismissLabel = "No",
        onDismiss = onNo,
        busy = busy,
    )
}
