package com.html2ppt.layout;

import com.html2ppt.model.*;
import com.html2ppt.parser.HtmlParser;
import org.junit.jupiter.api.Test;

import java.util.List;

class LayoutDebugTest {

    @Test
    void dumpLayoutTree() {
        String html = """
            <html data-layout="16x9">
            <head><style>
                section {
                    padding: 40px 50px;
                    display: flex;
                    flex-direction: column;
                }
                .title-slide {
                    background-color: #003366;
                    justify-content: center;
                    align-items: center;
                }
                .title-slide h1 { color: white; font-size: 36pt; }
                .title-slide p { color: #99ccff; font-size: 18pt; }
                h2 { font-size: 28pt; color: #003366; }
                .card-row { display: flex; gap: 20px; flex: 1; }
                .card {
                    flex: 1; display: flex; flex-direction: column;
                    padding: 20px; background-color: #f0f4f8; border-radius: 8px; gap: 8px;
                }
                .metric { font-size: 32pt; font-weight: bold; }
            </style></head>
            <body>
                <section class="title-slide">
                    <h1>Project Report</h1>
                    <p>Q4 2024</p>
                </section>
                <section>
                    <h2>Progress</h2>
                    <div class="card-row">
                        <div class="card"><p>Done</p><p class="metric">95%</p></div>
                        <div class="card"><p>Pending</p><p class="metric">3</p></div>
                        <div class="card"><p>Risk</p><p class="metric">1</p></div>
                    </div>
                </section>
            </body></html>
            """;

        HtmlParser parser = new HtmlParser();
        List<SlideNode.Slide> slides = parser.parseString(html);
        System.out.println("Slides: " + slides.size());

        YogaLayoutEngine engine = new YogaLayoutEngine();
        for (int i = 0; i < slides.size(); i++) {
            SlideNode.Slide slide = slides.get(i);
            engine.computeLayout(slide);
            System.out.println("\n=== Slide " + (i + 1) + " ===");
            printNode(slide, 0);
        }
    }

    private void printNode(SlideNode node, int depth) {
        String indent = "  ".repeat(depth);
        String type = node.getClass().getSimpleName();
        LayoutResult lr = node.layout();
        String layoutStr = lr != null
            ? String.format("x=%.1f y=%.1f w=%.1f h=%.1f", lr.x(), lr.y(), lr.width(), lr.height())
            : "NO LAYOUT";

        ComputedStyle style = node.style();
        StringBuilder sb = new StringBuilder();
        if (style != null) {
            appendIf(sb, "flex-dir", style.get("flex-direction"));
            appendIf(sb, "flex-grow", style.get("flex-grow"));
            appendIf(sb, "justify", style.get("justify-content"));
            appendIf(sb, "align", style.get("align-items"));
            appendIf(sb, "w", style.get("width"));
            appendIf(sb, "h", style.get("height"));
            appendIf(sb, "row-gap", style.get("row-gap"));
            appendIf(sb, "col-gap", style.get("column-gap"));
            appendIf(sb, "pad-top", style.get("padding-top"));
            appendIf(sb, "pad-left", style.get("padding-left"));
            appendIf(sb, "display", style.get("display"));
            appendIf(sb, "bg", style.get("background-color"));
        }

        String text = "";
        if (node instanceof SlideNode.TextBlock tb) {
            text = " \"" + tb.plainText() + "\"";
        }

        System.out.println(indent + type + text + " [" + layoutStr + "]" + sb);

        for (SlideNode child : node.children()) {
            printNode(child, depth + 1);
        }
    }

    private void appendIf(StringBuilder sb, String label, String value) {
        if (value != null) sb.append(" ").append(label).append("=").append(value);
    }
}
