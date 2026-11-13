package com.delulutopia.core.designsystem

import com.delulutopia.core.designsystem.component.formatKey
import com.delulutopia.core.designsystem.component.initialsOf
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FormattingTest {
    @Test
    fun `keys are grouped in fours and uppercased`() {
        assertThat(formatKey("k7qmwx4p2nzd")).isEqualTo("K7QM-WX4P-2NZD")
        assertThat(formatKey("K7QM-WX4P")).isEqualTo("K7QM-WX4P")
        assertThat(formatKey("k7qm wx4p 2nzd extra")).isEqualTo("K7QM-WX4P-2NZD")
    }

    @Test
    fun `initials take the first letters of up to two words`() {
        assertThat(initialsOf("Emin Israfilzade")).isEqualTo("EI")
        assertThat(initialsOf("Leyla")).isEqualTo("L")
        assertThat(initialsOf("  əli   veli  ")).isEqualTo("ƏV")
    }
}
