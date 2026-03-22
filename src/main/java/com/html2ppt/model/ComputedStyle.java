package com.html2ppt.model;

import java.util.*;

/**
 * Resolved CSS properties for an element.
 * Populated by StyleResolver after cascade, specificity, and inheritance.
 */
public class ComputedStyle {

    private final Map<String, String> properties = new LinkedHashMap<>();

    public void set(String property, String value) {
        if (value != null && !value.isBlank()) {
            properties.put(property, value.trim());
        }
    }

    public String get(String property) {
        return properties.get(property);
    }

    public String get(String property, String defaultValue) {
        return properties.getOrDefault(property, defaultValue);
    }

    public boolean has(String property) {
        return properties.containsKey(property);
    }

    // ─── Typed accessors ────────────────────────────────────────────

    public Double getLength(String property) {
        String val = get(property);
        if (val == null) return null;
        return parseLength(val);
    }

    public double getLength(String property, double defaultValue) {
        Double v = getLength(property);
        return v != null ? v : defaultValue;
    }

    public boolean isBold() {
        String fw = get("font-weight");
        if (fw == null) return false;
        return "bold".equals(fw) || "700".equals(fw) || "800".equals(fw) || "900".equals(fw);
    }

    public boolean isItalic() {
        return "italic".equals(get("font-style"));
    }

    public boolean isUnderline() {
        String td = get("text-decoration");
        return td != null && td.contains("underline");
    }

    public String getColor() {
        return normalizeColor(get("color"));
    }

    public String getBackgroundColor() {
        String value = get("background-color");
        // Check shorthand 'background' property if 'background-color' not set
        if (value == null) {
            value = get("background");
            // If background is a gradient, return null (handled separately)
            if (value != null && value.contains("gradient")) {
                return null;
            }
        }
        // Don't normalize rgba/rgb - let caller handle with parseColorWithAlpha
        if (value != null && (value.startsWith("rgba(") || value.startsWith("rgb("))) {
            return value;
        }
        return normalizeColor(value);
    }
    
    /**
     * Get object-fit value for images.
     * @return "fill", "contain", "cover", "none", "scale-down", or null
     */
    public String getObjectFit() {
        return get("object-fit");
    }
    
    /**
     * Get object-position value for images (e.g., "center", "top left", "50% 50%").
     * @return position string or null
     */
    public String getObjectPosition() {
        return get("object-position");
    }

    public Double getFontSize() {
        return getLength("font-size");
    }

    public double getFontSize(double defaultValue) {
        Double v = getFontSize();
        return v != null ? v : defaultValue;
    }

    public String getFontFamily() {
        return get("font-family");
    }

    public String getTextAlign() {
        return get("text-align");
    }

    public String getVerticalAlign() {
        return get("vertical-align");
    }

    public String getDisplay() {
        return get("display", "flex");
    }

    public String getFlexDirection() {
        return get("flex-direction", "row");
    }

    public String getJustifyContent() {
        return get("justify-content", "flex-start");
    }

    public String getAlignItems() {
        return get("align-items", "stretch");
    }

    public String getFlexWrap() {
        return get("flex-wrap", "nowrap");
    }

    public String getPosition() {
        return get("position", "relative");
    }

    /**
     * Get opacity (0.0 - 1.0). Default is 1.0 (fully opaque).
     */
    public double getOpacity() {
        String value = get("opacity");
        if (value == null) return 1.0;
        try {
            return Math.max(0.0, Math.min(1.0, Double.parseDouble(value)));
        } catch (NumberFormatException e) {
            return 1.0;
        }
    }

    /**
     * Create a copy for inheritance.
     * Only inheritable properties are carried over.
     */
    public ComputedStyle inheritableSubset() {
        ComputedStyle child = new ComputedStyle();
        for (String prop : INHERITABLE_PROPERTIES) {
            String val = get(prop);
            if (val != null) child.set(prop, val);
        }
        return child;
    }

    /**
     * Merge another style on top of this one (later values win).
     */
    public void merge(ComputedStyle other) {
        if (other != null) {
            properties.putAll(other.properties);
        }
    }

    /**
     * Merge inline style string (e.g. "color: red; font-size: 18px").
     */
    public void mergeInline(String inlineStyle) {
        if (inlineStyle == null || inlineStyle.isBlank()) return;
        for (String decl : inlineStyle.split(";")) {
            String trimmed = decl.trim();
            if (trimmed.isEmpty()) continue;
            int colon = trimmed.indexOf(':');
            if (colon > 0) {
                String prop = trimmed.substring(0, colon).trim();
                String val = trimmed.substring(colon + 1).trim();
                set(prop, val);
            }
        }
    }

    public Map<String, String> asMap() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public String toString() {
        return properties.toString();
    }

    // ─── Static utilities ───────────────────────────────────────────

    private static final Set<String> INHERITABLE_PROPERTIES = Set.of(
        "color", "font-size", "font-weight", "font-style", "font-family",
        "text-align", "line-height", "text-decoration"
    );

    /**
     * Parse a CSS length value to points.
     * Supports: px (=pt), pt, plain numbers.
     */
    public static Double parseLength(String value) {
        if (value == null || value.isBlank()) return null;
        value = value.trim().toLowerCase();
        if ("auto".equals(value) || "none".equals(value)) return null;

        try {
            if (value.endsWith("pt")) {
                return Double.parseDouble(value.substring(0, value.length() - 2).trim());
            } else if (value.endsWith("px")) {
                return Double.parseDouble(value.substring(0, value.length() - 2).trim());
            } else if (value.endsWith("rem")) {
                return Double.parseDouble(value.substring(0, value.length() - 3).trim()) * 18.0;
            } else if (value.endsWith("em")) {
                // Treat em as multiplier of 18pt (default font size)
                return Double.parseDouble(value.substring(0, value.length() - 2).trim()) * 18.0;
            } else if (value.endsWith("%")) {
                return null; // Percentage handled separately by Yoga
            } else {
                return Double.parseDouble(value);
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parse a percentage value (e.g. "50%") to a float (0.5).
     * Returns null if not a percentage.
     */
    public static Float parsePercentage(String value) {
        if (value == null || !value.trim().endsWith("%")) return null;
        try {
            return Float.parseFloat(value.trim().replace("%", "")) / 100f;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Normalize a CSS color value to 6-char hex (without #).
     */
    public static String normalizeColor(String color) {
        if (color == null) return null;
        color = color.trim();
        if (color.isEmpty()) return null;

        // Named colors
        if (TRANSPARENT_COLORS.contains(color.toLowerCase())) return color;
        String named = NAMED_COLORS.get(color.toLowerCase());
        if (named != null) return named;

        // #RGB → RRGGBB
        if (color.startsWith("#") && color.length() == 4) {
            char r = color.charAt(1), g = color.charAt(2), b = color.charAt(3);
            return "" + r + r + g + g + b + b;
        }

        // #RRGGBB → RRGGBB
        if (color.startsWith("#") && color.length() == 7) {
            return color.substring(1).toUpperCase();
        }

        // rgb(r, g, b)
        if (color.startsWith("rgb(") && color.endsWith(")")) {
            String inner = color.substring(4, color.length() - 1);
            String[] parts = inner.split(",");
            if (parts.length == 3) {
                try {
                    int r = Integer.parseInt(parts[0].trim());
                    int g = Integer.parseInt(parts[1].trim());
                    int b = Integer.parseInt(parts[2].trim());
                    return String.format("%02X%02X%02X", r, g, b);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        // Already 6-char hex?
        if (color.length() == 6 && color.matches("[0-9A-Fa-f]{6}")) {
            return color.toUpperCase();
        }

        return null;
    }

    /**
     * A parsed color with RGB hex and optional alpha (0.0-1.0).
     */
    public record ParsedColor(String hex, double alpha) {
        public ParsedColor(String hex) {
            this(hex, 1.0);
        }
    }

    /**
     * Parse a CSS color value to hex + alpha.
     * Supports: hex, rgb(), rgba(), named colors.
     */
    public static ParsedColor parseColorWithAlpha(String color) {
        if (color == null) return null;
        color = color.trim();
        if (color.isEmpty()) return null;

        // Handle transparent
        if ("transparent".equalsIgnoreCase(color)) {
            return new ParsedColor("FFFFFF", 0.0);
        }

        // Named colors
        String named = NAMED_COLORS.get(color.toLowerCase());
        if (named != null) return new ParsedColor(named);

        // #RGB → RRGGBB
        if (color.startsWith("#") && color.length() == 4) {
            char r = color.charAt(1), g = color.charAt(2), b = color.charAt(3);
            return new ParsedColor("" + r + r + g + g + b + b);
        }

        // #RRGGBB
        if (color.startsWith("#") && color.length() == 7) {
            return new ParsedColor(color.substring(1).toUpperCase());
        }

        // rgba(r, g, b, a)
        if (color.startsWith("rgba(") && color.endsWith(")")) {
            String inner = color.substring(5, color.length() - 1);
            String[] parts = inner.split(",");
            if (parts.length == 4) {
                try {
                    int r = Integer.parseInt(parts[0].trim());
                    int g = Integer.parseInt(parts[1].trim());
                    int b = Integer.parseInt(parts[2].trim());
                    double a = Double.parseDouble(parts[3].trim());
                    return new ParsedColor(String.format("%02X%02X%02X", r, g, b), a);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        // rgb(r, g, b)
        if (color.startsWith("rgb(") && color.endsWith(")")) {
            String inner = color.substring(4, color.length() - 1);
            String[] parts = inner.split(",");
            if (parts.length == 3) {
                try {
                    int r = Integer.parseInt(parts[0].trim());
                    int g = Integer.parseInt(parts[1].trim());
                    int b = Integer.parseInt(parts[2].trim());
                    return new ParsedColor(String.format("%02X%02X%02X", r, g, b));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        // Already 6-char hex?
        if (color.length() == 6 && color.matches("[0-9A-Fa-f]{6}")) {
            return new ParsedColor(color.toUpperCase());
        }

        return null;
    }

    private static final Map<String, String> NAMED_COLORS = Map.ofEntries(
        Map.entry("black", "000000"),
        Map.entry("white", "FFFFFF"),
        Map.entry("red", "FF0000"),
        Map.entry("green", "008000"),
        Map.entry("blue", "0000FF"),
        Map.entry("yellow", "FFFF00"),
        Map.entry("cyan", "00FFFF"),
        Map.entry("magenta", "FF00FF"),
        Map.entry("gray", "808080"),
        Map.entry("grey", "808080"),
        Map.entry("silver", "C0C0C0"),
        Map.entry("orange", "FFA500"),
        Map.entry("purple", "800080"),
        Map.entry("navy", "000080"),
        Map.entry("teal", "008080"),
        Map.entry("maroon", "800000"),
        Map.entry("lime", "00FF00"),
        Map.entry("aqua", "00FFFF"),
        Map.entry("fuchsia", "FF00FF"),
        Map.entry("olive", "808000")
    );

    // Handle special color values that don't map to hex
    private static final Set<String> TRANSPARENT_COLORS = Set.of("transparent", "inherit", "initial");

    // ─── Box Shadow Support ─────────────────────────────────────────

    /**
     * Parsed box-shadow value.
     * CSS: box-shadow: offsetX offsetY blurRadius spreadRadius color [inset]
     * We support: offsetX offsetY blurRadius color (spread and inset not supported in PPT)
     */
    public record BoxShadow(double offsetX, double offsetY, double blur, String colorHex, double alpha) {
        /** Distance from origin (for POI angle calculation) */
        public double distance() {
            return Math.sqrt(offsetX * offsetX + offsetY * offsetY);
        }

        /** Angle in degrees (for POI shadow direction) */
        public double angleDegrees() {
            // POI uses angle where 0° is right, clockwise
            // atan2(y, x) gives angle from positive x-axis
            return Math.toDegrees(Math.atan2(offsetY, offsetX));
        }
    }

    /**
     * Parse CSS box-shadow value.
     * Supports: "5px 10px 15px #000", "5px 10px rgba(0,0,0,0.5)", "5px 10px 15px black"
     */
    public static BoxShadow parseBoxShadow(String value) {
        if (value == null || value.isBlank() || "none".equalsIgnoreCase(value.trim())) {
            return null;
        }
        value = value.trim();

        // Skip "inset" keyword (not supported)
        if (value.startsWith("inset")) return null;

        // Parse by extracting color first (it could contain spaces like "rgb()")
        String colorPart = null;
        String lengthPart = value;

        // Check for rgba/rgb at the end
        int rgbaIdx = value.lastIndexOf("rgba(");
        int rgbIdx = value.lastIndexOf("rgb(");
        int colorFuncIdx = Math.max(rgbaIdx, rgbIdx);

        if (colorFuncIdx >= 0) {
            colorPart = value.substring(colorFuncIdx);
            lengthPart = value.substring(0, colorFuncIdx).trim();
        } else {
            // Look for color at end (hex or named)
            String[] tokens = value.split("\\s+");
            if (tokens.length >= 3) {
                String lastToken = tokens[tokens.length - 1];
                // Check if last token is a color (not a length)
                if (lastToken.startsWith("#") || NAMED_COLORS.containsKey(lastToken.toLowerCase())) {
                    colorPart = lastToken;
                    lengthPart = value.substring(0, value.lastIndexOf(lastToken)).trim();
                }
            }
        }

        // Parse lengths: offsetX offsetY [blur] [spread]
        String[] lengths = lengthPart.split("\\s+");
        if (lengths.length < 2) return null;

        Double offsetX = parseLength(lengths[0]);
        Double offsetY = parseLength(lengths[1]);
        Double blur = lengths.length > 2 ? parseLength(lengths[2]) : 0.0;
        // spread (lengths[3]) is not supported in PPT

        if (offsetX == null || offsetY == null) return null;
        if (blur == null) blur = 0.0;

        // Parse color
        ParsedColor parsed = colorPart != null ? parseColorWithAlpha(colorPart) : null;
        String colorHex = parsed != null ? parsed.hex() : "000000";
        double alpha = parsed != null ? parsed.alpha() : 0.5; // Default 50% opacity shadow

        return new BoxShadow(offsetX, offsetY, blur, colorHex, alpha);
    }

    /**
     * Get parsed box-shadow from style.
     */
    public BoxShadow getBoxShadow() {
        return parseBoxShadow(get("box-shadow"));
    }

    // ─── Linear Gradient Support ────────────────────────────────────

    /**
     * A parsed linear gradient with angle and color stops.
     */
    public record LinearGradient(double angleDegrees, List<GradientStop> stops) {}

    /**
     * A gradient color stop with position (0.0-1.0) and color.
     */
    public record GradientStop(double position, String colorHex, double alpha) {}

    /**
     * Parse CSS linear-gradient value.
     * Supports: linear-gradient(direction, color1, color2, ...)
     * Direction can be: to right, to bottom, 45deg, etc.
     */
    public static LinearGradient parseLinearGradient(String value) {
        if (value == null || !value.trim().toLowerCase().startsWith("linear-gradient(")) {
            return null;
        }
        value = value.trim();
        
        // Extract content inside linear-gradient(...)
        int start = value.indexOf('(');
        int end = value.lastIndexOf(')');
        if (start < 0 || end <= start) return null;
        
        String content = value.substring(start + 1, end).trim();
        
        // Split by commas (but careful with rgba() which contains commas)
        List<String> parts = splitGradientParts(content);
        if (parts.size() < 2) return null;
        
        // First part might be direction/angle, or it could be a color
        double angle = 180; // Default: to bottom
        int colorStartIndex = 0;
        
        String firstPart = parts.get(0).trim().toLowerCase();
        if (firstPart.endsWith("deg")) {
            // Explicit angle like "45deg"
            try {
                angle = Double.parseDouble(firstPart.substring(0, firstPart.length() - 3).trim());
            } catch (NumberFormatException e) {
                angle = 180;
            }
            colorStartIndex = 1;
        } else if (firstPart.startsWith("to ")) {
            // Direction keyword like "to right", "to bottom right"
            angle = parseGradientDirection(firstPart);
            colorStartIndex = 1;
        }
        // Otherwise first part is a color
        
        // Parse color stops
        List<GradientStop> stops = new ArrayList<>();
        int colorCount = parts.size() - colorStartIndex;
        
        for (int i = colorStartIndex; i < parts.size(); i++) {
            String stopPart = parts.get(i).trim();
            
            // Check if there's a position like "red 50%"
            double position = -1;
            String colorPart = stopPart;
            
            // Try to extract position (at end, like "red 50%")
            int lastSpace = stopPart.lastIndexOf(' ');
            if (lastSpace > 0) {
                String maybePct = stopPart.substring(lastSpace + 1).trim();
                Float pct = parsePercentage(maybePct);
                if (pct != null) {
                    position = pct;
                    colorPart = stopPart.substring(0, lastSpace).trim();
                }
            }
            
            // Parse color
            ParsedColor parsed = parseColorWithAlpha(colorPart);
            if (parsed == null) continue;
            
            // If no explicit position, distribute evenly
            if (position < 0) {
                int idx = i - colorStartIndex;
                position = colorCount > 1 ? (double) idx / (colorCount - 1) : 0;
            }
            
            stops.add(new GradientStop(position, parsed.hex(), parsed.alpha()));
        }
        
        if (stops.size() < 2) return null;
        
        return new LinearGradient(angle, stops);
    }

    /**
     * Parse direction keyword to angle.
     * CSS: to top = 0deg, to right = 90deg, to bottom = 180deg, to left = 270deg
     */
    private static double parseGradientDirection(String direction) {
        direction = direction.toLowerCase().trim();
        return switch (direction) {
            case "to top" -> 0;
            case "to top right", "to right top" -> 45;
            case "to right" -> 90;
            case "to bottom right", "to right bottom" -> 135;
            case "to bottom" -> 180;
            case "to bottom left", "to left bottom" -> 225;
            case "to left" -> 270;
            case "to top left", "to left top" -> 315;
            default -> 180; // Default to bottom
        };
    }

    /**
     * Split gradient content by commas, but preserve rgba() function calls.
     */
    private static List<String> splitGradientParts(String content) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        
        for (char c : content.toCharArray()) {
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) {
                parts.add(current.toString().trim());
                current = new StringBuilder();
                continue;
            }
            current.append(c);
        }
        
        if (!current.isEmpty()) {
            parts.add(current.toString().trim());
        }
        
        return parts;
    }

    /**
     * Get parsed linear-gradient from background or background-image.
     */
    public LinearGradient getLinearGradient() {
        String bg = get("background");
        if (bg != null && bg.toLowerCase().contains("linear-gradient")) {
            return parseLinearGradient(bg);
        }
        String bgImg = get("background-image");
        if (bgImg != null) {
            return parseLinearGradient(bgImg);
        }
        return null;
    }
}
