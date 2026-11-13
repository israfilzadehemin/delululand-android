package com.delulutopia.core.ui.editor

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/** A tap that does not take focus, so the editor keeps its selection and keyboard. */
fun Modifier.clickableNoFocus(onClick: () -> Unit): Modifier = pointerInput(onClick) { detectTapGestures { onClick() } }
