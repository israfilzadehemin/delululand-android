package com.delulutopia.core.ui.editor

import com.delulutopia.core.network.model.Block
import com.delulutopia.core.network.model.BulletList
import com.delulutopia.core.network.model.Doc
import com.delulutopia.core.network.model.ListItem
import com.delulutopia.core.network.model.Mark
import com.delulutopia.core.network.model.Paragraph
import com.delulutopia.core.network.model.TextNode

/*
 * The editor's model of the letter format (Technical Design §4.3; web editor/extensions.ts): plain
 * text with one line per paragraph or list item, mark ranges for bold, italic and underline, and
 * the set of lines that are bullets. Converted to and from the ProseMirror JSON the API stores.
 */

enum class MarkType(val wire: String) { Bold("bold"), Italic("italic"), Underline("underline") }

/** [start, end) in the text, one mark. */
data class MarkSpan(val start: Int, val end: Int, val type: MarkType)

data class RichDoc(val text: String = "", val marks: List<MarkSpan> = emptyList(), val bullets: Set<Int> = emptySet()) {
    val lines: List<String> get() = text.split('\n')

    /** Offset of the first character of each line. */
    fun lineStarts(): IntArray {
        val starts = ArrayList<Int>()
        starts += 0
        text.forEachIndexed { i, c -> if (c == '\n') starts += i + 1 }
        return starts.toIntArray()
    }

    fun lineOf(offset: Int): Int {
        var n = 0
        for (i in 0 until offset.coerceAtMost(text.length)) if (text[i] == '\n') n++
        return n
    }

    fun has(type: MarkType, offset: Int): Boolean = marks.any { it.type == type && offset >= it.start && offset < it.end }

    /** Whether every character of [start, end) carries [type]. */
    fun covers(type: MarkType, start: Int, end: Int): Boolean = (start until end).filter { text.getOrNull(it) != '\n' }.all { has(type, it) }

    fun toDoc(): Doc {
        val starts = lineStarts()
        val blocks = ArrayList<Block>()
        var list: MutableList<ListItem>? = null
        lines.forEachIndexed { i, line ->
            val paragraph = paragraphOf(starts[i], line)
            if (i in bullets) {
                if (list == null) list = mutableListOf()
                list!! += ListItem(content = listOf(paragraph))
            } else {
                list?.let { blocks += BulletList(it) }
                list = null
                blocks += paragraph
            }
        }
        list?.let { blocks += BulletList(it) }
        return Doc(content = blocks.ifEmpty { listOf(Paragraph()) })
    }

    private fun paragraphOf(start: Int, line: String): Paragraph {
        if (line.isEmpty()) return Paragraph()
        val nodes = ArrayList<TextNode>()
        var runStart = 0
        var runMarks = marksAt(start)
        for (i in 1..line.length) {
            val m = if (i < line.length) marksAt(start + i) else null
            if (m != runMarks) {
                nodes += TextNode(text = line.substring(runStart, i), marks = runMarks.takeIf { it.isNotEmpty() }?.map { Mark(it.wire) })
                runStart = i
                if (m != null) runMarks = m
            }
        }
        return Paragraph(nodes)
    }

    private fun marksAt(offset: Int): List<MarkType> = MarkType.entries.filter { has(it, offset) }

    companion object {
        fun of(doc: Doc): RichDoc {
            val text = StringBuilder()
            val marks = ArrayList<MarkSpan>()
            val bullets = HashSet<Int>()
            var line = 0
            fun paragraph(p: Paragraph) {
                if (line > 0) text.append('\n')
                p.content.orEmpty().forEach { t ->
                    val from = text.length
                    text.append(t.text.replace('\n', ' '))
                    t.marks.orEmpty().forEach { m -> MarkType.entries.firstOrNull { it.wire == m.type }?.let { marks += MarkSpan(from, text.length, it) } }
                }
                line++
            }
            doc.content.forEach { b ->
                when (b) {
                    is Paragraph -> paragraph(b)
                    is BulletList -> b.content.forEach { item ->
                        item.content.filterIsInstance<Paragraph>().forEach { p ->
                            bullets += line
                            paragraph(p)
                        }
                    }
                }
            }
            return RichDoc(text.toString(), merge(marks), bullets)
        }

        /** Adjacent or overlapping spans of one type become one. */
        fun merge(spans: List<MarkSpan>): List<MarkSpan> = MarkType.entries.flatMap { type ->
            val sorted = spans.filter { it.type == type && it.end > it.start }.sortedBy { it.start }
            val out = ArrayList<MarkSpan>()
            for (s in sorted) {
                val last = out.lastOrNull()
                if (last != null && s.start <= last.end) out[out.size - 1] = last.copy(end = maxOf(last.end, s.end)) else out += s
            }
            out
        }
    }
}

/**
 * The text changed from [old] to [new] (one edit: a typed run, a deletion, a paste). Marks and
 * bullet lines follow the text; typed characters take [typing] (the marks at the cursor, or the
 * ones just toggled). Enter on a bullet continues the list; Enter on an empty bullet leaves it.
 */
fun RichDoc.edited(new: String, typing: Set<MarkType>): RichDoc {
    val old = text
    if (old == new) return this
    var prefix = 0
    while (prefix < old.length && prefix < new.length && old[prefix] == new[prefix]) prefix++
    var suffix = 0
    while (suffix < old.length - prefix && suffix < new.length - prefix && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
    val removedEnd = old.length - suffix
    val insertedEnd = new.length - suffix
    val delta = (insertedEnd - prefix) - (removedEnd - prefix)

    // Marks: shift, clip, then add the typed run.
    val moved = marks.mapNotNull { s ->
        fun map(o: Int, isEnd: Boolean): Int = when {
            o < prefix || (o == prefix && !isEnd) -> o
            o >= removedEnd -> o + delta
            else -> prefix
        }
        val a = map(s.start, false)
        val b = map(s.end, true)
        if (b > a) MarkSpan(a, b, s.type) else null
    }.toMutableList()
    if (insertedEnd > prefix) typing.forEach { moved += MarkSpan(prefix, insertedEnd, it) }

    // Bullets: lines before the edit keep their index; lines after it shift by the newline balance.
    val startLine = lineOf(prefix)
    val removedNl = old.substring(prefix, removedEnd).count { it == '\n' }
    val inserted = new.substring(prefix, insertedEnd)
    val addedNl = inserted.count { it == '\n' }
    val nextBullets = HashSet<Int>()
    bullets.forEach { l ->
        when {
            l <= startLine -> nextBullets += l
            l > startLine + removedNl -> nextBullets += l - removedNl + addedNl
        }
    }
    // New lines from Enter inside a bullet are bullets too.
    if (startLine in bullets) for (k in 1..addedNl) nextBullets += startLine + k
    var result = RichDoc(new, RichDoc.merge(moved), nextBullets)
    // Enter on an empty bullet line leaves the list instead.
    if (inserted == "\n" && startLine in bullets && old.split('\n').getOrNull(startLine)?.isEmpty() == true) {
        val b = HashSet(result.bullets)
        b.remove(startLine)
        b.remove(startLine + 1)
        val starts = result.lineStarts()
        val cut = starts[startLine + 1] - 1
        result = RichDoc(result.text.removeRange(cut, cut + 1), result.marks, b.map { if (it > startLine) it - 1 else it }.toSet())
    }
    return result
}

/** Toggle [type] over [start, end): off when every character already has it, else on. */
fun RichDoc.toggled(type: MarkType, start: Int, end: Int): RichDoc {
    if (end <= start) return this
    return if (covers(type, start, end)) {
        val out = marks.flatMap { s ->
            if (s.type != type || s.end <= start || s.start >= end) {
                listOf(s)
            } else {
                listOfNotNull(
                    MarkSpan(s.start, start, type).takeIf { start > s.start },
                    MarkSpan(end, s.end, type).takeIf { s.end > end },
                )
            }
        }
        copy(marks = out)
    } else {
        copy(marks = RichDoc.merge(marks + MarkSpan(start, end, type)))
    }
}

/** The bullet list on or off for every line the selection touches. */
fun RichDoc.bulletToggled(start: Int, end: Int): RichDoc {
    val from = lineOf(start)
    val to = lineOf(end)
    val all = (from..to).all { it in bullets }
    val next = HashSet(bullets)
    for (l in from..to) if (all) next.remove(l) else next.add(l)
    return copy(bullets = next)
}
