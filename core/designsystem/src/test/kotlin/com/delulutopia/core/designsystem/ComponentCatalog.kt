package com.delulutopia.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.delulutopia.core.designsystem.component.DlAvatar
import com.delulutopia.core.designsystem.component.DlCountPill
import com.delulutopia.core.designsystem.component.DlDot
import com.delulutopia.core.designsystem.component.DlEditWindowChip
import com.delulutopia.core.designsystem.component.DlExtendedFab
import com.delulutopia.core.designsystem.component.DlFab
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlListItem
import com.delulutopia.core.designsystem.component.DlMailPill
import com.delulutopia.core.designsystem.component.DlNavigationBar
import com.delulutopia.core.designsystem.component.DlNavigationItem
import com.delulutopia.core.designsystem.component.DlNavigationYouItem
import com.delulutopia.core.designsystem.component.DlNewPill
import com.delulutopia.core.designsystem.component.DlOutlinedButton
import com.delulutopia.core.designsystem.component.DlSectionLabel
import com.delulutopia.core.designsystem.component.DlSnackbar
import com.delulutopia.core.designsystem.component.DlSwitch
import com.delulutopia.core.designsystem.component.DlTextButton
import com.delulutopia.core.designsystem.component.DlTextField
import com.delulutopia.core.designsystem.component.DlTonalButton
import com.delulutopia.core.designsystem.component.DlTopAppBar
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.designsystem.theme.NoteColor

/** Every P0 component in the states the canvas draws (Z-Components), for screenshot review. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComponentCatalog() {
    Column(
        Modifier
            .width(412.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Section("Buttons") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DlFilledButton("Send", {}, icon = DlIcons.Send)
                DlTonalButton("Draw", {}, icon = DlIcons.Draw)
                DlOutlinedButton("Edit", {})
                DlTextButton("Keep writing", {})
                DlTextButton("Log out", {}, destructive = true)
                DlFilledButton("Send", {}, enabled = false)
            }
        }
        Section("FABs") {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                DlFab(DlIcons.Pen, "Continue your letter", {}, hasDraft = true)
                DlFab(DlIcons.NotePlus, "Start a discussion", {})
                DlFab(DlIcons.EnvelopePlus, "New email", {})
            }
            Spacer(Modifier.height(12.dp))
            DlExtendedFab(DlIcons.Pen, "Continue your draft", {})
        }
        Section("Top app bar") { DlTopAppBar(title = "Letter", onBack = {}) }
        Section("Navigation bar") {
            DlNavigationBar {
                DlNavigationItem(false, {}, "Home", DlIcons.Home, DlIcons.HomeActive)
                DlNavigationItem(true, {}, "Letters", DlIcons.Letters, DlIcons.LettersActive, hasNew = true)
                DlNavigationItem(false, {}, "Discussions", DlIcons.Discussions, DlIcons.DiscussionsActive, hasNew = true)
                DlNavigationItem(false, {}, "Emails", DlIcons.Emails, DlIcons.EmailsActive)
                DlNavigationYouItem(false, {}, "You", alert = true) {
                    DlAvatar("EI", DlTheme.colors.userA, size = 24.dp)
                }
            }
        }
        Section("Selection and status") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                DlSwitch(checked = true, onCheckedChange = {})
                DlSwitch(checked = false, onCheckedChange = {})
                DlNewPill("NEW")
                DlNewPill("2 new")
                DlCountPill("2 new")
                DlMailPill("2 new")
                DlEditWindowChip("3:41 to edit")
                DlDot()
                DlDot(size = 9.dp, error = true)
            }
        }
        Section("Text fields") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DlTextField("Emin", {}, label = "Username", supportingText = "3–16 letters or digits", maxLength = 16)
                DlTextField("K7QM-WX4P", {}, label = "Key", errorText = "That key doesn’t fit.", centeredMono = true)
            }
        }
        Section("Avatars") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                DlAvatar("EI", DlTheme.colors.userA, size = 88.dp)
                DlAvatar("L", DlTheme.colors.userB, size = 44.dp, ring = DlTheme.colors.userB)
                DlAvatar("EI", DlTheme.colors.userA, size = 36.dp)
                DlAvatar("Ə", DlTheme.colors.userB, size = 24.dp)
            }
        }
        Section("Snackbar and list item") {
            DlSnackbar("Arrives at 14:50", actionLabel = "OK")
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                DlListItem("Notifications", supporting = "On · 4 of 12 events", icon = DlIcons.Bell, onClick = {})
            }
        }
        Section("Type") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Delulutopia", style = MaterialTheme.typography.displaySmall)
                Text("Discussions", style = MaterialTheme.typography.titleLarge)
                Text("On its way · arrives 14:50", style = MaterialTheme.typography.bodyMedium)
                Text("Sent 12:40 · Arrived 13:40", style = MaterialTheme.typography.bodySmall)
                Text("28:41", style = DlTheme.type.countdown)
                Text("Dear Emin, the week went by slower than I expected.", style = DlTheme.type.letterBody)
                Text("Əziz Leyla, Ənənəvi çay", style = DlTheme.type.noteTitle)
                Text("this sun is literally me today", style = DlTheme.type.boardNote)
            }
        }
        Section("Notes and materials") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NoteColor.entries.forEach { c ->
                    Box(
                        Modifier
                            .size(40.dp, 28.dp)
                            .background(DlTheme.colors.notes.getValue(c).face, RoundedCornerShape(4.dp)),
                    )
                }
                Box(Modifier.size(40.dp, 28.dp).background(DlTheme.colors.letterSheet, RoundedCornerShape(4.dp)))
                Box(Modifier.size(40.dp, 28.dp).background(DlTheme.colors.mailBackground, RoundedCornerShape(4.dp)))
                Box(Modifier.size(40.dp, 28.dp).background(DlTheme.colors.boardSurface, RoundedCornerShape(4.dp)))
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DlSectionLabel(title)
        content()
    }
}
