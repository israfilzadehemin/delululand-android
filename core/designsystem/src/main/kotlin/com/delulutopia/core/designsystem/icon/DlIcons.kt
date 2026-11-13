package com.delulutopia.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/*
 * The canvas icons: 24 × 24 line icons, round caps and joins, stroke 1.75 (2 when active). Path data
 * is copied from the Android canvas frames (Y-Home, Y-Discussions, Y-Emails, A-Create,
 * Z-Components). Tint with Icon(tint = …); the stroke colour below is replaced by the tint.
 */
object DlIcons {
    private const val THIN = 1.75f

    /** Icons that point along the reading direction mirror in Persian (L3). Declared first: the icons below are built at init. */
    private val MIRRORED = setOf("back", "send", "chevron.right", "plane", "log.out")
    private const val BOLD = 2f

    val Home = line("home", THIN, "M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z")
    val HomeActive = line("home.active", BOLD, "M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z")

    private val lettersPaths = arrayOf(rect(3f, 6f, 18f, 13f, 2f), "M3 9l9 6 9-6")
    val Letters = line("letters", THIN, *lettersPaths)
    val LettersActive = line("letters.active", BOLD, *lettersPaths)

    private val discussionsPaths = arrayOf(
        "M4 5h11a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2H9l-4 3v-3a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z",
        "M20 10v4a2 2 0 0 1-2 2h-1v3l-3-3",
    )
    val Discussions = line("discussions", THIN, *discussionsPaths)
    val DiscussionsActive = line("discussions.active", BOLD, *discussionsPaths)

    private val emailsPaths = arrayOf(rect(3f, 5f, 18f, 14f, 2f), "M3.5 6.5l8.5 6 8.5-6")
    val Emails = line("emails", THIN, *emailsPaths)
    val EmailsActive = line("emails.active", BOLD, *emailsPaths)

    /** Letters FAB: new letter / continue your draft. */
    val Pen = line("pen", BOLD, "M4 20l4-1 11-11-3-3L5 16z", "M13 7l3 3")

    /** Discussions FAB: start a discussion. */
    val NotePlus = line("note.plus", BOLD, "M5 4h14v10l-5 5H5z", "M14 19v-5h5", "M12 7v6M9 10h6")

    /** Emails FAB: new email. */
    val EnvelopePlus = line(
        "envelope.plus",
        BOLD,
        "M13 19H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v5",
        "M3.5 6.5l8.5 6 8.5-6",
        "M19 15v6M16 18h6",
    )

    val Back = line("back", THIN, "M20 12H5", "M11 18l-6-6 6-6")
    val More = filled("more", circle(12f, 5.5f, 1.4f), circle(12f, 12f, 1.4f), circle(12f, 18.5f, 1.4f))
    val Send = line("send", BOLD, "M4 12l16-8-6 16-2.5-6.5z", "M11.5 13.5L20 4")
    val Draw = line("draw", BOLD, "M3 18c2.5-4 4.5-4.5 5.5-2s2.5 2.5 4-1", "M14.5 13.5l6-6a1.77 1.77 0 0 0-2.5-2.5l-6 6-1 3.5z")
    val Plus = line("plus", 2.25f, "M12 5v14M5 12h14")
    val Check = line("check", BOLD, "M4 12l5 5L20 6")
    val ChevronRight = line("chevron.right", THIN, "M9 6l6 6-6 6")
    val Bell = line("bell", THIN, "M6 16V11a6 6 0 0 1 12 0v5l2 2H4z", "M10 21a2 2 0 0 0 4 0")
    val Plane = line("plane", BOLD, "M3 11l18-8-7 18-3-7z")

    /** PIN pad fingerprint key (S-PIN). */
    val Fingerprint = line(
        "fingerprint",
        THIN,
        "M6.5 18.5c1-1.8 1.5-4 1.5-6.5a4 4 0 0 1 8 0c0 2.6-.4 5-1.2 7",
        "M12 12c0 3.2-.8 6-2.2 8.2",
        "M4.5 15c.3-1 .5-2 .5-3a7 7 0 0 1 12.5-4.3",
        "M19 10.5c.3 2.6.1 5.2-.6 7.6",
    )
    val Backspace = line("backspace", THIN, "M8 5h12a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H8l-6-7z", "M11 9l6 6M17 9l-6 6")
    val Search = line("search", THIN, "M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14z", "M20 20l-4-4")
    val Copy = line(
        "copy",
        THIN,
        "M9 9h10a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H9a1 1 0 0 1-1-1V10a1 1 0 0 1 1-1z",
        "M5 15H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v1",
    )
    val Share = line("share", THIN, "M12 15V3", "M7 8l5-5 5 5", "M5 13v6a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-6")
    val LogOut = line("log.out", THIN, "M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3", "M10 17l-5-5 5-5", "M5 12h11")
    val Language = line("language", THIN, "M4 5h16v11H9l-5 4z", "M9.5 13l2.5-6 2.5 6M10.4 11h3.2")
    val Palette = line("palette", THIN, "M12 3a9 9 0 1 0 0 18c1.1 0 1.6-.8 1.6-1.6 0-1.2-1-1.4-1-2.4 0-.8.7-1.4 1.6-1.4H17a4 4 0 0 0 4-4c0-4.7-4-8.6-9-8.6z")

    private fun line(name: String, stroke: Float, vararg paths: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = name in MIRRORED,
        )
            .apply {
                paths.forEach { d ->
                    addPath(
                        pathData = PathParser().parsePathString(d).toNodes(),
                        fill = null,
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = stroke,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    )
                }
            }.build()

    private fun filled(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .apply {
                paths.forEach { d ->
                    addPath(pathData = PathParser().parsePathString(d).toNodes(), fill = SolidColor(Color.Black))
                }
            }.build()

    /** SVG `<rect rx>` as path data. */
    private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float): String = "M${x + r} ${y}h${w - 2 * r}" +
        "a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 -$r ${r}h-${w - 2 * r}a$r $r 0 0 1 -$r -${r}v-${h - 2 * r}" +
        "a$r $r 0 0 1 $r -${r}z"

    /** SVG `<circle>` as path data. */
    private fun circle(cx: Float, cy: Float, r: Float): String = "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 -${2 * r} 0z"
}
