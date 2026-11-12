package com.delulutopia.core.ui

import androidx.compose.runtime.Composable
import com.delulutopia.core.designsystem.component.DlDialog
import com.delulutopia.core.i18n.t

/** "Take it back?" (web letters/useCancel.tsx), with the draft warning when a draft exists. */
@Composable
fun TakeBackDialog(friend: String, draftEmpty: Boolean, busy: Boolean, onYes: () -> Unit, onNo: () -> Unit) {
    DlDialog(
        title = t("discussions.takeItBack"),
        text = t("letters.theLetterLeavesSHome", "friend" to friend),
        warning = if (draftEmpty) null else t("letters.thisReplacesYourCurrentDraft"),
        confirmLabel = t("discussions.yesTakeItBack"),
        onConfirm = onYes,
        dismissLabel = t("discussions.no"),
        onDismiss = onNo,
        busy = busy,
    )
}
