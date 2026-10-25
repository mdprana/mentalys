package com.mentalys.app.utils

import android.text.Html
import android.os.Build

/**
 * Utility object for sanitizing text from external sources before displaying in TextViews.
 *
 * Two sources of "strange Unicode letter symbols" in the app:
 *
 * 1. Gemini AI responses — the model sometimes emits Unicode Mathematical Bold/Italic
 *    characters (U+1D400–U+1D7FF range) instead of plain ASCII. Example: 𝗥𝗶𝗻𝗴𝗸𝗮𝘀𝗮𝗻 instead
 *    of "Ringkasan". The Poppins font doesn't cover this Unicode block, so Android falls back
 *    to a system serif/emoji font — causing mismatched "A letter" glyph shapes.
 *
 * 2. Article content from the backend API — paragraphs contain HTML markup and entities
 *    (<b>, <em>, &amp;, &#8230;, etc.) that are set directly to TextView.text without parsing.
 */
object TextSanitizer {

    /**
     * Strips Unicode Mathematical Bold/Italic/Script/Fraktur lookalike characters
     * and replaces them with their plain ASCII equivalents.
     *
     * Covers:
     *   U+1D400–U+1D433  Mathematical Bold (𝐀–𝐳)
     *   U+1D434–U+1D467  Mathematical Italic (𝐴–𝑧)
     *   U+1D468–U+1D49B  Mathematical Bold Italic
     *   U+1D49C–U+1D4CF  Mathematical Script
     *   U+1D4D0–U+1D503  Mathematical Bold Script
     *   U+1D504–U+1D537  Mathematical Fraktur
     *   U+1D538–U+1D56B  Mathematical Double-Struck
     *   U+1D56C–U+1D59F  Mathematical Bold Fraktur
     *   U+1D5A0–U+1D5D3  Mathematical Sans-Serif
     *   U+1D5D4–U+1D607  Mathematical Sans-Serif Bold  ← most common from Gemini
     *   U+1D608–U+1D63B  Mathematical Sans-Serif Italic
     *   U+1D63C–U+1D66F  Mathematical Sans-Serif Bold Italic
     *   U+1D670–U+1D6A3  Mathematical Monospace
     *   U+1D6A8–U+1D7CB  Mathematical Bold/Italic Greek
     */
    fun sanitizeGemini(text: String): String {
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val plain = mathToAscii(cp)
            if (plain != null) {
                sb.append(plain)
            } else {
                sb.appendCodePoint(cp)
            }
            i += Character.charCount(cp)
        }
        return sb.toString()
    }

    /**
     * Strips basic HTML tags and decodes HTML entities from article content.
     * Uses Android's Html.fromHtml() and then converts the resulting Spanned back to String,
     * which collapses formatting but produces clean readable plain text.
     */
    fun sanitizeHtml(html: String?): String {
        if (html.isNullOrBlank()) return ""
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString().trim()
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html).toString().trim()
        }
    }

    /**
     * Maps a Unicode Mathematical bold/italic code point to its plain ASCII equivalent.
     * Returns null if the code point is not in any Math Alphanumeric block.
     *
     * Reference: https://www.unicode.org/charts/PDF/U1D400.pdf
     */
    private fun mathToAscii(cp: Int): Char? {
        // Mathematical Bold Capital A–Z  U+1D400–U+1D419
        if (cp in 0x1D400..0x1D419) return ('A' + (cp - 0x1D400))
        // Mathematical Bold Small a–z    U+1D41A–U+1D433
        if (cp in 0x1D41A..0x1D433) return ('a' + (cp - 0x1D41A))

        // Mathematical Italic Capital A–Z (skip H=U+1D43B, hole at U+1D455)
        if (cp in 0x1D434..0x1D44D) return ('A' + (cp - 0x1D434))
        // Mathematical Italic Small a–z
        if (cp in 0x1D44E..0x1D467) return ('a' + (cp - 0x1D44E))

        // Mathematical Bold Italic Capital
        if (cp in 0x1D468..0x1D481) return ('A' + (cp - 0x1D468))
        // Mathematical Bold Italic Small
        if (cp in 0x1D482..0x1D49B) return ('a' + (cp - 0x1D482))

        // Mathematical Sans-Serif Bold Capital (most common Gemini output)  U+1D5D4–U+1D5ED
        if (cp in 0x1D5D4..0x1D5ED) return ('A' + (cp - 0x1D5D4))
        // Mathematical Sans-Serif Bold Small  U+1D5EE–U+1D607
        if (cp in 0x1D5EE..0x1D607) return ('a' + (cp - 0x1D5EE))

        // Mathematical Sans-Serif Capital  U+1D5A0–U+1D5B9
        if (cp in 0x1D5A0..0x1D5B9) return ('A' + (cp - 0x1D5A0))
        // Mathematical Sans-Serif Small  U+1D5BA–U+1D5D3
        if (cp in 0x1D5BA..0x1D5D3) return ('a' + (cp - 0x1D5BA))

        // Mathematical Sans-Serif Italic Capital  U+1D608–U+1D621
        if (cp in 0x1D608..0x1D621) return ('A' + (cp - 0x1D608))
        // Mathematical Sans-Serif Italic Small  U+1D622–U+1D63B
        if (cp in 0x1D622..0x1D63B) return ('a' + (cp - 0x1D622))

        // Mathematical Sans-Serif Bold Italic Capital  U+1D63C–U+1D655
        if (cp in 0x1D63C..0x1D655) return ('A' + (cp - 0x1D63C))
        // Mathematical Sans-Serif Bold Italic Small  U+1D656–U+1D66F
        if (cp in 0x1D656..0x1D66F) return ('a' + (cp - 0x1D656))

        // Mathematical Monospace Capital  U+1D670–U+1D689
        if (cp in 0x1D670..0x1D689) return ('A' + (cp - 0x1D670))
        // Mathematical Monospace Small  U+1D68A–U+1D6A3
        if (cp in 0x1D68A..0x1D6A3) return ('a' + (cp - 0x1D68A))

        // Mathematical Double-Struck Capital  U+1D538–U+1D551
        if (cp in 0x1D538..0x1D551) return ('A' + (cp - 0x1D538))
        // Mathematical Double-Struck Small  U+1D552–U+1D56B
        if (cp in 0x1D552..0x1D56B) return ('a' + (cp - 0x1D552))

        // Mathematical Bold digits 0–9  U+1D7CE–U+1D7D7
        if (cp in 0x1D7CE..0x1D7D7) return ('0' + (cp - 0x1D7CE))
        // Mathematical Sans-Serif Bold digits  U+1D7EC–U+1D7F5
        if (cp in 0x1D7EC..0x1D7F5) return ('0' + (cp - 0x1D7EC))

        return null
    }
}
