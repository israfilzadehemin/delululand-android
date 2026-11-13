package com.delulutopia.core.ui.photo

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The whole photo flow of Profile and the citizen step (P1–P3, U9): the source sheet while [sheet]
 * is up, then the picker or camera, the 10 MB check ([onTooBig]), and the crop; [onPhoto] gets the
 * cropped JPEG. Keep it composed while the screen is shown, so the picker's answer lands here.
 */
@Composable
fun PhotoChooser(sheet: Boolean, onDismissSheet: () -> Unit, onPhoto: (ByteArray) -> Unit, onTooBig: () -> Unit, onRemove: (() -> Unit)? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cropping by remember { mutableStateOf(false) }
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    val source = rememberPhotoSource { uri ->
        if ((sizeOf(context, uri) ?: 0L) > PHOTO_MAX) {
            onTooBig()
        } else {
            scope.launch {
                photo = withContext(Dispatchers.IO) { decodePhoto(context, uri) }
                cropping = true
            }
        }
    }
    if (sheet) PhotoSourceSheet(source, onDismissSheet, onRemove)
    if (cropping) {
        AvatarCrop(
            photo,
            onUse = { jpeg ->
                cropping = false
                photo = null
                onPhoto(jpeg)
            },
            onCancel = {
                cropping = false
                photo = null
            },
        )
    }
}
