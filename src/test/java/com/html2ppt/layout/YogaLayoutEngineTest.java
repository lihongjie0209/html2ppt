package com.html2ppt.layout;

import com.html2ppt.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class YogaLayoutEngineTest {

    private final YogaLayoutEngine engine = new YogaLayoutEngine();

    private SlideNode.Slide makeSlide(ComputedStyle style, List<SlideNode> children) {
        return new SlideNode.Slide(style, children);
    }

    private SlideNode.Box makeBox(ComputedStyle style, List<SlideNode> children) {
        return new SlideNode.Box(style, children);
    }

    private SlideNode.TextBlock makeText(String text, ComputedStyle style) {
        return new SlideNode.TextBlock(List.of(new TextRun(text)), style);
    }

    @Nested
    class BasicLayout {
        @Test
        void emptySlideGetsFullDimensions() {
            var style = new ComputedStyle();
            var slide = makeSlide(style, List.of());
            engine.computeLayout(slide);

            assertThat(slide.layout()).isNotNull();
            assertThat(slide.layout().width()).isCloseTo(720, within(1.0));
            assertThat(slide.layout().height()).isCloseTo(405, within(1.0));
        }

        @Test
        void singleTextChild() {
            var slideStyle = new ComputedStyle();
            slideStyle.set("flex-direction", "column");

            var textStyle = new ComputedStyle();
            textStyle.set("font-size", "18px");
            var text = makeText("Hello", textStyle);

            var slide = makeSlide(slideStyle, List.of(text));
            engine.computeLayout(slide);

            assertThat(text.layout()).isNotNull();
            assertThat(text.layout().width()).isGreaterThan(0);
            assertThat(text.layout().height()).isGreaterThan(0);
        }

        @Test
        void customSlideDimensions() {
            var slide = makeSlide(new ComputedStyle(), List.of());
            engine.computeLayout(slide, 720f, 540f);
            assertThat(slide.layout().height()).isCloseTo(540, within(1.0));
        }
    }

    @Nested
    class FlexDirection {
        @Test
        void rowLayoutChildrenHorizontal() {
            var slideStyle = new ComputedStyle();
            slideStyle.set("flex-direction", "row");

            var c1Style = new ComputedStyle();
            c1Style.set("width", "100px");
            c1Style.set("height", "50px");
            var c2Style = new ComputedStyle();
            c2Style.set("width", "100px");
            c2Style.set("height", "50px");

            var b1 = makeBox(c1Style, List.of());
            var b2 = makeBox(c2Style, List.of());

            var slide = makeSlide(slideStyle, List.of(b1, b2));
            engine.computeLayout(slide);

            // Row: b1 at x=0, b2 at x=100
            assertThat(b1.layout().x()).isCloseTo(0, within(1.0));
            assertThat(b2.layout().x()).isCloseTo(100, within(1.0));
        }

        @Test
        void columnLayoutChildrenVertical() {
            var slideStyle = new ComputedStyle();
            slideStyle.set("flex-direction", "column");

            var c1Style = new ComputedStyle();
            c1Style.set("width", "100px");
            c1Style.set("height", "50px");
            var c2Style = new ComputedStyle();
            c2Style.set("width", "100px");
            c2Style.set("height", "50px");

            var b1 = makeBox(c1Style, List.of());
            var b2 = makeBox(c2Style, List.of());

            var slide = makeSlide(slideStyle, List.of(b1, b2));
            engine.computeLayout(slide);

            // Column: b1 at y=0, b2 at y=50
            assertThat(b1.layout().y()).isCloseTo(0, within(1.0));
            assertThat(b2.layout().y()).isCloseTo(50, within(1.0));
        }
    }

    @Nested
    class FlexGrow {
        @Test
        void flexGrowDistributesSpace() {
            var slideStyle = new ComputedStyle();
            slideStyle.set("flex-direction", "row");

            var grow1 = new ComputedStyle();
            grow1.set("flex-grow", "1");
            grow1.set("height", "50px");

            var grow2 = new ComputedStyle();
            grow2.set("flex-grow", "2");
            grow2.set("height", "50px");

            var b1 = makeBox(grow1, List.of());
            var b2 = makeBox(grow2, List.of());

            var slide = makeSlide(slideStyle, List.of(b1, b2));
            engine.computeLayout(slide);

            // b2 should be roughly twice the width of b1
            assertThat(b2.layout().width()).isCloseTo(b1.layout().width() * 2, within(2.0));
        }
    }

    @Nested
    class Padding {
        @Test
        void paddingOffsetsChildren() {
            var slideStyle = new ComputedStyle();
            slideStyle.set("flex-direction", "column");
            slideStyle.set("padding-top", "20px");
            slideStyle.set("padding-left", "30px");

            var childStyle = new ComputedStyle();
            childStyle.set("width", "100px");
            childStyle.set("height", "50px");
            var child = makeBox(childStyle, List.of());

            var slide = makeSlide(slideStyle, List.of(child));
            engine.computeLayout(slide);

            // Child should be offset by padding
            assertThat(child.layout().x()).isCloseTo(30, within(1.0));
            assertThat(child.layout().y()).isCloseTo(20, within(1.0));
        }
    }

    @Nested
    class Gap {
        @Test
        void gapBetweenChildren() {
            var slideStyle = new ComputedStyle();
            slideStyle.set("flex-direction", "column");
            slideStyle.set("gap", "10px");

            var c1Style = new ComputedStyle();
            c1Style.set("width", "100px");
            c1Style.set("height", "50px");
            var c2Style = new ComputedStyle();
            c2Style.set("width", "100px");
            c2Style.set("height", "50px");

            var b1 = makeBox(c1Style, List.of());
            var b2 = makeBox(c2Style, List.of());

            var slide = makeSlide(slideStyle, List.of(b1, b2));
            engine.computeLayout(slide);

            // b2.y should be b1.y + b1.height + gap = 0 + 50 + 10 = 60
            assertThat(b2.layout().y()).isCloseTo(60, within(1.0));
        }
    }

    @Nested
    class JustifyContent {
        @Test
        void centerJustification() {
            var slideStyle = new ComputedStyle();
            slideStyle.set("flex-direction", "column");
            slideStyle.set("justify-content", "center");

            var childStyle = new ComputedStyle();
            childStyle.set("width", "100px");
            childStyle.set("height", "50px");
            var child = makeBox(childStyle, List.of());

            var slide = makeSlide(slideStyle, List.of(child));
            engine.computeLayout(slide);

            // Centered vertically in 405 height: (405 - 50) / 2 ≈ 177.5
            assertThat(child.layout().y()).isCloseTo(177.5, within(1.0));
        }

        @Test
        void spaceBetween() {
            var slideStyle = new ComputedStyle();
            slideStyle.set("flex-direction", "column");
            slideStyle.set("justify-content", "space-between");

            var c1Style = new ComputedStyle();
            c1Style.set("width", "100px");
            c1Style.set("height", "50px");
            var c2Style = new ComputedStyle();
            c2Style.set("width", "100px");
            c2Style.set("height", "50px");

            var b1 = makeBox(c1Style, List.of());
            var b2 = makeBox(c2Style, List.of());

            var slide = makeSlide(slideStyle, List.of(b1, b2));
            engine.computeLayout(slide);

            // space-between: first at top, last at bottom
            assertThat(b1.layout().y()).isCloseTo(0, within(1.0));
            assertThat(b2.layout().y()).isCloseTo(355, within(1.0)); // 405 - 50
        }
    }

    @Nested
    class AlignItems {
        @Test
        void centerAlignment() {
            var slideStyle = new ComputedStyle();
            slideStyle.set("flex-direction", "column");
            slideStyle.set("align-items", "center");

            var childStyle = new ComputedStyle();
            childStyle.set("width", "100px");
            childStyle.set("height", "50px");
            var child = makeBox(childStyle, List.of());

            var slide = makeSlide(slideStyle, List.of(child));
            engine.computeLayout(slide);

            // Centered horizontally in 720 width: (720 - 100) / 2 = 310
            assertThat(child.layout().x()).isCloseTo(310, within(1.0));
        }
    }

    @Nested
    class PercentageDimensions {
        @Test
        void percentageWidth() {
            var slideStyle = new ComputedStyle();

            var childStyle = new ComputedStyle();
            childStyle.set("width", "50%");
            childStyle.set("height", "50px");
            var child = makeBox(childStyle, List.of());

            var slide = makeSlide(slideStyle, List.of(child));
            engine.computeLayout(slide);

            // 50% of 720 = 360
            assertThat(child.layout().width()).isCloseTo(360, within(1.0));
        }
    }

    @Nested
    class AbsolutePositioning {
        @Test
        void absolutePositionedElement() {
            var slideStyle = new ComputedStyle();

            var childStyle = new ComputedStyle();
            childStyle.set("position", "absolute");
            childStyle.set("top", "100px");
            childStyle.set("left", "200px");
            childStyle.set("width", "50px");
            childStyle.set("height", "30px");
            var child = makeBox(childStyle, List.of());

            var slide = makeSlide(slideStyle, List.of(child));
            engine.computeLayout(slide);

            assertThat(child.layout().x()).isCloseTo(200, within(1.0));
            assertThat(child.layout().y()).isCloseTo(100, within(1.0));
        }
    }

    @Nested
    class TextMeasurement {
        @Test
        void textBlockGetsAutoSize() {
            var slideStyle = new ComputedStyle();
            slideStyle.set("flex-direction", "column");

            var textStyle = new ComputedStyle();
            textStyle.set("font-size", "18px");
            var text = makeText("Hello World", textStyle);

            var slide = makeSlide(slideStyle, List.of(text));
            engine.computeLayout(slide);

            // Text should have some height based on font size
            assertThat(text.layout().height()).isGreaterThan(0);
            assertThat(text.layout().width()).isGreaterThan(0);
        }
    }

    @Nested
    class ImageLayout {

        private SlideNode.ImageBlock makeImage(String src, ComputedStyle style) {
            return new SlideNode.ImageBlock(src, style);
        }

        @Test
        void imageWithExplicitDimensions() {
            var slideStyle = new ComputedStyle();
            slideStyle.set("flex-direction", "column");

            var imgStyle = new ComputedStyle();
            imgStyle.set("width", "200px");
            imgStyle.set("height", "150px");
            var img = makeImage("nonexistent.png", imgStyle);

            var slide = makeSlide(slideStyle, List.of(img));
            engine.computeLayout(slide);

            // Image should use explicit CSS dimensions
            assertThat(img.layout().width()).isCloseTo(200, within(1.0));
            assertThat(img.layout().height()).isCloseTo(150, within(1.0));
        }

        @Test
        void imageWithOnlyWidth() {
            var slideStyle = new ComputedStyle();
            slideStyle.set("flex-direction", "column");

            var imgStyle = new ComputedStyle();
            imgStyle.set("width", "100px");
            // No height specified - should use intrinsic ratio (defaults to 100x100)
            var img = makeImage("nonexistent.png", imgStyle);

            var slide = makeSlide(slideStyle, List.of(img));
            engine.computeLayout(slide);

            // Width is explicit, height calculated from intrinsic ratio (1:1 default)
            assertThat(img.layout().width()).isCloseTo(100, within(1.0));
            assertThat(img.layout().height()).isCloseTo(100, within(1.0));
        }

        @Test
        void imageWithNoSizeUsesDefault() {
            var slideStyle = new ComputedStyle();
            slideStyle.set("flex-direction", "column");

            var imgStyle = new ComputedStyle();
            // No dimensions specified - should use default 100x100
            var img = makeImage("nonexistent.png", imgStyle);

            var slide = makeSlide(slideStyle, List.of(img));
            engine.computeLayout(slide);

            // Default fallback size
            assertThat(img.layout().width()).isCloseTo(100, within(1.0));
            assertThat(img.layout().height()).isCloseTo(100, within(1.0));
        }

        @Test
        void imageInFlexContainer() {
            var slideStyle = new ComputedStyle();
            slideStyle.set("display", "flex");
            slideStyle.set("flex-direction", "row");
            slideStyle.set("gap", "10px");

            var imgStyle = new ComputedStyle();
            imgStyle.set("width", "80px");
            imgStyle.set("height", "80px");
            var img1 = makeImage("a.png", imgStyle);
            var img2 = makeImage("b.png", imgStyle);
            var img3 = makeImage("c.png", imgStyle);

            var slide = makeSlide(slideStyle, List.of(img1, img2, img3));
            engine.computeLayout(slide);

            // Images should be laid out in a row with gaps
            assertThat(img1.layout().x()).isCloseTo(0, within(1.0));
            assertThat(img2.layout().x()).isCloseTo(90, within(1.0)); // 80 + 10 gap
            assertThat(img3.layout().x()).isCloseTo(180, within(1.0)); // 80 + 10 + 80 + 10
        }

        @Test
        void imageWithMaxWidth() {
            var slideStyle = new ComputedStyle();
            slideStyle.set("flex-direction", "column");

            var imgStyle = new ComputedStyle();
            imgStyle.set("width", "500px");
            imgStyle.set("height", "500px");
            imgStyle.set("max-width", "200px");
            imgStyle.set("max-height", "150px");
            var img = makeImage("large.png", imgStyle);

            var slide = makeSlide(slideStyle, List.of(img));
            engine.computeLayout(slide);

            // Should be constrained by max-width/max-height
            assertThat(img.layout().width()).isLessThanOrEqualTo(200);
            assertThat(img.layout().height()).isLessThanOrEqualTo(150);
        }
    }
}
