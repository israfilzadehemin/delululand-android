package com.delulutopia.core.i18n

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The language in force (L6, L8). It is snapshot state: a screen that reads a text recomposes when the
 * language changes, so a pick applies at once. The account's language wins once signed in
 * (Home `me.language`, the live signal `language`); before that, the landing pick, then the phone's.
 * The phone's own per-app language setting is not used (L8).
 */
object I18n {
    private const val PREFS = "dl-lang"
    private const val KEY = "lang"

    private val current = mutableStateOf(Lang.EN)
    private val flow = MutableStateFlow(Lang.EN)
    private var prefs: SharedPreferences? = null

    val lang: Lang get() = current.value

    /** The language in force, for code outside composition (repositories reload server-worded texts on a change). */
    val changes: StateFlow<Lang> = flow.asStateFlow()

    /** Reads the remembered language, or the phone's. Call once from Application.onCreate. */
    fun init(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        current.value = Lang.parse(p.getString(KEY, null)) ?: Lang.device()
        flow.value = current.value
    }

    /** Applies [lang] at once and remembers it on this phone. */
    fun set(lang: Lang) {
        if (current.value != lang) current.value = lang
        flow.value = lang
        prefs?.edit()?.putString(KEY, lang.tag)?.apply()
    }

    /** Tests: back to English, nothing remembered. */
    fun resetForTests(lang: Lang = Lang.EN) {
        prefs = null
        current.value = lang
        flow.value = lang
    }
}

/** The text for [key] in the current language, with `{name}` placeholders filled from [args]. English when missing. */
fun t(key: String, vararg args: Pair<String, Any?>): String = translate(I18n.lang, key, *args)

/**
 * Plural: `<base>_<category>` by [n] and the CLDR rules of each language, else `<base>_other`; `{n}` is
 * filled too. Russian has `_one`, `_few` and `_many` ("1 письмо", "2 письма", "5 писем", L17); the others
 * say a noun once after a number, so `_other` mostly does.
 */
fun tn(base: String, n: Int, vararg args: Pair<String, Any?>): String {
    val lang = I18n.lang
    val own = "${base}_${pluralCategory(n, lang)}"
    val key = if (has(lang, own)) own else "${base}_other"
    return translate(lang, key, "n" to n, *args)
}

/** CLDR plural category of a whole number (written here so it runs on a plain JVM, as the tests do). */
fun pluralCategory(n: Int, lang: Lang): String {
    val abs = kotlin.math.abs(n)
    return when (lang) {
        Lang.FA -> if (abs == 0 || abs == 1) "one" else "other"
        Lang.RU -> when {
            abs % 10 == 1 && abs % 100 != 11 -> "one"
            abs % 10 in 2..4 && abs % 100 !in 12..14 -> "few"
            else -> "many"
        }
        else -> if (abs == 1) "one" else "other"
    }
}

fun translate(lang: Lang, key: String, vararg args: Pair<String, Any?>): String {
    val template = catalog(lang)[key] ?: Catalog.en[key] ?: key
    return fill(template, lang, args.toMap())
}

/** [key]'s sentence with its placeholders unfilled, in [lang] (English when missing). */
fun template(key: String, lang: Lang = I18n.lang): String = catalog(lang)[key] ?: Catalog.en[key] ?: key

/** One placeholder's value as [fill] writes it: the language's digits, a bidi isolate around Latin in Persian. */
fun fillValue(value: Any?, lang: Lang = I18n.lang): String = fill("{v}", lang, mapOf("v" to value))

internal fun has(lang: Lang, key: String) = catalog(lang).containsKey(key) || Catalog.en.containsKey(key)

private fun catalog(lang: Lang): Map<String, String> = when (lang) {
    Lang.EN -> Catalog.en
    Lang.AZ -> Catalog.az
    Lang.FA -> Catalog.fa
    Lang.TR -> Catalog.tr
    Lang.RU -> Catalog.ru
}

private val PLACEHOLDER = Regex("\\{([A-Za-z]+)\\}")

private const val FSI = '⁨'
private const val PDI = '⁩'

/**
 * Fills `{name}` once (a value is never re-scanned). Numbers take the language's digits. In a right-to-left
 * language a value with Latin letters sits in a bidi isolate, so "Leyla به Emin · ۱۶:۴۳" keeps its order.
 */
fun fill(template: String, lang: Lang, args: Map<String, Any?>): String {
    if (args.isEmpty()) return template
    return PLACEHOLDER.replace(template) { m ->
        if (!args.containsKey(m.groupValues[1])) return@replace m.value
        when (val v = args[m.groupValues[1]]) {
            null -> ""
            is Int, is Long -> formatNumber((v as Number).toLong(), lang)
            else -> {
                val s = v.toString()
                if (lang.rtl && hasForeignLetter(s)) "$FSI$s$PDI" else s
            }
        }
    }
}

/** A letter outside the Arabic script: such a value gets a bidi isolate inside a Persian sentence. */
internal fun hasForeignLetter(s: String): Boolean {
    var i = 0
    while (i < s.length) {
        val cp = s.codePointAt(i)
        if (Character.isLetter(cp) && Character.UnicodeScript.of(cp) != Character.UnicodeScript.ARABIC) return true
        i += Character.charCount(cp)
    }
    return false
}

/** 1,500 / 1 500 / ۱٬۵۰۰ / 1.500 (Turkish). */
fun formatNumber(n: Long, lang: Lang = I18n.lang): String = when (lang) {
    Lang.EN -> NumberFormat.getIntegerInstance(Locale.UK).format(n)
    Lang.AZ, Lang.RU -> NumberFormat.getIntegerInstance(Locale.UK).format(n).replace(',', ' ')
    Lang.TR -> NumberFormat.getIntegerInstance(Locale.UK).format(n).replace(',', '.')
    Lang.FA -> localDigits(NumberFormat.getIntegerInstance(Locale.UK).format(n).replace(',', '٬'), lang)
}

fun formatNumber(n: Int, lang: Lang = I18n.lang): String = formatNumber(n.toLong(), lang)

/** "12/16" in the language's digits; digits keep it left to right in a right-to-left line. */
fun counter(n: Int, max: Int, lang: Lang = I18n.lang): String = "${formatNumber(n, lang)}/${formatNumber(max, lang)}"

/** Persian digits ۰–۹ for every 0–9 in [s] when the language is Persian (L4); keys and invites never pass here. */
fun localDigits(s: String, lang: Lang = I18n.lang): String {
    if (lang != Lang.FA) return s
    val b = StringBuilder(s.length)
    for (c in s) b.append(if (c in '0'..'9') '۰' + (c - '0') else c)
    return b.toString()
}
