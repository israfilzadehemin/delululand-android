package com.delulutopia.core.designsystem.theme

import android.content.Context
import android.graphics.Typeface as AndroidTypeface
import android.graphics.fonts.Font as SystemFont
import android.graphics.fonts.FontFamily as SystemFontFamily
import android.graphics.fonts.FontStyle as SystemFontStyle
import android.os.Build
import androidx.annotation.FontRes
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.res.ResourcesCompat

/**
 * One interface weight: Manrope for Latin, with Vazirmatn (its non-Latin cut) behind it for the Arabic
 * script, as the web stacks 'Manrope', 'Vazirmatn' (doc "Delululand — Languages" L5). Android 10+ chains
 * the two faces; older phones fall back to the system's Arabic face.
 */
@OptIn(ExperimentalTextApi::class)
internal class UiFont(
    @FontRes val primary: Int,
    @FontRes val arabic: Int,
    override val weight: FontWeight,
) : AndroidFont(FontLoadingStrategy.Blocking, UiFontLoader, FontVariation.Settings()) {
    override val style: FontStyle = FontStyle.Normal
}

@OptIn(ExperimentalTextApi::class)
private object UiFontLoader : AndroidFont.TypefaceLoader {
    override fun loadBlocking(context: Context, font: AndroidFont): AndroidTypeface? {
        val f = font as UiFont
        if (Build.VERSION.SDK_INT >= 29) {
            runCatching { return withArabic(context, f) }
        }
        return ResourcesCompat.getFont(context, f.primary)
    }

    override suspend fun awaitLoad(context: Context, font: AndroidFont): AndroidTypeface? = loadBlocking(context, font)
}

@RequiresApi(29)
private fun withArabic(context: Context, font: UiFont): AndroidTypeface {
    val weight = font.weight.weight
    fun family(@FontRes res: Int) = SystemFontFamily.Builder(SystemFont.Builder(context.resources, res).setWeight(weight).build()).build()
    return AndroidTypeface.CustomFallbackBuilder(family(font.primary))
        .addCustomFallback(family(font.arabic))
        .setStyle(SystemFontStyle(weight, SystemFontStyle.FONT_SLANT_UPRIGHT))
        .build()
}
