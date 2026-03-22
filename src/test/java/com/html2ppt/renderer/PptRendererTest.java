package com.html2ppt.renderer;

import com.html2ppt.model.*;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.xslf.usermodel.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class PptRendererTest {

    private final PptRenderer renderer = new PptRenderer();

    private SlideNode.Slide makeSlide(ComputedStyle style, List<SlideNode> children) {
        var slide = new SlideNode.Slide(style, children);
        slide.setLayout(new LayoutResult(0, 0, 720, 405));
        return slide;
    }

    private SlideNode.TextBlock makeTextBlock(String text, double fontSize) {
        var style = new ComputedStyle();
        style.set("font-size", fontSize + "px");
        return new SlideNode.TextBlock(List.of(new TextRun(text)), style);
    }

    @Nested
    class RenderPresentation {
        @Test
        void emptyPresentation() throws IOException {
            XMLSlideShow pptx = renderer.renderPresentation(List.of(), "16x9");
            assertThat(pptx.getSlides()).isEmpty();
            pptx.close();
        }

        @Test
        void singleEmptySlide() throws IOException {
            var slide = makeSlide(new ComputedStyle(), List.of());
            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");
            assertThat(pptx.getSlides()).hasSize(1);
            pptx.close();
        }

        @Test
        void multipleSlides() throws IOException {
            var s1 = makeSlide(new ComputedStyle(), List.of());
            var s2 = makeSlide(new ComputedStyle(), List.of());
            var s3 = makeSlide(new ComputedStyle(), List.of());
            XMLSlideShow pptx = renderer.renderPresentation(List.of(s1, s2, s3), "16x9");
            assertThat(pptx.getSlides()).hasSize(3);
            pptx.close();
        }

        @Test
        void layout16x9() throws IOException {
            var slide = makeSlide(new ComputedStyle(), List.of());
            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");
            Dimension size = pptx.getPageSize();
            // 720pt * 12700 = 9144000 EMU width
            assertThat(size.getWidth()).isGreaterThan(0);
            pptx.close();
        }

        @Test
        void layout4x3() throws IOException {
            var slide = makeSlide(new ComputedStyle(), List.of());
            slide.setLayout(new LayoutResult(0, 0, 720, 540));
            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "4x3");
            Dimension size = pptx.getPageSize();
            assertThat(size.getWidth()).isGreaterThan(0);
            pptx.close();
        }
    }

    @Nested
    class RenderTextBlock {
        @Test
        void textBlockRendered() throws IOException {
            var textStyle = new ComputedStyle();
            textStyle.set("font-size", "24px");
            var tb = new SlideNode.TextBlock(
                List.of(new TextRun("Hello World")), textStyle);
            tb.setLayout(new LayoutResult(50, 50, 200, 40));

            var slide = makeSlide(new ComputedStyle(), List.of(tb));
            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");

            XSLFSlide xslfSlide = pptx.getSlides().get(0);
            // Should have at least one shape (the text box)
            assertThat(xslfSlide.getShapes()).isNotEmpty();
            pptx.close();
        }

        @Test
        void styledTextRuns() throws IOException {
            var textStyle = new ComputedStyle();
            var runs = List.of(
                new TextRun("Bold", true, false, false, "FF0000", 18.0, "Arial"),
                new TextRun(" Normal")
            );
            var tb = new SlideNode.TextBlock(runs, textStyle);
            tb.setLayout(new LayoutResult(10, 10, 300, 50));

            var slide = makeSlide(new ComputedStyle(), List.of(tb));
            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");

            assertThat(pptx.getSlides().get(0).getShapes()).isNotEmpty();
            pptx.close();
        }
    }

    @Nested
    class RenderBox {
        @Test
        void boxWithBackground() throws IOException {
            var boxStyle = new ComputedStyle();
            boxStyle.set("background-color", "#003366");
            var box = new SlideNode.Box(boxStyle, List.of());
            box.setLayout(new LayoutResult(10, 10, 200, 100));

            var slide = makeSlide(new ComputedStyle(), List.of(box));
            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");

            XSLFSlide xslfSlide = pptx.getSlides().get(0);
            assertThat(xslfSlide.getShapes()).isNotEmpty();
            pptx.close();
        }

        @Test
        void boxWithChildren() throws IOException {
            var childStyle = new ComputedStyle();
            var child = new SlideNode.TextBlock(List.of(new TextRun("Inside box")), childStyle);
            child.setLayout(new LayoutResult(5, 5, 100, 30));

            var boxStyle = new ComputedStyle();
            boxStyle.set("background-color", "#F0F0F0");
            var box = new SlideNode.Box(boxStyle, List.of(child));
            box.setLayout(new LayoutResult(10, 10, 200, 100));

            var slide = makeSlide(new ComputedStyle(), List.of(box));
            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");

            assertThat(pptx.getSlides().get(0).getShapes()).isNotEmpty();
            pptx.close();
        }

        @Test
        void boxWithBorderRadius() throws IOException {
            var boxStyle = new ComputedStyle();
            boxStyle.set("background-color", "#FFFFFF");
            boxStyle.set("border-radius", "8px");
            var box = new SlideNode.Box(boxStyle, List.of());
            box.setLayout(new LayoutResult(10, 10, 200, 100));

            var slide = makeSlide(new ComputedStyle(), List.of(box));
            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");

            assertThat(pptx.getSlides().get(0).getShapes()).isNotEmpty();
            pptx.close();
        }
    }

    @Nested
    class RenderShape {
        @Test
        void shapeBlockLine() throws IOException {
            var shapeStyle = new ComputedStyle();
            shapeStyle.set("border-color", "#000000");
            var shape = new SlideNode.ShapeBlock("line", shapeStyle);
            shape.setLayout(new LayoutResult(0, 100, 720, 2));

            var slide = makeSlide(new ComputedStyle(), List.of(shape));
            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");

            assertThat(pptx.getSlides().get(0).getShapes()).isNotEmpty();
            pptx.close();
        }

        @Test
        void shapeBlockRect() throws IOException {
            var shapeStyle = new ComputedStyle();
            shapeStyle.set("background-color", "#FF0000");
            var shape = new SlideNode.ShapeBlock("rect", shapeStyle);
            shape.setLayout(new LayoutResult(50, 50, 100, 80));

            var slide = makeSlide(new ComputedStyle(), List.of(shape));
            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");

            assertThat(pptx.getSlides().get(0).getShapes()).isNotEmpty();
            pptx.close();
        }
    }

    @Nested
    class RenderTable {
        @Test
        void simpleTable() throws IOException {
            var row1 = List.of(new TableCell("A"), new TableCell("B"));
            var row2 = List.of(new TableCell("C"), new TableCell("D"));
            var tableStyle = new ComputedStyle();
            var table = new SlideNode.TableBlock(List.of(row1, row2), tableStyle);
            table.setLayout(new LayoutResult(20, 20, 400, 200));

            var slide = makeSlide(new ComputedStyle(), List.of(table));
            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");

            assertThat(pptx.getSlides().get(0).getShapes()).isNotEmpty();
            pptx.close();
        }
    }

    @Nested
    class SlideBackground {
        @Test
        void slideWithBackgroundColor() throws IOException {
            var slideStyle = new ComputedStyle();
            slideStyle.set("background-color", "#003366");
            var slide = makeSlide(slideStyle, List.of());
            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");

            // Slide should exist and have a background
            assertThat(pptx.getSlides()).hasSize(1);
            pptx.close();
        }
    }

    @Nested
    class Metadata {
        @Test
        void setMeta() throws IOException {
            var slide = makeSlide(new ComputedStyle(), List.of());
            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");
            renderer.setMeta(pptx, "Test Title", "Author Name", "Subject");

            assertThat(pptx.getProperties().getCoreProperties().getTitle()).isEqualTo("Test Title");
            assertThat(pptx.getProperties().getCoreProperties().getCreator()).isEqualTo("Author Name");
            pptx.close();
        }

        @Test
        void setMetaWithNulls() throws IOException {
            var slide = makeSlide(new ComputedStyle(), List.of());
            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");
            renderer.setMeta(pptx, null, null, null);
            pptx.close();
        }
    }

    @Nested
    class MasterLayouts {
        @Test
        void titleLayoutMapsTitleAndSubtitle() throws IOException {
            var title = makeTextBlock("Quarterly Review", 44);
            var subtitle = makeTextBlock("Q1 2026", 24);
            var slide = makeSlide(new ComputedStyle(), List.of(title, subtitle));
            slide.setLayoutType("title");

            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");
            XSLFSlide renderedSlide = pptx.getSlides().get(0);

            assertThat(extractTextContents(renderedSlide))
                .contains("Quarterly Review", "Q1 2026")
                .allSatisfy(text -> assertThat(text).doesNotContain("Click to edit"));
            pptx.close();
        }

        @Test
        void sectionLayoutMapsSecondHeadingIntoBodyPlaceholder() throws IOException {
            var title = makeTextBlock("Getting Started", 40);
            var subtitle = makeTextBlock("Installation and Basic Usage", 22);
            var slide = makeSlide(new ComputedStyle(), List.of(title, subtitle));
            slide.setLayoutType("section");

            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");
            XSLFSlide renderedSlide = pptx.getSlides().get(0);

            XSLFTextShape titleShape = findPlaceholder(renderedSlide, Placeholder.TITLE);
            XSLFTextShape bodyShape = findPlaceholder(renderedSlide, Placeholder.BODY, Placeholder.CONTENT);

            assertThat(titleShape.getText()).isEqualTo("Getting Started");
            assertThat(bodyShape.getText()).isEqualTo("Installation and Basic Usage");
            assertThat(bodyShape.getText()).doesNotContain("Click to edit");
            pptx.close();
        }

        @Test
        void twoContentLayoutDistributesBodyBlocksAcrossPlaceholders() throws IOException {
            var title = makeTextBlock("Overview", 36);
            var left = makeTextBlock("Left pane", 18);
            var right = makeTextBlock("Right pane", 18);
            var slide = makeSlide(new ComputedStyle(), List.of(title, left, right));
            slide.setLayoutType("two-content");

            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");
            XSLFSlide renderedSlide = pptx.getSlides().get(0);

            assertThat(extractTextContents(renderedSlide))
                .contains("Overview", "Left pane", "Right pane")
                .allSatisfy(text -> assertThat(text).doesNotContain("Click to edit"));
            pptx.close();
        }

        @Test
        void titleContentLayoutClearsUnusedBodyPlaceholderText() throws IOException {
            var title = makeTextBlock("Agenda", 36);
            var slide = makeSlide(new ComputedStyle(), List.of(title));
            slide.setLayoutType("title-content");

            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");
            XSLFSlide renderedSlide = pptx.getSlides().get(0);

            assertThat(renderedSlide.getShapes())
                .filteredOn(XSLFTextShape.class::isInstance)
                .map(XSLFTextShape.class::cast)
                .extracting(XSLFTextShape::getText)
                .contains("Agenda")
                .allSatisfy(text -> assertThat(text).doesNotContain("Click to edit"));
            pptx.close();
        }

        @Test
        void unknownLayoutFallsBackToBlankRendering() throws IOException {
            var title = makeTextBlock("Fallback Title", 36);
            title.setLayout(new LayoutResult(60, 60, 240, 40));
            var body = makeTextBlock("Rendered on blank slide", 20);
            body.setLayout(new LayoutResult(60, 120, 300, 30));
            var slide = makeSlide(new ComputedStyle(), List.of(title, body));
            slide.setLayoutType("unknown-layout");

            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");
            XSLFSlide renderedSlide = pptx.getSlides().get(0);

            assertThat(extractTextContents(renderedSlide))
                .contains("Fallback Title", "Rendered on blank slide");
            pptx.close();
        }

        @Test
        void blankLayoutRendersChildrenAsRegularShapes() throws IOException {
            var title = makeTextBlock("Thank You!", 48);
            title.setLayout(new LayoutResult(120, 100, 300, 60));
            var subtitle = makeTextBlock("Questions?", 24);
            subtitle.setLayout(new LayoutResult(120, 180, 220, 40));

            var slideStyle = new ComputedStyle();
            slideStyle.set("background-color", "#1a1a2e");
            var slide = makeSlide(slideStyle, List.of(title, subtitle));
            slide.setLayoutType("blank");

            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");
            XSLFSlide renderedSlide = pptx.getSlides().get(0);

            assertThat(renderedSlide.getShapes())
                .filteredOn(XSLFTextShape.class::isInstance)
                .map(XSLFTextShape.class::cast)
                .extracting(XSLFTextShape::getText)
                .contains("Thank You!", "Questions?");
            pptx.close();
        }

        private List<String> extractTextContents(XSLFSlide slide) {
            return slide.getShapes().stream()
                .filter(XSLFTextShape.class::isInstance)
                .map(XSLFTextShape.class::cast)
                .map(XSLFTextShape::getText)
                .toList();
        }

        private XSLFTextShape findPlaceholder(XSLFSlide slide, Placeholder... placeholders) {
            return slide.getShapes().stream()
                .filter(XSLFTextShape.class::isInstance)
                .map(XSLFTextShape.class::cast)
                .filter(shape -> {
                    for (Placeholder placeholder : placeholders) {
                        if (placeholder.equals(shape.getTextType())) {
                            return true;
                        }
                    }
                    return false;
                })
                .findFirst()
                .orElseThrow();
        }
    }

    @Nested
    class MasterLayoutHelpers {
        @Test
        void mapLayoutTypeSupportsAliasesAndFallback() throws Exception {
            assertThat(invokeMapLayoutType("title")).isEqualTo(SlideLayout.TITLE);
            assertThat(invokeMapLayoutType("title-body")).isEqualTo(SlideLayout.TITLE_AND_CONTENT);
            assertThat(invokeMapLayoutType("section-header")).isEqualTo(SlideLayout.SECTION_HEADER);
            assertThat(invokeMapLayoutType("two-column")).isEqualTo(SlideLayout.TWO_OBJ);
            assertThat(invokeMapLayoutType("picture-caption")).isEqualTo(SlideLayout.PIC_TX);
            assertThat(invokeMapLayoutType("unknown-layout")).isEqualTo(SlideLayout.BLANK);
        }

        @Test
        void extractPlaceholderContentUsesSubtitleWhenPlaceholderExists() throws Exception {
            var title = makeTextBlock("Title", 40);
            var subtitle = makeTextBlock("Subtitle", 22);
            var body = makeTextBlock("Body", 18);

            Object content = invokeExtractPlaceholderContent(List.<SlideNode>of(title, subtitle, body), true);

            assertThat(readRecordValue(content, "titleText")).isEqualTo("Title");
            assertThat(readRecordValue(content, "subtitleText")).isEqualTo("Subtitle");
            assertThat(readRecordValue(content, "bodyBlocks")).isEqualTo(List.of("Body"));
        }

        @Test
        void extractPlaceholderContentTreatsSubtitleAsBodyWhenNoSubtitlePlaceholder() throws Exception {
            var title = makeTextBlock("Title", 40);
            var subtitle = makeTextBlock("Subtitle", 22);
            var body = makeTextBlock("Body", 18);

            Object content = invokeExtractPlaceholderContent(List.<SlideNode>of(title, subtitle, body), false);

            assertThat(readRecordValue(content, "titleText")).isEqualTo("Title");
            assertThat(readRecordValue(content, "subtitleText")).isNull();
            assertThat(readRecordValue(content, "bodyBlocks")).isEqualTo(List.of("Subtitle", "Body"));
        }

        @Test
        void joinParagraphsReturnsNullForEmptyList() throws Exception {
            assertThat(invokeJoinParagraphs(List.of())).isNull();
            assertThat(invokeJoinParagraphs(List.of("A", "B"))).isEqualTo("A\nB");
        }

        private SlideLayout invokeMapLayoutType(String layoutType) throws Exception {
            var method = PptRenderer.class.getDeclaredMethod("mapLayoutType", String.class);
            method.setAccessible(true);
            return (SlideLayout) method.invoke(renderer, layoutType);
        }

        private Object invokeExtractPlaceholderContent(List<SlideNode> children, boolean hasSubtitlePlaceholder) throws Exception {
            var method = PptRenderer.class.getDeclaredMethod("extractPlaceholderContent", List.class, boolean.class);
            method.setAccessible(true);
            return method.invoke(renderer, children, hasSubtitlePlaceholder);
        }

        private String invokeJoinParagraphs(List<String> paragraphs) throws Exception {
            var method = PptRenderer.class.getDeclaredMethod("joinParagraphs", List.class);
            method.setAccessible(true);
            return (String) method.invoke(renderer, paragraphs);
        }

        private Object readRecordValue(Object record, String accessorName) throws Exception {
            var method = record.getClass().getDeclaredMethod(accessorName);
            method.setAccessible(true);
            return method.invoke(record);
        }
    }

    @Nested
    class RichRendering {
        @Test
        void speakerNotesAreWrittenToNotesSlide() throws IOException {
            var slide = makeSlide(new ComputedStyle(), List.of());
            slide.setNotes("Presenter note");

            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");
            XSLFSlide renderedSlide = pptx.getSlides().get(0);
            XSLFNotes notes = pptx.getNotesSlide(renderedSlide);

            assertThat(notes.getShapes())
                .filteredOn(XSLFTextShape.class::isInstance)
                .map(XSLFTextShape.class::cast)
                .extracting(XSLFTextShape::getText)
                .anySatisfy(text -> assertThat(text).contains("Presenter note"));
            pptx.close();
        }

        @Test
        void slideGradientBackgroundWritesGradientFill() throws IOException {
            var slideStyle = new ComputedStyle();
            slideStyle.set("background", "linear-gradient(to right, #000000, rgba(255,255,255,0.5))");
            var slide = makeSlide(slideStyle, List.of());

            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");
            XSLFSlide renderedSlide = pptx.getSlides().get(0);
            var bgPr = renderedSlide.getXmlObject().getCSld().getBg().getBgPr();

            assertThat(bgPr.isSetGradFill()).isTrue();
            assertThat(bgPr.getGradFill().getGsLst().sizeOfGsArray()).isEqualTo(2);
            pptx.close();
        }

        @Test
        void boxGradientBackgroundCreatesGradientShape() throws IOException {
            var boxStyle = new ComputedStyle();
            boxStyle.set("background", "linear-gradient(to bottom, #ff0000, #00ff00)");
            var box = new SlideNode.Box(boxStyle, List.of());
            box.setLayout(new LayoutResult(10, 20, 200, 100));
            var slide = makeSlide(new ComputedStyle(), List.of(box));

            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");
            XSLFAutoShape shape = pptx.getSlides().get(0).getShapes().stream()
                .filter(XSLFAutoShape.class::isInstance)
                .map(XSLFAutoShape.class::cast)
                .findFirst()
                .orElseThrow();
            var ctShape = (org.openxmlformats.schemas.presentationml.x2006.main.CTShape) shape.getXmlObject();

            assertThat(ctShape.getSpPr().isSetGradFill()).isTrue();
            pptx.close();
        }

        @Test
        void imageContainRespectsAspectRatio(@TempDir Path tempDir) throws IOException {
            Path imagePath = writeImage(tempDir.resolve("contain.png"), 80, 40, "png");

            var imageStyle = new ComputedStyle();
            imageStyle.set("object-fit", "contain");
            var image = new SlideNode.ImageBlock(imagePath.toString(), imageStyle);
            image.setLayout(new LayoutResult(0, 0, 100, 100));
            var slide = makeSlide(new ComputedStyle(), List.of(image));

            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");
            XSLFPictureShape picture = pptx.getSlides().get(0).getShapes().stream()
                .filter(XSLFPictureShape.class::isInstance)
                .map(XSLFPictureShape.class::cast)
                .findFirst()
                .orElseThrow();

            assertThat(picture.getAnchor().getWidth()).isCloseTo(100, within(0.5));
            assertThat(picture.getAnchor().getHeight()).isCloseTo(50, within(0.5));
            assertThat(picture.getAnchor().getY()).isCloseTo(25, within(0.5));
            pptx.close();
        }

        @Test
        void imageCoverAppliesSourceClipping(@TempDir Path tempDir) throws IOException {
            Path imagePath = writeImage(tempDir.resolve("cover.png"), 40, 80, "png");

            var imageStyle = new ComputedStyle();
            imageStyle.set("object-fit", "cover");
            var image = new SlideNode.ImageBlock(imagePath.toString(), imageStyle);
            image.setLayout(new LayoutResult(0, 0, 100, 50));
            var slide = makeSlide(new ComputedStyle(), List.of(image));

            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");
            XSLFPictureShape picture = pptx.getSlides().get(0).getShapes().stream()
                .filter(XSLFPictureShape.class::isInstance)
                .map(XSLFPictureShape.class::cast)
                .findFirst()
                .orElseThrow();
            var ctPicture = (org.openxmlformats.schemas.presentationml.x2006.main.CTPicture) picture.getXmlObject();

            assertThat(ctPicture.getBlipFill().isSetSrcRect()).isTrue();
            pptx.close();
        }

        private Path writeImage(Path path, int width, int height, String format) throws IOException {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    image.setRGB(x, y, (x + y) % 2 == 0 ? 0x336699 : 0x99ccff);
                }
            }
            ImageIO.write(image, format, path.toFile());
            return path;
        }
    }

    @Nested
    class RenderingHelpers {
        @Test
        void calculateObjectFitRectSupportsModes() throws Exception {
            Rectangle2D contain = invokeObjectFit("contain", 0, 0, 100, 100, 200, 100);
            Rectangle2D cover = invokeObjectFit("cover", 0, 0, 100, 100, 200, 100);
            Rectangle2D none = invokeObjectFit("none", 0, 0, 100, 100, 40, 20);
            Rectangle2D scaleDown = invokeObjectFit("scale-down", 0, 0, 100, 100, 200, 100);
            Rectangle2D fill = invokeObjectFit("fill", 0, 0, 100, 100, 200, 100);

            assertThat(contain.getWidth()).isEqualTo(100);
            assertThat(contain.getHeight()).isEqualTo(50);
            assertThat(cover.getWidth()).isEqualTo(200);
            assertThat(cover.getHeight()).isEqualTo(100);
            assertThat(none.getWidth()).isEqualTo(40);
            assertThat(none.getHeight()).isEqualTo(20);
            assertThat(scaleDown.getWidth()).isEqualTo(100);
            assertThat(scaleDown.getHeight()).isEqualTo(50);
            assertThat(fill.getWidth()).isEqualTo(100);
            assertThat(fill.getHeight()).isEqualTo(100);
        }

        @Test
        void helperMethodsHandleCommonCases() throws Exception {
            assertThat(PptRenderer.parseColorWithAlpha("rgba(255, 0, 0, 0.5)").getAlpha()).isEqualTo(128);
            assertThat(PptRenderer.parseColorWithAlpha("bad").getRGB()).isEqualTo(Color.BLACK.getRGB());

            assertThat(invokeInferPicType("image.png")).isEqualTo(org.apache.poi.sl.usermodel.PictureData.PictureType.PNG);
            assertThat(invokeInferPicType("anim.gif")).isEqualTo(org.apache.poi.sl.usermodel.PictureData.PictureType.GIF);
            assertThat(invokeInferPicType("vector.svg")).isEqualTo(org.apache.poi.sl.usermodel.PictureData.PictureType.SVG);
            assertThat(invokeInferPicType("photo.jpg")).isEqualTo(org.apache.poi.sl.usermodel.PictureData.PictureType.JPEG);

            assertThat(invokeDetermineShapeType("50%", 100, 100)).isEqualTo(org.apache.poi.sl.usermodel.ShapeType.ELLIPSE);
            assertThat(invokeDetermineShapeType("8px", 100, 60)).isEqualTo(org.apache.poi.sl.usermodel.ShapeType.ROUND_RECT);
            assertThat(invokeDetermineShapeType(null, 100, 60)).isEqualTo(org.apache.poi.sl.usermodel.ShapeType.RECT);

            assertThat(invokeNeedsClipping(new Rectangle2D.Double(-10, 0, 120, 50), 0, 0, 100, 50)).isTrue();
            assertThat(invokeNeedsClipping(new Rectangle2D.Double(0, 0, 100, 50), 0, 0, 100, 50)).isFalse();

            assertThat(invokeHexToBytes("336699")).containsExactly((byte) 0x33, (byte) 0x66, (byte) 0x99);
            assertThat(invokeHexToBytes("bad")).containsExactly((byte) 0, (byte) 0, (byte) 0);
        }

        private Rectangle2D invokeObjectFit(String fit, double cx, double cy, double cw, double ch, double iw, double ih) throws Exception {
            var method = PptRenderer.class.getDeclaredMethod(
                "calculateObjectFitRect", String.class, double.class, double.class, double.class, double.class, double.class, double.class);
            method.setAccessible(true);
            return (Rectangle2D) method.invoke(renderer, fit, cx, cy, cw, ch, iw, ih);
        }

        private org.apache.poi.sl.usermodel.PictureData.PictureType invokeInferPicType(String path) throws Exception {
            var method = PptRenderer.class.getDeclaredMethod("inferPicType", String.class);
            method.setAccessible(true);
            return (org.apache.poi.sl.usermodel.PictureData.PictureType) method.invoke(renderer, path);
        }

        private org.apache.poi.sl.usermodel.ShapeType invokeDetermineShapeType(String radius, double width, double height) throws Exception {
            var method = PptRenderer.class.getDeclaredMethod("determineShapeType", String.class, double.class, double.class);
            method.setAccessible(true);
            return (org.apache.poi.sl.usermodel.ShapeType) method.invoke(renderer, radius, width, height);
        }

        private boolean invokeNeedsClipping(Rectangle2D rect, double cx, double cy, double cw, double ch) throws Exception {
            var method = PptRenderer.class.getDeclaredMethod("needsClipping", Rectangle2D.class, double.class, double.class, double.class, double.class);
            method.setAccessible(true);
            return (boolean) method.invoke(renderer, rect, cx, cy, cw, ch);
        }

        private byte[] invokeHexToBytes(String hex) throws Exception {
            var method = PptRenderer.class.getDeclaredMethod("hexToBytes", String.class);
            method.setAccessible(true);
            return (byte[]) method.invoke(renderer, hex);
        }
    }

    @Nested
    class TextBlockStyling {
        @Test
        void textBlockWithBackground() throws IOException {
            var textStyle = new ComputedStyle();
            textStyle.set("background-color", "#FFFF00");
            var tb = new SlideNode.TextBlock(List.of(new TextRun("Highlighted")), textStyle);
            tb.setLayout(new LayoutResult(10, 10, 200, 40));

            var slide = makeSlide(new ComputedStyle(), List.of(tb));
            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");
            assertThat(pptx.getSlides().get(0).getShapes()).isNotEmpty();
            pptx.close();
        }

        @Test
        void textBlockWithAlignment() throws IOException {
            var textStyle = new ComputedStyle();
            textStyle.set("text-align", "center");
            textStyle.set("vertical-align", "middle");
            var tb = new SlideNode.TextBlock(List.of(new TextRun("Centered")), textStyle);
            tb.setLayout(new LayoutResult(10, 10, 200, 40));

            var slide = makeSlide(new ComputedStyle(), List.of(tb));
            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");
            assertThat(pptx.getSlides().get(0).getShapes()).isNotEmpty();
            pptx.close();
        }

        @Test
        void textBlockRightAlign() throws IOException {
            var textStyle = new ComputedStyle();
            textStyle.set("text-align", "right");
            textStyle.set("vertical-align", "bottom");
            var tb = new SlideNode.TextBlock(List.of(new TextRun("Right")), textStyle);
            tb.setLayout(new LayoutResult(10, 10, 200, 40));

            var slide = makeSlide(new ComputedStyle(), List.of(tb));
            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");
            assertThat(pptx.getSlides().get(0).getShapes()).isNotEmpty();
            pptx.close();
        }
    }

    @Nested
    class TableStyling {
        @Test
        void tableWithStyledCells() throws IOException {
            var cellStyle = new ComputedStyle();
            cellStyle.set("background-color", "#003366");
            cellStyle.set("color", "white");
            cellStyle.set("font-weight", "bold");
            cellStyle.set("font-size", "14pt");

            var row = List.of(new TableCell("Header", cellStyle), new TableCell("Value"));
            var tableStyle = new ComputedStyle();
            var table = new SlideNode.TableBlock(List.of(row), tableStyle);
            table.setLayout(new LayoutResult(20, 20, 400, 100));

            var slide = makeSlide(new ComputedStyle(), List.of(table));
            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");
            assertThat(pptx.getSlides().get(0).getShapes()).isNotEmpty();
            pptx.close();
        }

        @Test
        void emptyTableSkipped() throws IOException {
            var table = new SlideNode.TableBlock(List.of(), new ComputedStyle());
            table.setLayout(new LayoutResult(20, 20, 400, 100));

            var slide = makeSlide(new ComputedStyle(), List.of(table));
            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");
            // Empty table should not crash
            assertThat(pptx.getSlides()).hasSize(1);
            pptx.close();
        }
    }

    @Nested
    class ShapeStyling {
        @Test
        void shapeWithBorder() throws IOException {
            var shapeStyle = new ComputedStyle();
            shapeStyle.set("background-color", "#FF0000");
            shapeStyle.set("border-color", "#000000");
            shapeStyle.set("border-width", "2px");
            var shape = new SlideNode.ShapeBlock("rect", shapeStyle);
            shape.setLayout(new LayoutResult(50, 50, 100, 80));

            var slide = makeSlide(new ComputedStyle(), List.of(shape));
            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");
            assertThat(pptx.getSlides().get(0).getShapes()).isNotEmpty();
            pptx.close();
        }

        @Test
        void ellipseShape() throws IOException {
            var shapeStyle = new ComputedStyle();
            shapeStyle.set("background-color", "#00FF00");
            var shape = new SlideNode.ShapeBlock("ellipse", shapeStyle);
            shape.setLayout(new LayoutResult(50, 50, 100, 100));

            var slide = makeSlide(new ComputedStyle(), List.of(shape));
            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");
            assertThat(pptx.getSlides().get(0).getShapes()).isNotEmpty();
            pptx.close();
        }

        @Test
        void roundRectShape() throws IOException {
            var shapeStyle = new ComputedStyle();
            shapeStyle.set("background-color", "#0000FF");
            var shape = new SlideNode.ShapeBlock("roundRect", shapeStyle);
            shape.setLayout(new LayoutResult(50, 50, 100, 100));

            var slide = makeSlide(new ComputedStyle(), List.of(shape));
            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");
            assertThat(pptx.getSlides().get(0).getShapes()).isNotEmpty();
            pptx.close();
        }
    }

    @Nested
    class NestedBoxes {
        @Test
        void deeplyNestedBoxes() throws IOException {
            var innerText = new SlideNode.TextBlock(List.of(new TextRun("Deep")), new ComputedStyle());
            innerText.setLayout(new LayoutResult(5, 5, 80, 20));

            var innerBoxStyle = new ComputedStyle();
            innerBoxStyle.set("background-color", "#EEEEEE");
            var innerBox = new SlideNode.Box(innerBoxStyle, List.of(innerText));
            innerBox.setLayout(new LayoutResult(10, 10, 100, 50));

            var outerBoxStyle = new ComputedStyle();
            outerBoxStyle.set("background-color", "#CCCCCC");
            var outerBox = new SlideNode.Box(outerBoxStyle, List.of(innerBox));
            outerBox.setLayout(new LayoutResult(20, 20, 200, 100));

            var slide = makeSlide(new ComputedStyle(), List.of(outerBox));
            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");
            assertThat(pptx.getSlides().get(0).getShapes()).isNotEmpty();
            pptx.close();
        }

        @Test
        void boxWithBorder() throws IOException {
            var boxStyle = new ComputedStyle();
            boxStyle.set("background-color", "#FFFFFF");
            boxStyle.set("border-color", "#000000");
            boxStyle.set("border-width", "1px");
            var box = new SlideNode.Box(boxStyle, List.of());
            box.setLayout(new LayoutResult(10, 10, 200, 100));

            var slide = makeSlide(new ComputedStyle(), List.of(box));
            XMLSlideShow pptx = renderer.renderPresentation(List.of(slide), "16x9");
            assertThat(pptx.getSlides().get(0).getShapes()).isNotEmpty();
            pptx.close();
        }
    }

    @Nested
    class FileOutput {
        @Test
        void renderToFile(@TempDir Path tempDir) throws IOException {
            var slide = makeSlide(new ComputedStyle(), List.of());
            Path output = tempDir.resolve("test.pptx");
            renderer.renderToFile(List.of(slide), "16x9", output);

            assertThat(Files.exists(output)).isTrue();
            assertThat(Files.size(output)).isGreaterThan(0);

            // Verify it's a valid PPTX
            XMLSlideShow verify = new XMLSlideShow(Files.newInputStream(output));
            assertThat(verify.getSlides()).hasSize(1);
            verify.close();
        }
    }

    @Nested
    class ParseColor {
        @Test
        void parseValidHexColor() throws Exception {
            // Use reflection to test the private static method
            var method = PptRenderer.class.getDeclaredMethod("parseColor", String.class);
            method.setAccessible(true);
            var color = (java.awt.Color) method.invoke(null, "FF0000");
            assertThat(color.getRed()).isEqualTo(255);
            assertThat(color.getGreen()).isEqualTo(0);
            assertThat(color.getBlue()).isEqualTo(0);
        }

        @Test
        void parseBlueColor() throws Exception {
            var method = PptRenderer.class.getDeclaredMethod("parseColor", String.class);
            method.setAccessible(true);
            var color = (java.awt.Color) method.invoke(null, "0000FF");
            assertThat(color.getBlue()).isEqualTo(255);
        }
    }
}
