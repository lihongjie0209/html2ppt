package com.html2ppt.parser;

import com.html2ppt.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class HtmlParserTest {

    private final HtmlParser parser = new HtmlParser();

    @Nested
    class BasicParsing {
        @Test
        void parseEmptyBody() {
            List<SlideNode.Slide> slides = parser.parseString("<html><body></body></html>");
            assertThat(slides).isEmpty();
        }

        @Test
        void parseSingleSlide() {
            String html = "<html><body><section><p>Hello</p></section></body></html>";
            List<SlideNode.Slide> slides = parser.parseString(html);
            assertThat(slides).hasSize(1);
        }

        @Test
        void parseMultipleSlides() {
            String html = """
                <html><body>
                    <section><p>Slide 1</p></section>
                    <section><p>Slide 2</p></section>
                    <section><p>Slide 3</p></section>
                </body></html>
                """;
            List<SlideNode.Slide> slides = parser.parseString(html);
            assertThat(slides).hasSize(3);
        }
    }

    @Nested
    class ElementConversion {
        @Test
        void paragraphBecomesTextBlock() {
            String html = "<html><body><section><p>Hello World</p></section></body></html>";
            var slides = parser.parseString(html);
            assertThat(slides.get(0).children()).hasSize(1);
            assertThat(slides.get(0).children().get(0)).isInstanceOf(SlideNode.TextBlock.class);
            var tb = (SlideNode.TextBlock) slides.get(0).children().get(0);
            assertThat(tb.plainText()).isEqualTo("Hello World");
        }

        @Test
        void headingsBecomesTextBlock() {
            String html = "<html><body><section><h1>Title</h1><h2>Subtitle</h2></section></body></html>";
            var slides = parser.parseString(html);
            assertThat(slides.get(0).children()).hasSize(2);
            var h1 = (SlideNode.TextBlock) slides.get(0).children().get(0);
            assertThat(h1.plainText()).isEqualTo("Title");
        }

        @Test
        void divBecomesBox() {
            String html = "<html><body><section><div><p>Inside</p></div></section></body></html>";
            var slides = parser.parseString(html);
            var box = slides.get(0).children().get(0);
            assertThat(box).isInstanceOf(SlideNode.Box.class);
            assertThat(box.children()).hasSize(1);
        }

        @Test
        void imageBecomesImageBlock() {
            String html = "<html><body><section><img src='logo.png'/></section></body></html>";
            var slides = parser.parseString(html);
            var img = slides.get(0).children().get(0);
            assertThat(img).isInstanceOf(SlideNode.ImageBlock.class);
            assertThat(((SlideNode.ImageBlock) img).src()).isEqualTo("logo.png");
        }

        @Test
        void hrBecomesShapeBlock() {
            String html = "<html><body><section><hr/></section></body></html>";
            var slides = parser.parseString(html);
            var shape = slides.get(0).children().get(0);
            assertThat(shape).isInstanceOf(SlideNode.ShapeBlock.class);
            assertThat(((SlideNode.ShapeBlock) shape).shapeType()).isEqualTo("line");
        }

        @Test
        void tableBecomesTableBlock() {
            String html = """
                <html><body><section>
                    <table><tr><td>A</td><td>B</td></tr></table>
                </section></body></html>
                """;
            var slides = parser.parseString(html);
            var table = slides.get(0).children().get(0);
            assertThat(table).isInstanceOf(SlideNode.TableBlock.class);
            var tb = (SlideNode.TableBlock) table;
            assertThat(tb.rows()).hasSize(1);
            assertThat(tb.rows().get(0)).hasSize(2);
            assertThat(tb.rows().get(0).get(0).text()).isEqualTo("A");
        }
    }

    @Nested
    class InlineFormatting {
        @Test
        void boldText() {
            String html = "<html><body><section><p><b>Bold</b> text</p></section></body></html>";
            var slides = parser.parseString(html);
            var tb = (SlideNode.TextBlock) slides.get(0).children().get(0);
            assertThat(tb.runs().size()).isGreaterThanOrEqualTo(2);
            var boldRun = tb.runs().stream().filter(r -> r.text().contains("Bold")).findFirst().orElseThrow();
            assertThat(boldRun.bold()).isTrue();
        }

        @Test
        void italicText() {
            String html = "<html><body><section><p><i>Italic</i></p></section></body></html>";
            var slides = parser.parseString(html);
            var tb = (SlideNode.TextBlock) slides.get(0).children().get(0);
            var italicRun = tb.runs().stream().filter(r -> r.text().contains("Italic")).findFirst().orElseThrow();
            assertThat(italicRun.italic()).isTrue();
        }

        @Test
        void underlineText() {
            String html = "<html><body><section><p><u>Underlined</u></p></section></body></html>";
            var slides = parser.parseString(html);
            var tb = (SlideNode.TextBlock) slides.get(0).children().get(0);
            var uRun = tb.runs().stream().filter(r -> r.text().contains("Underlined")).findFirst().orElseThrow();
            assertThat(uRun.underline()).isTrue();
        }

        @Test
        void strongAndEm() {
            String html = "<html><body><section><p><strong>Bold</strong> and <em>Italic</em></p></section></body></html>";
            var slides = parser.parseString(html);
            var tb = (SlideNode.TextBlock) slides.get(0).children().get(0);
            var boldRun = tb.runs().stream().filter(r -> r.text().contains("Bold")).findFirst().orElseThrow();
            assertThat(boldRun.bold()).isTrue();
            var italicRun = tb.runs().stream().filter(r -> r.text().contains("Italic")).findFirst().orElseThrow();
            assertThat(italicRun.italic()).isTrue();
        }
    }

    @Nested
    class ListConversion {
        @Test
        void unorderedList() {
            String html = "<html><body><section><ul><li>Item 1</li><li>Item 2</li></ul></section></body></html>";
            var slides = parser.parseString(html);
            var node = slides.get(0).children().get(0);
            assertThat(node).isInstanceOf(SlideNode.Box.class);
            var box = (SlideNode.Box) node;
            assertThat(box.children()).hasSize(2);
            var first = (SlideNode.TextBlock) box.children().get(0);
            assertThat(first.plainText()).contains("•");
        }

        @Test
        void orderedList() {
            String html = "<html><body><section><ol><li>First</li><li>Second</li></ol></section></body></html>";
            var slides = parser.parseString(html);
            var box = (SlideNode.Box) slides.get(0).children().get(0);
            var first = (SlideNode.TextBlock) box.children().get(0);
            assertThat(first.plainText()).contains("1.");
            var second = (SlideNode.TextBlock) box.children().get(1);
            assertThat(second.plainText()).contains("2.");
        }
    }

    @Nested
    class StyleIntegration {
        @Test
        void inlineStyleApplied() {
            String html = "<html><body><section><p style='color: red; font-size: 24px'>Styled</p></section></body></html>";
            var slides = parser.parseString(html);
            var tb = (SlideNode.TextBlock) slides.get(0).children().get(0);
            assertThat(tb.style().get("color")).isEqualTo("red");
            assertThat(tb.style().get("font-size")).isEqualTo("24px");
        }

        @Test
        void styleBlockApplied() {
            String html = """
                <html><head><style>.red { color: red; }</style></head>
                <body><section><p class='red'>Red text</p></section></body></html>
                """;
            var slides = parser.parseString(html);
            var tb = (SlideNode.TextBlock) slides.get(0).children().get(0);
            assertThat(tb.style().get("color")).isEqualTo("red");
        }

        @Test
        void slideDefaultsToFlexColumn() {
            String html = "<html><body><section><p>test</p></section></body></html>";
            var slides = parser.parseString(html);
            // Slides should default to flex column
            assertThat(slides.get(0).style().getDisplay()).isEqualTo("flex");
        }
    }

    @Nested
    class Metadata {
        @Test
        void parseMeta() {
            String html = "<html data-layout='16x9' data-title='My Deck' data-author='John'><body></body></html>";
            HtmlParser.PresentationMeta meta = parser.parseMeta(html);
            assertThat(meta.layout()).isEqualTo("16x9");
            assertThat(meta.title()).isEqualTo("My Deck");
            assertThat(meta.author()).isEqualTo("John");
        }

        @Test
        void parseMetaDefaults() {
            String html = "<html><body></body></html>";
            HtmlParser.PresentationMeta meta = parser.parseMeta(html);
            assertThat(meta.layout()).isEqualTo("16x9");
            // title may be null or empty when not specified
            assertThat(meta.title()).satisfiesAnyOf(
                t -> assertThat(t).isEmpty(),
                t -> assertThat(t).isNull()
            );
        }
    }

    @Nested
    class MasterLayouts {
        @Test
        void parseSlideLayoutType() {
            String html = """
                <html><body>
                    <section data-layout="title-content">
                        <h1>Title</h1>
                        <p>Body</p>
                    </section>
                </body></html>
                """;

            var slides = parser.parseString(html);

            assertThat(slides).hasSize(1);
            assertThat(slides.get(0).layoutType()).isEqualTo("title-content");
        }

        @Test
        void preservesLayoutAliases() {
            String html = """
                <html><body>
                    <section data-layout="title-body"><h1>A</h1></section>
                    <section data-layout="section-header"><h1>B</h1></section>
                    <section data-layout="two-column"><h1>C</h1></section>
                    <section data-layout="picture-caption"><h1>D</h1></section>
                    <section data-layout="blank"><h1>E</h1></section>
                </body></html>
                """;

            var slides = parser.parseString(html);

            assertThat(slides).extracting(SlideNode.Slide::layoutType)
                .containsExactly("title-body", "section-header", "two-column", "picture-caption", "blank");
        }

        @Test
        void missingSlideLayoutTypeStaysNull() {
            String html = "<html><body><section><h1>Title</h1></section></body></html>";

            var slides = parser.parseString(html);

            assertThat(slides).hasSize(1);
            assertThat(slides.get(0).layoutType()).isNull();
        }

        @Test
        void emptyLayoutAttributeIsIgnored() {
            String html = "<html><body><section data-layout=''><h1>Title</h1></section></body></html>";

            var slides = parser.parseString(html);

            assertThat(slides).hasSize(1);
            assertThat(slides.get(0).layoutType()).isNull();
        }

        @Test
        void layoutTypeAndNotesCanCoexist() {
            String html = """
                <html><body><section data-layout="section">
                    <h1>Title</h1>
                    <aside class="notes">Speaker note</aside>
                </section></body></html>
                """;

            var slides = parser.parseString(html);

            assertThat(slides).hasSize(1);
            assertThat(slides.get(0).layoutType()).isEqualTo("section");
            assertThat(slides.get(0).notes()).isEqualTo("Speaker note");
            assertThat(slides.get(0).children()).hasSize(1);
        }
    }

    @Nested
    class NestedStructure {
        @Test
        void nestedDivs() {
            String html = """
                <html><body><section>
                    <div>
                        <div><p>Deep</p></div>
                    </div>
                </section></body></html>
                """;
            var slides = parser.parseString(html);
            var outerBox = (SlideNode.Box) slides.get(0).children().get(0);
            var innerBox = (SlideNode.Box) outerBox.children().get(0);
            var textBlock = (SlideNode.TextBlock) innerBox.children().get(0);
            assertThat(textBlock.plainText()).isEqualTo("Deep");
        }

        @Test
        void mixedContent() {
            String html = """
                <html><body><section>
                    <h1>Title</h1>
                    <div><p>Content</p></div>
                    <img src='pic.png'/>
                </section></body></html>
                """;
            var slides = parser.parseString(html);
            assertThat(slides.get(0).children()).hasSize(3);
            assertThat(slides.get(0).children().get(0)).isInstanceOf(SlideNode.TextBlock.class);
            assertThat(slides.get(0).children().get(1)).isInstanceOf(SlideNode.Box.class);
            assertThat(slides.get(0).children().get(2)).isInstanceOf(SlideNode.ImageBlock.class);
        }
    }

    @Nested
    class TableColspanRowspan {
        @Test
        void parseColspanAttribute() {
            String html = """
                <html><body><section>
                    <table>
                        <tr><td colspan="3">Header</td></tr>
                        <tr><td>A</td><td>B</td><td>C</td></tr>
                    </table>
                </section></body></html>
                """;
            var slides = parser.parseString(html);
            var table = (SlideNode.TableBlock) slides.get(0).children().get(0);
            assertThat(table.rows().get(0).get(0).colspan()).isEqualTo(3);
            assertThat(table.rows().get(0).get(0).rowspan()).isEqualTo(1);
            assertThat(table.rows().get(1).get(0).colspan()).isEqualTo(1);
        }

        @Test
        void parseRowspanAttribute() {
            String html = """
                <html><body><section>
                    <table>
                        <tr><td rowspan="2">Category</td><td>Item 1</td></tr>
                        <tr><td>Item 2</td></tr>
                    </table>
                </section></body></html>
                """;
            var slides = parser.parseString(html);
            var table = (SlideNode.TableBlock) slides.get(0).children().get(0);
            assertThat(table.rows().get(0).get(0).rowspan()).isEqualTo(2);
            assertThat(table.rows().get(0).get(0).colspan()).isEqualTo(1);
        }

        @Test
        void parseCombinedColspanRowspan() {
            String html = """
                <html><body><section>
                    <table>
                        <tr><td colspan="2" rowspan="2">Big Cell</td><td>Top</td></tr>
                        <tr><td>Bottom</td></tr>
                    </table>
                </section></body></html>
                """;
            var slides = parser.parseString(html);
            var table = (SlideNode.TableBlock) slides.get(0).children().get(0);
            var cell = table.rows().get(0).get(0);
            assertThat(cell.colspan()).isEqualTo(2);
            assertThat(cell.rowspan()).isEqualTo(2);
            assertThat(cell.text()).isEqualTo("Big Cell");
        }

        @Test
        void defaultColspanRowspanIsOne() {
            String html = """
                <html><body><section>
                    <table><tr><td>Normal</td></tr></table>
                </section></body></html>
                """;
            var slides = parser.parseString(html);
            var table = (SlideNode.TableBlock) slides.get(0).children().get(0);
            var cell = table.rows().get(0).get(0);
            assertThat(cell.colspan()).isEqualTo(1);
            assertThat(cell.rowspan()).isEqualTo(1);
        }

        @Test
        void invalidColspanDefaultsToOne() {
            String html = """
                <html><body><section>
                    <table><tr><td colspan="abc">Bad</td></tr></table>
                </section></body></html>
                """;
            var slides = parser.parseString(html);
            var table = (SlideNode.TableBlock) slides.get(0).children().get(0);
            assertThat(table.rows().get(0).get(0).colspan()).isEqualTo(1);
        }
    }

    @Nested
    class SpeakerNotes {
        @Test
        void parseNotesFromAside() {
            String html = """
                <html><body><section>
                    <h1>Title</h1>
                    <aside class="notes">Speaker notes here</aside>
                </section></body></html>
                """;
            var slides = parser.parseString(html);
            assertThat(slides.get(0).notes()).isEqualTo("Speaker notes here");
        }

        @Test
        void notesNotRenderedAsContent() {
            String html = """
                <html><body><section>
                    <h1>Title</h1>
                    <aside class="notes">Hidden notes</aside>
                </section></body></html>
                """;
            var slides = parser.parseString(html);
            // Notes should be extracted, not rendered as a child
            assertThat(slides.get(0).children()).hasSize(1);
            assertThat(slides.get(0).children().get(0)).isInstanceOf(SlideNode.TextBlock.class);
        }

        @Test
        void slideWithoutNotes() {
            String html = """
                <html><body><section>
                    <p>No notes here</p>
                </section></body></html>
                """;
            var slides = parser.parseString(html);
            assertThat(slides.get(0).notes()).isNull();
        }

        @Test
        void emptyNotesIgnored() {
            String html = """
                <html><body><section>
                    <p>Content</p>
                    <aside class="notes">   </aside>
                </section></body></html>
                """;
            var slides = parser.parseString(html);
            // Whitespace-only notes should be treated as null/empty
            assertThat(slides.get(0).notes()).satisfiesAnyOf(
                n -> assertThat(n).isNull(),
                n -> assertThat(n).isBlank()
            );
        }

        @Test
        void multilineNotes() {
            String html = """
                <html><body><section>
                    <h1>Title</h1>
                    <aside class="notes">
                        Line 1
                        Line 2
                        Line 3
                    </aside>
                </section></body></html>
                """;
            var slides = parser.parseString(html);
            assertThat(slides.get(0).notes()).contains("Line 1");
            assertThat(slides.get(0).notes()).contains("Line 2");
        }
    }
}
