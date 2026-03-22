package com.html2ppt.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Internal tree model for the presentation.
 * Each node type maps to a specific PPT shape.
 * Layout results are mutable — filled by YogaLayoutEngine after tree construction.
 */
public sealed interface SlideNode {

    ComputedStyle style();
    List<SlideNode> children();
    LayoutResult layout();
    void setLayout(LayoutResult layout);

    // ─── Concrete node types ────────────────────────────────────────

    /** One slide in the presentation. */
    final class Slide implements SlideNode {
        private final ComputedStyle style;
        private final List<SlideNode> children;
        private LayoutResult layout;
        private String notes;
        private String layoutType;  // "title", "title-content", "blank", etc.

        public Slide(ComputedStyle style, List<SlideNode> children) {
            this.style = style;
            this.children = new ArrayList<>(children);
            this.layout = LayoutResult.ZERO;
            this.notes = null;
            this.layoutType = null;  // null = custom (no master layout)
        }

        @Override public ComputedStyle style() { return style; }
        @Override public List<SlideNode> children() { return children; }
        @Override public LayoutResult layout() { return layout; }
        @Override public void setLayout(LayoutResult layout) { this.layout = layout; }
        
        public String notes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        
        public String layoutType() { return layoutType; }
        public void setLayoutType(String layoutType) { this.layoutType = layoutType; }
    }

    /** Generic container (<div>). Maps to XSLFGroupShape. */
    final class Box implements SlideNode {
        private final ComputedStyle style;
        private final List<SlideNode> children;
        private LayoutResult layout;

        public Box(ComputedStyle style, List<SlideNode> children) {
            this.style = style;
            this.children = new ArrayList<>(children);
            this.layout = LayoutResult.ZERO;
        }

        @Override public ComputedStyle style() { return style; }
        @Override public List<SlideNode> children() { return children; }
        @Override public LayoutResult layout() { return layout; }
        @Override public void setLayout(LayoutResult layout) { this.layout = layout; }
    }

    /** Text element (<p>, <h1>-<h6>). Maps to XSLFTextBox. */
    final class TextBlock implements SlideNode {
        private final List<TextRun> runs;
        private final ComputedStyle style;
        private LayoutResult layout;

        public TextBlock(List<TextRun> runs, ComputedStyle style) {
            this.runs = new ArrayList<>(runs);
            this.style = style;
            this.layout = LayoutResult.ZERO;
        }

        public List<TextRun> runs() { return runs; }
        public String plainText() {
            StringBuilder sb = new StringBuilder();
            for (TextRun run : runs) sb.append(run.text());
            return sb.toString();
        }

        @Override public ComputedStyle style() { return style; }
        @Override public List<SlideNode> children() { return Collections.emptyList(); }
        @Override public LayoutResult layout() { return layout; }
        @Override public void setLayout(LayoutResult layout) { this.layout = layout; }
    }

    /** Image element (<img>). Maps to XSLFPictureShape. */
    final class ImageBlock implements SlideNode {
        private final String src;
        private final ComputedStyle style;
        private LayoutResult layout;

        public ImageBlock(String src, ComputedStyle style) {
            this.src = src;
            this.style = style;
            this.layout = LayoutResult.ZERO;
        }

        public String src() { return src; }

        @Override public ComputedStyle style() { return style; }
        @Override public List<SlideNode> children() { return Collections.emptyList(); }
        @Override public LayoutResult layout() { return layout; }
        @Override public void setLayout(LayoutResult layout) { this.layout = layout; }
    }

    /** Table element (<table>). Maps to XSLFTable. */
    final class TableBlock implements SlideNode {
        private final List<List<TableCell>> rows;
        private final ComputedStyle style;
        private LayoutResult layout;

        public TableBlock(List<List<TableCell>> rows, ComputedStyle style) {
            this.rows = rows;
            this.style = style;
            this.layout = LayoutResult.ZERO;
        }

        public List<List<TableCell>> rows() { return rows; }

        @Override public ComputedStyle style() { return style; }
        @Override public List<SlideNode> children() { return Collections.emptyList(); }
        @Override public LayoutResult layout() { return layout; }
        @Override public void setLayout(LayoutResult layout) { this.layout = layout; }
    }

    /** Shape element (<hr>, decorative shapes). Maps to XSLFAutoShape. */
    final class ShapeBlock implements SlideNode {
        private final String shapeType; // "line", "rect", "roundRect", "ellipse"
        private final ComputedStyle style;
        private LayoutResult layout;

        public ShapeBlock(String shapeType, ComputedStyle style) {
            this.shapeType = shapeType;
            this.style = style;
            this.layout = LayoutResult.ZERO;
        }

        public String shapeType() { return shapeType; }

        @Override public ComputedStyle style() { return style; }
        @Override public List<SlideNode> children() { return Collections.emptyList(); }
        @Override public LayoutResult layout() { return layout; }
        @Override public void setLayout(LayoutResult layout) { this.layout = layout; }
    }
}
