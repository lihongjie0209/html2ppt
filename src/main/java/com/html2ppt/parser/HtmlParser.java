package com.html2ppt.parser;

import com.html2ppt.css.CssParser;
import com.html2ppt.css.StyleResolver;
import com.html2ppt.model.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses HTML files into SlideNode trees.
 * Combines Jsoup HTML parsing + CssParser + StyleResolver + DomConverter.
 */
public class HtmlParser {

    private final CssParser cssParser = new CssParser();
    private Path basePath = null;  // Base path for resolving relative URLs

    /**
     * Parse an HTML file into a list of SlideNode.Slide nodes.
     */
    public List<SlideNode.Slide> parseFile(Path htmlFile) throws IOException {
        String html = Files.readString(htmlFile);
        this.basePath = htmlFile.getParent();  // Set base path for relative URLs
        return parseString(html);
    }

    /**
     * Parse an HTML string into a list of SlideNode.Slide nodes.
     */
    public List<SlideNode.Slide> parseString(String html) {
        Document doc = Jsoup.parse(html);

        // Extract <style> blocks
        StringBuilder cssText = new StringBuilder();
        Elements styleElements = doc.select("head style");
        for (Element style : styleElements) {
            cssText.append(style.html()).append("\n");
        }

        // Parse CSS rules
        List<CssParser.CssRule> rules = cssParser.parse(cssText.toString());
        StyleResolver resolver = new StyleResolver(rules);

        // Find all <section> elements (= slides)
        Elements sections = doc.select("body > section");
        List<SlideNode.Slide> slides = new ArrayList<>();
        for (Element section : sections) {
            ComputedStyle slideStyle = resolver.resolve(section, null);
            // Slides default to flex column
            if (!slideStyle.has("display")) slideStyle.set("display", "flex");
            if (!slideStyle.has("flex-direction")) slideStyle.set("flex-direction", "column");

            // Extract speaker notes from <aside class="notes">
            Element notesEl = section.selectFirst("aside.notes");
            String notes = null;
            if (notesEl != null) {
                notes = notesEl.text();
                notesEl.remove(); // Remove from DOM so it's not rendered as content
            }

            // Extract slide layout type from data-layout attribute
            String layoutType = section.attr("data-layout");
            
            List<SlideNode> children = convertChildren(section, resolver, slideStyle);
            SlideNode.Slide slide = new SlideNode.Slide(slideStyle, children);
            if (notes != null && !notes.isEmpty()) {
                slide.setNotes(notes);
            }
            if (!layoutType.isEmpty()) {
                slide.setLayoutType(layoutType);
            }
            slides.add(slide);
        }

        return slides;
    }

    /**
     * Extract presentation metadata from &lt;html&gt; data attributes.
     */
    public PresentationMeta parseMeta(String html) {
        Document doc = Jsoup.parse(html);
        Element htmlEl = doc.selectFirst("html");
        String layout = htmlEl != null ? htmlEl.attr("data-layout") : "";
        String title = htmlEl != null ? htmlEl.attr("data-title") : "";
        String author = htmlEl != null ? htmlEl.attr("data-author") : "";
        String subject = htmlEl != null ? htmlEl.attr("data-subject") : "";
        return new PresentationMeta(
            layout.isEmpty() ? "16x9" : layout,
            title.isEmpty() ? null : title,
            author.isEmpty() ? null : author,
            subject.isEmpty() ? null : subject
        );
    }

    /**
     * Convert child elements of a container into SlideNode list.
     */
    private List<SlideNode> convertChildren(Element parent, StyleResolver resolver, ComputedStyle parentStyle) {
        List<SlideNode> children = new ArrayList<>();

        for (Node child : parent.childNodes()) {
            if (child instanceof Element el) {
                SlideNode node = convertElement(el, resolver, parentStyle);
                if (node != null) children.add(node);
            } else if (child instanceof TextNode textNode) {
                String text = textNode.getWholeText().trim();
                if (!text.isEmpty()) {
                    // Bare text inside a container → create TextBlock
                    ComputedStyle textStyle = parentStyle.inheritableSubset();
                    List<TextRun> runs = List.of(new TextRun(text));
                    children.add(new SlideNode.TextBlock(runs, textStyle));
                }
            }
        }

        return children;
    }

    /**
     * Convert a single DOM element to a SlideNode.
     */
    private SlideNode convertElement(Element el, StyleResolver resolver, ComputedStyle parentStyle) {
        String tag = el.tagName().toLowerCase();
        ComputedStyle style = resolver.resolve(el, parentStyle);

        // Skip display:none elements
        if ("none".equals(style.getDisplay())) return null;

        return switch (tag) {
            case "div" -> convertBox(el, resolver, style);
            case "p", "h1", "h2", "h3", "h4", "h5", "h6" -> convertTextBlock(el, style);
            case "img" -> convertImage(el, style);
            case "table" -> convertTable(el, resolver, style);
            case "hr" -> new SlideNode.ShapeBlock("line", style);
            case "br", "style", "script", "link", "meta" -> null; // Skip
            case "span", "b", "strong", "i", "em", "u", "a" -> {
                // Inline elements at block level → treat as text block
                List<TextRun> runs = extractTextRuns(el, style);
                yield new SlideNode.TextBlock(runs, style);
            }
            case "ul", "ol" -> convertList(el, resolver, style);
            case "pre" -> convertCodeBlock(el, style);
            case "code" -> {
                // <code> at block level (not inside <pre>) → inline code style
                style.set("font-family", "Consolas");
                if (style.getBackgroundColor() == null) {
                    style.set("background-color", "#f5f5f5");
                }
                List<TextRun> runs = extractTextRuns(el, style);
                yield new SlideNode.TextBlock(runs, style);
            }
            default -> convertBox(el, resolver, style); // Unknown → treat as div
        };
    }

    private SlideNode.Box convertBox(Element el, StyleResolver resolver, ComputedStyle style) {
        List<SlideNode> children = convertChildren(el, resolver, style);
        return new SlideNode.Box(style, children);
    }

    private SlideNode.TextBlock convertTextBlock(Element el, ComputedStyle style) {
        List<TextRun> runs = extractTextRuns(el, style);
        return new SlideNode.TextBlock(runs, style);
    }

    /**
     * Convert a <pre> element (possibly containing <code>) to a code block.
     * Preserves whitespace and line breaks.
     */
    private SlideNode convertCodeBlock(Element el, ComputedStyle style) {
        // Apply code styling defaults
        if (style.getFontFamily() == null) {
            style.set("font-family", "Consolas");
        }
        if (style.getBackgroundColor() == null) {
            style.set("background-color", "#f5f5f5");
        }
        if (!style.has("padding-top")) {
            style.set("padding-top", "10px");
            style.set("padding-right", "15px");
            style.set("padding-bottom", "10px");
            style.set("padding-left", "15px");
        }
        if (!style.has("border-radius")) {
            style.set("border-radius", "4px");
        }
        if (!style.has("font-size")) {
            style.set("font-size", "12pt");
        }

        // Get the raw text preserving whitespace
        Element codeEl = el.selectFirst("code");
        String rawText = codeEl != null ? codeEl.wholeText() : el.wholeText();
        
        // Preserve whitespace but normalize line endings
        rawText = rawText.replace("\r\n", "\n").replace("\r", "\n");
        
        // Create text runs for each line
        List<TextRun> runs = new ArrayList<>();
        String fontFamily = style.getFontFamily() != null ? style.getFontFamily() : "Consolas";
        Double fontSize = style.getFontSize();
        String color = style.getColor();
        
        runs.add(new TextRun(rawText, false, false, false, color, fontSize, fontFamily, null));
        
        return new SlideNode.TextBlock(runs, style);
    }

    /**
     * Extract styled text runs from an element's content.
     * Handles inline formatting: <b>, <i>, <u>, <span>, plain text.
     */
    List<TextRun> extractTextRuns(Element el, ComputedStyle baseStyle) {
        List<TextRun> runs = new ArrayList<>();
        for (Node child : el.childNodes()) {
            if (child instanceof TextNode textNode) {
                String text = textNode.getWholeText();
                if (!text.trim().isEmpty()) {
                    runs.add(new TextRun(
                        text,
                        baseStyle.isBold(),
                        baseStyle.isItalic(),
                        baseStyle.isUnderline(),
                        baseStyle.getColor(),
                        baseStyle.getFontSize(),
                        baseStyle.getFontFamily()
                    ));
                }
            } else if (child instanceof Element inlineEl) {
                String inlineTag = inlineEl.tagName().toLowerCase();
                boolean bold = baseStyle.isBold();
                boolean italic = baseStyle.isItalic();
                boolean underline = baseStyle.isUnderline();
                String color = baseStyle.getColor();
                Double fontSize = baseStyle.getFontSize();
                String fontFamily = baseStyle.getFontFamily();
                String href = null;

                // Apply inline formatting
                switch (inlineTag) {
                    case "b", "strong" -> bold = true;
                    case "i", "em" -> italic = true;
                    case "u" -> underline = true;
                    case "a" -> {
                        href = inlineEl.attr("href");
                        underline = true;  // Links are underlined by default
                        if (color == null) color = "#0066cc";  // Default link color
                    }
                }

                // Check span/inline style overrides
                String inlineStyle = inlineEl.attr("style");
                if (!inlineStyle.isEmpty()) {
                    ComputedStyle spanStyle = new ComputedStyle();
                    spanStyle.mergeInline(inlineStyle);
                    if (spanStyle.getColor() != null) color = spanStyle.getColor();
                    if (spanStyle.getFontSize() != null) fontSize = spanStyle.getFontSize();
                    if (spanStyle.getFontFamily() != null) fontFamily = spanStyle.getFontFamily();
                    if (spanStyle.isBold()) bold = true;
                    if (spanStyle.isItalic()) italic = true;
                    if (spanStyle.isUnderline()) underline = true;
                }

                String text = inlineEl.text();
                if (!text.isEmpty()) {
                    runs.add(new TextRun(text, bold, italic, underline, color, fontSize, fontFamily, href));
                }
            }
        }
        return runs;
    }

    private SlideNode.ImageBlock convertImage(Element el, ComputedStyle style) {
        String src = el.attr("src");
        if (src.isEmpty()) {
            throw new IllegalArgumentException("<img> requires 'src' attribute");
        }
        
        // Resolve relative paths against basePath
        if (basePath != null && !src.startsWith("data:") && !src.startsWith("/") && !src.contains(":")) {
            src = basePath.resolve(src).toString();
        }
        
        return new SlideNode.ImageBlock(src, style);
    }

    private SlideNode.TableBlock convertTable(Element el, StyleResolver resolver, ComputedStyle style) {
        List<List<TableCell>> rows = new ArrayList<>();
        for (Element tr : el.select("tr")) {
            List<TableCell> row = new ArrayList<>();
            for (Element cell : tr.select("td, th")) {
                ComputedStyle cellStyle = resolver.resolve(cell, style);
                int colspan = parseIntAttr(cell, "colspan", 1);
                int rowspan = parseIntAttr(cell, "rowspan", 1);
                row.add(new TableCell(cell.text(), cellStyle, colspan, rowspan));
            }
            if (!row.isEmpty()) rows.add(row);
        }
        return new SlideNode.TableBlock(rows, style);
    }
    
    private int parseIntAttr(Element el, String attr, int defaultValue) {
        String val = el.attr(attr);
        if (val == null || val.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private SlideNode convertList(Element el, StyleResolver resolver, ComputedStyle style) {
        // Convert <ul>/<ol> to a Box containing TextBlock children for each <li>
        List<SlideNode> items = new ArrayList<>();
        boolean ordered = "ol".equals(el.tagName().toLowerCase());
        int index = 1;
        
        // Get list-style-type from the parent list element
        String listStyleType = style.get("list-style-type");
        
        for (Element li : el.select("> li")) {
            ComputedStyle liStyle = resolver.resolve(li, style);
            
            // Determine bullet character based on list-style-type
            String prefix;
            if (ordered) {
                prefix = index++ + ". ";
            } else {
                prefix = getBulletChar(listStyleType != null ? listStyleType : liStyle.get("list-style-type"));
            }
            
            String text = prefix + li.text();
            List<TextRun> runs = List.of(new TextRun(
                text,
                liStyle.isBold(),
                liStyle.isItalic(),
                liStyle.isUnderline(),
                liStyle.getColor(),
                liStyle.getFontSize(),
                liStyle.getFontFamily()
            ));
            items.add(new SlideNode.TextBlock(runs, liStyle));
        }
        // Lists stack items vertically
        if (!style.has("flex-direction")) {
            style.set("flex-direction", "column");
        }
        return new SlideNode.Box(style, items);
    }
    
    private String getBulletChar(String listStyleType) {
        if (listStyleType == null) return "• ";
        return switch (listStyleType.toLowerCase().trim()) {
            case "disc" -> "• ";
            case "circle" -> "○ ";
            case "square" -> "▪ ";
            case "none" -> "";
            case "dash" -> "— ";
            case "arrow" -> "→ ";
            case "check" -> "✓ ";
            case "star" -> "★ ";
            default -> {
                // Custom character: use as-is if single char or emoji
                if (listStyleType.startsWith("\"") && listStyleType.endsWith("\"")) {
                    yield listStyleType.substring(1, listStyleType.length() - 1) + " ";
                }
                yield "• ";
            }
        };
    }

    /**
     * Presentation metadata extracted from HTML.
     */
    public record PresentationMeta(String layout, String title, String author, String subject) {}
}
