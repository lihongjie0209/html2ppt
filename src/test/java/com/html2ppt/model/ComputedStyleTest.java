package com.html2ppt.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ComputedStyleTest {

    @Nested
    class SetAndGet {
        @Test
        void setAndGetProperty() {
            var style = new ComputedStyle();
            style.set("color", "red");
            assertThat(style.get("color")).isEqualTo("red");
        }

        @Test
        void getReturnsNullForMissing() {
            var style = new ComputedStyle();
            assertThat(style.get("color")).isNull();
        }

        @Test
        void getWithDefault() {
            var style = new ComputedStyle();
            assertThat(style.get("color", "black")).isEqualTo("black");
            style.set("color", "red");
            assertThat(style.get("color", "black")).isEqualTo("red");
        }

        @Test
        void hasProperty() {
            var style = new ComputedStyle();
            assertThat(style.has("color")).isFalse();
            style.set("color", "red");
            assertThat(style.has("color")).isTrue();
        }

        @Test
        void setIgnoresNullAndBlank() {
            var style = new ComputedStyle();
            style.set("color", null);
            assertThat(style.has("color")).isFalse();
            style.set("color", "  ");
            assertThat(style.has("color")).isFalse();
        }

        @Test
        void setTrimsValue() {
            var style = new ComputedStyle();
            style.set("color", "  red  ");
            assertThat(style.get("color")).isEqualTo("red");
        }

        @Test
        void asMapReturnsUnmodifiableCopy() {
            var style = new ComputedStyle();
            style.set("color", "red");
            Map<String, String> map = style.asMap();
            assertThat(map).containsEntry("color", "red");
            assertThatThrownBy(() -> map.put("a", "b"))
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class FontProperties {
        @Test
        void isBoldWithKeyword() {
            var style = new ComputedStyle();
            assertThat(style.isBold()).isFalse();
            style.set("font-weight", "bold");
            assertThat(style.isBold()).isTrue();
        }

        @Test
        void isBoldWithNumericWeight() {
            var style = new ComputedStyle();
            style.set("font-weight", "700");
            assertThat(style.isBold()).isTrue();
            style.set("font-weight", "400");
            assertThat(style.isBold()).isFalse();
        }

        @Test
        void isItalic() {
            var style = new ComputedStyle();
            assertThat(style.isItalic()).isFalse();
            style.set("font-style", "italic");
            assertThat(style.isItalic()).isTrue();
        }

        @Test
        void isUnderline() {
            var style = new ComputedStyle();
            assertThat(style.isUnderline()).isFalse();
            style.set("text-decoration", "underline");
            assertThat(style.isUnderline()).isTrue();
        }

        @Test
        void getFontSize() {
            var style = new ComputedStyle();
            assertThat(style.getFontSize()).isNull();
            style.set("font-size", "24px");
            assertThat(style.getFontSize()).isEqualTo(24.0);
        }

        @Test
        void getFontSizeWithDefault() {
            var style = new ComputedStyle();
            assertThat(style.getFontSize(18)).isEqualTo(18.0);
            style.set("font-size", "24px");
            assertThat(style.getFontSize(18)).isEqualTo(24.0);
        }

        @Test
        void getFontFamily() {
            var style = new ComputedStyle();
            assertThat(style.getFontFamily()).isNull();
            style.set("font-family", "Arial");
            assertThat(style.getFontFamily()).isEqualTo("Arial");
        }
    }

    @Nested
    class LayoutProperties {
        @Test
        void displayDefaultsFlex() {
            var style = new ComputedStyle();
            assertThat(style.getDisplay()).isEqualTo("flex");
        }

        @Test
        void flexDirectionDefaultsRow() {
            var style = new ComputedStyle();
            assertThat(style.getFlexDirection()).isEqualTo("row");
        }

        @Test
        void justifyContentDefaultsFlexStart() {
            var style = new ComputedStyle();
            assertThat(style.getJustifyContent()).isEqualTo("flex-start");
        }

        @Test
        void alignItemsDefaultsStretch() {
            var style = new ComputedStyle();
            assertThat(style.getAlignItems()).isEqualTo("stretch");
        }

        @Test
        void flexWrapDefaultsNowrap() {
            var style = new ComputedStyle();
            assertThat(style.getFlexWrap()).isEqualTo("nowrap");
        }

        @Test
        void positionDefaultsRelative() {
            var style = new ComputedStyle();
            assertThat(style.getPosition()).isEqualTo("relative");
        }

        @Test
        void getTextAlign() {
            var style = new ComputedStyle();
            assertThat(style.getTextAlign()).isNull();
            style.set("text-align", "center");
            assertThat(style.getTextAlign()).isEqualTo("center");
        }

        @Test
        void getVerticalAlign() {
            var style = new ComputedStyle();
            assertThat(style.getVerticalAlign()).isNull();
            style.set("vertical-align", "middle");
            assertThat(style.getVerticalAlign()).isEqualTo("middle");
        }
    }

    @Nested
    class ParseLength {
        @Test
        void parsePx() {
            assertThat(ComputedStyle.parseLength("24px")).isEqualTo(24.0);
        }

        @Test
        void parsePt() {
            assertThat(ComputedStyle.parseLength("18pt")).isEqualTo(18.0);
        }

        @Test
        void parseEm() {
            // em = value * 18 (default font size)
            assertThat(ComputedStyle.parseLength("2em")).isEqualTo(36.0);
        }

        @Test
        void parseRem() {
            assertThat(ComputedStyle.parseLength("1.5rem")).isEqualTo(27.0);
        }

        @Test
        void parseBareNumber() {
            assertThat(ComputedStyle.parseLength("12")).isEqualTo(12.0);
        }

        @Test
        void parseNullAndInvalid() {
            assertThat(ComputedStyle.parseLength(null)).isNull();
            assertThat(ComputedStyle.parseLength("auto")).isNull();
            assertThat(ComputedStyle.parseLength("")).isNull();
        }

        @Test
        void getLengthFromStyle() {
            var style = new ComputedStyle();
            style.set("padding-top", "10px");
            assertThat(style.getLength("padding-top")).isEqualTo(10.0);
            assertThat(style.getLength("padding-bottom")).isNull();
            assertThat(style.getLength("padding-bottom", 0)).isEqualTo(0.0);
        }
    }

    @Nested
    class ParsePercentage {
        @Test
        void parseValidPercentage() {
            assertThat(ComputedStyle.parsePercentage("50%")).isEqualTo(0.5f);
            assertThat(ComputedStyle.parsePercentage("100%")).isEqualTo(1.0f);
        }

        @Test
        void parseNonPercentage() {
            assertThat(ComputedStyle.parsePercentage("50px")).isNull();
            assertThat(ComputedStyle.parsePercentage(null)).isNull();
        }
    }

    @Nested
    class NormalizeColor {
        @Test
        void hex6Passthrough() {
            assertThat(ComputedStyle.normalizeColor("#FF0000")).isEqualTo("FF0000");
        }

        @Test
        void hex3Expansion() {
            assertThat(ComputedStyle.normalizeColor("#F00")).isEqualTo("FF0000");
        }

        @Test
        void rgbFunction() {
            assertThat(ComputedStyle.normalizeColor("rgb(255, 0, 128)")).isEqualTo("FF0080");
        }

        @Test
        void namedColors() {
            assertThat(ComputedStyle.normalizeColor("red")).isEqualTo("FF0000");
            assertThat(ComputedStyle.normalizeColor("white")).isEqualTo("FFFFFF");
            assertThat(ComputedStyle.normalizeColor("black")).isEqualTo("000000");
            assertThat(ComputedStyle.normalizeColor("blue")).isEqualTo("0000FF");
        }

        @Test
        void nullReturnsNull() {
            assertThat(ComputedStyle.normalizeColor(null)).isNull();
        }

        @Test
        void unknownReturnsAsIs() {
            assertThat(ComputedStyle.normalizeColor("transparent")).isEqualTo("transparent");
        }

        @Test
        void unknownColorReturnsNull() {
            assertThat(ComputedStyle.normalizeColor("notacolor")).isNull();
        }
    }

    @Nested
    class ParseColorWithAlpha {
        @Test
        void rgbaWithSpaces() {
            var c = ComputedStyle.parseColorWithAlpha("rgba(76, 175, 80, 0.5)");
            assertThat(c).isNotNull();
            assertThat(c.hex()).isEqualTo("4CAF50");
            assertThat(c.alpha()).isCloseTo(0.5, within(0.01));
        }

        @Test
        void rgbaWithoutSpaces() {
            var c = ComputedStyle.parseColorWithAlpha("rgba(76,175,80,1)");
            assertThat(c).isNotNull();
            assertThat(c.hex()).isEqualTo("4CAF50");
            assertThat(c.alpha()).isCloseTo(1.0, within(0.01));
        }

        @Test
        void rgbaZeroAlpha() {
            var c = ComputedStyle.parseColorWithAlpha("rgba(255,0,0,0)");
            assertThat(c).isNotNull();
            assertThat(c.hex()).isEqualTo("FF0000");
            assertThat(c.alpha()).isCloseTo(0.0, within(0.01));
        }

        @Test
        void transparent() {
            var c = ComputedStyle.parseColorWithAlpha("transparent");
            assertThat(c).isNotNull();
            assertThat(c.alpha()).isCloseTo(0.0, within(0.01));
        }

        @Test
        void hexColor() {
            var c = ComputedStyle.parseColorWithAlpha("#FF5500");
            assertThat(c).isNotNull();
            assertThat(c.hex()).isEqualTo("FF5500");
            assertThat(c.alpha()).isCloseTo(1.0, within(0.01));
        }
    }

    @Nested
    class InheritanceAndMerge {
        @Test
        void inheritableSubsetCopiesOnlyInheritable() {
            var style = new ComputedStyle();
            style.set("color", "red");
            style.set("font-size", "18px");
            style.set("background-color", "blue"); // not inheritable
            style.set("padding-top", "10px"); // not inheritable

            ComputedStyle inherited = style.inheritableSubset();
            assertThat(inherited.get("color")).isEqualTo("red");
            assertThat(inherited.get("font-size")).isEqualTo("18px");
            assertThat(inherited.get("background-color")).isNull();
            assertThat(inherited.get("padding-top")).isNull();
        }

        @Test
        void mergeOverwritesProperties() {
            var base = new ComputedStyle();
            base.set("color", "red");
            base.set("font-size", "18px");

            var other = new ComputedStyle();
            other.set("color", "blue");
            other.set("padding-top", "5px");

            base.merge(other);
            assertThat(base.get("color")).isEqualTo("blue");
            assertThat(base.get("font-size")).isEqualTo("18px");
            assertThat(base.get("padding-top")).isEqualTo("5px");
        }

        @Test
        void mergeInlineStyle() {
            var style = new ComputedStyle();
            style.set("color", "red");
            style.mergeInline("color: blue; font-size: 24px");
            assertThat(style.get("color")).isEqualTo("blue");
            assertThat(style.get("font-size")).isEqualTo("24px");
        }

        @Test
        void mergeInlineHandlesNull() {
            var style = new ComputedStyle();
            style.mergeInline(null);
            style.mergeInline("");
            assertThat(style.asMap()).isEmpty();
        }
    }

    @Nested
    class ColorAccessors {
        @Test
        void getColorNormalized() {
            var style = new ComputedStyle();
            style.set("color", "#FF0000");
            assertThat(style.getColor()).isEqualTo("FF0000");
        }

        @Test
        void getBackgroundColorNormalized() {
            var style = new ComputedStyle();
            // Hex colors are normalized
            style.set("background-color", "#0000FF");
            assertThat(style.getBackgroundColor()).isEqualTo("0000FF");
            
            // rgb() and rgba() are NOT normalized - returned as-is for parseColorWithAlpha
            style.set("background-color", "rgb(0,0,255)");
            assertThat(style.getBackgroundColor()).isEqualTo("rgb(0,0,255)");
            
            style.set("background-color", "rgba(0,0,255,0.5)");
            assertThat(style.getBackgroundColor()).isEqualTo("rgba(0,0,255,0.5)");
        }
        
        @Test
        void getBackgroundColorFromShorthand() {
            var style = new ComputedStyle();
            // 'background' shorthand should work as fallback
            style.set("background", "#4285f4");
            assertThat(style.getBackgroundColor()).isEqualTo("4285F4");
            
            // Named colors via shorthand
            style.set("background", "red");
            assertThat(style.getBackgroundColor()).isEqualTo("FF0000");
            
            // rgba via shorthand
            style.set("background", "rgba(66, 133, 244, 0.5)");
            assertThat(style.getBackgroundColor()).isEqualTo("rgba(66, 133, 244, 0.5)");
        }
        
        @Test
        void getBackgroundColorReturnsNullForGradient() {
            var style = new ComputedStyle();
            // Gradient backgrounds should return null (handled by getLinearGradient)
            style.set("background", "linear-gradient(to right, red, blue)");
            assertThat(style.getBackgroundColor()).isNull();
        }
        
        @Test
        void backgroundColorTakesPrecedenceOverShorthand() {
            var style = new ComputedStyle();
            // If both set, background-color wins
            style.set("background", "red");
            style.set("background-color", "blue");
            assertThat(style.getBackgroundColor()).isEqualTo("0000FF");
        }
    }

    @Nested
    class ObjectFitParsing {
        @Test
        void getObjectFitFromInlineStyle() {
            var style = new ComputedStyle();
            style.mergeInline("width: 100%; height: 100%; object-fit: contain;");
            assertThat(style.getObjectFit()).isEqualTo("contain");
        }
        
        @Test
        void getObjectFitCover() {
            var style = new ComputedStyle();
            style.set("object-fit", "cover");
            assertThat(style.getObjectFit()).isEqualTo("cover");
        }
        
        @Test
        void getObjectFitReturnsNullWhenNotSet() {
            var style = new ComputedStyle();
            assertThat(style.getObjectFit()).isNull();
        }
        
        @Test
        void getObjectPosition() {
            var style = new ComputedStyle();
            style.set("object-position", "center top");
            assertThat(style.getObjectPosition()).isEqualTo("center top");
        }
    }

    @Nested
    class BoxShadowParsing {
        @Test
        void parseBasicShadow() {
            var shadow = ComputedStyle.parseBoxShadow("5px 10px 15px #000000");
            assertThat(shadow).isNotNull();
            assertThat(shadow.offsetX()).isCloseTo(5.0, within(0.01));
            assertThat(shadow.offsetY()).isCloseTo(10.0, within(0.01));
            assertThat(shadow.blur()).isCloseTo(15.0, within(0.01));
            assertThat(shadow.colorHex()).isEqualTo("000000");
            assertThat(shadow.alpha()).isCloseTo(1.0, within(0.01)); // hex without alpha = 1.0
        }

        @Test
        void parseRgbaShadow() {
            var shadow = ComputedStyle.parseBoxShadow("4px 4px 12px rgba(33, 150, 243, 0.5)");
            assertThat(shadow).isNotNull();
            assertThat(shadow.offsetX()).isCloseTo(4.0, within(0.01));
            assertThat(shadow.offsetY()).isCloseTo(4.0, within(0.01));
            assertThat(shadow.blur()).isCloseTo(12.0, within(0.01));
            assertThat(shadow.colorHex()).isEqualTo("2196F3");
            assertThat(shadow.alpha()).isCloseTo(0.5, within(0.01));
        }

        @Test
        void parseNamedColorShadow() {
            var shadow = ComputedStyle.parseBoxShadow("5px 5px 10px black");
            assertThat(shadow).isNotNull();
            assertThat(shadow.colorHex()).isEqualTo("000000");
            assertThat(shadow.alpha()).isCloseTo(1.0, within(0.01));
        }

        @Test
        void parseShadowNoBlur() {
            var shadow = ComputedStyle.parseBoxShadow("3px 3px #FF0000");
            assertThat(shadow).isNotNull();
            assertThat(shadow.offsetX()).isCloseTo(3.0, within(0.01));
            assertThat(shadow.offsetY()).isCloseTo(3.0, within(0.01));
            assertThat(shadow.blur()).isCloseTo(0.0, within(0.01));
            assertThat(shadow.colorHex()).isEqualTo("FF0000");
        }

        @Test
        void distanceAndAngle() {
            // 3-4-5 triangle: offsetX=3, offsetY=4 → distance=5
            var shadow = ComputedStyle.parseBoxShadow("3px 4px 10px black");
            assertThat(shadow.distance()).isCloseTo(5.0, within(0.01));
            // atan2(4,3) ≈ 53.13 degrees
            assertThat(shadow.angleDegrees()).isCloseTo(53.13, within(0.1));
        }

        @Test
        void parseNoneReturnsNull() {
            assertThat(ComputedStyle.parseBoxShadow("none")).isNull();
            assertThat(ComputedStyle.parseBoxShadow(null)).isNull();
            assertThat(ComputedStyle.parseBoxShadow("")).isNull();
        }

        @Test
        void parseInsetReturnsNull() {
            // inset shadows not supported
            assertThat(ComputedStyle.parseBoxShadow("inset 5px 5px 10px black")).isNull();
        }

        @Test
        void getBoxShadowFromStyle() {
            var style = new ComputedStyle();
            assertThat(style.getBoxShadow()).isNull();
            
            style.set("box-shadow", "2px 2px 4px rgba(0,0,0,0.2)");
            var shadow = style.getBoxShadow();
            assertThat(shadow).isNotNull();
            assertThat(shadow.offsetX()).isCloseTo(2.0, within(0.01));
        }
    }

    @Nested
    class LinearGradientParsing {
        @Test
        void parseToBottom() {
            var g = ComputedStyle.parseLinearGradient("linear-gradient(to bottom, #ff0000, #0000ff)");
            assertThat(g).isNotNull();
            assertThat(g.angleDegrees()).isCloseTo(180.0, within(0.1));
            assertThat(g.stops()).hasSize(2);
            assertThat(g.stops().get(0).colorHex()).isEqualToIgnoringCase("FF0000");
            assertThat(g.stops().get(0).position()).isCloseTo(0.0, within(0.01));
            assertThat(g.stops().get(1).colorHex()).isEqualToIgnoringCase("0000FF");
            assertThat(g.stops().get(1).position()).isCloseTo(1.0, within(0.01));
        }

        @Test
        void parseToRight() {
            var g = ComputedStyle.parseLinearGradient("linear-gradient(to right, red, blue)");
            assertThat(g).isNotNull();
            assertThat(g.angleDegrees()).isCloseTo(90.0, within(0.1));
        }

        @Test
        void parseToTop() {
            var g = ComputedStyle.parseLinearGradient("linear-gradient(to top, #000, #fff)");
            assertThat(g).isNotNull();
            assertThat(g.angleDegrees()).isCloseTo(0.0, within(0.1));
        }

        @Test
        void parseToLeft() {
            var g = ComputedStyle.parseLinearGradient("linear-gradient(to left, green, yellow)");
            assertThat(g).isNotNull();
            assertThat(g.angleDegrees()).isCloseTo(270.0, within(0.1));
        }

        @Test
        void parseDegreeAngle() {
            var g = ComputedStyle.parseLinearGradient("linear-gradient(45deg, #f00, #00f)");
            assertThat(g).isNotNull();
            assertThat(g.angleDegrees()).isCloseTo(45.0, within(0.1));
        }

        @Test
        void parseMultipleStops() {
            var g = ComputedStyle.parseLinearGradient("linear-gradient(to right, red, yellow, green, blue)");
            assertThat(g).isNotNull();
            assertThat(g.stops()).hasSize(4);
            // Evenly distributed: 0%, 33.3%, 66.6%, 100%
            assertThat(g.stops().get(0).position()).isCloseTo(0.0, within(0.01));
            assertThat(g.stops().get(1).position()).isCloseTo(0.333, within(0.01));
            assertThat(g.stops().get(2).position()).isCloseTo(0.666, within(0.01));
            assertThat(g.stops().get(3).position()).isCloseTo(1.0, within(0.01));
        }

        @Test
        void parseRgbaColor() {
            var g = ComputedStyle.parseLinearGradient("linear-gradient(to right, rgba(255,0,0,0.5), rgba(0,0,255,1))");
            assertThat(g).isNotNull();
            assertThat(g.stops()).hasSize(2);
            assertThat(g.stops().get(0).colorHex()).isEqualToIgnoringCase("FF0000");
            assertThat(g.stops().get(0).alpha()).isCloseTo(0.5, within(0.01));
            assertThat(g.stops().get(1).colorHex()).isEqualToIgnoringCase("0000FF");
            assertThat(g.stops().get(1).alpha()).isCloseTo(1.0, within(0.01));
        }

        @Test
        void parseDefaultDirectionIsToBottom() {
            var g = ComputedStyle.parseLinearGradient("linear-gradient(#ff0000, #0000ff)");
            assertThat(g).isNotNull();
            assertThat(g.angleDegrees()).isCloseTo(180.0, within(0.1)); // default is to bottom
        }

        @Test
        void parseToBottomRight() {
            var g = ComputedStyle.parseLinearGradient("linear-gradient(to bottom right, #000, #fff)");
            assertThat(g).isNotNull();
            assertThat(g.angleDegrees()).isCloseTo(135.0, within(0.1));
        }

        @Test
        void parseReturnsNullForInvalid() {
            assertThat(ComputedStyle.parseLinearGradient(null)).isNull();
            assertThat(ComputedStyle.parseLinearGradient("")).isNull();
            assertThat(ComputedStyle.parseLinearGradient("red")).isNull();
            assertThat(ComputedStyle.parseLinearGradient("radial-gradient(red, blue)")).isNull();
        }

        @Test
        void getLinearGradientFromStyle() {
            var style = new ComputedStyle();
            assertThat(style.getLinearGradient()).isNull();
            
            style.set("background", "linear-gradient(to right, #667eea, #764ba2)");
            var g = style.getLinearGradient();
            assertThat(g).isNotNull();
            assertThat(g.angleDegrees()).isCloseTo(90.0, within(0.1));
            assertThat(g.stops()).hasSize(2);
        }

        @Test
        void getLinearGradientFromBackgroundImage() {
            var style = new ComputedStyle();
            style.set("background-image", "linear-gradient(135deg, #ff6b6b, #feca57)");
            var g = style.getLinearGradient();
            assertThat(g).isNotNull();
            assertThat(g.angleDegrees()).isCloseTo(135.0, within(0.1));
        }
    }
}
