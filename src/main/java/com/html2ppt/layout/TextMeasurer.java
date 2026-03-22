package com.html2ppt.layout;

/**
 * Estimates text dimensions for Yoga measure functions.
 * Used to compute auto-height for text elements.
 */
public class TextMeasurer {

    /** Average character width ratio for Latin characters (fraction of font size). */
    private static final double LATIN_WIDTH_RATIO = 0.6;

    /** Average character width ratio for CJK characters (fraction of font size). */
    private static final double CJK_WIDTH_RATIO = 1.1;

    /** Default line height multiplier. */
    private static final double LINE_HEIGHT_MULTIPLIER = 1.4;

    /** Bold text width multiplier (bold chars are ~10% wider). */
    private static final double BOLD_WIDTH_MULTIPLIER = 1.1;

    /**
     * Estimate the height needed to render text at a given font size and available width.
     */
    public double measureHeight(String text, double fontSize, double availableWidth) {
        return measureHeight(text, fontSize, availableWidth, false);
    }

    /**
     * Estimate the height needed to render text at a given font size, available width, and bold flag.
     * Handles explicit newlines (for preformatted text) and wrapping.
     */
    public double measureHeight(String text, double fontSize, double availableWidth, boolean bold) {
        if (text == null || text.isEmpty()) return fontSize * LINE_HEIGHT_MULTIPLIER;
        if (availableWidth <= 0) return fontSize * LINE_HEIGHT_MULTIPLIER;

        // Split by explicit newlines first (for preformatted/code blocks)
        String[] explicitLines = text.split("\n", -1);  // -1 to keep trailing empty strings
        int totalLines = 0;
        
        for (String line : explicitLines) {
            if (line.isEmpty()) {
                // Empty line still takes one line height
                totalLines++;
            } else {
                // Calculate how many visual lines this text line needs (wrapping)
                double lineWidth = estimateTextWidth(line, fontSize, bold);
                int wrappedLines = Math.max(1, (int) Math.ceil(lineWidth / availableWidth));
                totalLines += wrappedLines;
            }
        }
        
        return totalLines * fontSize * LINE_HEIGHT_MULTIPLIER;
    }

    /**
     * Estimate the width needed to render text in a single line.
     */
    public double measureWidth(String text, double fontSize) {
        return measureWidth(text, fontSize, false);
    }

    /**
     * Estimate the width needed to render text in a single line, accounting for bold.
     */
    public double measureWidth(String text, double fontSize, boolean bold) {
        if (text == null || text.isEmpty()) return 0;
        return estimateTextWidth(text, fontSize, bold);
    }

    /**
     * Estimate total text width accounting for CJK vs Latin character widths and bold.
     */
    private double estimateTextWidth(String text, double fontSize, boolean bold) {
        double totalWidth = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (isCJK(ch)) {
                totalWidth += fontSize * CJK_WIDTH_RATIO;
            } else {
                totalWidth += fontSize * LATIN_WIDTH_RATIO;
            }
        }
        if (bold) totalWidth *= BOLD_WIDTH_MULTIPLIER;
        // Safety margin to prevent premature wrapping (especially for short text at large sizes)
        return totalWidth + fontSize * 0.1;
    }

    /**
     * Check if a character is CJK (Chinese/Japanese/Korean) — full-width.
     */
    static boolean isCJK(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
            || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
            || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
            || block == Character.UnicodeBlock.HIRAGANA
            || block == Character.UnicodeBlock.KATAKANA
            || block == Character.UnicodeBlock.HANGUL_SYLLABLES
            || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }
}
