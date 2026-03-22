package com.html2ppt.layout;

import com.html2ppt.model.ComputedStyle;
import com.html2ppt.model.LayoutResult;
import com.html2ppt.model.SlideNode;
import com.html2ppt.model.TableCell;
import com.html2ppt.model.TextRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LayoutDebuggerTest {

    private SlideNode.Slide makeSlide(ComputedStyle style, List<SlideNode> children) {
        var slide = new SlideNode.Slide(style, children);
        slide.setLayout(new LayoutResult(0, 0, 720, 405));
        return slide;
    }

    private SlideNode.TextBlock makeText(String text, ComputedStyle style) {
        var block = new SlideNode.TextBlock(List.of(new TextRun(text)), style);
        block.setLayout(new LayoutResult(20, 30, 200, 40));
        return block;
    }

    @Test
    void dumpIncludesHeaderLegendNodeTypesAndCssSummary() {
        var slideStyle = new ComputedStyle();
        slideStyle.set("display", "flex");
        slideStyle.set("flex-direction", "column");
        slideStyle.set("justify-content", "center");
        slideStyle.set("align-items", "center");
        slideStyle.set("padding-top", "10px");
        slideStyle.set("padding-right", "20px");
        slideStyle.set("padding-bottom", "30px");
        slideStyle.set("padding-left", "40px");
        slideStyle.set("margin-top", "1px");
        slideStyle.set("margin-right", "2px");
        slideStyle.set("margin-bottom", "3px");
        slideStyle.set("margin-left", "4px");
        slideStyle.set("background-color", "#112233");

        var textStyle = new ComputedStyle();
        textStyle.set("font-size", "18px");
        textStyle.set("color", "#ffffff");
        var text = makeText("This is a long line of debug text that should be truncated.", textStyle);

        var boxStyle = new ComputedStyle();
        boxStyle.set("width", "200px");
        boxStyle.set("height", "100px");
        var box = new SlideNode.Box(boxStyle, List.of(text));
        box.setLayout(new LayoutResult(10, 15, 220, 120));

        var image = new SlideNode.ImageBlock("demo.png", new ComputedStyle());
        image.setLayout(null);

        var table = new SlideNode.TableBlock(List.of(List.of(new TableCell("A"))), new ComputedStyle());
        table.setLayout(new LayoutResult(0, 0, 100, 50));

        var shape = new SlideNode.ShapeBlock("line", new ComputedStyle());
        shape.setLayout(new LayoutResult(5, 5, 50, 2));

        var slide = makeSlide(slideStyle, List.of(box, image, table, shape));

        String dump = LayoutDebugger.dump(List.of(slide));

        assertThat(dump).contains("html2ppt Layout Debug Dump");
        assertThat(dump).contains("Legend");
        assertThat(dump).contains("Slide [x=0.0");
        assertThat(dump).contains("Box [x=10.0");
        assertThat(dump).contains("Text [x=20.0");
        assertThat(dump).contains("Image [NO LAYOUT]");
        assertThat(dump).contains("Table [x=0.0");
        assertThat(dump).contains("Shape(line) [x=5.0");
        assertThat(dump).contains("This is a long line");
        assertThat(dump).doesNotContain("This is a long line of debug text that should be truncated.");
        assertThat(dump).contains("display=flex");
        assertThat(dump).contains("flex-dir=column");
        assertThat(dump).contains("justify=center");
        assertThat(dump).contains("align=center");
        assertThat(dump).contains("pad=[10px 20px 30px 40px]");
        assertThat(dump).contains("margin=[1px 2px 3px 4px]");
        assertThat(dump).contains("bg=#112233");
    }

    @Test
    void dumpToFileWritesSameContent(@TempDir Path tempDir) throws IOException {
        var slide = makeSlide(new ComputedStyle(), List.of());
        Path output = tempDir.resolve("layout.txt");

        LayoutDebugger.dumpToFile(List.of(slide), output);

        assertThat(Files.exists(output)).isTrue();
        assertThat(Files.readString(output)).contains("Slide 1");
    }

    @Test
    void buildCssInfoReturnsEmptyStringForNullStyle() throws Exception {
        var method = LayoutDebugger.class.getDeclaredMethod("buildCssInfo", ComputedStyle.class);
        method.setAccessible(true);

        assertThat((String) method.invoke(null, new Object[]{null})).isEmpty();
    }
}
