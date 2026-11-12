package com.delulutopia.feature.you

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.delulutopia.core.designsystem.component.DlBottomSheet
import com.delulutopia.core.designsystem.component.DlPinDots
import com.delulutopia.core.designsystem.component.DlPinPad
import com.delulutopia.core.designsystem.component.PIN_LENGTH
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.i18n.t
import com.delulutopia.core.ui.LocalBrand
import kotlinx.coroutines.delay

private val STEPS get() = listOf(t("home.pinStep.current"), t("home.pinStep.new"), t("home.pinStep.confirm"))

/**
 * Change PIN (FS-A17, artboard ChangePIN): current, new, confirm in one sheet with one keypad. A
 * wrong current PIN restarts from the first step, differing new PINs restart the second. Every other
 * device stays logged in. [onChanged] gets the new PIN (fingerprint unlock keeps it).
 */
@Composable
fun ChangePinSheet(onClose: () -> Unit, onChanged: (String) -> Unit, vm: ChangePinViewModel = hiltViewModel()) {
    val brand = LocalBrand.current
    // The view model outlives the sheet (it belongs to the You screen): a second opening must not
    // flash the last one's "PIN changed" or its digits, so the sheet resets when it closes.
    val close = {
        onClose()
        vm.reset()
    }
    LaunchedEffect(vm.changed) {
        if (vm.changed) {
            delay(1_200)
            close()
        }
    }
    DlBottomSheet(onDismissRequest = { if (!vm.busy) close() }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(t("settings.changePin"), style = MaterialTheme.typography.titleLarge)
            if (vm.changed) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(DlIcons.Check, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(t("home.pinChanged"), fontWeight = FontWeight.SemiBold)
                }
                return@Column
            }
            STEPS.forEachIndexed { i, label ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        label,
                        Modifier.weight(1f),
                        fontSize = 15.sp,
                        fontWeight = if (i == vm.step) FontWeight.Bold else FontWeight.Medium,
                        color = if (i == vm.step) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    when {
                        i < vm.step -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(DlIcons.Check, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(t("home.entered"), style = MaterialTheme.typography.bodySmall)
                        }
                        i == vm.step -> DlPinDots(
                            vm.values[i].length,
                            t(
                                "home.ofDigits",
                                "label" to label,
                                "count" to vm.values[i].length,
                                "pinlength" to PIN_LENGTH,
                            ),
                        )
                        else -> Text(t("home.next"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(
                vm.error ?: t("home.yourOtherDevicesStayLogged"),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = if (vm.error !=
                        null
                    ) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                ),
                color = if (vm.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DlPinPad(
                digits = vm.values[vm.step],
                onDigits = vm::digits,
                onComplete = { vm.complete(it, brand, onChanged) },
                enabled = !vm.busy,
            )
        }
    }
}

/** X1: "Enter your PIN" before fingerprint unlock is turned on. */
@Composable
fun FingerprintPinSheet(userId: Int, onClose: () -> Unit, onPin: (String) -> Unit, vm: FingerprintPinViewModel = hiltViewModel()) {
    val brand = LocalBrand.current
    DlBottomSheet(onDismissRequest = { if (!vm.busy) onClose() }) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(t("settings.fingerprint"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
            Text(t("pin.enterYourPin"), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            DlPinDots(vm.digits.length, t("pin.dots.enter", "n" to vm.digits.length, "total" to PIN_LENGTH), error = vm.error != null)
            Text(
                vm.error ?: t("settings.fingerprintKept"),
                style = MaterialTheme.typography.bodySmall,
                color = if (vm.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DlPinPad(digits = vm.digits, onDigits = { vm.digits = it }, onComplete = { vm.check(userId, it, brand, onPin) }, enabled = !vm.busy)
        }
    }
}
