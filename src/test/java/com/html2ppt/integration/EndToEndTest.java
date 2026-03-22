package com.html2ppt.integration;

import com.html2ppt.layout.YogaLayoutEngine;
import com.html2ppt.model.SlideNode;
import com.html2ppt.parser.HtmlParser;
import com.html2ppt.renderer.PptRenderer;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class EndToEndTest {

    @Test
    void fullPipelineMinimal() throws IOException {
        String html = """
            <html><body>
                <section>
                    <p>Hello World</p>
                </section>
            </body></html>
            """;

        HtmlParser parser = new HtmlParser();
        List<SlideNode.Slide> slides = parser.parseString(html);
        assertThat(slides).hasSize(1);

        YogaLayoutEngine layoutEngine = new YogaLayoutEngine();
        for (var slide : slides) {
            layoutEngine.computeLayout(slide);
        }

        // Verify layout was computed
        assertThat(slides.get(0).layout()).isNotNull();
        assertThat(slides.get(0).layout().width()).isCloseTo(720, within(1.0));

        PptRenderer renderer = new PptRenderer();
        XMLSlideShow pptx = renderer.renderPresentation(slides, "16x9");
        assertThat(pptx.getSlides()).hasSize(1);
        pptx.close();
    }

    @Test
    void fullPipelineWithStyling(@TempDir Path tempDir) throws IOException {
        String html = """
            <html data-title="Test Deck" data-author="Tester">
            <head><style>
                .title-slide {
                    background-color: #003366;
                    display: flex;
                    flex-direction: column;
                    justify-content: center;
                    align-items: center;
                }
                .title-slide h1 { color: white; font-size: 36pt; }
                .title-slide p { color: #99ccff; font-size: 18pt; }
                h2 { font-size: 28pt; color: #003366; }
                .card { 
                    flex: 1; padding: 20px; 
                    background-color: #f0f4f8; border-radius: 8px; 
                }
            </style></head>
            <body>
                <section class="title-slide">
                    <h1>Project Report</h1>
                    <p>Q4 2024</p>
                </section>
                <section style="display: flex; flex-direction: column;">
                    <h2>Progress</h2>
                    <div style="display: flex; gap: 20px;">
                        <div class="card"><p>Done: 95%</p></div>
                        <div class="card"><p>Pending: 3</p></div>
                    </div>
                </section>
            </body></html>
            """;

        HtmlParser parser = new HtmlParser();
        HtmlParser.PresentationMeta meta = parser.parseMeta(html);
        assertThat(meta.title()).isEqualTo("Test Deck");
        assertThat(meta.author()).isEqualTo("Tester");

        List<SlideNode.Slide> slides = parser.parseString(html);
        assertThat(slides).hasSize(2);

        // Verify first slide is title with correct styling
        var titleSlide = slides.get(0);
        assertThat(titleSlide.style().get("background-color")).isNotNull();

        // Layout
        YogaLayoutEngine layoutEngine = new YogaLayoutEngine();
        for (var slide : slides) {
            layoutEngine.computeLayout(slide);
        }

        // Every node should have layout
        for (var slide : slides) {
            assertThat(slide.layout()).isNotNull();
            assertLayoutRecursive(slide);
        }

        // Render
        PptRenderer renderer = new PptRenderer();
        Path output = tempDir.resolve("output.pptx");
        renderer.renderToFile(slides, meta.layout(), output);

        assertThat(Files.exists(output)).isTrue();
        assertThat(Files.size(output)).isGreaterThan(0);

        // Re-read and verify
        XMLSlideShow verify = new XMLSlideShow(Files.newInputStream(output));
        assertThat(verify.getSlides()).hasSize(2);
        verify.close();
    }

    @Test
    void pipelineWithTable() throws IOException {
        String html = """
            <html><body>
                <section>
                    <h2>Data Table</h2>
                    <table>
                        <tr><td>Name</td><td>Value</td></tr>
                        <tr><td>A</td><td>100</td></tr>
                    </table>
                </section>
            </body></html>
            """;

        HtmlParser parser = new HtmlParser();
        List<SlideNode.Slide> slides = parser.parseString(html);

        YogaLayoutEngine layoutEngine = new YogaLayoutEngine();
        for (var slide : slides) {
            layoutEngine.computeLayout(slide);
        }

        PptRenderer renderer = new PptRenderer();
        XMLSlideShow pptx = renderer.renderPresentation(slides, "16x9");
        assertThat(pptx.getSlides()).hasSize(1);
        pptx.close();
    }

    @Test
    void pipelineWithList() throws IOException {
        String html = """
            <html><body>
                <section>
                    <h2>Action Items</h2>
                    <ul>
                        <li>First item</li>
                        <li>Second item</li>
                        <li>Third item</li>
                    </ul>
                </section>
            </body></html>
            """;

        HtmlParser parser = new HtmlParser();
        List<SlideNode.Slide> slides = parser.parseString(html);

        YogaLayoutEngine layoutEngine = new YogaLayoutEngine();
        for (var slide : slides) {
            layoutEngine.computeLayout(slide);
        }

        PptRenderer renderer = new PptRenderer();
        XMLSlideShow pptx = renderer.renderPresentation(slides, "16x9");
        assertThat(pptx.getSlides()).hasSize(1);
        pptx.close();
    }

    @Test
    void pipelineWithHr() throws IOException {
        String html = """
            <html><body>
                <section>
                    <h1>Title</h1>
                    <hr/>
                    <p>Content below line</p>
                </section>
            </body></html>
            """;

        HtmlParser parser = new HtmlParser();
        List<SlideNode.Slide> slides = parser.parseString(html);

        YogaLayoutEngine layoutEngine = new YogaLayoutEngine();
        for (var slide : slides) {
            layoutEngine.computeLayout(slide);
        }

        PptRenderer renderer = new PptRenderer();
        XMLSlideShow pptx = renderer.renderPresentation(slides, "16x9");
        assertThat(pptx.getSlides()).hasSize(1);
        pptx.close();
    }

    private void assertLayoutRecursive(SlideNode node) {
        assertThat(node.layout()).as("Layout for " + node.getClass().getSimpleName()).isNotNull();
        for (var child : node.children()) {
            assertLayoutRecursive(child);
        }
    }
}
