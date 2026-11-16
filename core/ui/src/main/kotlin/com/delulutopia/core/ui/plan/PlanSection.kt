package com.delulutopia.core.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.delulutopia.core.data.Time
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.i18n.I18n
import com.delulutopia.core.i18n.t
import com.delulutopia.core.network.model.Plan
import com.delulutopia.core.network.model.PlanLimits

/** YU-1, PL-2: "Premium" (seal fill, star) or "Free" (outlined); 11 sp, 800, caps, .08 em. */
@Composable
fun TierChip(tier: String, large: Boolean = false) {
    val premium = tier == "premium"
    val shape = RoundedCornerShape(50)
    val height: Dp = if (large) 26.dp else 22.dp
    val base = Modifier.height(height)
    Row(
        if (premium) {
            base.background(MaterialTheme.colorScheme.primary, shape).padding(start = 8.dp, end = 10.dp)
        } else {
            base.border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, shape).padding(horizontal = 10.dp)
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        val ink = if (premium) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        if (premium) Icon(WebIcons.Star, null, Modifier.size(12.dp), tint = ink)
        Text(
            (if (premium) t("plan.premium") else t("plan.free")).uppercase(I18n.lang.locale),
            style = TextStyle(fontSize = 11.sp, lineHeight = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.08.em),
            color = ink,
        )
    }
}

/**
 * Settings › Universe › Plan (doc "Delululand — Subscription Tiers" PL-1–PL-5; web
 * src/plan/PlanSection.tsx): tier, allowance and what this citizen has left today, read-only for
 * both citizens, no button (ST14). Without [plan] (Home not loaded) the Today column is left out.
 */
@Composable
fun PlanSection(tier: String, limits: PlanLimits, plan: Plan?) {
    val left = plan?.left
    Column(
        Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                t("plan.title").uppercase(I18n.lang.locale),
                Modifier.weight(1f),
                style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.06.em),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TierChip(tier, large = true)
        }
        Text(t("plan.both"), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp))
        val rows = listOf(
            Triple(t("plan.emails"), t("plan.perDay", "n" to limits.emailsDaily), left?.emails),
            Triple(t("plan.photos"), t("plan.perMessage", "n" to limits.photosPerMessage, "daily" to limits.photosDaily), left?.photos),
            Triple(t("plan.videos"), t("plan.videoEach", "seconds" to limits.videoSeconds, "daily" to limits.videosDaily), left?.videos),
            Triple(
                t("plan.boardDoodles"),
                limits.boardDoodlesDaily?.let { t("plan.perDay", "n" to it) } ?: t("plan.unlimited"),
                if (limits.boardDoodlesDaily == null) -1 else left?.boardDoodles,
            ),
        )
        rows.forEachIndexed { i, (label, allow, today) ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(allow, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (left != null && today != null) {
                    val none = today == 0
                    Text(
                        when {
                            today < 0 -> "—"
                            none -> t("plan.noneLeft")
                            else -> t("plan.left", "n" to today)
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (none) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        if (plan != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(WebIcons.ClockSmall, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(t("plan.renews", "time" to Time.time(plan.renewsAt)), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
