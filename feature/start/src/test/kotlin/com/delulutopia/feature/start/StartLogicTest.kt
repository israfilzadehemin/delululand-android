package com.delulutopia.feature.start

import com.delulutopia.feature.start.PinViewModel.Problem
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StartLogicTest {
    @Test
    fun `keys group in fours`() {
        assertThat(groupKey("k7qm wx4p-2hrn")).isEqualTo("K7QM-WX4P-2HRN")
        assertThat(groupKey("k7qmwx4p2hrnzz")).isEqualTo("K7QM-WX4P-2HRN")
    }

    @Test
    fun `universe name and username rules`() {
        assertThat(Rules.nameOk("Moonbase")).isTrue()
        assertThat(Rules.nameOk("Ənənəvi")).isTrue()
        assertThat(Rules.nameOk("M")).isFalse()
        assertThat(Rules.nameOk("Moon base")).isFalse()
        assertThat(Rules.nameOk("Thirteenchars")).isFalse()
        assertThat(Rules.usernameOk("Leyla")).isTrue()
        assertThat(Rules.usernameOk("Le")).isFalse()
        assertThat(Rules.usernameTaken("leyla", listOf("Leyla"))).isTrue()
    }

    @Test
    fun `welcome template follows the name`() {
        assertThat(Rules.welcomeTemplate("Moonbase")).startsWith("Moonbase is a place")
        assertThat(Rules.paragraphs(Rules.welcomeTemplate("Moonbase"))).hasSize(3)
    }

    @Test
    fun `wizard PIN confirms before submitting`() {
        val (afterFirst, none) = WizardPin().complete("123456")
        assertThat(none).isNull()
        assertThat(afterFirst.confirming).isTrue()
        val (differ, none2) = afterFirst.complete("654321")
        assertThat(none2).isNull()
        assertThat(differ.problem).isEqualTo(Problem.Differ)
        assertThat(differ.confirming).isFalse()
        val (_, pin) = afterFirst.complete("123456")
        assertThat(pin).isEqualTo("123456")
    }
}
