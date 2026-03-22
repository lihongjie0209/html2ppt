package com.html2ppt.model;

/**
 * A single table cell with text, style, and optional colspan/rowspan.
 */
public record TableCell(String text, ComputedStyle style, int colspan, int rowspan) {
    public TableCell(String text) {
        this(text, new ComputedStyle(), 1, 1);
    }
    
    public TableCell(String text, ComputedStyle style) {
        this(text, style, 1, 1);
    }
}
