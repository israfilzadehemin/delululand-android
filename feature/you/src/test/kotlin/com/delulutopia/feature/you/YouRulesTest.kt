package com.delulutopia.feature.you

import com.delulutopia.core.network.model.NotifyEvent
import com.delulutopia.core.network.model.NotifySection
import com.delulutopia.core.network.model.NotifySettings
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Profile Q2/Q3 rules, the universe rules and the optimistic notification patch (web rules.ts, patch.ts). */
class YouRulesTest {
    @Test fun usernameRule() {
        assertThat(ProfileForm("Emin", timezone = "Asia/Baku").problems()).isEmpty()
        assertThat(ProfileForm("Em", timezone = "Asia/Baku").problems()).containsKey("username")
        assertThat(ProfileForm("Emin Israfil", timezone = "Asia/Baku").problems()).containsKey("username")
        assertThat(ProfileForm("Əli2", timezone = "Asia/Baku").problems()).isEmpty()
    }

    @Test fun nameRule() {
        assertThat(ProfileForm("Emin", "Mary-Jane", "O'Neil").problems()).isEmpty()
        assertThat(ProfileForm("Emin", "R2D2").problems()).containsEntry("firstName", "Letters, spaces, - and ’ only")
        assertThat(ProfileForm("Emin", "a".repeat(41)).problems()).containsEntry("firstName", "Up to 40 characters")
    }

    @Test fun initials() {
        assertThat(ProfileForm("emin", "Emin", "Israfilzade").initials()).isEqualTo("EI")
        assertThat(ProfileForm("leyla").initials()).isEqualTo("L")
    }

    @Test fun universeRules() {
        assertThat(universeNameError("Moonbase")).isNull()
        assertThat(universeNameError("M")).isNotNull()
        assertThat(welcomeError("  ")).isEqualTo("Write a few words")
        assertThat(welcomeError("x".repeat(1501))).isEqualTo("Up to 1,500 characters")
    }

    @Test fun patch() {
        val s = NotifySettings(false, listOf(NotifySection("letters", true, listOf(NotifyEvent("letter.arrived", true), NotifyEvent("letter.read", true)))))
        val p = applyPatch(s, null, mapOf("letter.read" to false, "section.letters" to false))
        assertThat(p.sections[0].enabled).isFalse()
        assertThat(p.sections[0].events.map { it.enabled }).containsExactly(true, false).inOrder()
        assertThat(sectionCount(1, 2)).isEqualTo("1 of 2 on")
        assertThat(sectionCount(0, 2)).isEqualTo("None on")
        assertThat(sectionCount(2, 2)).isNull()
        assertThat(applyPatch(s, true, emptyMap()).paused).isTrue()
    }
}
