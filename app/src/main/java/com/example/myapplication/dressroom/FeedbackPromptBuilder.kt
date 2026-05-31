package com.example.myapplication.dressroom

import com.example.myapplication.model.ClothingItem

object FeedbackPromptBuilder {

    fun buildPrompt(
        top: ClothingItem?,
        bottom: ClothingItem?,
        outerwear: ClothingItem?,
        event: String
    ): String {
        return buildString {
            append("You are a professional fashion stylist. Analyze this outfit combination and provide feedback.\n\n")

            append("EVENT: $event\n\n")

            if (top != null) {
                append("TOP:\n")
                append("  Type: ${top.type}\n")
                append("  Name: ${top.name}\n")
                append("  Colors: ${top.color.joinToString(", ")}\n")
                append("  Pattern: ${top.pattern}\n")
                append("  Thickness: ${top.thickness}\n")
                append("  Style Keywords: ${top.styleKeywords.joinToString(", ")}\n\n")
            }

            if (bottom != null) {
                append("BOTTOM:\n")
                append("  Type: ${bottom.type}\n")
                append("  Name: ${bottom.name}\n")
                append("  Colors: ${bottom.color.joinToString(", ")}\n")
                append("  Pattern: ${bottom.pattern}\n")
                append("  Thickness: ${bottom.thickness}\n")
                append("  Style Keywords: ${bottom.styleKeywords.joinToString(", ")}\n\n")
            }

            if (outerwear != null) {
                append("OUTERWEAR:\n")
                append("  Type: ${outerwear.type}\n")
                append("  Name: ${outerwear.name}\n")
                append("  Colors: ${outerwear.color.joinToString(", ")}\n")
                append("  Pattern: ${outerwear.pattern}\n")
                append("  Thickness: ${outerwear.thickness}\n")
                append("  Style Keywords: ${outerwear.styleKeywords.joinToString(", ")}\n\n")
            }

            append("Please provide:\n")
            append("1. Is this outfit suitable for the event? (Yes/No and why)\n")
            append("2. Is the style combination good? (Feedback on colors, patterns, and overall coherence)\n")
            append("3. If not ideal, what would be a better alternative from similar pieces?\n")
            append("4. Styling tips to make this outfit work better\n\n")
            append("Be concise, friendly, and constructive.")
        }
    }
}