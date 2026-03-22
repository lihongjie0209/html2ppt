package com.html2ppt.layout;

import com.html2ppt.model.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Dumps the computed layout tree to a human-readable text file for debugging.
 * Output includes node types, computed coordinates, and relevant CSS properties.
 *
 * Usage from CLI: html2ppt compile input.html --debug
 * Produces: input.layout.txt alongside the .pptx
 */
public class LayoutDebugger {

    /**
     * Dump all slides' layout trees to a file.
     */
    public static void dumpToFile(List<SlideNode.Slide> slides, Path outputPath) throws IOException {
        String content = dump(slides);
        Files.writeString(outputPath, content);
    }

    /**
     * Dump all slides' layout trees to a string.
     */
    public static String dump(List<SlideNode.Slide> slides) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("╔══════════════════════════════════════════════════════════════╗");
        pw.println("║              html2ppt Layout Debug Dump                     ║");
        pw.println("╚══════════════════════════════════════════════════════════════╝");
        pw.println();

        for (int i = 0; i < slides.size(); i++) {
            pw.printf("━━━ Slide %d ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━%n", i + 1);
            printNode(pw, slides.get(i), 0);
            pw.println();
        }

        pw.println("━━━ Legend ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        pw.println("  Coordinates: [x, y, w, h] in points (1pt = 1/72 inch)");
        pw.println("  Slide 16:9 = 720×405pt, 4:3 = 720×540pt");
        pw.println("  Child coordinates are relative to parent group origin");
        pw.flush();
        return sw.toString();
    }

    private static void printNode(PrintWriter pw, SlideNode node, int depth) {
        String indent = "│ ".repeat(depth);
        String connector = depth == 0 ? "" : "├─";

        // Node type label
        String typeLabel = switch (node) {
            case SlideNode.Slide s -> "Slide";
            case SlideNode.Box b -> "Box";
            case SlideNode.TextBlock t -> "Text";
            case SlideNode.ImageBlock i -> "Image";
            case SlideNode.TableBlock t -> "Table";
            case SlideNode.ShapeBlock s -> "Shape(" + s.shapeType() + ")";
        };

        // Layout coordinates
        LayoutResult lr = node.layout();
        String coords = lr != null
            ? String.format("[x=%-6.1f y=%-6.1f w=%-6.1f h=%-6.1f]", lr.x(), lr.y(), lr.width(), lr.height())
            : "[NO LAYOUT]";

        // Text content (truncated)
        String textPreview = "";
        if (node instanceof SlideNode.TextBlock tb) {
            String plain = tb.plainText();
            if (plain.length() > 30) plain = plain.substring(0, 27) + "...";
            textPreview = " \"" + plain + "\"";
        }

        // CSS properties summary
        String cssInfo = buildCssInfo(node.style());

        pw.printf("%s%s %s %s%s%s%n", indent, connector, typeLabel, coords, textPreview, cssInfo);

        // Children
        List<SlideNode> children = node.children();
        for (int i = 0; i < children.size(); i++) {
            printNode(pw, children.get(i), depth + 1);
        }
    }

    private static String buildCssInfo(ComputedStyle style) {
        if (style == null) return "";

        StringBuilder sb = new StringBuilder();

        // Flex layout properties
        append(sb, "display", style.get("display"));
        append(sb, "flex-dir", style.get("flex-direction"));
        append(sb, "justify", style.get("justify-content"));
        append(sb, "align", style.get("align-items"));
        append(sb, "flex-wrap", style.get("flex-wrap"));
        append(sb, "flex-grow", style.get("flex-grow"));
        append(sb, "flex-shrink", style.get("flex-shrink"));
        append(sb, "flex-basis", style.get("flex-basis"));

        // Gap
        append(sb, "gap", style.get("gap"));
        append(sb, "row-gap", style.get("row-gap"));
        append(sb, "col-gap", style.get("column-gap"));

        // Sizing
        append(sb, "width", style.get("width"));
        append(sb, "height", style.get("height"));
        append(sb, "min-w", style.get("min-width"));
        append(sb, "max-w", style.get("max-width"));
        append(sb, "min-h", style.get("min-height"));
        append(sb, "max-h", style.get("max-height"));

        // Padding (summarize)
        String pt = style.get("padding-top"), pr = style.get("padding-right"),
               pb = style.get("padding-bottom"), pl = style.get("padding-left");
        if (pt != null || pr != null || pb != null || pl != null) {
            sb.append(" pad=[")
              .append(pt != null ? pt : "0").append(" ")
              .append(pr != null ? pr : "0").append(" ")
              .append(pb != null ? pb : "0").append(" ")
              .append(pl != null ? pl : "0").append("]");
        }

        // Margin (summarize)
        String mt = style.get("margin-top"), mr = style.get("margin-right"),
               mb = style.get("margin-bottom"), ml = style.get("margin-left");
        if (mt != null || mr != null || mb != null || ml != null) {
            sb.append(" margin=[")
              .append(mt != null ? mt : "0").append(" ")
              .append(mr != null ? mr : "0").append(" ")
              .append(mb != null ? mb : "0").append(" ")
              .append(ml != null ? ml : "0").append("]");
        }

        // Visual
        append(sb, "bg", style.get("background-color"));
        append(sb, "color", style.get("color"));
        append(sb, "font-size", style.get("font-size"));
        append(sb, "font-weight", style.get("font-weight"));
        append(sb, "text-align", style.get("text-align"));
        append(sb, "position", style.get("position"));

        if (sb.isEmpty()) return "";
        return "\n" + "│ ".repeat(1) + "  ╰─ " + sb.toString().trim();
    }

    private static void append(StringBuilder sb, String label, String value) {
        if (value != null) sb.append(label).append("=").append(value).append("  ");
    }
}
