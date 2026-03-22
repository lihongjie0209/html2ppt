package com.html2ppt.model;

/**
 * A styled text segment within a TextBlock.
 */
public record TextRun(
    String text,
    boolean bold,
    boolean italic,
    boolean underline,
    String color,
    Double fontSize,
    String fontFamily,
    String href  // Hyperlink URL (null if not a link)
) {
    public TextRun(String text) {
        this(text, false, false, false, null, null, null, null);
    }

    // Backwards compatibility constructor without href
    public TextRun(String text, boolean bold, boolean italic, boolean underline,
                   String color, Double fontSize, String fontFamily) {
        this(text, bold, italic, underline, color, fontSize, fontFamily, null);
    }
}
