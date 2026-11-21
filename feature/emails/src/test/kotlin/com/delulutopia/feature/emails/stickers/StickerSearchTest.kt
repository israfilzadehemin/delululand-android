package com.delulutopia.feature.emails.stickers

import com.delulutopia.core.i18n.t
import com.delulutopia.core.network.model.EmailQuote
import com.delulutopia.core.network.model.Sticker
import com.delulutopia.core.network.model.StickerCatalogue
import com.delulutopia.core.network.model.StickerCustom
import com.delulutopia.core.network.model.StickerPack
import com.delulutopia.feature.emails.quoteLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Stickers (doc "Delululand — Stickers"): search (SK5), the picker's sections (FS-SK3), the quote (FS-SK15). */
class StickerSearchTest {
    private fun s(id: String, pack: String? = "love", keywords: List<String> = emptyList(), emojis: List<String> = emptyList(), custom: Boolean = false) =
        Sticker(id, pack, keywords, emojis, "/api/stickers/$id/image?size=256", "/api/stickers/$id/image?size=512", if (custom) StickerCustom("u1") else null)

    @Test
    fun keywordsMatchFoldedInAnyLanguage_andEmojisMatch() {
        val hug = s("1", keywords = listOf("hug", "Qucaq", "обнять"), emojis = listOf("🤗"))
        assertTrue(stickerMatches(hug, "HUG"))
        assertTrue(stickerMatches(hug, "qucaq"))
        assertTrue(stickerMatches(hug, "QÜÇAQ"))
        assertTrue(stickerMatches(hug, "обн"))
        assertTrue(stickerMatches(hug, "🤗"))
        assertTrue(stickerMatches(hug, "  "))
        assertFalse(stickerMatches(hug, "sorry"))
    }

    @Test
    fun sectionsHideEmptyRecentAndFavourites_butAlwaysShowOurs() {
        val cat = StickerCatalogue(
            packs = listOf(StickerPack("love", "Love", 1), StickerPack("silly", "Silly", 2)),
            stickers = listOf(s("1"), s("2"), s("3", pack = null, custom = true)),
        )
        assertEquals(listOf("ours", "pack-love"), sections(cat).map { it.key })
        val ours = sections(cat).first { it.key == "ours" }
        assertTrue(ours.add)
        assertEquals(listOf("3"), ours.items.map { it.id })
    }

    @Test
    fun recentAndFavouritesKeepTheirOrder_andDropStickersNoLongerThere() {
        val cat = StickerCatalogue(
            packs = listOf(StickerPack("love", "Love", 1)),
            stickers = listOf(s("1"), s("2")),
            recent = listOf("2", "gone", "1"),
            favourites = listOf("1"),
        )
        val out = sections(cat)
        assertEquals(listOf("recent", "favourites", "ours", "pack-love"), out.map { it.key })
        assertEquals(listOf("2", "1"), out[0].items.map { it.id })
        assertEquals(listOf("1"), out[1].items.map { it.id })
    }

    @Test
    fun aQuotedStickerReadsSticker() {
        val q = EmailQuote(id = "e1", senderId = "u1", sentAt = "2026-11-20T10:00:00Z", sticker = "/api/stickers/1/image?size=256")
        assertEquals(t("stickers.sticker"), quoteLine(q))
    }

    @Test
    fun checkRefusesLargeFilesAndUnknownTypes() {
        assertEquals(null, checkSticker(1000, "image/png"))
        assertEquals(t("stickers.tooLarge"), checkSticker(StickerStore.MAX_BYTES + 1, "image/png"))
        assertEquals(t("stickers.unsupported"), checkSticker(1000, "image/heic"))
    }
}
