package com.delulutopia.feature.home

import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.delulutopia.core.data.Time
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlOutlinedButton
import com.delulutopia.core.designsystem.component.LocalServerBaseUrl
import com.delulutopia.core.designsystem.component.resolveMedia
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.theme.Caveat
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.i18n.t
import com.delulutopia.core.i18n.tn
import com.delulutopia.core.network.model.Home
import com.delulutopia.core.network.model.Selfie
import com.delulutopia.core.ui.LocalNotice
import com.delulutopia.core.ui.NewChip
import com.delulutopia.core.ui.ageWords
import com.delulutopia.core.ui.media.rememberSelfieCapture
import java.time.Instant
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val HOUR = 3_600_000L
private val Paper = Color(0xFFFBFAF6)
private val PaperDark = Color(0xFFE9E5DA)
private val Ink = Color(0xFF33323B)
private val InkQuiet = Color(0xFF55535F)
private val Navy = Color(0xFF202B45)
private val PinA = Color(0xFFC96F52)
private val PinB = Color(0xFF5F8A7D)
private val PinNone = Color(0xFF8C8A93)

/** Selfies Q2: the friend's selfie stays locked until mine is up. */
private val LockIcon: ImageVector by lazy {
    ImageVector.Builder("Lock", 24.dp, 24.dp, 24f, 24f).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        moveTo(7f, 11f)
        lineTo(17f, 11f)
        arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
        verticalLineToRelative(5f)
        arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
        lineTo(7f, 20f)
        arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
        verticalLineToRelative(-5f)
        arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
        close()
        moveTo(8f, 11f)
        verticalLineTo(8f)
        arcToRelative(4f, 4f, 0f, false, true, 8f, 0f)
        verticalLineToRelative(3f)
    }.build()
}

private fun alive(s: Selfie?, nowMs: Long): Selfie? = s?.takeIf { (Time.ms(it.expiresAt) ?: 0L) > nowMs }

private fun hoursLeft(iso: String, nowMs: Long): Int = maxOf(1, (((Time.ms(iso) ?: nowMs) - nowMs + HOUR - 1) / HOUR).toInt())

private fun blurBytes(dataUri: String?): ByteArray? = dataUri?.substringAfter(',', "")?.takeIf { it.isNotEmpty() }?.let {
    runCatching { Base64.decode(it, Base64.DEFAULT) }.getOrNull()
}

private class Shown(val selfie: Selfie, val mine: Boolean, val name: String)

/**
 * Daily selfies (web selfies/SelfieBoard.tsx, doc "Delululand — Selfies"): a cork board in a light
 * wood frame under the Board with my polaroid on the left and the friend's on the right (DS1, DS2).
 * My empty frame opens the phone's camera on the front lens (Q1), then Your selfie with Retake / Use
 * (DS4). Nothing is ever changed or removed (DS3); a selfie leaves at `expiresAt` (DS7). The
 * friend's stays blurred until mine is up (Q2) and carries NEW until shown unlocked (Q3). Folds
 * like the Board (DS1).
 */
@Composable
internal fun SelfieBoard(home: Home, nowMs: Long, vm: HomeActions) {
    val board = home.selfies ?: return
    val notice = LocalNotice.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val base = LocalServerBaseUrl.current
    val mini by vm.selfieMini.collectAsStateWithLifecycle()
    val mine = alive(board.mine, nowMs)
    val theirs = alive(board.theirs, nowMs)
    val locked = theirs != null && (theirs.locked || mine == null || theirs.url == null)
    val friend = home.friendName
    val waiting = home.friend == null

    var review by remember { mutableStateOf<Uri?>(null) }
    var pending by remember { mutableStateOf<Uri?>(null) }
    var viewing by remember { mutableStateOf<Int?>(null) }

    var ribbon by remember { mutableStateOf(if (home.selfieNew && theirs != null) theirs.id else null) }
    if (home.selfieNew && theirs != null && ribbon != theirs.id) ribbon = theirs.id
    LaunchedEffect(mini, home.selfieNew, theirs?.id, locked) {
        if (!mini && home.selfieNew && theirs != null && !locked) vm.selfieSeen(theirs.id)
    }

    val capture = rememberSelfieCapture(onPhoto = { review = it }, onNoCamera = { notice(t("media.noCameraApp")) })
    val use: (Uri) -> Unit = { uri ->
        review = null
        pending = uri
        scope.launch {
            val bytes = withContext(Dispatchers.IO) { runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull() }
            val error = if (bytes == null) t("selfie.notPinned") else vm.postSelfie(bytes)
            pending = null
            error?.let(notice)
        }
    }

    val gallery = buildList {
        if (mine != null) add(Shown(mine, true, home.me.nickname))
        if (theirs != null && !locked) add(Shown(theirs, false, friend))
    }

    Fold(mini, Modifier.fillMaxWidth(), strip = { Strip(home, mine, theirs, locked, nowMs, base) { vm.setSelfieMini(false) } }) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Cork {
                // Left: mine (or the empty frame, or the one developing).
                when {
                    pending != null -> Polaroid(-3f, PinA, caption = home.me.nickname, time = Time.timeOf(nowMs)) {
                        AsyncImage(pending, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.55f)
                        Text(
                            t("selfie.pinning"),
                            Modifier.align(Alignment.BottomEnd).padding(5.dp).background(Color(0x99141317), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                    mine != null -> Polaroid(
                        -3f,
                        PinA,
                        caption = home.me.nickname,
                        time = Time.time(mine.createdAt),
                        label = t("selfie.yourSelfieOpen", "time" to Time.time(mine.createdAt)),
                        onClick = { viewing = 0 },
                    ) {
                        AsyncImage(resolveMedia(base, mine.url.orEmpty()), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                    else -> EmptyPolaroid(-3f, home.me.nickname) {
                        Column(
                            Modifier.fillMaxSize().background(Color(0x99FFFFFF)).clickable(role = Role.Button, onClick = capture),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(Modifier.size(44.dp).clip(CircleShape).background(Navy), contentAlignment = Alignment.Center) {
                                Icon(WebIcons.Camera, null, Modifier.size(22.dp), tint = Color.White)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                t("selfie.addYourSelfie"),
                                color = Navy,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                // Right: the friend's.
                when {
                    waiting -> EmptyPolaroid(2.5f, "?") { QuietWords(t("selfie.waiting")) }
                    theirs == null -> EmptyPolaroid(2.5f, friend) { QuietWords(t("selfie.noSelfieYet", "friend" to friend)) }
                    locked -> Polaroid(
                        2.5f,
                        PinB,
                        caption = friend,
                        time = Time.time(theirs.createdAt),
                        label = t("selfie.lockedLabel", "friend" to friend),
                        onClick = if (pending == null) capture else null,
                        newChip = ribbon == theirs.id,
                    ) {
                        AsyncImage(blurBytes(theirs.blurPreview), null, Modifier.fillMaxSize().blur(10.dp), contentScale = ContentScale.Crop)
                        Column(
                            Modifier.fillMaxSize().padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0x8C141317)), contentAlignment = Alignment.Center) {
                                Icon(LockIcon, null, Modifier.size(18.dp), tint = Color.White)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                t("selfie.addYoursToSeeIt"),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                    else -> Polaroid(
                        2.5f,
                        PinB,
                        caption = friend,
                        time = Time.time(theirs.createdAt),
                        label = t("selfie.friendsSelfieOpen", "friend" to friend, "time" to Time.time(theirs.createdAt)),
                        onClick = { viewing = gallery.indexOfFirst { it.selfie.id == theirs.id }.coerceAtLeast(0) },
                        newChip = ribbon == theirs.id,
                    ) {
                        AsyncImage(resolveMedia(base, theirs.url.orEmpty()), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                val meta = when {
                    pending != null -> t("selfie.metaPinning")
                    mine != null -> tn("selfie.metaYoursFades", hoursLeft(mine.expiresAt, nowMs))
                    theirs != null -> t("selfie.metaAddYours", "friend" to friend)
                    else -> tn("selfie.metaStays", board.ttlHours)
                }
                Text(
                    t("selfie.todays") + " · " + meta,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(onClick = { vm.setSelfieMini(true) }) { Icon(WebIcons.ChevronUp, t("selfie.fold")) }
            }
        }
    }

    review?.let { uri ->
        SelfieReview(
            uri = uri,
            name = home.me.nickname,
            time = Time.timeOf(nowMs),
            until = Time.clock(Instant.ofEpochMilli(nowMs + board.ttlHours * HOUR).toString(), nowMs),
            onRetake = {
                review = null
                capture()
            },
            onUse = { use(uri) },
            onClose = { review = null },
        )
    }
    viewing?.let { start ->
        if (gallery.isNotEmpty()) SelfieViewer(gallery, start.coerceIn(0, gallery.lastIndex), nowMs, base) { viewing = null }
    }
}

/**
 * DS2: a cork board in a light wood frame, both real material (BoardTextures), the frame's lip
 * shading the cork just inside it; one bevel line and a low shadow, as iOS.
 */
@Composable
private fun Cork(content: @Composable () -> Unit) {
    val dark = DlTheme.isDark
    val shape = RoundedCornerShape(5.dp)
    Box(
        Modifier.fillMaxWidth().shadow(3.dp, shape).clip(shape).drawBehind { fill(BoardTextures.wood(dark)) }
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Black.copy(alpha = 0.22f))), shape)
            .padding(10.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().height(232.dp).clipToBounds().drawBehind {
                fill(BoardTextures.cork(dark))
                lip(if (dark) 0.45f else 0.32f)
            },
        ) {
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                content()
            }
        }
    }
}

/** Draws [image] scaled to fill and centred, cropping the overflow (SwiftUI scaledToFill). */
private fun DrawScope.fill(image: ImageBitmap) {
    val scale = maxOf(size.width / image.width, size.height / image.height)
    val w = (image.width * scale).roundToInt()
    val h = (image.height * scale).roundToInt()
    drawImage(
        image,
        dstOffset = IntOffset(((size.width - w) / 2).roundToInt(), ((size.height - h) / 2).roundToInt()),
        dstSize = IntSize(w, h),
        filterQuality = FilterQuality.Medium,
    )
}

/** The frame's lip: a soft shade along each inner edge of the cork. */
private fun DrawScope.lip(alpha: Float) {
    val d = 7.dp.toPx()
    val shade = Color.Black.copy(alpha = alpha * 0.7f)
    drawRect(Brush.verticalGradient(listOf(shade, Color.Transparent), 0f, d), size = Size(size.width, d))
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, shade), size.height - d, size.height), Offset(0f, size.height - d), Size(size.width, d))
    drawRect(Brush.horizontalGradient(listOf(shade, Color.Transparent), 0f, d), size = Size(d, size.height))
    drawRect(Brush.horizontalGradient(listOf(Color.Transparent, shade), size.width - d, size.width), Offset(size.width - d, 0f), Size(d, size.height))
}

/** Polaroid card stock: faintly warm, a touch darker toward the bottom, a hairline edge. */
private fun Modifier.polaroidPaper(dark: Boolean): Modifier {
    val top = if (dark) PaperDark else Paper
    return background(if (dark) Color(0xFFCCC7BA) else Color(0xFFEDEBE3))
        .background(Brush.verticalGradient(listOf(top, top.copy(alpha = 0.94f))))
        .border(0.5.dp, Color.Black.copy(alpha = 0.07f))
}

@Composable
private fun Pin(color: Color, modifier: Modifier) {
    Box(
        modifier.size(15.dp).shadow(2.dp, CircleShape).clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.75f), color), center = Offset(15f, 12f), radius = 22f)),
    )
}

@Composable
private fun Polaroid(
    tilt: Float,
    pin: Color,
    caption: String,
    time: String,
    label: String? = null,
    onClick: (() -> Unit)? = null,
    newChip: Boolean = false,
    photo: @Composable BoxScope.() -> Unit,
) {
    val dark = DlTheme.isDark
    Box(Modifier.rotate(tilt)) {
        Column(
            // Pinned flat to the cork: a tight contact shadow, not a lift.
            Modifier.width(138.dp).shadow(1.5.dp).polaroidPaper(dark)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier)
                .padding(start = 8.dp, end = 8.dp, top = 8.dp),
        ) {
            Box(Modifier.size(122.dp).background(Color(0xFF3A3840)).border(0.5.dp, Color.Black.copy(alpha = 0.16f)), content = photo)
            Caption(caption, time)
        }
        Pin(pin, Modifier.align(Alignment.TopCenter).offset(y = (-6).dp))
        if (newChip) NewChip(Modifier.padding(13.dp), paper = false)
    }
}

@Composable
private fun EmptyPolaroid(tilt: Float, caption: String, inside: @Composable BoxScope.() -> Unit) {
    val dashed = Color(0x8C33323B)
    Box(Modifier.rotate(tilt)) {
        Column(
            Modifier.width(138.dp).background(Color(0x9EFBFAF6)).dashedBorder(dashed).padding(start = 8.dp, end = 8.dp, top = 8.dp),
        ) {
            Box(Modifier.size(122.dp), content = inside)
            Caption(caption, "", quiet = true)
        }
        Pin(PinNone, Modifier.align(Alignment.TopCenter).offset(y = (-6).dp))
    }
}

/** The dashed outline of an empty frame (web .dl-sf-pol--empty). */
private fun Modifier.dashedBorder(color: Color): Modifier = drawBehind {
    drawRect(color, style = Stroke(width = 1.5f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * density, 4f * density))))
}

@Composable
private fun BoxScope.QuietWords(text: String) {
    Text(
        text,
        Modifier.align(Alignment.Center).padding(horizontal = 10.dp),
        color = Color(0xFF48536C),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
private fun Caption(name: String, time: String, quiet: Boolean = false, big: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().height(if (big) 68.dp else 36.dp).padding(top = if (big) 12.dp else 5.dp, start = 2.dp, end = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            name,
            style = TextStyle(fontFamily = Caveat, fontWeight = FontWeight.SemiBold, fontSize = if (big) 34.sp else 21.sp),
            color = if (quiet) InkQuiet else Ink,
            maxLines = 1,
        )
        if (time.isNotEmpty()) {
            Text(
                time,
                style = TextStyle(fontFamily = Caveat, fontWeight = FontWeight.SemiBold, fontSize = if (big) 24.sp else 16.sp),
                color = InkQuiet,
            )
        }
    }
}

/** DS1: the folded strip, the Board's on wood, with NEW while unseen. */
@Composable
private fun Strip(home: Home, mine: Selfie?, theirs: Selfie?, locked: Boolean, nowMs: Long, base: String, onUnfold: () -> Unit) {
    val dark = DlTheme.isDark
    Row(
        Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp)).drawBehind { fill(BoardTextures.wood(dark)) }
            .clickable(onClickLabel = t("selfie.unfold"), onClick = onUnfold)
            .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.size(72.dp, 54.dp).clip(RoundedCornerShape(3.dp)).drawBehind { fill(BoardTextures.cork(dark)) },
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniPolaroid(-5f, mine?.url?.let { resolveMedia(base, it) }, false)
            MiniPolaroid(
                4f,
                if (theirs == null) {
                    null
                } else if (locked) {
                    blurBytes(theirs.blurPreview)
                } else {
                    theirs.url?.let { resolveMedia(base, it) }
                },
                locked,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t("selfie.selfies"), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), color = Color.White)
                if (home.selfieNew && theirs != null) NewChip()
            }
            Text(
                when {
                    theirs != null -> t("selfie.strip.friend", "friend" to home.friendName, "age" to ageWords(theirs.createdAt, nowMs))
                    mine != null -> t("selfie.strip.mine", "age" to ageWords(mine.createdAt, nowMs))
                    else -> t("selfie.strip.none")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
        Icon(WebIcons.ChevronDown, null, tint = Color.White)
    }
}

@Composable
private fun MiniPolaroid(tilt: Float, model: Any?, locked: Boolean) {
    Box(
        Modifier.rotate(tilt).size(25.dp, 31.dp).shadow(1.dp).background(if (model == null) Color(0x80FBFAF6) else Paper)
            .padding(start = 2.dp, end = 2.dp, top = 2.dp, bottom = 7.dp),
    ) {
        if (model != null) AsyncImage(model, null, Modifier.fillMaxSize().then(if (locked) Modifier.blur(3.dp) else Modifier), contentScale = ContentScale.Crop)
    }
}

/** DS4: the shot as its polaroid, before it is pinned for good. */
@Composable
private fun SelfieReview(uri: Uri, name: String, time: String, until: String, onRetake: () -> Unit, onUse: () -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(WebIcons.Close, t("selfie.close")) }
                    Text(t("selfie.reviewTitle"), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
                Column(
                    Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    BigPolaroid(-1.5f, name, time) {
                        AsyncImage(uri, t("selfie.alt.mine"), Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                    Spacer(Modifier.height(22.dp))
                    Text(
                        t("selfie.reviewStaysUntil", "when" to until),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        t("selfie.reviewFinal"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DlOutlinedButton(t("selfie.retake"), onRetake, Modifier.weight(1f), icon = WebIcons.Camera)
                    DlFilledButton(t("selfie.use"), onUse, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BigPolaroid(tilt: Float, name: String, time: String, photo: @Composable BoxScope.() -> Unit) {
    Column(
        Modifier.rotate(tilt).width(310.dp).shadow(3.dp).polaroidPaper(DlTheme.isDark).padding(start = 16.dp, end = 16.dp, top = 16.dp),
    ) {
        Box(Modifier.size(278.dp).background(Color(0xFF3A3840)).border(0.5.dp, Color.Black.copy(alpha = 0.16f)), content = photo)
        Caption(name, time, big = true)
    }
}

/** DS8: full screen, swipe between the two; who, when, and how long it stays. No actions. */
@Composable
private fun SelfieViewer(items: List<Shown>, start: Int, nowMs: Long, base: String, onClose: () -> Unit) {
    val c = DlTheme.colors
    val pager = rememberPagerState(initialPage = start) { items.size }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(
            Modifier.fillMaxSize().semantics { contentDescription = t("selfie.viewerLabel", "n" to pager.currentPage + 1, "total" to items.size) },
            color = c.lightbox,
            contentColor = c.onLightbox,
        ) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onClose) { Icon(WebIcons.Close, t("selfie.close"), tint = c.onLightbox) }
                }
                HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), key = { items[it].selfie.id }) { page ->
                    val item = items[page]
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BigPolaroid(-1f, item.name, Time.time(item.selfie.createdAt)) {
                            AsyncImage(
                                resolveMedia(base, item.selfie.url.orEmpty()),
                                if (item.mine) t("selfie.alt.mine") else t("selfie.alt.friends", "friend" to item.name),
                                Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
                val item = items[pager.currentPage]
                val left = (Time.ms(item.selfie.expiresAt) ?: nowMs) - nowMs
                val gone = if (left >=
                    HOUR
                ) {
                    tn("selfie.goneIn", ((left + HOUR - 1) / HOUR).toInt())
                } else {
                    tn("selfie.goneInMinutes", maxOf(1, ((left + 59_999) / 60_000).toInt()))
                }
                Column(
                    Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        (if (item.mine) t("selfie.you") else item.name) + " · " + t("selfie.taken", "time" to Time.time(item.selfie.createdAt)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.onLightbox,
                    )
                    Text(gone, style = MaterialTheme.typography.bodySmall, color = c.onLightboxQuiet)
                    if (items.size > 1) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items.indices.forEach { i ->
                                Box(
                                    Modifier.size(7.dp).clip(CircleShape).background(
                                        if (i ==
                                            pager.currentPage
                                        ) {
                                            Color.White
                                        } else {
                                            Color.White.copy(alpha = 0.3f)
                                        },
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
