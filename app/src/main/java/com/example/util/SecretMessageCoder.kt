package com.example.util

/**
 * Custom End-to-End Encryption (E2EE) Substitution Cipher for Linko.
 *
 * Provides two primary machines:
 * 1. [encodeMessage]: Converts plain text characters into Linko's custom symbol dictionary,
 *    joining each encoded token with a single space (" ") while leaving unrecognized
 *    characters (such as numbers, punctuation, or emojis) unchanged.
 * 2. [decodeMessage]: Splits a space-separated encoded string from Firebase and converts
 *    the custom symbols back into readable text.
 */
object SecretMessageCoder {

    /**
     * Exact character-to-symbol mapping dictionary (A-Z and Space).
     */
    private val charToSymbolMap: Map<Char, String> = mapOf(
        'A' to "/.",
        'B' to "//",
        'C' to ".#",
        'D' to "##",
        'E' to "..",
        'F' to "><",
        'G' to ">>",
        'H' to "<<",
        'I' to "||",
        'J' to ".-.",
        'K' to "..-",
        'L' to "--.",
        'M' to "---",
        'N' to "-.",
        'O' to "::",
        'P' to "[]",
        'Q' to "{}",
        'R' to "~~",
        'S' to "*/",
        'T' to "++",
        'U' to "()",
        'V' to "\\/",
        'W' to "\\/\\/",
        'X' to "><>",
        'Y' to "->",
        'Z' to "<-",
        ' ' to "___"
    )

    /**
     * Reverse symbol-to-character lookup dictionary used by the Decoder Machine.
     */
    private val symbolToCharMap: Map<String, Char> =
        charToSymbolMap.entries.associate { (char, symbol) -> symbol to char }

    /**
     * Encoder Machine:
     * Converts each character of [plainText] into its corresponding custom symbol
     * and joins them with a single space (" ") in between so they do not merge.
     * Unrecognized characters (numbers, emojis, punctuation) remain unchanged.
     */
    fun encodeMessage(plainText: String): String {
        if (plainText.isEmpty()) return ""

        val encodedTokens = mutableListOf<String>()
        var i = 0
        while (i < plainText.length) {
            val ch = plainText[i]
            // Preserve multi-char UTF-16 surrogate pairs (e.g., emojis like 😀 or 🚀) as a single token
            if (ch.isHighSurrogate() && i + 1 < plainText.length && plainText[i + 1].isLowSurrogate()) {
                val emoji = plainText.substring(i, i + 2)
                encodedTokens.add(emoji)
                i += 2
            } else {
                val symbol = charToSymbolMap[ch.uppercaseChar()] ?: ch.toString()
                encodedTokens.add(symbol)
                i += 1
            }
        }

        return encodedTokens.joinToString(" ")
    }

    /**
     * Decoder Machine:
     * Takes the space-separated [encodedText] from Firebase, splits it by space (" "),
     * and converts each symbol back into normal readable text.
     */
    fun decodeMessage(encodedText: String): String {
        if (encodedText.isEmpty()) return ""

        return encodedText
            .split(" ")
            .joinToString("") { token ->
                symbolToCharMap[token]?.toString() ?: token
            }
    }

    /**
     * Helper to check whether a raw string from Firebase is encoded with [SecretMessageCoder].
     * Allows backward compatibility with any pre-existing unencoded messages in Firestore.
     */
    fun isEncodedFormat(text: String): Boolean {
        if (text.isBlank()) return false
        val tokens = text.split(" ").filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return false
        return tokens.all { token ->
            symbolToCharMap.containsKey(token) ||
                token.length == 1 ||
                (token.length == 2 && token[0].isHighSurrogate() && token[1].isLowSurrogate())
        } && tokens.any { symbolToCharMap.containsKey(it) }
    }
}
