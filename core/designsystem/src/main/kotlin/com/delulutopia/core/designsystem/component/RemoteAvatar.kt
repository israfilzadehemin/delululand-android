package com.delulutopia.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

/** The server's base URL ("https://…/"), so relative media paths from the API (`/api/users/7/avatar/3`) resolve. */
val LocalServerBaseUrl = staticCompositionLocalOf { "" }

/** Resolves an API media path against [LocalServerBaseUrl]; absolute URLs pass through. */
fun resolveMedia(base: String, path: String): String =
    if (path.startsWith("http://") || path.startsWith("https://")) path else base.trimEnd('/') + "/" + path.trimStart('/')

/**
 * A citizen's picture: the uploaded photo when [avatarUrl] is set (loaded with the app's cookies),
 * else initials on the signature colour. [ring] as in [DlAvatar].
 */
@Composable
fun DlRemoteAvatar(
    avatarUrl: String?,
    name: String,
    signature: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    ring: Color? = null,
    ringWidth: Dp = 2.dp,
    initials: String = initialsOf(name),
    initialsColor: Color = Color(0xFF202B45),
) {
    val base = LocalServerBaseUrl.current
    DlAvatar(
        initials = initials,
        signature = signature,
        modifier = modifier,
        size = size,
        ring = ring,
        ringWidth = ringWidth,
        initialsColor = initialsColor,
        content = if (avatarUrl.isNullOrBlank()) {
            null
        } else {
            {
                SubcomposeAsyncImage(
                    model = resolveMedia(base, avatarUrl),
                    contentDescription = null,
                    // Fill the circle: a small file must not sit as a stamp in the middle of it.
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = { DlAvatar(initials, signature, size = size, initialsColor = initialsColor) },
                    error = { DlAvatar(initials, signature, size = size, initialsColor = initialsColor) },
                )
            }
        },
    )
}
