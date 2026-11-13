package com.delulutopia.core.i18n

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import org.junit.After
import org.junit.Test

class I18nTest {
    @After fun reset() = I18n.resetForTests()

    @Test fun englishIsTheDefault_andEveryLanguageHasEveryKey() {
        assertThat(t("tabs.home")).isEqualTo("Home")
        assertThat(Catalog.az.keys).containsExactlyElementsIn(Catalog.en.keys)
        assertThat(Catalog.fa.keys).containsExactlyElementsIn(Catalog.en.keys)
    }

    @Test fun aSwitchAppliesAtOnce() {
        I18n.set(Lang.AZ)
        assertThat(t("tabs.home")).isEqualTo("Əsas səhifə")
        assertThat(I18n.changes.value).isEqualTo(Lang.AZ)
        I18n.set(Lang.FA)
        assertThat(t("tabs.home")).isEqualTo("خانه")
    }

    @Test fun placeholdersFillOnce_andNumbersTakeTheLanguagesDigits() {
        assertThat(t("common.age.min", "n" to 5)).isEqualTo("5 min ago")
        I18n.set(Lang.FA)
        assertThat(t("common.age.min", "n" to 5)).isEqualTo("۵ دقیقه پیش")
        // A value is never re-scanned for placeholders.
        I18n.set(Lang.EN)
        assertThat(t("letters.to", "friend" to "{friend}")).isEqualTo("To {friend}")
    }

    @Test fun inPersianALatinNameIsIsolated() {
        I18n.set(Lang.FA)
        assertThat(t("letters.to", "friend" to "Leyla")).isEqualTo("به ⁨Leyla⁩")
        assertThat(t("letters.to", "friend" to "لیلا")).isEqualTo("به لیلا")
        I18n.set(Lang.AZ)
        assertThat(t("letters.to", "friend" to "Leyla")).isEqualTo("Kimə: Leyla")
    }

    @Test fun pluralsFollowEachLanguage() {
        assertThat(tn("discussions.comments", 1)).isEqualTo("1 comment")
        assertThat(tn("discussions.comments", 3)).isEqualTo("3 comments")
        I18n.set(Lang.FA)
        assertThat(tn("discussions.comments", 3)).isEqualTo("۳ نظر")
    }

    @Test fun digitsCountersAndNumbers() {
        assertThat(localDigits("12:05", Lang.FA)).isEqualTo("۱۲:۰۵")
        assertThat(localDigits("12:05", Lang.AZ)).isEqualTo("12:05")
        assertThat(counter(10, 12, Lang.FA)).isEqualTo("۱۰/۱۲")
        assertThat(formatNumber(1500, Lang.EN)).isEqualTo("1,500")
        assertThat(formatNumber(1500, Lang.FA)).isEqualTo("۱٬۵۰۰")
    }

    @Test fun beforeSignInThePhonesLanguageWhenItIsOneOfOurs() {
        assertThat(Lang.device(Locale.forLanguageTag("az-AZ"))).isEqualTo(Lang.AZ)
        assertThat(Lang.device(Locale.forLanguageTag("fa-IR"))).isEqualTo(Lang.FA)
        assertThat(Lang.device(Locale.forLanguageTag("pl-PL"))).isEqualTo(Lang.EN)
    }

    @Test fun englishAndAzerbaijaniDaysOnAPlainJvm() {
        val d = ZonedDateTime.of(2026, 11, 10, 21, 5, 0, 0, ZoneId.of("Europe/Warsaw"))
        assertThat(Dates.day(d, year = false, lang = Lang.EN)).isEqualTo("10 Nov")
        assertThat(Dates.day(d, year = true, lang = Lang.AZ)).isEqualTo("10 noyabr 2026")
        assertThat(Dates.time(d, Lang.FA)).isEqualTo("۲۱:۰۵")
    }

    /** Every t("key") / tn("base") in the app's code names a sentence the catalogue has. */
    @Test fun everyKeyInTheCodeExists() {
        val root = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "settings.gradle.kts").exists() }
        val call = Regex("""\b(t|tn|tBold)\(\s*"([A-Za-z0-9_.]+)"""")
        val missing = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && "/src/main/" in it.path && "/build/" !in it.path }
            .flatMap { f -> call.findAll(f.readText()).map { f.name to it } }
            .filter { (_, m) ->
                val key = m.groupValues[2]
                if (m.groupValues[1] == "tn") !Catalog.en.containsKey("${key}_other") else !Catalog.en.containsKey(key)
            }
            .map { (f, m) -> "$f: ${m.groupValues[2]}" }
            .toList()
        assertThat(missing).isEmpty()
    }
}
