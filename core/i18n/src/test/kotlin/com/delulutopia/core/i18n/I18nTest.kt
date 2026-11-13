package com.delulutopia.core.i18n

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
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
        assertThat(Catalog.tr.keys).containsExactlyElementsIn(Catalog.en.keys)
        // Russian adds the plural forms English has no use for (L17): `_few`, `_many` of a key with `_other`.
        assertThat(Catalog.ru.keys).containsAtLeastElementsIn(Catalog.en.keys)
        val extra = Catalog.ru.keys - Catalog.en.keys
        assertThat(extra).isNotEmpty()
        extra.forEach { assertThat(Catalog.en).containsKey(it.replace(Regex("_(few|many)$"), "_other")) }
    }

    /** Glossary Tone rules: Azerbaijani and Turkish never put a suffix on a placeholder (vowel harmony can't be known). */
    @Test fun noPlaceholderCarriesASuffix() {
        val suffixed = Regex("""\{[A-Za-z]+}['’]?\p{L}""")
        for ((name, catalog) in listOf("az" to Catalog.az, "tr" to Catalog.tr)) {
            catalog.forEach { (key, value) -> assertWithMessage("$name $key: $value").that(suffixed.containsMatchIn(value)).isFalse() }
        }
    }

    /** Glossary Tone rules (ru): a name is never declined (after a preposition) nor the subject of a past tense (gender is unknown). */
    @Test fun russianNeverDeclinesANameOrBuildsAPastTenseOnIt() {
        val person = """\{(?:name|friend|nickname|author|authorName|username|who|sender|recipient)}"""
        val afterPreposition = Regex("""(?:^|\s)(?:от|у|к|ко|для|с|со|о|об|в|во|на|по|без|из|до|про|при|за|через)\s$person""", RegexOption.IGNORE_CASE)
        val pastTense = Regex("""$person\s+[а-яё]+(?:л|ла|ло|ли)(?=[\s.,!?:]|$)""")
        Catalog.ru.forEach { (key, value) ->
            assertWithMessage("$key: $value").that(afterPreposition.containsMatchIn(value)).isFalse()
            assertWithMessage("$key: $value").that(pastTense.containsMatchIn(value)).isFalse()
        }
    }

    @Test fun aSwitchAppliesAtOnce() {
        I18n.set(Lang.AZ)
        assertThat(t("tabs.home")).isEqualTo("Əsas səhifə")
        assertThat(I18n.changes.value).isEqualTo(Lang.AZ)
        I18n.set(Lang.FA)
        assertThat(t("tabs.home")).isEqualTo("خانه")
        I18n.set(Lang.TR)
        assertThat(t("tabs.home")).isEqualTo("Ana sayfa")
        I18n.set(Lang.RU)
        assertThat(t("tabs.home")).isEqualTo("Главная")
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
        I18n.set(Lang.TR)
        assertThat(tn("emails.count", 1)).isEqualTo("1 e-posta")
        assertThat(tn("emails.count", 4)).isEqualTo("4 e-posta")
        I18n.set(Lang.RU)
        assertThat(tn("discussions.comments", 1)).isEqualTo("1 комментарий")
        assertThat(tn("discussions.comments", 3)).isEqualTo("3 комментария")
        assertThat(tn("discussions.comments", 5)).isEqualTo("5 комментариев")
        assertThat(tn("discussions.comments", 11)).isEqualTo("11 комментариев")
        assertThat(tn("discussions.comments", 21)).isEqualTo("21 комментарий")
        assertThat(tn("emails.creditsLeft", 37, "time" to "04:00")).isEqualTo("Сегодня можно отправить ещё 37 имейлов · обновление в 04:00")
        assertThat(tn("emails.creditsLeft", 2, "time" to "04:00")).isEqualTo("Сегодня можно отправить ещё 2 имейла · обновление в 04:00")
    }

    @Test fun digitsCountersAndNumbers() {
        assertThat(localDigits("12:05", Lang.FA)).isEqualTo("۱۲:۰۵")
        assertThat(localDigits("12:05", Lang.AZ)).isEqualTo("12:05")
        assertThat(counter(10, 12, Lang.FA)).isEqualTo("۱۰/۱۲")
        assertThat(formatNumber(1500, Lang.EN)).isEqualTo("1,500")
        assertThat(formatNumber(1500, Lang.FA)).isEqualTo("۱٬۵۰۰")
        assertThat(formatNumber(1500, Lang.TR)).isEqualTo("1.500")
        assertThat(formatNumber(1500, Lang.RU)).isEqualTo("1 500")
    }

    @Test fun beforeSignInThePhonesLanguageWhenItIsOneOfOurs() {
        assertThat(Lang.device(Locale.forLanguageTag("az-AZ"))).isEqualTo(Lang.AZ)
        assertThat(Lang.device(Locale.forLanguageTag("fa-IR"))).isEqualTo(Lang.FA)
        assertThat(Lang.device(Locale.forLanguageTag("tr-TR"))).isEqualTo(Lang.TR)
        assertThat(Lang.device(Locale.forLanguageTag("ru-RU"))).isEqualTo(Lang.RU)
        assertThat(Lang.device(Locale.forLanguageTag("pl-PL"))).isEqualTo(Lang.EN)
    }

    @Test fun englishAndAzerbaijaniDaysOnAPlainJvm() {
        val d = ZonedDateTime.of(2026, 11, 10, 21, 5, 0, 0, ZoneId.of("Europe/Warsaw"))
        assertThat(Dates.day(d, year = false, lang = Lang.EN)).isEqualTo("10 Nov")
        assertThat(Dates.day(d, year = true, lang = Lang.AZ)).isEqualTo("10 noyabr 2026")
        assertThat(Dates.time(d, Lang.FA)).isEqualTo("۲۱:۰۵")
    }

    @Test fun turkishAndRussianDaysOnAPlainJvm() {
        val d = ZonedDateTime.of(2026, 11, 13, 9, 5, 0, 0, ZoneId.of("Europe/Warsaw"))
        assertThat(Dates.day(d, year = false, lang = Lang.TR)).isEqualTo("13 Kasım")
        assertThat(Dates.day(d, year = true, lang = Lang.TR)).isEqualTo("13 Kasım 2026")
        assertThat(Dates.day(d, year = false, lang = Lang.RU)).isEqualTo("13 ноября")
        assertThat(Dates.day(d, year = true, lang = Lang.RU)).isEqualTo("13 ноября 2026")
        assertThat(Dates.time(d, Lang.RU)).isEqualTo("09:05")
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
