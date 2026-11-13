package com.delulutopia.core.designsystem

import com.delulutopia.core.designsystem.component.Zones
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ZonesTest {
    @Test
    fun `offsets read like the web`() {
        assertThat(Zones.offsetWords(0)).isEqualTo("UTC")
        assertThat(Zones.offsetWords(3_600_000)).isEqualTo("UTC+1")
        assertThat(Zones.offsetWords(12_600_000)).isEqualTo("UTC+3:30")
        assertThat(Zones.offsetWords(-12_600_000)).isEqualTo("UTC−3:30")
    }

    @Test
    fun `city search is accent-blind and city-first`() {
        val zones = listOf("America/Sao_Paulo", "Europe/Warsaw", "Asia/Tehran")
        assertThat(Zones.match("são", zones)).containsExactly("America/Sao_Paulo")
        assertThat(Zones.match("war", zones).first()).isEqualTo("Europe/Warsaw")
        assertThat(Zones.cityOf("America/Sao_Paulo")).isEqualTo("Sao Paulo")
    }
}
