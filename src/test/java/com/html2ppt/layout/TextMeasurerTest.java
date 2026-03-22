package com.html2ppt.layout;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TextMeasurerTest {

    private final TextMeasurer measurer = new TextMeasurer();

    @Test
    void measureWidthSingleLine() {
        // "Hello" = 5 Latin chars, fontSize=18, charWidth=0.6*18=10.8, total=54.0 + safety margin 18*0.1=1.8 → 55.8
        double width = measurer.measureWidth("Hello", 18);
        assertThat(width).isCloseTo(55.8, within(0.01));
    }

    @Test
    void measureWidthEmptyText() {
        double width = measurer.measureWidth("", 18);
        assertThat(width).isEqualTo(0);
    }

    @Test
    void measureHeightSingleLine() {
        // "Hi" = 2 Latin chars, width=2*0.55*18=19.8, availWidth=200 → 1 line
        // height = 1 * 18 * 1.4 = 25.2
        double height = measurer.measureHeight("Hi", 18, 200);
        assertThat(height).isCloseTo(25.2, within(0.01));
    }

    @Test
    void measureHeightWithWrapping() {
        // "ABCDEFGHIJ" = 10 Latin chars, charWidth=0.55*18=9.9
        // textWidth = 99, availWidth=60 → ceil(99/60) = 2 lines
        // height = 2 * 18 * 1.4 = 50.4
        double height = measurer.measureHeight("ABCDEFGHIJ", 18, 60);
        assertThat(height).isCloseTo(50.4, within(0.01));
    }

    @Test
    void measureHeightEmpty() {
        // empty text → 1 line minimum
        double height = measurer.measureHeight("", 18, 200);
        assertThat(height).isCloseTo(25.2, within(0.01));
    }

    @Test
    void measureHeightNullText() {
        double height = measurer.measureHeight(null, 18, 200);
        assertThat(height).isCloseTo(25.2, within(0.01));
    }

    @Test
    void differentFontSizes() {
        // Larger font = wider and taller
        double small = measurer.measureWidth("Test", 12);
        double large = measurer.measureWidth("Test", 24);
        assertThat(large).isGreaterThan(small);
    }

    @Test
    void heightIncreasesWithNarrowWidth() {
        // Same text, narrower → more lines → taller
        double wide = measurer.measureHeight("A long piece of text here", 18, 500);
        double narrow = measurer.measureHeight("A long piece of text here", 18, 50);
        assertThat(narrow).isGreaterThan(wide);
    }

    @Test
    void cjkCharactersWider() {
        // CJK characters use 1.1 ratio vs Latin 0.6
        double latin = measurer.measureWidth("AB", 18);   // 2 * 0.6 * 18 + 18*0.1 = 21.6 + 1.8 = 23.4
        double cjk = measurer.measureWidth("你好", 18);    // 2 * 1.1 * 18 + 18*0.1 = 39.6 + 1.8 = 41.4
        assertThat(cjk).isGreaterThan(latin);
        assertThat(cjk).isCloseTo(41.4, within(0.01));
    }

    @Test
    void isCJKDetection() {
        assertThat(TextMeasurer.isCJK('你')).isTrue();
        assertThat(TextMeasurer.isCJK('A')).isFalse();
        assertThat(TextMeasurer.isCJK('1')).isFalse();
        assertThat(TextMeasurer.isCJK('ア')).isTrue(); // Katakana
    }

    @Test
    void measureHeightWithExplicitNewlines() {
        // Code block with 3 lines: "line1\nline2\nline3"
        // Each line is short, no wrapping needed
        // height = 3 * 18 * 1.4 = 75.6
        double height = measurer.measureHeight("line1\nline2\nline3", 18, 500);
        assertThat(height).isCloseTo(75.6, within(0.01));
    }

    @Test
    void measureHeightWithEmptyLinesInCode() {
        // Code with empty line: "a\n\nb" → 3 lines
        // height = 3 * 18 * 1.4 = 75.6
        double height = measurer.measureHeight("a\n\nb", 18, 500);
        assertThat(height).isCloseTo(75.6, within(0.01));
    }

    @Test
    void measureHeightCodeBlockMultiLine() {
        // Simulate a real code block with 5 lines
        String code = "function test() {\n    return 42;\n}\n\nconsole.log(test());";
        // 5 lines total → 5 * 18 * 1.4 = 126.0
        double height = measurer.measureHeight(code, 18, 600);
        assertThat(height).isCloseTo(126.0, within(0.01));
    }

    @Test
    void measureHeightCombinesNewlinesAndWrapping() {
        // Line 1: short (1 line)
        // Line 2: very long, needs wrapping
        // "Short\nVeryLongLineAAAAAAAAAAAAAAAAAAAAAAAA"
        // At width=100, the long line should wrap
        String text = "Hi\nAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        double height = measurer.measureHeight(text, 18, 100);
        // "Hi" = 1 line, long line width ≈ 38 * 0.6 * 18 + margin = 410.4 + 1.8 ≈ 412
        // At width 100: ceil(412/100) = 5 lines
        // Total = 1 + 5 = 6 lines → 6 * 18 * 1.4 = 151.2
        assertThat(height).isCloseTo(151.2, within(0.1));
    }
}
