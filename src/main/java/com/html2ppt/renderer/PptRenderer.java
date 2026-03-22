package com.html2ppt.renderer;

import com.html2ppt.model.*;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.sl.usermodel.VerticalAlignment;
import org.apache.poi.xslf.usermodel.*;
import org.openxmlformats.schemas.drawingml.x2006.main.*;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders SlideNode trees (with computed layouts) into PPTX via Apache POI.
 * Follows spec/SPEC.md — update spec first before modifying.
 */
public class PptRenderer {

    /** Default font — CJK-capable, fallback chain for cross-platform. */
    private static final String DEFAULT_FONT = "Microsoft YaHei";

    private XMLSlideShow currentPptx;

    /**
     * Render slides to a PPTX file.
     */
    public void renderToFile(List<SlideNode.Slide> slides, String layout, Path outputPath) throws IOException {
        try (XMLSlideShow pptx = renderPresentation(slides, layout);
             OutputStream out = Files.newOutputStream(outputPath)) {
            pptx.write(out);
        }
    }

    /**
     * Render slides to an in-memory XMLSlideShow.
     */
    public XMLSlideShow renderPresentation(List<SlideNode.Slide> slides, String layout) {
        XMLSlideShow pptx = new XMLSlideShow();
        this.currentPptx = pptx;

        // Set slide dimensions
        switch (layout != null ? layout : "16x9") {
            case "4x3" -> pptx.setPageSize(new Dimension(720, 540));
            default -> pptx.setPageSize(new Dimension(720, 405));
        }

        for (SlideNode.Slide slideNode : slides) {
            renderSlide(pptx, slideNode);
        }

        return pptx;
    }

    /**
     * Set presentation metadata.
     */
    public void setMeta(XMLSlideShow pptx, String title, String author, String subject) {
        var coreProps = pptx.getProperties().getCoreProperties();
        if (title != null) coreProps.setTitle(title);
        if (author != null) coreProps.setCreator(author);
        if (subject != null) coreProps.setSubjectProperty(subject);
    }

    private void renderSlide(XMLSlideShow pptx, SlideNode.Slide slideNode) {
        XSLFSlide slide;
        
        // Check if a master layout is specified
        String layoutType = slideNode.layoutType();
        if (layoutType != null) {
            slide = createSlideWithLayout(pptx, layoutType, slideNode);
        } else {
            slide = pptx.createSlide();
            
            // Background — check for gradient first, then solid color
            var gradient = slideNode.style().getLinearGradient();
            if (gradient != null) {
                setSlideGradientBackground(slide, gradient);
            } else {
                String bgColor = slideNode.style().getBackgroundColor();
                setSlideBackground(slide, parseColor(bgColor != null ? bgColor : "ffffff"));
            }
            
            // Render children into the slide
            for (SlideNode child : slideNode.children()) {
                renderNode(slide, null, child);
            }
        }
        
        // Add speaker notes if present
        if (slideNode.notes() != null && !slideNode.notes().isEmpty()) {
            addSpeakerNotes(pptx, slide, slideNode.notes());
        }
    }
    
    /**
     * Create a slide using a master layout and fill placeholders.
     */
    private XSLFSlide createSlideWithLayout(XMLSlideShow pptx, String layoutType, SlideNode.Slide slideNode) {
        XSLFSlideMaster master = pptx.getSlideMasters().get(0);
        SlideLayout poiLayout = mapLayoutType(layoutType);
        XSLFSlideLayout layout = master.getLayout(poiLayout);
        
        if (layout == null) {
            // Fallback to blank if layout not found
            layout = master.getLayout(SlideLayout.BLANK);
        }
        
        XSLFSlide slide = pptx.createSlide(layout);
        
        // Apply background if specified (overrides master)
        var gradient = slideNode.style().getLinearGradient();
        if (gradient != null) {
            setSlideGradientBackground(slide, gradient);
        } else {
            String bgColor = slideNode.style().getBackgroundColor();
            if (bgColor != null) {
                setSlideBackground(slide, parseColor(bgColor));
            }
            // If no background specified, use master's background
        }
        
        // For blank layout, render children directly (like custom slides)
        // For other layouts, fill placeholders
        if (poiLayout == SlideLayout.BLANK) {
            for (SlideNode child : slideNode.children()) {
                renderNode(slide, null, child);
            }
        } else {
            fillPlaceholders(slide, slideNode.children());
        }
        
        return slide;
    }
    
    /**
     * Map HTML data-layout values to POI SlideLayout enum.
     */
    private SlideLayout mapLayoutType(String layoutType) {
        return switch (layoutType.toLowerCase()) {
            case "title" -> SlideLayout.TITLE;
            case "title-content", "title-body" -> SlideLayout.TITLE_AND_CONTENT;
            case "section", "section-header" -> SlideLayout.SECTION_HEADER;
            case "two-content", "two-column" -> SlideLayout.TWO_OBJ;
            case "title-only" -> SlideLayout.TITLE_ONLY;
            case "blank" -> SlideLayout.BLANK;
            case "picture-caption" -> SlideLayout.PIC_TX;
            default -> SlideLayout.BLANK;
        };
    }
    
    /**
     * Fill slide placeholders with content from children nodes.
     */
    private void fillPlaceholders(XSLFSlide slide, List<SlideNode> children) {
        boolean hasSubtitlePlaceholder = slide.getShapes().stream()
            .filter(XSLFTextShape.class::isInstance)
            .map(XSLFTextShape.class::cast)
            .anyMatch(this::isSubtitlePlaceholder);

        PlaceholderContent content = extractPlaceholderContent(children, hasSubtitlePlaceholder);
        List<XSLFTextShape> bodyPlaceholders = slide.getShapes().stream()
            .filter(XSLFTextShape.class::isInstance)
            .map(XSLFTextShape.class::cast)
            .filter(this::isBodyPlaceholder)
            .toList();
        int bodyPlaceholderCount = bodyPlaceholders.size();
        int bodyBlockIndex = 0;

        for (XSLFShape shape : slide.getShapes()) {
            if (!(shape instanceof XSLFTextShape textShape) || !textShape.isPlaceholder()) {
                continue;
            }

            if (isTitlePlaceholder(textShape)) {
                setPlaceholderText(textShape, content.titleText());
                continue;
            }

            if (isSubtitlePlaceholder(textShape)) {
                setPlaceholderText(textShape, content.subtitleText());
                continue;
            }

            if (isBodyPlaceholder(textShape)) {
                String bodyText = null;
                if (bodyPlaceholderCount <= 1) {
                    bodyText = joinParagraphs(content.bodyBlocks());
                } else if (bodyBlockIndex < content.bodyBlocks().size()) {
                    bodyText = content.bodyBlocks().get(bodyBlockIndex++);
                }
                setPlaceholderText(textShape, bodyText);
            }
        }
    }

    private PlaceholderContent extractPlaceholderContent(List<SlideNode> children, boolean hasSubtitlePlaceholder) {
        String titleText = null;
        String subtitleText = null;
        List<String> bodyBlocks = new ArrayList<>();

        for (SlideNode child : children) {
            if (!(child instanceof SlideNode.TextBlock text)) {
                continue;
            }

            String plainText = extractPlainText(text).trim();
            if (plainText.isEmpty()) {
                continue;
            }

            Double fontSize = text.style().getFontSize();
            if (titleText == null && isTitleCandidate(fontSize)) {
                titleText = plainText;
                continue;
            }

            if (hasSubtitlePlaceholder && subtitleText == null && titleText != null && isSubtitleCandidate(fontSize)) {
                subtitleText = plainText;
                continue;
            }

            bodyBlocks.add(plainText);
        }

        return new PlaceholderContent(titleText, subtitleText, bodyBlocks);
    }

    private boolean isTitleCandidate(Double fontSize) {
        return fontSize != null && fontSize >= 28;
    }

    private boolean isTitlePlaceholder(XSLFTextShape textShape) {
        Placeholder type = textShape.getTextType();
        return type == Placeholder.TITLE || type == Placeholder.CENTERED_TITLE;
    }

    private boolean isSubtitlePlaceholder(XSLFTextShape textShape) {
        return textShape.getTextType() == Placeholder.SUBTITLE;
    }

    private boolean isBodyPlaceholder(XSLFTextShape textShape) {
        if (!textShape.isPlaceholder()) {
            return false;
        }
        Placeholder type = textShape.getTextType();
        if (type == Placeholder.BODY || type == Placeholder.CONTENT) {
            return true;
        }
        return type == null
            && textShape.getTextPlaceholder() == org.apache.poi.sl.usermodel.TextShape.TextPlaceholder.BODY;
    }

    private boolean isSubtitleCandidate(Double fontSize) {
        return fontSize != null && fontSize >= 20;
    }

    private void setPlaceholderText(XSLFTextShape textShape, String text) {
        textShape.clearText();
        if (text != null && !text.isBlank()) {
            textShape.setText(text);
        }
    }

    private String joinParagraphs(List<String> paragraphs) {
        if (paragraphs.isEmpty()) {
            return null;
        }
        return String.join("\n", paragraphs);
    }

    private record PlaceholderContent(String titleText, String subtitleText, List<String> bodyBlocks) {}
    
    /**
     * Extract plain text from a TextBlock.
     */
    private String extractPlainText(SlideNode.TextBlock text) {
        StringBuilder sb = new StringBuilder();
        for (TextRun run : text.runs()) {
            sb.append(run.text());
        }
        return sb.toString();
    }
    
    private void addSpeakerNotes(XMLSlideShow pptx, XSLFSlide slide, String notesText) {
        XSLFNotes notes = pptx.getNotesSlide(slide);
        // Find the text placeholder in the notes slide and set the text
        for (XSLFShape shape : notes.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                // Check if this is the body placeholder (type 2 = body)
                if (textShape.getTextType() == org.apache.poi.sl.usermodel.Placeholder.BODY) {
                    textShape.clearText();
                    XSLFTextParagraph para = textShape.addNewTextParagraph();
                    XSLFTextRun run = para.addNewTextRun();
                    run.setText(notesText);
                    run.setFontFamily(DEFAULT_FONT);
                    run.setFontSize(12.0);
                    return;
                }
            }
        }
        // Fallback: if no body placeholder found, try any text shape
        for (XSLFShape shape : notes.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                textShape.clearText();
                XSLFTextParagraph para = textShape.addNewTextParagraph();
                XSLFTextRun run = para.addNewTextRun();
                run.setText(notesText);
                return;
            }
        }
    }

    /**
     * Set slide background color via direct XML manipulation.
     * POI's XSLFBackground.setFillColor() doesn't always write the XML correctly.
     */
    private void setSlideBackground(XSLFSlide slide, Color color) {
        var xmlSlide = slide.getXmlObject();
        var cSld = xmlSlide.getCSld();
        var bg = cSld.isSetBg() ? cSld.getBg() : cSld.addNewBg();
        var bgPr = bg.isSetBgPr() ? bg.getBgPr() : bg.addNewBgPr();

        var solidFill = org.openxmlformats.schemas.drawingml.x2006.main.CTSolidColorFillProperties.Factory.newInstance();
        var srgbClr = solidFill.addNewSrgbClr();
        srgbClr.setVal(new byte[]{(byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue()});
        bgPr.setSolidFill(solidFill);

        // Effect list required by schema
        if (!bgPr.isSetEffectLst()) bgPr.addNewEffectLst();
    }

    /**
     * Set slide gradient background via direct XML manipulation.
     */
    private void setSlideGradientBackground(XSLFSlide slide, ComputedStyle.LinearGradient gradient) {
        try {
            var xmlSlide = slide.getXmlObject();
            var cSld = xmlSlide.getCSld();
            var bg = cSld.isSetBg() ? cSld.getBg() : cSld.addNewBg();
            var bgPr = bg.isSetBgPr() ? bg.getBgPr() : bg.addNewBgPr();

            // Clear any existing fill
            if (bgPr.isSetSolidFill()) bgPr.unsetSolidFill();
            if (bgPr.isSetNoFill()) bgPr.unsetNoFill();

            // Create gradient fill
            var gradFill = bgPr.addNewGradFill();
            gradFill.setRotWithShape(true);

            // Linear gradient properties
            var linShade = gradFill.addNewLin();
            double poiAngle = (gradient.angleDegrees() - 90 + 360) % 360;
            linShade.setAng((int) Math.round(poiAngle * 60000));

            // Gradient stops
            var gsLst = gradFill.addNewGsLst();
            for (ComputedStyle.GradientStop stop : gradient.stops()) {
                var gs = gsLst.addNewGs();
                gs.setPos((int) Math.round(stop.position() * 100000));
                var srgbClr = gs.addNewSrgbClr();
                srgbClr.setVal(hexToBytes(stop.colorHex()));
                if (stop.alpha() < 1.0) {
                    var alphaMod = srgbClr.addNewAlpha();
                    alphaMod.setVal((int) Math.round(stop.alpha() * 100000));
                }
            }

            // Effect list required by schema
            if (!bgPr.isSetEffectLst()) bgPr.addNewEffectLst();

        } catch (Exception e) {
            // Fall back to white background
            System.err.println("Warning: Could not apply slide gradient: " + e.getMessage());
            setSlideBackground(slide, Color.WHITE);
        }
    }

    private void renderNode(XSLFSlide slide, XSLFGroupShape group, SlideNode node) {
        switch (node) {
            case SlideNode.Box box -> renderBox(slide, group, box);
            case SlideNode.TextBlock text -> renderTextBlock(slide, group, text);
            case SlideNode.ImageBlock img -> renderImage(slide, group, img);
            case SlideNode.TableBlock table -> renderTable(slide, group, table);
            case SlideNode.ShapeBlock shape -> renderShape(slide, group, shape);
            case SlideNode.Slide s -> {} // Slides don't nest
        }
    }

    private void renderBox(XSLFSlide slide, XSLFGroupShape parentGroup, SlideNode.Box box) {
        LayoutResult lr = box.layout();

        // Check if this box has any visual properties (background or border)
        String bgColorValue = box.style().getBackgroundColor();
        var parsedBgColor = ComputedStyle.parseColorWithAlpha(bgColorValue);
        var gradient = box.style().getLinearGradient();
        String borderColor = ComputedStyle.normalizeColor(box.style().get("border-color"));
        Double borderWidth = box.style().getLength("border-width");
        boolean hasBorder = borderColor != null || (borderWidth != null && borderWidth > 0);
        boolean hasBackground = (parsedBgColor != null && parsedBgColor.alpha() > 0) || gradient != null;
        boolean hasVisual = hasBackground || hasBorder;

        // Only skip if this box has no children AND no visual properties
        if (box.children().isEmpty() && !hasVisual) return;

        // If box has background color or border, render a shape behind it
        if (hasVisual) {
            XSLFAutoShape bgShape = (parentGroup != null)
                ? parentGroup.createAutoShape()
                : slide.createAutoShape();
            bgShape.setShapeType(ShapeType.RECT);
            bgShape.setAnchor(toRect(lr));

            // Apply opacity from CSS
            double opacity = box.style().getOpacity();

            if (gradient != null) {
                // Apply linear gradient
                applyGradient(bgShape, gradient);
            } else if (parsedBgColor != null && parsedBgColor.alpha() > 0) {
                // Use already parsed color with alpha
                int r = Integer.parseInt(parsedBgColor.hex().substring(0, 2), 16);
                int g = Integer.parseInt(parsedBgColor.hex().substring(2, 4), 16);
                int b = Integer.parseInt(parsedBgColor.hex().substring(4, 6), 16);
                // Combine color alpha with element opacity
                int combinedAlpha = (int) Math.round(parsedBgColor.alpha() * opacity * 255);
                Color colorWithOpacity = new Color(r, g, b, combinedAlpha);
                bgShape.setFillColor(colorWithOpacity);
            } else {
                // No fill — transparent background, but shape still needed for border
                bgShape.setFillColor(null);
            }

            if (!hasBorder) {
                bgShape.setLineWidth(0); // No border
            }

            // Border radius → determine shape type
            String borderRadiusValue = box.style().get("border-radius");
            ShapeType shapeType = determineShapeType(borderRadiusValue, lr.width(), lr.height());
            if (shapeType != ShapeType.RECT) {
                bgShape.setShapeType(shapeType);
            }

            // Border
            applyBorder(bgShape, box.style());
            
            // Box shadow
            applyShadow(bgShape, box.style());
        }

        if (box.children().isEmpty()) return;

        // Create group for children
        XSLFGroupShape group = (parentGroup != null)
            ? parentGroup.createGroup()
            : slide.createGroup();
        group.setAnchor(toRect(lr));
        group.setInteriorAnchor(new Rectangle2D.Double(0, 0, lr.width(), lr.height()));

        for (SlideNode child : box.children()) {
            renderNode(slide, group, child);
        }
    }

    private void renderTextBlock(XSLFSlide slide, XSLFGroupShape group, SlideNode.TextBlock textBlock) {
        LayoutResult lr = textBlock.layout();
        XSLFTextBox textBox = (group != null) ? group.createTextBox() : slide.createTextBox();
        textBox.setAnchor(toRect(lr));

        // Zero internal margins — Yoga handles all spacing
        textBox.setLeftInset(0);
        textBox.setRightInset(0);
        textBox.setTopInset(0);
        textBox.setBottomInset(0);
        textBox.setWordWrap(true);

        // Background
        String bgColor = textBlock.style().getBackgroundColor();
        if (bgColor != null) textBox.setFillColor(parseColor(bgColor));

        // Vertical alignment
        String valign = textBlock.style().getVerticalAlign();
        if (valign != null) {
            textBox.setVerticalAlignment(switch (valign) {
                case "middle" -> VerticalAlignment.MIDDLE;
                case "bottom" -> VerticalAlignment.BOTTOM;
                default -> VerticalAlignment.TOP;
            });
        }

        textBox.clearText();
        XSLFTextParagraph para = textBox.addNewTextParagraph();

        // Text alignment
        String align = textBlock.style().getTextAlign();
        if (align != null) {
            para.setTextAlign(switch (align) {
                case "center" -> TextParagraph.TextAlign.CENTER;
                case "right" -> TextParagraph.TextAlign.RIGHT;
                default -> TextParagraph.TextAlign.LEFT;
            });
        }

        // Render text runs
        for (TextRun run : textBlock.runs()) {
            XSLFTextRun textRun = para.addNewTextRun();
            textRun.setText(run.text());
            textRun.setBold(run.bold());
            textRun.setItalic(run.italic());
            textRun.setUnderlined(run.underline());
            textRun.setFontFamily(run.fontFamily() != null ? run.fontFamily() : DEFAULT_FONT);

            if (run.color() != null) textRun.setFontColor(parseColor(run.color()));
            if (run.fontSize() != null) textRun.setFontSize(run.fontSize());

            // Hyperlink support
            if (run.href() != null && !run.href().isEmpty()) {
                textRun.createHyperlink().setAddress(run.href());
            }
        }
    }

    private void renderImage(XSLFSlide slide, XSLFGroupShape group, SlideNode.ImageBlock img) {
        String src = img.src();
        try {
            byte[] imageData = Files.readAllBytes(Path.of(src));
            XSLFPictureData pictureData = currentPptx.addPicture(imageData, inferPicType(src));
            
            // Get container dimensions from layout
            LayoutResult containerLayout = img.layout();
            double containerX = containerLayout.x();
            double containerY = containerLayout.y();
            double containerW = containerLayout.width();
            double containerH = containerLayout.height();
            
            // Get object-fit property
            String objectFit = img.style().getObjectFit();
            if (objectFit == null) objectFit = "fill";  // default
            
            // Get natural image dimensions
            Dimension imgDim = pictureData.getImageDimension();
            double imgNaturalW = imgDim.getWidth();
            double imgNaturalH = imgDim.getHeight();
            
            // Calculate final image position and size based on object-fit
            Rectangle2D imageRect = calculateObjectFitRect(
                objectFit, containerX, containerY, containerW, containerH, imgNaturalW, imgNaturalH);
            
            if ("cover".equals(objectFit) && needsClipping(imageRect, containerX, containerY, containerW, containerH)) {
                // Create a group with clipping for cover mode
                renderCoverImage(slide, group, pictureData, containerX, containerY, containerW, containerH, imageRect);
            } else {
                // Simple placement for fill, contain, none, scale-down
                XSLFPictureShape pic = (group != null)
                    ? group.createPicture(pictureData)
                    : slide.createPicture(pictureData);
                pic.setAnchor(imageRect);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read image: " + src, e);
        }
    }
    
    /**
     * Calculate image rect based on object-fit mode.
     */
    private Rectangle2D calculateObjectFitRect(String objectFit, double cx, double cy, double cw, double ch,
                                                double imgW, double imgH) {
        switch (objectFit) {
            case "contain" -> {
                // Scale to fit inside container, preserving aspect ratio
                double scale = Math.min(cw / imgW, ch / imgH);
                double w = imgW * scale;
                double h = imgH * scale;
                double x = cx + (cw - w) / 2;  // center horizontally
                double y = cy + (ch - h) / 2;  // center vertically
                return new Rectangle2D.Double(x, y, w, h);
            }
            case "cover" -> {
                // Scale to cover container, preserving aspect ratio (may crop)
                double scale = Math.max(cw / imgW, ch / imgH);
                double w = imgW * scale;
                double h = imgH * scale;
                double x = cx + (cw - w) / 2;  // center horizontally
                double y = cy + (ch - h) / 2;  // center vertically
                return new Rectangle2D.Double(x, y, w, h);
            }
            case "none" -> {
                // No scaling, center the image at natural size
                double x = cx + (cw - imgW) / 2;
                double y = cy + (ch - imgH) / 2;
                return new Rectangle2D.Double(x, y, imgW, imgH);
            }
            case "scale-down" -> {
                // Use 'none' or 'contain', whichever results in smaller size
                if (imgW <= cw && imgH <= ch) {
                    // Image fits, use natural size
                    double x = cx + (cw - imgW) / 2;
                    double y = cy + (ch - imgH) / 2;
                    return new Rectangle2D.Double(x, y, imgW, imgH);
                } else {
                    // Image larger, use contain
                    return calculateObjectFitRect("contain", cx, cy, cw, ch, imgW, imgH);
                }
            }
            default -> { // "fill" or unknown
                // Stretch to fill container (default behavior)
                return new Rectangle2D.Double(cx, cy, cw, ch);
            }
        }
    }
    
    private boolean needsClipping(Rectangle2D imageRect, double cx, double cy, double cw, double ch) {
        return imageRect.getX() < cx || imageRect.getY() < cy ||
               imageRect.getX() + imageRect.getWidth() > cx + cw ||
               imageRect.getY() + imageRect.getHeight() > cy + ch;
    }
    
    /**
     * Render an image with clipping (for cover mode).
     * Uses relative fill rectangle to achieve clipping effect.
     */
    private void renderCoverImage(XSLFSlide slide, XSLFGroupShape parentGroup, XSLFPictureData pictureData,
                                   double cx, double cy, double cw, double ch, Rectangle2D imageRect) {
        // Create the picture shape at container bounds
        XSLFPictureShape pic = (parentGroup != null)
            ? parentGroup.createPicture(pictureData)
            : slide.createPicture(pictureData);
        pic.setAnchor(new Rectangle2D.Double(cx, cy, cw, ch));
        
        // Calculate the relative fill rect to achieve clipping
        // The fill rect determines which portion of the image is shown
        double imgX = imageRect.getX();
        double imgY = imageRect.getY();
        double imgW = imageRect.getWidth();
        double imgH = imageRect.getHeight();
        
        // Calculate offsets as percentage of container
        double left = (cx - imgX) / imgW;    // How much to crop from left
        double top = (cy - imgY) / imgH;     // How much to crop from top
        double right = ((imgX + imgW) - (cx + cw)) / imgW;  // How much to crop from right
        double bottom = ((imgY + imgH) - (cy + ch)) / imgH; // How much to crop from bottom
        
        // Ensure values are in valid range [0, 1)
        left = Math.max(0, Math.min(0.99, left));
        top = Math.max(0, Math.min(0.99, top));
        right = Math.max(0, Math.min(0.99, right));
        bottom = Math.max(0, Math.min(0.99, bottom));
        
        // Apply relative fill rectangle using POI's native API
        // The srcRect defines the portion of the image to display
        try {
            // Access the picture's fill properties through XML
            var ct = pic.getXmlObject();
            if (ct instanceof org.openxmlformats.schemas.presentationml.x2006.main.CTPicture ctPic) {
                var blipFill = ctPic.getBlipFill();
                if (blipFill != null) {
                    var srcRect = blipFill.isSetSrcRect() ? blipFill.getSrcRect() : blipFill.addNewSrcRect();
                    srcRect.setL((int)(left * 100000));    // Values in 1/100000 (EMUs percentage)
                    srcRect.setT((int)(top * 100000));
                    srcRect.setR((int)(right * 100000));
                    srcRect.setB((int)(bottom * 100000));
                }
            }
        } catch (Exception e) {
            // Fallback: just stretch the image if clipping fails
            System.err.println("Warning: Could not apply image clipping: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void renderTable(XSLFSlide slide, XSLFGroupShape group, SlideNode.TableBlock tableBlock) {
        List<List<TableCell>> rows = tableBlock.rows();
        if (rows.isEmpty()) return;

        // Calculate actual grid dimensions considering colspan/rowspan
        int numRows = calculateGridRows(rows);
        int numCols = calculateGridCols(rows);

        XSLFTable table = (group != null)
            ? group.createTable(numRows, numCols)
            : slide.createTable(numRows, numCols);
        table.setAnchor(toRect(tableBlock.layout()));

        // Distribute column widths evenly across the table width
        double tableWidth = tableBlock.layout().width();
        double colWidth = tableWidth / numCols;
        for (int c = 0; c < numCols; c++) {
            table.setColumnWidth(c, colWidth);
        }

        // Track which cells are occupied by previous spans
        boolean[][] occupied = new boolean[numRows][numCols];
        
        // First pass: populate cells and track spans
        List<int[]> merges = new ArrayList<>(); // [firstRow, lastRow, firstCol, lastCol]
        
        int gridRow = 0;
        for (int r = 0; r < rows.size() && gridRow < numRows; r++) {
            List<TableCell> row = rows.get(r);
            int gridCol = 0;
            
            for (TableCell cellData : row) {
                // Find next unoccupied column
                while (gridCol < numCols && occupied[gridRow][gridCol]) {
                    gridCol++;
                }
                if (gridCol >= numCols) break;
                
                XSLFTableCell cell = table.getCell(gridRow, gridCol);
                cell.setText(cellData.text());
                applyCellStyle(cell, cellData.style());
                
                // Mark cells as occupied and record merge regions
                int cs = Math.min(cellData.colspan(), numCols - gridCol);
                int rs = Math.min(cellData.rowspan(), numRows - gridRow);
                
                for (int dr = 0; dr < rs; dr++) {
                    for (int dc = 0; dc < cs; dc++) {
                        if (gridRow + dr < numRows && gridCol + dc < numCols) {
                            occupied[gridRow + dr][gridCol + dc] = true;
                        }
                    }
                }
                
                // Record merge region if spans > 1
                if (cs > 1 || rs > 1) {
                    merges.add(new int[]{gridRow, gridRow + rs - 1, gridCol, gridCol + cs - 1});
                }
                
                gridCol += cs;
            }
            gridRow++;
        }
        
        // Second pass: apply merges
        for (int[] m : merges) {
            table.mergeCells(m[0], m[1], m[2], m[3]);
        }
    }
    
    private int calculateGridRows(List<List<TableCell>> rows) {
        int maxRows = rows.size();
        // Account for rowspan extending beyond logical rows
        int gridRow = 0;
        int[][] rowspanRemaining = new int[rows.size() * 10][100]; // generous size
        
        for (int r = 0; r < rows.size(); r++) {
            int gridCol = 0;
            for (TableCell cell : rows.get(r)) {
                while (gridCol < 100 && rowspanRemaining[gridRow][gridCol] > 0) {
                    gridCol++;
                }
                int rs = cell.rowspan();
                if (gridRow + rs > maxRows) {
                    maxRows = gridRow + rs;
                }
                gridCol += cell.colspan();
            }
            gridRow++;
        }
        return maxRows;
    }
    
    private int calculateGridCols(List<List<TableCell>> rows) {
        int maxCols = 0;
        for (List<TableCell> row : rows) {
            int cols = 0;
            for (TableCell cell : row) {
                cols += cell.colspan();
            }
            maxCols = Math.max(maxCols, cols);
        }
        return Math.max(1, maxCols);
    }
    
    private void applyCellStyle(XSLFTableCell cell, ComputedStyle cellStyle) {
        if (cellStyle == null) return;
        
        String cellBg = cellStyle.getBackgroundColor();
        if (cellBg != null) cell.setFillColor(parseColor(cellBg));

        if (!cell.getTextParagraphs().isEmpty()) {
            XSLFTextParagraph p = cell.getTextParagraphs().get(0);
            
            // Text alignment
            String align = cellStyle.getTextAlign();
            if (align != null) {
                p.setTextAlign(switch (align) {
                    case "center" -> TextParagraph.TextAlign.CENTER;
                    case "right" -> TextParagraph.TextAlign.RIGHT;
                    default -> TextParagraph.TextAlign.LEFT;
                });
            }
            
            if (!p.getTextRuns().isEmpty()) {
                XSLFTextRun run = p.getTextRuns().get(0);
                run.setFontFamily(cellStyle.getFontFamily() != null ? cellStyle.getFontFamily() : DEFAULT_FONT);
                if (cellStyle.isBold()) run.setBold(true);
                if (cellStyle.getFontSize() != null) run.setFontSize(cellStyle.getFontSize());
                String cellColor = cellStyle.getColor();
                if (cellColor != null) run.setFontColor(parseColor(cellColor));
            }
        }
    }

    private void renderShape(XSLFSlide slide, XSLFGroupShape group, SlideNode.ShapeBlock shape) {
        XSLFAutoShape autoShape = (group != null) ? group.createAutoShape() : slide.createAutoShape();
        autoShape.setAnchor(toRect(shape.layout()));

        // Shape type
        autoShape.setShapeType(switch (shape.shapeType()) {
            case "line" -> ShapeType.LINE;
            case "ellipse" -> ShapeType.ELLIPSE;
            case "roundRect" -> ShapeType.ROUND_RECT;
            default -> ShapeType.RECT;
        });

        // Fill
        String fill = shape.style().getBackgroundColor();
        if (fill != null) autoShape.setFillColor(parseColor(fill));

        // Border
        applyBorder(autoShape, shape.style());
    }

    // ─── Utility methods ────────────────────────────────────────────

    private void applyBorder(XSLFAutoShape shape, ComputedStyle style) {
        String borderColor = ComputedStyle.normalizeColor(style.get("border-color"));
        if (borderColor != null) shape.setLineColor(parseColor(borderColor));
        Double borderWidth = style.getLength("border-width");
        if (borderWidth != null) shape.setLineWidth(borderWidth);
    }

    /**
     * Apply box-shadow to a shape using low-level XML manipulation.
     * POI doesn't have high-level shadow API, so we access the underlying XML.
     */
    private void applyShadow(XSLFAutoShape shape, ComputedStyle style) {
        ComputedStyle.BoxShadow shadow = style.getBoxShadow();
        if (shadow == null) return;

        try {
            // Access the underlying XML - need to cast to CTShape
            org.openxmlformats.schemas.presentationml.x2006.main.CTShape ctShape = 
                (org.openxmlformats.schemas.presentationml.x2006.main.CTShape) shape.getXmlObject();
            CTShapeProperties spPr = ctShape.getSpPr();
            if (spPr == null) {
                spPr = ctShape.addNewSpPr();
            }

            // Create or get the effect list
            CTEffectList effectLst = spPr.isSetEffectLst() ? spPr.getEffectLst() : spPr.addNewEffectLst();

            // Create outer shadow
            CTOuterShadowEffect outerShdw = effectLst.addNewOuterShdw();

            // Convert shadow offset to distance and angle
            // Distance in EMUs (English Metric Units): 1 point = 12700 EMUs
            long distEmu = Math.round(shadow.distance() * 12700);
            outerShdw.setDist(distEmu);

            // Angle in 1/60000th of a degree (POI convention)
            // Convert degrees to the POI angle format
            int angleDeg60k = (int) Math.round(shadow.angleDegrees() * 60000);
            outerShdw.setDir(angleDeg60k);

            // Blur radius in EMUs
            long blurEmu = Math.round(shadow.blur() * 12700);
            outerShdw.setBlurRad(blurEmu);

            // Shadow color
            CTSRgbColor srgbClr = outerShdw.addNewSrgbClr();
            srgbClr.setVal(hexToBytes(shadow.colorHex()));

            // Alpha (transparency)
            if (shadow.alpha() < 1.0) {
                CTPositiveFixedPercentage alphaMod = srgbClr.addNewAlpha();
                // POI uses percentage * 1000 (e.g., 50% = 50000)
                alphaMod.setVal((int) Math.round(shadow.alpha() * 100000));
            }

        } catch (Exception e) {
            // Shadow is optional, don't fail rendering if it doesn't work
            System.err.println("Warning: Could not apply shadow: " + e.getMessage());
        }
    }

    /**
     * Apply linear gradient fill to a shape using low-level XML manipulation.
     */
    private void applyGradient(XSLFAutoShape shape, ComputedStyle.LinearGradient gradient) {
        try {
            // Access the underlying XML
            org.openxmlformats.schemas.presentationml.x2006.main.CTShape ctShape = 
                (org.openxmlformats.schemas.presentationml.x2006.main.CTShape) shape.getXmlObject();
            CTShapeProperties spPr = ctShape.getSpPr();
            if (spPr == null) {
                spPr = ctShape.addNewSpPr();
            }

            // Remove any existing fill
            if (spPr.isSetSolidFill()) spPr.unsetSolidFill();
            if (spPr.isSetNoFill()) spPr.unsetNoFill();
            if (spPr.isSetGradFill()) spPr.unsetGradFill();

            // Create gradient fill
            CTGradientFillProperties gradFill = spPr.addNewGradFill();
            gradFill.setRotWithShape(true);

            // Create linear gradient properties
            CTLinearShadeProperties linShade = gradFill.addNewLin();
            // POI angle: 0° = left→right, but CSS: 0° = to top
            // CSS to POI conversion: POI angle = (CSS angle + 90) mod 360
            // CSS: to bottom (180°) → POI: 270° but POI uses 0° for top→bottom
            // Actually, POI uses: 0 = left→right, 90 = top→bottom, 180 = right→left, 270 = bottom→top
            // CSS uses: 0 = to top, 90 = to right, 180 = to bottom, 270 = to left
            // So: POI angle = 90 - CSS angle + 90 = 180 - CSS angle  (mod 360)
            // Wait, let me verify: CSS "to right" (90°) should be POI 0° (left to right)
            // CSS "to bottom" (180°) should be POI 90° (top to bottom)
            // So: POI = CSS - 90
            double poiAngle = (gradient.angleDegrees() - 90 + 360) % 360;
            linShade.setAng((int) Math.round(poiAngle * 60000));

            // Create gradient stop list
            CTGradientStopList gsLst = gradFill.addNewGsLst();

            for (ComputedStyle.GradientStop stop : gradient.stops()) {
                CTGradientStop gs = gsLst.addNewGs();
                // Position: 0-100000 (representing 0-100%)
                gs.setPos((int) Math.round(stop.position() * 100000));

                // Color
                CTSRgbColor srgbClr = gs.addNewSrgbClr();
                srgbClr.setVal(hexToBytes(stop.colorHex()));

                // Alpha if not fully opaque
                if (stop.alpha() < 1.0) {
                    CTPositiveFixedPercentage alphaMod = srgbClr.addNewAlpha();
                    alphaMod.setVal((int) Math.round(stop.alpha() * 100000));
                }
            }

        } catch (Exception e) {
            // Gradient is optional, fall back to no fill
            System.err.println("Warning: Could not apply gradient: " + e.getMessage());
        }
    }

    /**
     * Convert hex color string to byte array for XML.
     */
    private byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() != 6) return new byte[]{0, 0, 0};
        return new byte[]{
            (byte) Integer.parseInt(hex.substring(0, 2), 16),
            (byte) Integer.parseInt(hex.substring(2, 4), 16),
            (byte) Integer.parseInt(hex.substring(4, 6), 16)
        };
    }

    private Rectangle2D.Double toRect(LayoutResult lr) {
        return new Rectangle2D.Double(lr.x(), lr.y(), lr.width(), lr.height());
    }

    static Color parseColor(String hex) {
        if (hex == null || hex.length() != 6) return Color.BLACK;
        try {
            return new Color(
                Integer.parseInt(hex.substring(0, 2), 16),
                Integer.parseInt(hex.substring(2, 4), 16),
                Integer.parseInt(hex.substring(4, 6), 16)
            );
        } catch (NumberFormatException e) {
            return Color.BLACK;
        }
    }

    /**
     * Parse a color with alpha support.
     * Returns a Color with alpha channel (0-255).
     */
    static Color parseColorWithAlpha(String colorValue) {
        var parsed = ComputedStyle.parseColorWithAlpha(colorValue);
        if (parsed == null) return Color.BLACK;
        
        try {
            int r = Integer.parseInt(parsed.hex().substring(0, 2), 16);
            int g = Integer.parseInt(parsed.hex().substring(2, 4), 16);
            int b = Integer.parseInt(parsed.hex().substring(4, 6), 16);
            int a = (int) Math.round(parsed.alpha() * 255);
            return new Color(r, g, b, a);
        } catch (Exception e) {
            return Color.BLACK;
        }
    }

    private PictureData.PictureType inferPicType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".png")) return PictureData.PictureType.PNG;
        if (lower.endsWith(".gif")) return PictureData.PictureType.GIF;
        if (lower.endsWith(".svg")) return PictureData.PictureType.SVG;
        return PictureData.PictureType.JPEG;
    }
    
    /**
     * Determine shape type based on border-radius value.
     * - 50% → ELLIPSE (circle when square, ellipse when rectangle)
     * - Large values → ELLIPSE (when radius >= min dimension / 2)
     * - Small values → ROUND_RECT
     * - null/0 → RECT
     */
    private ShapeType determineShapeType(String borderRadiusValue, double width, double height) {
        if (borderRadiusValue == null || borderRadiusValue.isBlank()) {
            return ShapeType.RECT;
        }
        
        String value = borderRadiusValue.trim().toLowerCase();
        double minDimension = Math.min(width, height);
        
        // Handle percentage
        if (value.endsWith("%")) {
            try {
                double percent = Double.parseDouble(value.replace("%", ""));
                if (percent >= 50) {
                    return ShapeType.ELLIPSE;
                } else if (percent > 0) {
                    return ShapeType.ROUND_RECT;
                }
            } catch (NumberFormatException e) {
                // Fall through
            }
        }
        
        // Handle pixel/point values
        Double radiusPx = ComputedStyle.parseLength(borderRadiusValue);
        if (radiusPx != null && radiusPx > 0) {
            // If radius is >= half the smaller dimension, use ellipse
            if (radiusPx >= minDimension / 2) {
                return ShapeType.ELLIPSE;
            }
            return ShapeType.ROUND_RECT;
        }
        
        return ShapeType.RECT;
    }
}
