package com.html2ppt.layout;

import com.html2ppt.model.*;

import org.appliedenergistics.yoga.*;
import org.appliedenergistics.yoga.algorithm.CalculateLayout;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

/**
 * Flexbox layout engine powered by Facebook Yoga (pure Java port).
 * Converts SlideNode trees into Yoga node trees, computes layout,
 * and writes computed positions back into SlideNode LayoutResults.
 */
public class YogaLayoutEngine {

    private final TextMeasurer textMeasurer = new TextMeasurer();

    /** Default slide width in points (16:9). */
    public static final float SLIDE_WIDTH_16x9 = 720f;
    /** Default slide height in points (16:9). */
    public static final float SLIDE_HEIGHT_16x9 = 405f;

    /**
     * Compute layout for a single slide.
     * Sets LayoutResult on each SlideNode in the tree.
     *
     * @param slide the slide node
     * @param slideWidth slide width in points
     * @param slideHeight slide height in points
     */
    public void computeLayout(SlideNode.Slide slide, float slideWidth, float slideHeight) {
        YogaNode root = createYogaNode(slide);
        root.setWidth(slideWidth);
        root.setHeight(slideHeight);

        CalculateLayout.calculateLayout(root, slideWidth, slideHeight, YogaDirection.LTR);

        applyLayout(slide, root);
        freeRecursive(root);
    }

    /**
     * Compute layout with default 16:9 dimensions.
     */
    public void computeLayout(SlideNode.Slide slide) {
        computeLayout(slide, SLIDE_WIDTH_16x9, SLIDE_HEIGHT_16x9);
    }

    /**
     * Create a YogaNode tree from a SlideNode tree.
     */
    YogaNode createYogaNode(SlideNode node) {
        YogaNode yogaNode = new YogaNode();

        applyStyle(yogaNode, node.style());

        if (node instanceof SlideNode.TextBlock textBlock) {
            // Text nodes are leaf nodes with a measure function
            double fontSize = textBlock.style().getFontSize(18);
            
            // Set minimum height to prevent text from being squashed below its natural line height
            float minLineHeight = (float) (fontSize * 1.4); // LINE_HEIGHT_MULTIPLIER
            yogaNode.setMinHeight(minLineHeight);
            
            yogaNode.setMeasureFunction((n, width, widthMode, height, heightMode) -> {
                boolean bold = textBlock.style().isBold();
                String text = textBlock.plainText();
                float measuredWidth = (float) textMeasurer.measureWidth(text, fontSize, bold);
                float measuredHeight = (float) textMeasurer.measureHeight(text, fontSize,
                    widthMode == YogaMeasureMode.UNDEFINED ? Float.MAX_VALUE : width, bold);

                float finalWidth = switch (widthMode) {
                    case EXACTLY -> width;
                    case AT_MOST -> Math.min(measuredWidth, width);
                    case UNDEFINED -> measuredWidth;
                };
                float finalHeight = switch (heightMode) {
                    case EXACTLY -> height;
                    case AT_MOST -> Math.min(measuredHeight, height);
                    case UNDEFINED -> measuredHeight;
                };

                return new YogaSize(finalWidth, finalHeight);
            });
        } else if (node instanceof SlideNode.ImageBlock imageBlock) {
            // Images: use explicit CSS dimensions or intrinsic size from file
            String widthStr = imageBlock.style().get("width");
            String heightStr = imageBlock.style().get("height");
            
            // Try to read intrinsic dimensions from file
            float intrinsicWidth = 100;  // default fallback
            float intrinsicHeight = 100;
            try {
                java.awt.Dimension dim = getImageDimensions(imageBlock.src());
                if (dim != null) {
                    intrinsicWidth = (float) dim.getWidth();
                    intrinsicHeight = (float) dim.getHeight();
                }
            } catch (Exception ignored) {}
            
            // Handle percentage or pixel dimensions
            boolean hasWidth = false, hasHeight = false;
            
            if (widthStr != null && !widthStr.equals("auto")) {
                Float pct = ComputedStyle.parsePercentage(widthStr);
                if (pct != null) {
                    yogaNode.setWidthPercent(pct * 100);
                    hasWidth = true;
                } else {
                    Double len = ComputedStyle.parseLength(widthStr);
                    if (len != null) {
                        yogaNode.setWidth(len.floatValue());
                        hasWidth = true;
                    }
                }
            }
            
            if (heightStr != null && !heightStr.equals("auto")) {
                Float pct = ComputedStyle.parsePercentage(heightStr);
                if (pct != null) {
                    yogaNode.setHeightPercent(pct * 100);
                    hasHeight = true;
                } else {
                    Double len = ComputedStyle.parseLength(heightStr);
                    if (len != null) {
                        yogaNode.setHeight(len.floatValue());
                        hasHeight = true;
                    }
                }
            }
            
            // If only one dimension is set, use intrinsic ratio
            if (hasWidth && !hasHeight) {
                // Width specified, set aspect ratio for height
                yogaNode.setAspectRatio(intrinsicWidth / intrinsicHeight);
            } else if (hasHeight && !hasWidth) {
                // Height specified, set aspect ratio for width
                yogaNode.setAspectRatio(intrinsicWidth / intrinsicHeight);
            } else if (!hasWidth && !hasHeight) {
                // No dimensions - use intrinsic size
                yogaNode.setWidth(intrinsicWidth);
                yogaNode.setHeight(intrinsicHeight);
            }
            // If both are set, Yoga will use them directly
            
            // Max-width/max-height constraints
            Double maxWidth = imageBlock.style().getLength("max-width");
            Double maxHeight = imageBlock.style().getLength("max-height");
            if (maxWidth != null) yogaNode.setMaxWidth(maxWidth.floatValue());
            if (maxHeight != null) yogaNode.setMaxHeight(maxHeight.floatValue());
            
        } else if (node instanceof SlideNode.TableBlock tableBlock) {
            // Tables: take full width and estimate height based on rows
            yogaNode.setWidthPercent(100);
            int numRows = tableBlock.rows().size();
            double rowHeight = tableBlock.style().getFontSize(14) * 2.0; // ~2x font size per row
            yogaNode.setHeight((float) (numRows * rowHeight));
        } else {
            // Container nodes: add children
            List<SlideNode> children = node.children();
            
            // Calculate minimum height based on children content
            float minContentHeight = 0;
            for (SlideNode child : children) {
                if (child instanceof SlideNode.TextBlock textChild) {
                    // Text children need minimum height for their line
                    double fontSize = textChild.style().getFontSize(18);
                    minContentHeight = Math.max(minContentHeight, (float) (fontSize * 1.4));
                }
            }
            
            // If this box has text children, set min-height accounting for padding
            if (minContentHeight > 0) {
                Double padTop = node.style().getLength("padding-top");
                Double padBot = node.style().getLength("padding-bottom");
                float totalPadding = (padTop != null ? padTop.floatValue() : 0) 
                                   + (padBot != null ? padBot.floatValue() : 0);
                yogaNode.setMinHeight(minContentHeight + totalPadding);
            }
            
            for (int i = 0; i < children.size(); i++) {
                YogaNode childYoga = createYogaNode(children.get(i));
                yogaNode.addChildAt(childYoga, i);
            }
        }

        return yogaNode;
    }

    /**
     * Apply CSS styles from ComputedStyle to a YogaNode.
     * YogaNode implements YogaProps — all setter methods are on the node itself.
     */
    void applyStyle(YogaNode yogaNode, ComputedStyle style) {
        if (style == null) return;

        // Display
        String display = style.getDisplay();
        if ("none".equals(display)) {
            yogaNode.getStyle().setDisplay(YogaDisplay.NONE);
            return;
        }
        yogaNode.getStyle().setDisplay(YogaDisplay.FLEX);

        // Flex direction logic:
        // - If display:flex is explicitly set → CSS default is ROW
        // - If display is NOT explicitly set → normal block flow → COLUMN
        String flexDir = style.get("flex-direction");
        boolean explicitFlex = "flex".equals(style.get("display"));
        String defaultDir = explicitFlex ? "row" : "column";
        yogaNode.setFlexDirection(switch (flexDir != null ? flexDir : defaultDir) {
            case "column" -> YogaFlexDirection.COLUMN;
            case "row-reverse" -> YogaFlexDirection.ROW_REVERSE;
            case "column-reverse" -> YogaFlexDirection.COLUMN_REVERSE;
            default -> YogaFlexDirection.ROW;
        });

        // Justify content
        String justify = style.get("justify-content");
        if (justify != null) {
            yogaNode.setJustifyContent(switch (justify) {
                case "center" -> YogaJustify.CENTER;
                case "flex-end" -> YogaJustify.FLEX_END;
                case "space-between" -> YogaJustify.SPACE_BETWEEN;
                case "space-around" -> YogaJustify.SPACE_AROUND;
                case "space-evenly" -> YogaJustify.SPACE_EVENLY;
                default -> YogaJustify.FLEX_START;
            });
        }

        // Align items
        String alignItems = style.get("align-items");
        if (alignItems != null) {
            yogaNode.setAlignItems(switch (alignItems) {
                case "flex-start" -> YogaAlign.FLEX_START;
                case "center" -> YogaAlign.CENTER;
                case "flex-end" -> YogaAlign.FLEX_END;
                case "baseline" -> YogaAlign.BASELINE;
                default -> YogaAlign.STRETCH;
            });
        }

        // Align self
        String alignSelf = style.get("align-self");
        if (alignSelf != null) {
            yogaNode.setAlignSelf(switch (alignSelf) {
                case "flex-start" -> YogaAlign.FLEX_START;
                case "center" -> YogaAlign.CENTER;
                case "flex-end" -> YogaAlign.FLEX_END;
                case "stretch" -> YogaAlign.STRETCH;
                default -> YogaAlign.AUTO;
            });
        }

        // Flex wrap
        String flexWrap = style.get("flex-wrap");
        if (flexWrap != null) {
            yogaNode.setWrap("wrap".equals(flexWrap) ? YogaWrap.WRAP : YogaWrap.NO_WRAP);
        }

        // Flex grow / shrink / basis
        Double flexGrow = style.getLength("flex-grow");
        if (flexGrow != null) yogaNode.setFlexGrow(flexGrow.floatValue());

        Double flexShrink = style.getLength("flex-shrink");
        if (flexShrink != null) yogaNode.setFlexShrink(flexShrink.floatValue());

        String flexBasis = style.get("flex-basis");
        if (flexBasis != null && !"auto".equals(flexBasis)) {
            Double basisVal = ComputedStyle.parseLength(flexBasis);
            if (basisVal != null) yogaNode.setFlexBasis(basisVal.floatValue());
        }

        // Dimensions
        applyDimension(yogaNode, style, "width", true);
        applyDimension(yogaNode, style, "height", false);
        applyMinMax(yogaNode, style);

        // Padding
        applyEdge(yogaNode, style, "padding", true);

        // Margin
        applyEdge(yogaNode, style, "margin", false);

        // Gap
        Double gap = style.getLength("gap");
        if (gap != null) yogaNode.setGap(YogaGutter.ALL, gap.floatValue());
        Double rowGap = style.getLength("row-gap");
        if (rowGap != null) yogaNode.setGap(YogaGutter.ROW, rowGap.floatValue());
        Double colGap = style.getLength("column-gap");
        if (colGap != null) yogaNode.setGap(YogaGutter.COLUMN, colGap.floatValue());

        // Position
        String position = style.get("position");
        if ("absolute".equals(position)) {
            yogaNode.setPositionType(YogaPositionType.ABSOLUTE);
        }
        applyPosition(yogaNode, style, "top", YogaEdge.TOP);
        applyPosition(yogaNode, style, "right", YogaEdge.RIGHT);
        applyPosition(yogaNode, style, "bottom", YogaEdge.BOTTOM);
        applyPosition(yogaNode, style, "left", YogaEdge.LEFT);
    }

    private void applyDimension(YogaNode node, ComputedStyle style, String prop, boolean isWidth) {
        String value = style.get(prop);
        if (value == null || "auto".equals(value)) return;

        Float pct = ComputedStyle.parsePercentage(value);
        if (pct != null) {
            if (isWidth) node.setWidthPercent(pct * 100);
            else node.setHeightPercent(pct * 100);
        } else {
            Double len = ComputedStyle.parseLength(value);
            if (len != null) {
                if (isWidth) node.setWidth(len.floatValue());
                else node.setHeight(len.floatValue());
            }
        }
    }

    private void applyMinMax(YogaNode node, ComputedStyle style) {
        Double v;
        if ((v = style.getLength("min-width")) != null) node.setMinWidth(v.floatValue());
        if ((v = style.getLength("min-height")) != null) node.setMinHeight(v.floatValue());
        if ((v = style.getLength("max-width")) != null) node.setMaxWidth(v.floatValue());
        if ((v = style.getLength("max-height")) != null) node.setMaxHeight(v.floatValue());
    }

    private void applyEdge(YogaNode node, ComputedStyle style, String prop, boolean isPadding) {
        String[] sides = {"top", "right", "bottom", "left"};
        YogaEdge[] edges = {YogaEdge.TOP, YogaEdge.RIGHT, YogaEdge.BOTTOM, YogaEdge.LEFT};
        for (int i = 0; i < 4; i++) {
            Double val = style.getLength(prop + "-" + sides[i]);
            if (val != null) {
                if (isPadding) node.setPadding(edges[i], val.floatValue());
                else node.setMargin(edges[i], val.floatValue());
            }
        }
    }

    private void applyPosition(YogaNode node, ComputedStyle style, String prop, YogaEdge edge) {
        Double val = style.getLength(prop);
        if (val != null) node.setPosition(edge, val.floatValue());
    }

    /**
     * Read computed layout from YogaNode and write into SlideNode.
     */
    private void applyLayout(SlideNode node, YogaNode yogaNode) {
        node.setLayout(new LayoutResult(
            yogaNode.getLayoutX(),
            yogaNode.getLayoutY(),
            yogaNode.getLayoutWidth(),
            yogaNode.getLayoutHeight()
        ));

        List<SlideNode> children = node.children();
        int yogaChildIndex = 0;
        for (SlideNode child : children) {
            if (yogaChildIndex < yogaNode.getChildCount()) {
                applyLayout(child, yogaNode.getChild(yogaChildIndex));
                yogaChildIndex++;
            }
        }
    }

    private void freeRecursive(YogaNode node) {
        for (int i = node.getChildCount() - 1; i >= 0; i--) {
            freeRecursive(node.getChild(i));
        }
    }

    /**
     * Get image dimensions without loading the full image into memory.
     * Supports local files and data URIs.
     */
    private Dimension getImageDimensions(String src) {
        try {
            byte[] data;
            if (src.startsWith("data:")) {
                // Data URI: data:image/png;base64,XXXXX
                int commaIdx = src.indexOf(',');
                if (commaIdx > 0) {
                    data = Base64.getDecoder().decode(src.substring(commaIdx + 1));
                } else {
                    return null;
                }
            } else {
                // Local file path
                Path path = Path.of(src);
                if (!Files.exists(path)) return null;
                data = Files.readAllBytes(path);
            }
            
            try (ImageInputStream in = ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(data))) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    try {
                        reader.setInput(in);
                        return new Dimension(reader.getWidth(0), reader.getHeight(0));
                    } finally {
                        reader.dispose();
                    }
                }
            }
        } catch (IOException ignored) {}
        return null;
    }
}
