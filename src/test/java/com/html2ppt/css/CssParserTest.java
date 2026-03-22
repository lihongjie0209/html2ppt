package com.html2ppt.css;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class CssParserTest {

    private final CssParser parser = new CssParser();

    @Nested
    class ParseStyleBlocks {
        @Test
        void parseSingleRule() {
            var rules = parser.parse("p { color: red; }");
            assertThat(rules).hasSize(1);
            assertThat(rules.get(0).selector()).isEqualTo("p");
            assertThat(rules.get(0).declarations()).containsEntry("color", "red");
        }

        @Test
        void parseMultipleRules() {
            var rules = parser.parse("h1 { font-size: 36pt; } p { color: blue; }");
            assertThat(rules).hasSize(2);
        }

        @Test
        void parseMultipleDeclarations() {
            var rules = parser.parse("div { color: red; font-size: 18px; padding: 10px; }");
            assertThat(rules).hasSize(1);
            assertThat(rules.get(0).declarations())
                .containsEntry("color", "red")
                .containsEntry("font-size", "18px");
        }

        @Test
        void parseClassSelector() {
            var rules = parser.parse(".title { font-size: 36pt; }");
            assertThat(rules).hasSize(1);
            assertThat(rules.get(0).selector()).isEqualTo(".title");
        }

        @Test
        void parseIdSelector() {
            var rules = parser.parse("#header { color: blue; }");
            assertThat(rules).hasSize(1);
            assertThat(rules.get(0).selector()).isEqualTo("#header");
        }

        @Test
        void parseDescendantSelector() {
            var rules = parser.parse(".card p { color: gray; }");
            assertThat(rules).hasSize(1);
            assertThat(rules.get(0).selector()).isEqualTo(".card p");
        }

        @Test
        void parseChildSelector() {
            var rules = parser.parse("div > p { margin: 0; }");
            assertThat(rules).hasSize(1);
            assertThat(rules.get(0).selector()).contains(">");
        }

        @Test
        void parseMultiSelector() {
            // Multi-selector should expand to separate rules
            var rules = parser.parse("h1, h2 { font-weight: bold; }");
            assertThat(rules).hasSizeGreaterThanOrEqualTo(2);
            var selectors = rules.stream().map(CssParser.CssRule::selector).toList();
            assertThat(selectors).contains("h1", "h2");
        }

        @Test
        void emptyInput() {
            assertThat(parser.parse("")).isEmpty();
            assertThat(parser.parse(null)).isEmpty();
        }
    }

    @Nested
    class ParseInline {
        @Test
        void parseSimpleInline() {
            Map<String, String> props = parser.parseInline("color: red; font-size: 18px");
            assertThat(props).containsEntry("color", "red");
            assertThat(props).containsEntry("font-size", "18px");
        }

        @Test
        void parseEmptyInline() {
            assertThat(parser.parseInline("")).isEmpty();
            assertThat(parser.parseInline(null)).isEmpty();
        }
    }

    @Nested
    class ShorthandExpansion {
        @Test
        void expandPaddingFourValues() {
            var rules = parser.parse("div { padding: 10px 20px 30px 40px; }");
            var decl = rules.get(0).declarations();
            assertThat(decl).containsEntry("padding-top", "10px");
            assertThat(decl).containsEntry("padding-right", "20px");
            assertThat(decl).containsEntry("padding-bottom", "30px");
            assertThat(decl).containsEntry("padding-left", "40px");
        }

        @Test
        void expandPaddingTwoValues() {
            var rules = parser.parse("div { padding: 10px 20px; }");
            var decl = rules.get(0).declarations();
            assertThat(decl).containsEntry("padding-top", "10px");
            assertThat(decl).containsEntry("padding-right", "20px");
            assertThat(decl).containsEntry("padding-bottom", "10px");
            assertThat(decl).containsEntry("padding-left", "20px");
        }

        @Test
        void expandPaddingOneValue() {
            var rules = parser.parse("div { padding: 15px; }");
            var decl = rules.get(0).declarations();
            assertThat(decl).containsEntry("padding-top", "15px");
            assertThat(decl).containsEntry("padding-right", "15px");
            assertThat(decl).containsEntry("padding-bottom", "15px");
            assertThat(decl).containsEntry("padding-left", "15px");
        }

        @Test
        void expandPaddingThreeValues() {
            var rules = parser.parse("div { padding: 10px 20px 30px; }");
            var decl = rules.get(0).declarations();
            assertThat(decl).containsEntry("padding-top", "10px");
            assertThat(decl).containsEntry("padding-right", "20px");
            assertThat(decl).containsEntry("padding-bottom", "30px");
            assertThat(decl).containsEntry("padding-left", "20px");
        }

        @Test
        void expandMarginSimilar() {
            var rules = parser.parse("div { margin: 5px 10px; }");
            var decl = rules.get(0).declarations();
            assertThat(decl).containsEntry("margin-top", "5px");
            assertThat(decl).containsEntry("margin-right", "10px");
        }

        @Test
        void expandFlexShorthand() {
            var rules = parser.parse("div { flex: 1; }");
            var decl = rules.get(0).declarations();
            assertThat(decl).containsEntry("flex-grow", "1");
            assertThat(decl).containsEntry("flex-shrink", "1");
            assertThat(decl).containsEntry("flex-basis", "0");
        }

        @Test
        void expandFlexNone() {
            var rules = parser.parse("div { flex: none; }");
            var decl = rules.get(0).declarations();
            assertThat(decl).containsEntry("flex-grow", "0");
            assertThat(decl).containsEntry("flex-shrink", "0");
            assertThat(decl).containsEntry("flex-basis", "auto");
        }

        @Test
        void expandFlexAuto() {
            var rules = parser.parse("div { flex: auto; }");
            var decl = rules.get(0).declarations();
            assertThat(decl).containsEntry("flex-grow", "1");
            assertThat(decl).containsEntry("flex-shrink", "1");
            assertThat(decl).containsEntry("flex-basis", "auto");
        }

        @Test
        void expandGap() {
            var rules = parser.parse("div { gap: 10px; }");
            var decl = rules.get(0).declarations();
            assertThat(decl).containsEntry("row-gap", "10px");
            assertThat(decl).containsEntry("column-gap", "10px");
        }

        @Test
        void expandBorder() {
            var rules = parser.parse("div { border: 2px solid #FF0000; }");
            var decl = rules.get(0).declarations();
            assertThat(decl).containsEntry("border-width", "2px");
            assertThat(decl).containsEntry("border-color", "#FF0000");
        }
    }
}
