package com.html2ppt.css;

import com.helger.css.ECSSVersion;
import com.helger.css.decl.*;
import com.helger.css.reader.CSSReader;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses CSS from &lt;style&gt; blocks using ph-css.
 * Returns structured rule lists for use by StyleResolver.
 */
public class CssParser {

    /**
     * A parsed CSS rule: selector string + property declarations.
     */
    public record CssRule(String selector, Map<String, String> declarations) {}

    /**
     * Parse a CSS string (content of a &lt;style&gt; block) into a list of rules.
     */
    public List<CssRule> parse(String cssText) {
        List<CssRule> rules = new ArrayList<>();
        if (cssText == null || cssText.isBlank()) return rules;

        CascadingStyleSheet css = CSSReader.readFromString(cssText, StandardCharsets.UTF_8, ECSSVersion.CSS30);
        if (css == null) return rules;

        for (CSSStyleRule styleRule : css.getAllStyleRules()) {
            Map<String, String> declarations = new LinkedHashMap<>();
            for (CSSDeclaration decl : styleRule.getAllDeclarations()) {
                String property = decl.getProperty();
                String value = decl.getExpression().getAsCSSString();
                declarations.put(property, value);
            }

            // Each selector in a rule group generates a separate CssRule
            for (CSSSelector selector : styleRule.getAllSelectors()) {
                String selectorText = selector.getAsCSSString();
                // Expand shorthand properties
                Map<String, String> expanded = expandShorthands(declarations);
                rules.add(new CssRule(selectorText.trim(), expanded));
            }
        }

        return rules;
    }

    /**
     * Parse an inline style string (e.g. "color: red; font-size: 18px") into declarations.
     */
    public Map<String, String> parseInline(String inlineStyle) {
        Map<String, String> declarations = new LinkedHashMap<>();
        if (inlineStyle == null || inlineStyle.isBlank()) return declarations;

        for (String decl : inlineStyle.split(";")) {
            String trimmed = decl.trim();
            if (trimmed.isEmpty()) continue;
            int colon = trimmed.indexOf(':');
            if (colon > 0) {
                String prop = trimmed.substring(0, colon).trim();
                String val = trimmed.substring(colon + 1).trim();
                declarations.put(prop, val);
            }
        }
        return expandShorthands(declarations);
    }

    /**
     * Expand CSS shorthand properties into their longhand equivalents.
     */
    static Map<String, String> expandShorthands(Map<String, String> declarations) {
        Map<String, String> expanded = new LinkedHashMap<>(declarations);

        // flex shorthand: flex: <grow> [<shrink>] [<basis>]
        if (expanded.containsKey("flex")) {
            String flex = expanded.remove("flex");
            expandFlex(flex, expanded);
        }

        // margin shorthand
        expandBoxShorthand("margin", expanded);

        // padding shorthand
        expandBoxShorthand("padding", expanded);

        // gap shorthand → row-gap + column-gap
        if (expanded.containsKey("gap")) {
            String gap = expanded.remove("gap");
            String[] parts = gap.trim().split("\\s+");
            if (parts.length == 1) {
                expanded.putIfAbsent("row-gap", parts[0]);
                expanded.putIfAbsent("column-gap", parts[0]);
            } else if (parts.length >= 2) {
                expanded.putIfAbsent("row-gap", parts[0]);
                expanded.putIfAbsent("column-gap", parts[1]);
            }
        }

        // border shorthand: border: <width> <style> <color>
        if (expanded.containsKey("border")) {
            String border = expanded.remove("border");
            expandBorder(border, expanded);
        }

        return expanded;
    }

    private static void expandFlex(String flex, Map<String, String> out) {
        flex = flex.trim();
        if ("none".equals(flex)) {
            out.putIfAbsent("flex-grow", "0");
            out.putIfAbsent("flex-shrink", "0");
            out.putIfAbsent("flex-basis", "auto");
            return;
        }
        if ("auto".equals(flex)) {
            out.putIfAbsent("flex-grow", "1");
            out.putIfAbsent("flex-shrink", "1");
            out.putIfAbsent("flex-basis", "auto");
            return;
        }

        String[] parts = flex.split("\\s+");
        if (parts.length >= 1) out.putIfAbsent("flex-grow", parts[0]);
        if (parts.length >= 2) out.putIfAbsent("flex-shrink", parts[1]);
        if (parts.length >= 3) out.putIfAbsent("flex-basis", parts[2]);
        else {
            // Single number: flex: N → grow=N, shrink=1, basis=0
            try {
                Double.parseDouble(parts[0]);
                out.putIfAbsent("flex-shrink", "1");
                out.putIfAbsent("flex-basis", "0");
            } catch (NumberFormatException e) {
                // Not a number, might be basis like "100px"
                out.putIfAbsent("flex-basis", parts[0]);
            }
        }
    }

    private static void expandBoxShorthand(String property, Map<String, String> out) {
        if (!out.containsKey(property)) return;
        String value = out.remove(property);
        String[] parts = value.trim().split("\\s+");
        switch (parts.length) {
            case 1 -> {
                out.putIfAbsent(property + "-top", parts[0]);
                out.putIfAbsent(property + "-right", parts[0]);
                out.putIfAbsent(property + "-bottom", parts[0]);
                out.putIfAbsent(property + "-left", parts[0]);
            }
            case 2 -> {
                out.putIfAbsent(property + "-top", parts[0]);
                out.putIfAbsent(property + "-right", parts[1]);
                out.putIfAbsent(property + "-bottom", parts[0]);
                out.putIfAbsent(property + "-left", parts[1]);
            }
            case 3 -> {
                out.putIfAbsent(property + "-top", parts[0]);
                out.putIfAbsent(property + "-right", parts[1]);
                out.putIfAbsent(property + "-bottom", parts[2]);
                out.putIfAbsent(property + "-left", parts[1]);
            }
            default -> {
                out.putIfAbsent(property + "-top", parts[0]);
                out.putIfAbsent(property + "-right", parts[1]);
                out.putIfAbsent(property + "-bottom", parts[2]);
                out.putIfAbsent(property + "-left", parts[3]);
            }
        }
    }

    private static void expandBorder(String border, Map<String, String> out) {
        String[] parts = border.trim().split("\\s+");
        for (String part : parts) {
            if (part.endsWith("px") || part.endsWith("pt") || part.matches("\\d+(\\.\\d+)?")) {
                out.putIfAbsent("border-width", part);
            } else if (part.startsWith("#") || part.startsWith("rgb(") || isNamedColor(part)) {
                out.putIfAbsent("border-color", part);
            }
            // Skip border-style (solid, dashed) — PPT only supports solid effectively
        }
    }

    private static boolean isNamedColor(String value) {
        return switch (value.toLowerCase()) {
            case "black", "white", "red", "green", "blue", "yellow", "orange",
                 "purple", "gray", "grey", "silver", "navy", "teal" -> true;
            default -> false;
        };
    }
}
