package com.delulutopia.core.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LinksTest {
    private fun links(s: String) = Links.linkify(s).filter { it.href != null }.map { it.text to it.href }

    @Test
    fun plainTextStaysOneSegment() {
        assertThat(Links.linkify("hello there")).containsExactly(Links.Segment("hello there"))
        assertThat(Links.linkify("")).containsExactly(Links.Segment(""))
    }

    @Test
    fun findsHttpHttpsAndWwwInOrderAndRoundTrips() {
        val s = "a http://x.com b https://y.org/p?q=1#h c www.z.net d"
        assertThat(links(s)).containsExactly(
            "http://x.com" to "http://x.com",
            "https://y.org/p?q=1#h" to "https://y.org/p?q=1#h",
            "www.z.net" to "https://www.z.net",
        ).inOrder()
        assertThat(Links.linkify(s).joinToString("") { it.text }).isEqualTo(s)
    }

    @Test
    fun bareDomainsEmailsAndPhonesStayText() {
        assertThat(Links.hasLink("see site.com or me@mail.com or +48 123 456 789 file.txt")).isFalse()
        assertThat(Links.hasLink("me@www.mail.com")).isFalse()
    }

    @Test
    fun trailingPunctuationStaysOutside() {
        assertThat(links("see https://x.com.")).containsExactly("https://x.com" to "https://x.com")
        assertThat(links("really? www.x.com!?")).containsExactly("www.x.com" to "https://www.x.com")
        assertThat(links("\"https://x.com/a\",")).containsExactly("https://x.com/a" to "https://x.com/a")
    }

    @Test
    fun balancedBracketsStayAndUnbalancedGo() {
        assertThat(links("(https://x.com/a)")).containsExactly("https://x.com/a" to "https://x.com/a")
        val wiki = "https://en.wikipedia.org/wiki/Baku_(city)"
        assertThat(links(wiki)).containsExactly(wiki to wiki)
    }

    @Test
    fun rightToLeftAndNewLines() {
        assertThat(links("سلام https://x.com\nwww.y.com").map { it.first }).containsExactly("https://x.com", "www.y.com").inOrder()
    }

    @Test
    fun onlyHttpWithADottedHostOpens() {
        assertThat(Links.hasLink("https:// and www.")).isFalse()
        assertThat(Links.hrefOf("https://localhost")).isNull()
        assertThat(Links.hrefOf("javascript:alert(1)")).isNull()
    }
}
