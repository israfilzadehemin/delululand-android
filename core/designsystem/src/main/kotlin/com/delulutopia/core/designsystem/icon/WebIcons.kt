package com.delulutopia.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * The web's icon set, generated from delululand-web src/components/icons.tsx (24-unit grid, round
 * caps and joins). Tint with Icon(tint = …). Regenerate rather than edit by hand.
 */
@Suppress("ktlint:standard:max-line-length")
object WebIcons {
    val Back: ImageVector by lazy {
        icon(
            "IconBack",
            24f,
            24f,
            E("M15 5l-7 7 7 7", fill = false, stroke = true, width = 1.75f),
        )
    }

    val Forward: ImageVector by lazy {
        icon(
            "IconForward",
            24f,
            24f,
            E("M9 5l7 7-7 7", fill = false, stroke = true, width = 1.75f),
        )
    }

    val Close: ImageVector by lazy {
        icon(
            "IconClose",
            24f,
            24f,
            E("M6 6l12 12M18 6L6 18", fill = false, stroke = true, width = 2.5f),
        )
    }

    val Photo: ImageVector by lazy {
        icon(
            "IconPhoto",
            24f,
            24f,
            E(
                "M5.0 5.0h14.0a2.0 2.0 0 0 1 2.0 2.0v10.0a2.0 2.0 0 0 1 -2.0 2.0h-14.0a2.0 2.0 0 0 1 -2.0 -2.0v-10.0a2.0 2.0 0 0 1 2.0 -2.0z",
                fill = false,
                stroke = true,
                width = 1.75f,
            ),
            E("M7.0 10.0a2.0 2.0 0 1 0 4.0 0a2.0 2.0 0 1 0 -4.0 0z", fill = false, stroke = true, width = 1.75f),
            E("M21 16l-5-5-8 8", fill = false, stroke = true, width = 1.75f),
        )
    }

    val Mic: ImageVector by lazy {
        icon(
            "IconMic",
            24f,
            24f,
            E(
                "M12.0 3.0h0.0a3.0 3.0 0 0 1 3.0 3.0v5.0a3.0 3.0 0 0 1 -3.0 3.0h-0.0a3.0 3.0 0 0 1 -3.0 -3.0v-5.0a3.0 3.0 0 0 1 3.0 -3.0z",
                fill = false,
                stroke = true,
                width = 1.75f,
            ),
            E("M5 11a7 7 0 0 0 14 0M12 18v3", fill = false, stroke = true, width = 1.75f),
        )
    }

    val AudioFile: ImageVector by lazy {
        icon(
            "IconAudioFile",
            24f,
            24f,
            E("M9 18V6l10-2v12", fill = false, stroke = true, width = 1.75f),
            E("M4.0 18.0a2.5 2.5 0 1 0 5.0 0a2.5 2.5 0 1 0 -5.0 0z", fill = false, stroke = true, width = 1.75f),
            E("M14.0 16.0a2.5 2.5 0 1 0 5.0 0a2.5 2.5 0 1 0 -5.0 0z", fill = false, stroke = true, width = 1.75f),
        )
    }

    val Play: ImageVector by lazy {
        icon(
            "IconPlay",
            24f,
            24f,
            E("M7 4l13 8-13 8z", fill = true, stroke = false, width = 1.75f),
        )
    }

    val Pause: ImageVector by lazy {
        icon(
            "IconPause",
            24f,
            24f,
            E("M6 4h4v16H6zM14 4h4v16h-4z", fill = true, stroke = false, width = 1.75f),
        )
    }

    val List: ImageVector by lazy {
        icon(
            "IconList",
            24f,
            24f,
            E("M9 6h12M9 12h12M9 18h12", fill = false, stroke = true, width = 1.75f),
            E("M3.3 6.0a1.2 1.2 0 1 0 2.4 0a1.2 1.2 0 1 0 -2.4 0z", fill = true, stroke = false, width = 1.75f),
            E("M3.3 12.0a1.2 1.2 0 1 0 2.4 0a1.2 1.2 0 1 0 -2.4 0z", fill = true, stroke = false, width = 1.75f),
            E("M3.3 18.0a1.2 1.2 0 1 0 2.4 0a1.2 1.2 0 1 0 -2.4 0z", fill = true, stroke = false, width = 1.75f),
        )
    }

    val Emoji: ImageVector by lazy {
        icon(
            "IconEmoji",
            24f,
            24f,
            E("M3.0 12.0a9.0 9.0 0 1 0 18.0 0a9.0 9.0 0 1 0 -18.0 0z", fill = false, stroke = true, width = 1.75f),
            E("M8.5 14.5a4.5 4.5 0 0 0 7 0", fill = false, stroke = true, width = 1.75f),
            E("M8.0 10.0a1.0 1.0 0 1 0 2.0 0a1.0 1.0 0 1 0 -2.0 0z", fill = true, stroke = false, width = 1.75f),
            E("M14.0 10.0a1.0 1.0 0 1 0 2.0 0a1.0 1.0 0 1 0 -2.0 0z", fill = true, stroke = false, width = 1.75f),
        )
    }

    val Home: ImageVector by lazy {
        icon(
            "IconHome",
            24f,
            24f,
            E("M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z", fill = false, stroke = true, width = 1.75f),
        )
    }

    val Write: ImageVector by lazy {
        icon(
            "IconWrite",
            24f,
            24f,
            E("M4 20l4-1 11-11-3-3L5 16z", fill = false, stroke = true, width = 1.75f),
            E("M13 7l3 3", fill = false, stroke = true, width = 1.75f),
        )
    }

    val Pencil: ImageVector by lazy {
        icon(
            "IconPencil",
            24f,
            24f,
            E("M4 20l4-1 11-11-3-3L5 16z", fill = false, stroke = true, width = 2.0f),
            E("M13 7l3 3", fill = false, stroke = true, width = 2.0f),
        )
    }

    val Letters: ImageVector by lazy {
        icon(
            "IconLetters",
            24f,
            24f,
            E(
                "M5.0 6.0h14.0a2.0 2.0 0 0 1 2.0 2.0v9.0a2.0 2.0 0 0 1 -2.0 2.0h-14.0a2.0 2.0 0 0 1 -2.0 -2.0v-9.0a2.0 2.0 0 0 1 2.0 -2.0z",
                fill = false,
                stroke = true,
                width = 1.75f,
            ),
            E("M3 9l9 6 9-6", fill = false, stroke = true, width = 1.75f),
        )
    }

    val DiscussionNew: ImageVector by lazy {
        icon(
            "IconDiscussionNew",
            24f,
            24f,
            E("M5 4h14v10l-5 5H5z", fill = false, stroke = true, width = 1.75f),
            E("M14 19v-5h5", fill = false, stroke = true, width = 1.75f),
            E("M12 7v6M9 10h6", fill = false, stroke = true, width = 1.75f),
        )
    }

    val EmailNew: ImageVector by lazy {
        icon(
            "IconEmailNew",
            24f,
            24f,
            E("M13 19H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v5", fill = false, stroke = true, width = 1.75f),
            E("M3.5 6.5l8.5 6 8.5-6", fill = false, stroke = true, width = 1.75f),
            E("M19 15v6M16 18h6", fill = false, stroke = true, width = 1.75f),
        )
    }

    val Discussions: ImageVector by lazy {
        icon(
            "IconDiscussions",
            24f,
            24f,
            E("M4 5h11a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2H9l-4 3v-3a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z", fill = false, stroke = true, width = 1.75f),
            E("M20 10v4a2 2 0 0 1-2 2h-1v3l-3-3", fill = false, stroke = true, width = 1.75f),
        )
    }

    val Emails: ImageVector by lazy {
        icon(
            "IconEmails",
            24f,
            24f,
            E(
                "M5.0 5.0h14.0a2.0 2.0 0 0 1 2.0 2.0v10.0a2.0 2.0 0 0 1 -2.0 2.0h-14.0a2.0 2.0 0 0 1 -2.0 -2.0v-10.0a2.0 2.0 0 0 1 2.0 -2.0z",
                fill = false,
                stroke = true,
                width = 1.75f,
            ),
            E("M3.5 6.5l8.5 6 8.5-6", fill = false, stroke = true, width = 1.75f),
        )
    }

    val Video: ImageVector by lazy {
        icon(
            "IconVideo",
            24f,
            24f,
            E(
                "M5.0 6.0h9.0a2.0 2.0 0 0 1 2.0 2.0v8.0a2.0 2.0 0 0 1 -2.0 2.0h-9.0a2.0 2.0 0 0 1 -2.0 -2.0v-8.0a2.0 2.0 0 0 1 2.0 -2.0z",
                fill = false,
                stroke = true,
                width = 1.75f,
            ),
            E("M16 10l5-3v10l-5-3z", fill = false, stroke = true, width = 1.75f),
        )
    }

    val VideoFile: ImageVector by lazy {
        icon(
            "IconVideoFile",
            24f,
            24f,
            E("M6 3h8l4 4v14H6z", fill = false, stroke = true, width = 1.75f),
            E("M10 11v6l5-3z", fill = false, stroke = true, width = 1.75f),
        )
    }

    val Plane: ImageVector by lazy {
        icon(
            "IconPlane",
            24f,
            24f,
            E("M3 11l18-8-7 18-3-7z", fill = true, stroke = true, width = 1.5f),
        )
    }

    val Envelope: ImageVector by lazy {
        icon(
            "IconEnvelope",
            56f,
            44f,
            E(
                "M5.0 9.0h46.0a4.0 4.0 0 0 1 4.0 4.0v26.0a4.0 4.0 0 0 1 -4.0 4.0h-46.0a4.0 4.0 0 0 1 -4.0 -4.0v-26.0a4.0 4.0 0 0 1 4.0 -4.0z",
                fill = true,
                stroke = true,
                width = 1.5f,
            ),
            E("M1 13l27 18L55 13", fill = false, stroke = true, width = 1.5f),
            E("M30 10l18-8-7 18-3-7z", fill = true, stroke = true, width = 1.5f),
        )
    }

    val Check: ImageVector by lazy {
        icon(
            "IconCheck",
            24f,
            24f,
            E("M4 12l5 5L20 6", fill = false, stroke = true, width = 2.0f),
        )
    }

    val DoubleCheck: ImageVector by lazy {
        icon(
            "IconDoubleCheck",
            24f,
            24f,
            E("M2 12l4 4 7-7M10 16l2 2 10-10", fill = false, stroke = true, width = 2.0f),
        )
    }

    val Reply: ImageVector by lazy {
        icon(
            "IconReply",
            24f,
            24f,
            E("M9 14l-5-5 5-5", fill = false, stroke = true, width = 1.75f),
            E("M4 9h9a7 7 0 0 1 7 7v4", fill = false, stroke = true, width = 1.75f),
        )
    }

    val Spinner: ImageVector by lazy {
        icon(
            "IconSpinner",
            24f,
            24f,
            E("M12 3a9 9 0 1 0 9 9", fill = false, stroke = true, width = 2.0f),
        )
    }

    val Offline: ImageVector by lazy {
        icon(
            "IconOffline",
            24f,
            24f,
            E("M3 9a14 14 0 0 1 18 0M6.5 12.5a9 9 0 0 1 11 0M10 16a4 4 0 0 1 4 0", fill = false, stroke = true, width = 1.75f),
            E("M12 19.5v.01M4 4l16 16", fill = false, stroke = true, width = 1.75f),
        )
    }

    val PlaneSmall: ImageVector by lazy {
        icon(
            "IconPlaneSmall",
            24f,
            24f,
            E("M3 11l18-8-7 18-3-7z", fill = false, stroke = true, width = 2.0f),
        )
    }

    val Bell: ImageVector by lazy {
        icon(
            "IconBell",
            24f,
            24f,
            E("M6 16V11a6 6 0 0 1 12 0v5l2 2H4z", fill = false, stroke = true, width = 1.75f),
            E("M10 21a2 2 0 0 0 4 0", fill = false, stroke = true, width = 1.75f),
        )
    }

    val Share: ImageVector by lazy {
        icon(
            "IconShare",
            24f,
            24f,
            E("M12 3v12M8 7l4-4 4 4", fill = false, stroke = true, width = 1.75f),
            E("M5 11v9h14v-9", fill = false, stroke = true, width = 1.75f),
        )
    }

    val AddSquare: ImageVector by lazy {
        icon(
            "IconAddSquare",
            24f,
            24f,
            E(
                "M7.0 4.0h10.0a3.0 3.0 0 0 1 3.0 3.0v10.0a3.0 3.0 0 0 1 -3.0 3.0h-10.0a3.0 3.0 0 0 1 -3.0 -3.0v-10.0a3.0 3.0 0 0 1 3.0 -3.0z",
                fill = false,
                stroke = true,
                width = 1.75f,
            ),
            E("M12 8v8M8 12h8", fill = false, stroke = true, width = 1.75f),
        )
    }

    val Grid: ImageVector by lazy {
        icon(
            "IconGrid",
            24f,
            24f,
            E(
                "M5.5 4.0h4.0a1.5 1.5 0 0 1 1.5 1.5v4.0a1.5 1.5 0 0 1 -1.5 1.5h-4.0a1.5 1.5 0 0 1 -1.5 -1.5v-4.0a1.5 1.5 0 0 1 1.5 -1.5z",
                fill = false,
                stroke = true,
                width = 1.75f,
            ),
            E(
                "M14.5 4.0h4.0a1.5 1.5 0 0 1 1.5 1.5v4.0a1.5 1.5 0 0 1 -1.5 1.5h-4.0a1.5 1.5 0 0 1 -1.5 -1.5v-4.0a1.5 1.5 0 0 1 1.5 -1.5z",
                fill = false,
                stroke = true,
                width = 1.75f,
            ),
            E(
                "M5.5 13.0h4.0a1.5 1.5 0 0 1 1.5 1.5v4.0a1.5 1.5 0 0 1 -1.5 1.5h-4.0a1.5 1.5 0 0 1 -1.5 -1.5v-4.0a1.5 1.5 0 0 1 1.5 -1.5z",
                fill = false,
                stroke = true,
                width = 1.75f,
            ),
            E(
                "M14.5 13.0h4.0a1.5 1.5 0 0 1 1.5 1.5v4.0a1.5 1.5 0 0 1 -1.5 1.5h-4.0a1.5 1.5 0 0 1 -1.5 -1.5v-4.0a1.5 1.5 0 0 1 1.5 -1.5z",
                fill = false,
                stroke = true,
                width = 1.75f,
            ),
        )
    }

    val PhotoSmall: ImageVector by lazy {
        icon(
            "IconPhotoSmall",
            24f,
            24f,
            E(
                "M5.0 5.0h14.0a2.0 2.0 0 0 1 2.0 2.0v10.0a2.0 2.0 0 0 1 -2.0 2.0h-14.0a2.0 2.0 0 0 1 -2.0 -2.0v-10.0a2.0 2.0 0 0 1 2.0 -2.0z",
                fill = false,
                stroke = true,
                width = 2.0f,
            ),
            E("M7.0 10.0a2.0 2.0 0 1 0 4.0 0a2.0 2.0 0 1 0 -4.0 0z", fill = false, stroke = true, width = 2.0f),
            E("M21 16l-5-5-8 8", fill = false, stroke = true, width = 2.0f),
        )
    }

    val MicSmall: ImageVector by lazy {
        icon(
            "IconMicSmall",
            24f,
            24f,
            E(
                "M12.0 3.0h0.0a3.0 3.0 0 0 1 3.0 3.0v5.0a3.0 3.0 0 0 1 -3.0 3.0h-0.0a3.0 3.0 0 0 1 -3.0 -3.0v-5.0a3.0 3.0 0 0 1 3.0 -3.0z",
                fill = false,
                stroke = true,
                width = 2.0f,
            ),
            E("M5 11a7 7 0 0 0 14 0M12 18v3", fill = false, stroke = true, width = 2.0f),
        )
    }

    val Moon: ImageVector by lazy {
        icon(
            "IconMoon",
            24f,
            24f,
            E("M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z", fill = false, stroke = true, width = 1.75f),
        )
    }

    val Sun: ImageVector by lazy {
        icon(
            "IconSun",
            24f,
            24f,
            E("M8.0 12.0a4.0 4.0 0 1 0 8.0 0a4.0 4.0 0 1 0 -8.0 0z", fill = false, stroke = true, width = 1.75f),
            E("M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4", fill = false, stroke = true, width = 1.75f),
        )
    }

    val Doodle: ImageVector by lazy {
        icon(
            "IconDoodle",
            24f,
            24f,
            E("M3 18c2.5-4 4.5-4.5 5.5-2s2.5 2.5 4-1", fill = false, stroke = true, width = 1.75f),
            E("M14.5 13.5l6-6a1.77 1.77 0 0 0-2.5-2.5l-6 6-1 3.5z", fill = false, stroke = true, width = 1.75f),
        )
    }

    val DoodleSmall: ImageVector by lazy {
        icon(
            "IconDoodleSmall",
            24f,
            24f,
            E("M3 18c2.5-4 4.5-4.5 5.5-2s2.5 2.5 4-1", fill = false, stroke = true, width = 2.0f),
            E("M14.5 13.5l6-6a1.77 1.77 0 0 0-2.5-2.5l-6 6-1 3.5z", fill = false, stroke = true, width = 2.0f),
        )
    }

    val Undo: ImageVector by lazy {
        icon(
            "IconUndo",
            24f,
            24f,
            E("M9 14L4 9l5-5", fill = false, stroke = true, width = 1.75f),
            E("M4 9h10.5a5.5 5.5 0 0 1 0 11H11", fill = false, stroke = true, width = 1.75f),
        )
    }

    val Redo: ImageVector by lazy {
        icon(
            "IconRedo",
            24f,
            24f,
            E("M15 14l5-5-5-5", fill = false, stroke = true, width = 1.75f),
            E("M20 9H9.5a5.5 5.5 0 0 0 0 11H13", fill = false, stroke = true, width = 1.75f),
        )
    }

    val Eraser: ImageVector by lazy {
        icon(
            "IconEraser",
            24f,
            24f,
            E("M7 21h13", fill = false, stroke = true, width = 1.75f),
            E("M5.5 15.5l9.6-9.6a2 2 0 0 1 2.8 0l2.2 2.2a2 2 0 0 1 0 2.8L12 19H9z", fill = false, stroke = true, width = 1.75f),
            E("M9.5 11.5l5 5", fill = false, stroke = true, width = 1.75f),
        )
    }

    val Trash: ImageVector by lazy {
        icon(
            "IconTrash",
            24f,
            24f,
            E("M4 7h16M10 11v6M14 11v6", fill = false, stroke = true, width = 1.75f),
            E("M6 7l1 12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-12M9 7V4h6v3", fill = false, stroke = true, width = 1.75f),
        )
    }

    val Archive: ImageVector by lazy {
        icon(
            "IconArchive",
            24f,
            24f,
            E(
                "M4.0 4.0h16.0a1.0 1.0 0 0 1 1.0 1.0v3.0a1.0 1.0 0 0 1 -1.0 1.0h-16.0a1.0 1.0 0 0 1 -1.0 -1.0v-3.0a1.0 1.0 0 0 1 1.0 -1.0z",
                fill = false,
                stroke = true,
                width = 1.75f,
            ),
            E("M5 9v9a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V9M10 13h4", fill = false, stroke = true, width = 1.75f),
        )
    }

    val ChevronDown: ImageVector by lazy {
        icon(
            "IconChevronDown",
            24f,
            24f,
            E("M6 9l6 6 6-6", fill = false, stroke = true, width = 2.0f),
        )
    }

    val ChevronUp: ImageVector by lazy {
        icon(
            "IconChevronUp",
            24f,
            24f,
            E("M6 15l6-6 6 6", fill = false, stroke = true, width = 2.0f),
        )
    }

    val Key: ImageVector by lazy {
        icon(
            "IconKey",
            24f,
            24f,
            E("M4.0 15.0a4.0 4.0 0 1 0 8.0 0a4.0 4.0 0 1 0 -8.0 0z", fill = false, stroke = true, width = 1.75f),
            E("M11 12l9-9M17 6l3 3M14 9l2 2", fill = false, stroke = true, width = 1.75f),
        )
    }

    val Download: ImageVector by lazy {
        icon(
            "IconDownload",
            24f,
            24f,
            E("M12 4v11M7 10l5 5 5-5M5 20h14", fill = false, stroke = true, width = 1.75f),
        )
    }

    val Info: ImageVector by lazy {
        icon(
            "IconInfo",
            24f,
            24f,
            E("M3.0 12.0a9.0 9.0 0 1 0 18.0 0a9.0 9.0 0 1 0 -18.0 0z", fill = false, stroke = true, width = 1.75f),
            E("M12 11v6M12 7.5v.5", fill = false, stroke = true, width = 1.75f),
        )
    }

    val Logout: ImageVector by lazy {
        icon(
            "IconLogout",
            24f,
            24f,
            E("M15 4h4a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1h-4M10 16l-4-4 4-4M6 12h10", fill = false, stroke = true, width = 1.75f),
        )
    }

    val Palette: ImageVector by lazy {
        icon(
            "IconPalette",
            24f,
            24f,
            E("M3.0 12.0a9.0 9.0 0 1 0 18.0 0a9.0 9.0 0 1 0 -18.0 0z", fill = false, stroke = true, width = 1.75f),
            E("M6.8 10.0a1.2 1.2 0 1 0 2.4 0a1.2 1.2 0 1 0 -2.4 0z", fill = false, stroke = true, width = 1.75f),
            E("M10.8 7.5a1.2 1.2 0 1 0 2.4 0a1.2 1.2 0 1 0 -2.4 0z", fill = false, stroke = true, width = 1.75f),
            E("M14.8 10.0a1.2 1.2 0 1 0 2.4 0a1.2 1.2 0 1 0 -2.4 0z", fill = false, stroke = true, width = 1.75f),
            E("M12 21a3 3 0 0 1 0-6h2", fill = false, stroke = true, width = 1.75f),
        )
    }

    val Camera: ImageVector by lazy {
        icon(
            "IconCamera",
            24f,
            24f,
            E("M4 8h3l2-3h6l2 3h3v11H4z", fill = false, stroke = true, width = 1.75f),
            E("M8.5 13.0a3.5 3.5 0 1 0 7.0 0a3.5 3.5 0 1 0 -7.0 0z", fill = false, stroke = true, width = 1.75f),
        )
    }

    val ChevronRight: ImageVector by lazy {
        icon(
            "IconChevronRight",
            24f,
            24f,
            E("M9 6l6 6-6 6", fill = false, stroke = true, width = 1.75f),
        )
    }

    private class E(val d: String, val fill: Boolean, val stroke: Boolean, val width: Float)

    private fun icon(name: String, w: Float, h: Float, vararg els: E): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp * (h / w), viewportWidth = w, viewportHeight = h)
            .apply {
                els.forEach { e ->
                    addPath(
                        pathData = PathParser().parsePathString(e.d).toNodes(),
                        fill = if (e.fill) SolidColor(Color.Black) else null,
                        stroke = if (e.stroke) SolidColor(Color.Black) else null,
                        strokeLineWidth = e.width,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    )
                }
            }.build()
}
