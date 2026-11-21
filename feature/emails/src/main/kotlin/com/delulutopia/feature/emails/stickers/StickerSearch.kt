package com.delulutopia.feature.emails.stickers

import com.delulutopia.core.i18n.t
import com.delulutopia.core.network.model.Sticker
import com.delulutopia.core.network.model.StickerCatalogue
import java.text.Normalizer

/** Case- and diacritic-insensitive, as the stamp search (web stamps fold): `qucaq` finds "Qucaq". */
fun fold(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase().replace('ı', 'i')

/** SK5: hidden keywords in every language, and the emojis that find a sticker. */
fun stickerMatches(s: Sticker, query: String): Boolean {
    val raw = query.trim()
    if (raw.isEmpty()) return true
    if (s.emojis.any { raw.contains(it) || it.contains(raw) }) return true
    val f = fold(raw)
    return s.keywords.any { fold(it).contains(f) }
}

data class Section(val key: String, val title: String, val items: List<Sticker>, val add: Boolean = false)

/** FS-SK3: Recent · Favourites · Ours (with +) · the packs; empty Recent and Favourites hidden, Ours never. */
fun sections(cat: StickerCatalogue): List<Section> {
    val by = cat.stickers.associateBy { it.id }
    val out = mutableListOf<Section>()
    val recent = cat.recent.mapNotNull { by[it] }
    if (recent.isNotEmpty()) out += Section("recent", t("stickers.recent"), recent)
    val favs = cat.favourites.mapNotNull { by[it] }
    if (favs.isNotEmpty()) out += Section("favourites", t("stickers.favourites"), favs)
    out += Section("ours", t("stickers.ours"), cat.stickers.filter { it.custom != null }, add = true)
    cat.packs.forEach { p ->
        val items = cat.stickers.filter { it.pack == p.slug }
        if (items.isNotEmpty()) out += Section("pack-${p.slug}", p.name, items)
    }
    return out
}

/** FS-SK18: "Sticker: hug". */
fun stickerLabel(s: Sticker): String = s.keywords.firstOrNull()?.let { t("stickers.labelWith", "word" to it) } ?: t("stickers.label")
