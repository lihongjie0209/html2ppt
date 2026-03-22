package com.html2ppt.css;

import com.html2ppt.model.ComputedStyle;
import org.jsoup.nodes.Element;

import java.util.*;

/**
 * Resolves CSS rules to computed styles per element.
 * Handles cascade, specificity, inline styles, and inheritance.
 */
public class StyleResolver {

    private final List<CssParser.CssRule> rules;
    private final CssParser cssParser = new CssParser();

    public StyleResolver(List<CssParser.CssRule> rules) {
        this.rules = rules != null ? rules : List.of();
    }

    /**
     * Resolve the computed style for a DOM element, considering:
     * 1. Matching CSS rules (sorted by specificity)
     * 2. Inline style attribute
     * 3. Inherited properties from parent
     * 4. Element-type defaults (h1-h6 font sizes, bold)
     */
    public ComputedStyle resolve(Element element, ComputedStyle inheritedStyle) {
        // Start with inherited properties
        ComputedStyle computed = inheritedStyle != null ? inheritedStyle.inheritableSubset() : new ComputedStyle();

        // Apply element-type defaults
        applyDefaults(element.tagName().toLowerCase(), computed);

        // Collect matching rules, sorted by specificity
        List<MatchedRule> matched = new ArrayList<>();
        for (CssParser.CssRule rule : rules) {
            if (matchesSelector(element, rule.selector())) {
                int specificity = calculateSpecificity(rule.selector());
                matched.add(new MatchedRule(rule, specificity));
            }
        }
        matched.sort(Comparator.comparingInt(MatchedRule::specificity));

        // Apply rules in specificity order (lowest first, highest wins)
        for (MatchedRule mr : matched) {
            for (var entry : mr.rule().declarations().entrySet()) {
                computed.set(entry.getKey(), entry.getValue());
            }
        }

        // Apply inline style (highest priority)
        String inlineStyle = element.attr("style");
        if (!inlineStyle.isEmpty()) {
            Map<String, String> inlineDecls = cssParser.parseInline(inlineStyle);
            for (var entry : inlineDecls.entrySet()) {
                computed.set(entry.getKey(), entry.getValue());
            }
        }

        return computed;
    }

    /**
     * Check if a CSS selector matches a DOM element.
     * Supports: element, .class, #id, descendant, child (>), and combined selectors.
     */
    boolean matchesSelector(Element element, String selector) {
        selector = selector.trim();

        // Handle comma-separated selectors (should be pre-split by CssParser)
        if (selector.contains(",")) {
            return Arrays.stream(selector.split(","))
                .anyMatch(s -> matchesSelector(element, s.trim()));
        }

        // Handle child combinator: "parent > child"
        if (selector.contains(">")) {
            String[] parts = selector.split(">");
            if (parts.length == 2) {
                String childSel = parts[1].trim();
                String parentSel = parts[0].trim();
                return matchesSimple(element, childSel)
                    && element.parent() != null
                    && matchesSelector(element.parent(), parentSel);
            }
        }

        // Handle descendant combinator: "ancestor descendant"
        if (selector.contains(" ")) {
            String[] parts = selector.split("\\s+");
            String lastPart = parts[parts.length - 1];
            if (!matchesSimple(element, lastPart)) return false;

            // Walk up ancestors to match remaining parts
            String ancestorSelector = String.join(" ", Arrays.copyOf(parts, parts.length - 1));
            Element parent = element.parent();
            while (parent != null) {
                if (matchesSelector(parent, ancestorSelector)) return true;
                parent = parent.parent();
            }
            return false;
        }

        // Simple selector
        return matchesSimple(element, selector);
    }

    /**
     * Match a simple selector (no combinators): element, .class, #id, or combined.
     */
    private boolean matchesSimple(Element element, String selector) {
        selector = selector.trim();
        if (selector.isEmpty()) return false;

        // Parse the simple selector into parts
        String tagPart = null;
        List<String> classParts = new ArrayList<>();
        String idPart = null;

        int i = 0;
        while (i < selector.length()) {
            char c = selector.charAt(i);
            if (c == '.') {
                int start = i + 1;
                int end = findEnd(selector, start);
                classParts.add(selector.substring(start, end));
                i = end;
            } else if (c == '#') {
                int start = i + 1;
                int end = findEnd(selector, start);
                idPart = selector.substring(start, end);
                i = end;
            } else {
                int end = findEnd(selector, i);
                tagPart = selector.substring(i, end);
                i = end;
            }
        }

        // Check tag
        if (tagPart != null && !tagPart.equals("*") &&
            !tagPart.equalsIgnoreCase(element.tagName())) {
            return false;
        }

        // Check id
        if (idPart != null && !idPart.equals(element.id())) {
            return false;
        }

        // Check classes
        Set<String> elementClasses = element.classNames();
        for (String cls : classParts) {
            if (!elementClasses.contains(cls)) return false;
        }

        return true;
    }

    private int findEnd(String selector, int start) {
        int i = start;
        while (i < selector.length() && selector.charAt(i) != '.' && selector.charAt(i) != '#') {
            i++;
        }
        return i;
    }

    /**
     * Calculate CSS specificity for a selector.
     * Returns: ids*100 + classes*10 + elements*1
     */
    int calculateSpecificity(String selector) {
        int ids = 0, classes = 0, elements = 0;

        // Strip combinators, split into parts
        String[] tokens = selector.replaceAll("[>]", " ").trim().split("\\s+");
        for (String token : tokens) {
            int i = 0;
            while (i < token.length()) {
                char c = token.charAt(i);
                if (c == '#') {
                    ids++;
                    i = findEnd(token, i + 1);
                } else if (c == '.') {
                    classes++;
                    i = findEnd(token, i + 1);
                } else {
                    int end = findEnd(token, i);
                    String part = token.substring(i, end);
                    if (!part.isEmpty() && !part.equals("*")) {
                        elements++;
                    }
                    i = end;
                }
            }
        }

        return ids * 100 + classes * 10 + elements;
    }

    /**
     * Apply element-type default styles (h1-h6 sizes, bold).
     */
    private void applyDefaults(String tagName, ComputedStyle style) {
        switch (tagName) {
            case "h1" -> { style.set("font-size", "36pt"); style.set("font-weight", "bold"); }
            case "h2" -> { style.set("font-size", "28pt"); style.set("font-weight", "bold"); }
            case "h3" -> { style.set("font-size", "24pt"); style.set("font-weight", "bold"); }
            case "h4" -> { style.set("font-size", "20pt"); style.set("font-weight", "bold"); }
            case "h5" -> { style.set("font-size", "18pt"); style.set("font-weight", "bold"); }
            case "h6" -> { style.set("font-size", "16pt"); style.set("font-weight", "bold"); }
            case "p" -> style.set("font-size", "18pt");
            case "th" -> style.set("font-weight", "bold");
        }
    }

    private record MatchedRule(CssParser.CssRule rule, int specificity) {}
}
