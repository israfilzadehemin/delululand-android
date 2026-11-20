package com.delulutopia.core.ui.media

import com.delulutopia.core.i18n.I18n
import com.delulutopia.core.i18n.Lang
import com.delulutopia.core.i18n.tn
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test

/** Doc "Delululand — Attachment Retention" AR6, AR14 (web src/media/Expired.test.tsx). */
class ExpiredTest {
    @After
    fun reset() {
        I18n.resetForTests()
        Retention.letterDays = null
        Retention.otherDays = null
    }

    @Test
    fun placeholdersNameTheirKind() {
        I18n.resetForTests(Lang.EN)
        assertThat(expiredLabel("photo")).isEqualTo("Photo expired")
        assertThat(expiredLabel("video")).isEqualTo("Video expired")
        assertThat(expiredLabel("doodle")).isEqualTo("Doodle expired")
    }

    @Test
    fun noticeCountsDaysInEachLanguage() {
        I18n.resetForTests(Lang.EN)
        assertThat(tn("retention.kept", 90)).isEqualTo("Attachments are kept for 90 days")
        assertThat(tn("retention.kept", 1)).isEqualTo("Attachments are kept for 1 day")
        I18n.resetForTests(Lang.RU)
        assertThat(tn("retention.kept", 30)).isEqualTo("Вложения хранятся 30 дней")
        assertThat(tn("retention.kept", 2)).isEqualTo("Вложения хранятся 2 дня")
    }

    @Test
    fun scopesReadTheirOwnDays() {
        assertThat(Retention.days(RetentionScope.Letter)).isNull()
        Retention.letterDays = 90
        Retention.otherDays = 30
        assertThat(Retention.days(RetentionScope.Letter)).isEqualTo(90)
        assertThat(Retention.days(RetentionScope.Other)).isEqualTo(30)
    }
}
