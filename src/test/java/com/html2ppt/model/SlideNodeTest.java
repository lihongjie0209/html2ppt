package com.html2ppt.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class SlideNodeTest {

    @Test
    void slideCreation() {
        var style = new ComputedStyle();
        style.set("background-color", "#003366");
        var slide = new SlideNode.Slide(style, List.of());
        assertThat(slide.style()).isSameAs(style);
        assertThat(slide.children()).isEmpty();
        assertThat(slide.layout()).isEqualTo(LayoutResult.ZERO);
    }

    @Test
    void slideWithChildren() {
        var childStyle = new ComputedStyle();
        var textRun = new TextRun("Hello");
        var textBlock = new SlideNode.TextBlock(List.of(textRun), childStyle);
        var slide = new SlideNode.Slide(new ComputedStyle(), List.of(textBlock));
        assertThat(slide.children()).hasSize(1);
        assertThat(slide.children().get(0)).isInstanceOf(SlideNode.TextBlock.class);
    }

    @Test
    void boxCreation() {
        var box = new SlideNode.Box(new ComputedStyle(), List.of());
        assertThat(box.children()).isEmpty();
    }

    @Test
    void textBlockPlainText() {
        var runs = List.of(
            new TextRun("Hello "),
            new TextRun("World", true, false, false, null, null, null)
        );
        var tb = new SlideNode.TextBlock(runs, new ComputedStyle());
        assertThat(tb.plainText()).isEqualTo("Hello World");
        assertThat(tb.runs()).hasSize(2);
        assertThat(tb.children()).isEmpty(); // leaf node
    }

    @Test
    void imageBlock() {
        var img = new SlideNode.ImageBlock("logo.png", new ComputedStyle());
        assertThat(img.src()).isEqualTo("logo.png");
        assertThat(img.children()).isEmpty();
    }

    @Test
    void tableBlock() {
        var row = List.of(new TableCell("A"), new TableCell("B"));
        var table = new SlideNode.TableBlock(List.of(row), new ComputedStyle());
        assertThat(table.rows()).hasSize(1);
        assertThat(table.rows().get(0)).hasSize(2);
        assertThat(table.children()).isEmpty();
    }

    @Test
    void shapeBlock() {
        var shape = new SlideNode.ShapeBlock("line", new ComputedStyle());
        assertThat(shape.shapeType()).isEqualTo("line");
        assertThat(shape.children()).isEmpty();
    }

    @Test
    void setLayoutIsReflected() {
        var slide = new SlideNode.Slide(new ComputedStyle(), List.of());
        assertThat(slide.layout()).isEqualTo(LayoutResult.ZERO);
        var lr = new LayoutResult(10, 20, 100, 50);
        slide.setLayout(lr);
        assertThat(slide.layout()).isEqualTo(lr);
    }

    @Test
    void layoutResultRecordAccessors() {
        var lr = new LayoutResult(5, 10, 200, 150);
        assertThat(lr.x()).isEqualTo(5);
        assertThat(lr.y()).isEqualTo(10);
        assertThat(lr.width()).isEqualTo(200);
        assertThat(lr.height()).isEqualTo(150);
    }

    @Test
    void layoutResultWithSize() {
        var lr = new LayoutResult(5, 10, 200, 150);
        var resized = lr.withSize(300, 250);
        assertThat(resized.x()).isEqualTo(5);
        assertThat(resized.y()).isEqualTo(10);
        assertThat(resized.width()).isEqualTo(300);
        assertThat(resized.height()).isEqualTo(250);
    }

    @Test
    void layoutResultWithPosition() {
        var lr = new LayoutResult(5, 10, 200, 150);
        var moved = lr.withPosition(20, 30);
        assertThat(moved.x()).isEqualTo(20);
        assertThat(moved.y()).isEqualTo(30);
        assertThat(moved.width()).isEqualTo(200);
        assertThat(moved.height()).isEqualTo(150);
    }

    @Test
    void layoutResultZero() {
        assertThat(LayoutResult.ZERO.x()).isEqualTo(0);
        assertThat(LayoutResult.ZERO.y()).isEqualTo(0);
        assertThat(LayoutResult.ZERO.width()).isEqualTo(0);
        assertThat(LayoutResult.ZERO.height()).isEqualTo(0);
    }

    @Test
    void textRunConvenienceConstructor() {
        var run = new TextRun("hello");
        assertThat(run.text()).isEqualTo("hello");
        assertThat(run.bold()).isFalse();
        assertThat(run.italic()).isFalse();
        assertThat(run.underline()).isFalse();
        assertThat(run.color()).isNull();
        assertThat(run.fontSize()).isNull();
        assertThat(run.fontFamily()).isNull();
    }

    @Test
    void textRunFullConstructor() {
        var run = new TextRun("hello", true, true, true, "FF0000", 24.0, "Arial");
        assertThat(run.bold()).isTrue();
        assertThat(run.italic()).isTrue();
        assertThat(run.underline()).isTrue();
        assertThat(run.color()).isEqualTo("FF0000");
        assertThat(run.fontSize()).isEqualTo(24.0);
        assertThat(run.fontFamily()).isEqualTo("Arial");
    }

    @Test
    void tableCellConvenienceConstructor() {
        var cell = new TableCell("content");
        assertThat(cell.text()).isEqualTo("content");
        assertThat(cell.style()).isNotNull();
    }

    @Test
    void tableCellFullConstructor() {
        var style = new ComputedStyle();
        style.set("font-weight", "bold");
        var cell = new TableCell("content", style);
        assertThat(cell.style().isBold()).isTrue();
    }
}
