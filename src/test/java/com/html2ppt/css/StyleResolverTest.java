package com.html2ppt.css;

import com.html2ppt.model.ComputedStyle;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class StyleResolverTest {

    private Element parseAndSelect(String html, String cssSelector) {
        Document doc = Jsoup.parse(html);
        return doc.selectFirst(cssSelector);
    }

    private StyleResolver resolverFor(String css) {
        CssParser parser = new CssParser();
        List<CssParser.CssRule> rules = parser.parse(css);
        return new StyleResolver(rules);
    }

    @Nested
    class SelectorMatching {
        @Test
        void matchElementSelector() {
            var resolver = resolverFor("p { color: red; }");
            Element el = parseAndSelect("<p>hello</p>", "p");
            ComputedStyle style = resolver.resolve(el, null);
            assertThat(style.get("color")).isEqualTo("red");
        }

        @Test
        void matchClassSelector() {
            var resolver = resolverFor(".highlight { color: yellow; }");
            Element el = parseAndSelect("<p class='highlight'>hi</p>", "p");
            ComputedStyle style = resolver.resolve(el, null);
            assertThat(style.get("color")).isEqualTo("yellow");
        }

        @Test
        void matchIdSelector() {
            var resolver = resolverFor("#main { color: green; }");
            Element el = parseAndSelect("<div id='main'>content</div>", "#main");
            ComputedStyle style = resolver.resolve(el, null);
            assertThat(style.get("color")).isEqualTo("green");
        }

        @Test
        void matchDescendantSelector() {
            var resolver = resolverFor("div p { color: blue; }");
            Element el = parseAndSelect("<div><p>text</p></div>", "p");
            ComputedStyle style = resolver.resolve(el, null);
            assertThat(style.get("color")).isEqualTo("blue");
        }

        @Test
        void matchChildSelector() {
            var resolver = resolverFor("div > p { color: red; }");
            Element el = parseAndSelect("<div><p>direct child</p></div>", "p");
            ComputedStyle style = resolver.resolve(el, null);
            assertThat(style.get("color")).isEqualTo("red");
        }

        @Test
        void childSelectorNotMatchingGrandchild() {
            var resolver = resolverFor("div > p { color: red; }");
            Element el = parseAndSelect("<div><section><p>grandchild</p></section></div>", "p");
            ComputedStyle style = resolver.resolve(el, null);
            // p is grandchild of div, not direct child — rule should NOT match
            assertThat(style.get("color")).isNull();
        }

        @Test
        void combinedElementClass() {
            var resolver = resolverFor("p.note { color: gray; }");
            Element el = parseAndSelect("<p class='note'>text</p>", "p.note");
            ComputedStyle style = resolver.resolve(el, null);
            assertThat(style.get("color")).isEqualTo("gray");
        }

        @Test
        void noMatchReturnsEmpty() {
            var resolver = resolverFor("h1 { color: red; }");
            Element el = parseAndSelect("<p>text</p>", "p");
            ComputedStyle style = resolver.resolve(el, null);
            assertThat(style.get("color")).isNull();
        }
    }

    @Nested
    class Specificity {
        @Test
        void idBeatsClass() {
            var resolver = resolverFor("#main { color: red; } .main { color: blue; }");
            Element el = parseAndSelect("<div id='main' class='main'>text</div>", "#main");
            ComputedStyle style = resolver.resolve(el, null);
            assertThat(style.get("color")).isEqualTo("red");
        }

        @Test
        void classBeatsElement() {
            var resolver = resolverFor("div { color: red; } .box { color: blue; }");
            Element el = parseAndSelect("<div class='box'>text</div>", "div");
            ComputedStyle style = resolver.resolve(el, null);
            assertThat(style.get("color")).isEqualTo("blue");
        }

        @Test
        void laterRuleWinsAtSameSpecificity() {
            var resolver = resolverFor("p { color: red; } p { color: blue; }");
            Element el = parseAndSelect("<p>text</p>", "p");
            ComputedStyle style = resolver.resolve(el, null);
            assertThat(style.get("color")).isEqualTo("blue");
        }
    }

    @Nested
    class InlineStyles {
        @Test
        void inlineStyleOverridesRules() {
            var resolver = resolverFor("p { color: red; }");
            Element el = parseAndSelect("<p style='color: blue'>text</p>", "p");
            ComputedStyle style = resolver.resolve(el, null);
            assertThat(style.get("color")).isEqualTo("blue");
        }
    }

    @Nested
    class Inheritance {
        @Test
        void inheritColorFromParent() {
            var parentStyle = new ComputedStyle();
            parentStyle.set("color", "red");
            parentStyle.set("font-size", "24px");

            var resolver = resolverFor("");
            // Use <span> (no defaults) to test pure inheritance
            Element el = parseAndSelect("<div><span>text</span></div>", "span");
            ComputedStyle style = resolver.resolve(el, parentStyle);
            assertThat(style.get("color")).isEqualTo("red");
            assertThat(style.get("font-size")).isEqualTo("24px");
        }

        @Test
        void doNotInheritNonInheritable() {
            var parentStyle = new ComputedStyle();
            parentStyle.set("background-color", "blue");
            parentStyle.set("padding-top", "20px");

            var resolver = resolverFor("");
            Element el = parseAndSelect("<div><p>text</p></div>", "p");
            ComputedStyle style = resolver.resolve(el, parentStyle);
            assertThat(style.get("background-color")).isNull();
            assertThat(style.get("padding-top")).isNull();
        }

        @Test
        void ruleOverridesInheritance() {
            var parentStyle = new ComputedStyle();
            parentStyle.set("color", "red");

            var resolver = resolverFor("p { color: green; }");
            Element el = parseAndSelect("<div><p>text</p></div>", "p");
            ComputedStyle style = resolver.resolve(el, parentStyle);
            assertThat(style.get("color")).isEqualTo("green");
        }
    }

    @Nested
    class Defaults {
        @Test
        void h1DefaultFontSize() {
            var resolver = resolverFor("");
            Element el = parseAndSelect("<h1>Title</h1>", "h1");
            ComputedStyle style = resolver.resolve(el, null);
            assertThat(style.get("font-size")).isEqualTo("36pt");
            assertThat(style.isBold()).isTrue();
        }

        @Test
        void h2DefaultFontSize() {
            var resolver = resolverFor("");
            Element el = parseAndSelect("<h2>Subtitle</h2>", "h2");
            ComputedStyle style = resolver.resolve(el, null);
            assertThat(style.get("font-size")).isEqualTo("28pt");
        }

        @Test
        void pDefaultFontSize() {
            var resolver = resolverFor("");
            Element el = parseAndSelect("<p>Text</p>", "p");
            ComputedStyle style = resolver.resolve(el, null);
            assertThat(style.get("font-size")).isEqualTo("18pt");
        }
    }
}
