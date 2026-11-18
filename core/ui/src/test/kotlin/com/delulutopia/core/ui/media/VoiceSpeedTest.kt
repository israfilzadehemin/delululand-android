package com.delulutopia.core.ui.media

import com.delulutopia.core.i18n.Lang
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Doc "Delululand — Voice Note Speed" VS3, VS9 (web src/media/speed.test.tsx). */
class VoiceSpeedTest {
    @Test
    fun cyclesFromHalfToDoubleAndStartsAtOne() {
        assertThat(VoiceSpeed.DEFAULT).isEqualTo(1f)
        assertThat(VoiceSpeed.next(0.5f)).isEqualTo(1f)
        assertThat(VoiceSpeed.next(1f)).isEqualTo(1.5f)
        assertThat(VoiceSpeed.next(1.5f)).isEqualTo(2f)
        assertThat(VoiceSpeed.next(2f)).isEqualTo(0.5f)
    }

    @Test
    fun writesTheChipInEachLanguage() {
        assertThat(VoiceSpeed.label(0.5f, Lang.EN)).isEqualTo("0.5×")
        assertThat(VoiceSpeed.label(1f, Lang.AZ)).isEqualTo("1×")
        assertThat(VoiceSpeed.label(2f, Lang.RU)).isEqualTo("2×")
        assertThat(VoiceSpeed.label(0.5f, Lang.FA)).isEqualTo("۰٫۵×")
        assertThat(VoiceSpeed.label(1.5f, Lang.FA)).isEqualTo("۱٫۵×")
    }
}
