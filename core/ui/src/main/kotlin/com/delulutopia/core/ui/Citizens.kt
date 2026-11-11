package com.delulutopia.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.network.model.Citizen

/**
 * Writer colour key (web components/whoOf.ts): the universe's first citizen (lowest id) is "a"
 * (terracotta), the other "b" (sage). Until Home has loaded, user 1 is "a".
 */
val LocalFirstCitizen = staticCompositionLocalOf<Int?> { null }

enum class Who { A, B }

@Composable
@ReadOnlyComposable
fun whoOf(userId: Int?): Who {
    val first = LocalFirstCitizen.current
    return if ((if (first == null) userId == 1 else userId == first)) Who.A else Who.B
}

@Composable
@ReadOnlyComposable
fun signatureOf(userId: Int?): Color = if (whoOf(userId) == Who.A) DlTheme.colors.userA else DlTheme.colors.userB

@Composable
@ReadOnlyComposable
fun tintOf(userId: Int?): Color = if (whoOf(userId) == Who.A) DlTheme.colors.userATint else DlTheme.colors.userBTint

@Composable
@ReadOnlyComposable
fun Who.signature(): Color = if (this == Who.A) DlTheme.colors.userA else DlTheme.colors.userB

/** P4: "Name Surname" initials, else the username's first letter. */
fun initialsOf(nickname: String, firstName: String?, lastName: String?): String {
    val first = firstName?.trim().orEmpty()
    val last = lastName?.trim().orEmpty()
    return when {
        first.isNotEmpty() && last.isNotEmpty() -> (first.take(1) + last.take(1)).uppercase()
        first.isNotEmpty() -> first.take(1).uppercase()
        else -> nickname.take(1).uppercase()
    }
}

fun Citizen.initials(): String = initialsOf(nickname, firstName, lastName)

/** A person's current mood as it looks now (CM7): "🍵 Calm", or null. */
fun Citizen.moodText(): String? = com.delulutopia.core.data.Moods.text(mood, moodEmoji, moodLabel)
