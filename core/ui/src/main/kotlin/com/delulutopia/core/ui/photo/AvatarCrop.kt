package com.delulutopia.core.ui.photo

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlTextButton
import com.delulutopia.core.i18n.t
import kotlin.math.max
import kotlin.math.min

private const val MAX_ZOOM = 4f
private val VIEW = 280.dp

/** Zoom 1–4× and the photo's top-left inside the window, in px. */
private data class Pose(val zoom: Float, val x: Float, val y: Float)

/**
 * Crop (doc "Delululand — Menu, Profile & Settings" P2, P3; canvas "Crop"): the photo under a round
 * window over a square crop; drag to move, pinch or the slider to zoom 1–4×. Use hands back a
 * [CROP_OUT] px square JPEG; the photo itself never leaves the device.
 */
@Composable
fun AvatarCrop(photo: Bitmap?, onUse: (ByteArray) -> Unit, onCancel: () -> Unit) {
    val view = with(LocalDensity.current) { VIEW.toPx() }
    val base = if (photo != null) max(view / photo.width, view / photo.height) else 1f
    fun clamp(p: Pose): Pose {
        if (photo == null) return p
        val s = base * p.zoom
        return p.copy(x = min(0f, max(view - photo.width * s, p.x)), y = min(0f, max(view - photo.height * s, p.y)))
    }
    fun fit(zoom: Float, prev: Pose?): Pose {
        if (photo == null) return Pose(zoom, 0f, 0f)
        val z = zoom.coerceIn(1f, MAX_ZOOM)
        val s = base * z
        if (prev == null) return clamp(Pose(z, (view - photo.width * s) / 2, (view - photo.height * s) / 2))
        val ps = base * prev.zoom
        val cx = (view / 2 - prev.x) / ps
        val cy = (view / 2 - prev.y) / ps
        return clamp(Pose(z, view / 2 - cx * s, view / 2 - cy * s))
    }
    var pose by remember(photo) { mutableStateOf(fit(1f, null)) }
    val current by rememberUpdatedState(pose)
    val image = remember(photo) { photo?.asImageBitmap() }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(
            Modifier.fillMaxSize().background(Color(0xFF14171F)).statusBarsPadding().navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                DlTextButton(t("profile.cancel"), onCancel)
                Text(t("profile.moveAndZoom"), Modifier.weight(1f), color = Color.White, style = MaterialTheme.typography.titleMedium)
                DlFilledButton(
                    t("profile.use"),
                    {
                        val p = photo ?: return@DlFilledButton
                        val s = base * current.zoom
                        onUse(cropJpeg(p, -current.x / s, -current.y / s, view / s))
                    },
                    enabled = photo != null,
                )
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (image == null) {
                    Text(t("profile.thisFileIsntAPhoto"), color = Color.White)
                } else {
                    Canvas(
                        Modifier
                            .size(VIEW)
                            .clipToBounds()
                            .semantics { contentDescription = t("profile.photoPositionPinch") }
                            .pointerInput(photo) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val p = current
                                    var next = if (zoom != 1f) fit(p.zoom * zoom, p) else p
                                    next = clamp(next.copy(x = next.x + pan.x, y = next.y + pan.y))
                                    pose = next
                                }
                            },
                    ) {
                        val s = base * pose.zoom
                        drawImage(
                            image,
                            dstOffset = IntOffset(pose.x.toInt(), pose.y.toInt()),
                            dstSize = IntSize((image.width * s).toInt(), (image.height * s).toInt()),
                        )
                        val ring = Path().apply { addOval(Rect(Offset.Zero, size)) }
                        clipPath(ring, ClipOp.Difference) { drawRect(Color.Black.copy(alpha = 0.5f)) }
                        drawCircle(Color.White, radius = size.minDimension / 2 - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
                    }
                }
            }
            if (image != null) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("−", color = Color.White)
                    Slider(
                        value = pose.zoom,
                        onValueChange = { pose = fit(it, pose) },
                        valueRange = 1f..MAX_ZOOM,
                        modifier = Modifier.weight(1f).semantics { contentDescription = t("profile.zoom") },
                    )
                    Text("+", color = Color.White)
                }
                Text(
                    t("profile.dragToMovePinchOr"),
                    Modifier.padding(16.dp),
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
