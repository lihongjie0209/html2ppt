package com.html2ppt.tools;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFBackground;
import org.apache.poi.xslf.usermodel.XSLFSlide;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Renders each slide of a .pptx to PNG images for visual verification.
 * Usage: SlideRenderer <input.pptx> [outputDir]
 */
public class SlideRenderer {

    private static final double SCALE = 2.0; // 2x for high-DPI

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: SlideRenderer <input.pptx> [outputDir]");
            System.exit(1);
        }

        Path pptxPath = Path.of(args[0]);
        Path outputDir = args.length > 1 ? Path.of(args[1]) : pptxPath.getParent();
        String baseName = pptxPath.getFileName().toString().replace(".pptx", "");

        Files.createDirectories(outputDir);

        try (XMLSlideShow pptx = new XMLSlideShow(new FileInputStream(pptxPath.toFile()))) {
            Dimension pageSize = pptx.getPageSize();
            int imgW = (int) (pageSize.width * SCALE);
            int imgH = (int) (pageSize.height * SCALE);

            System.out.printf("Slide size: %dx%d pt, rendering at %dx%d px (%.0fx)%n",
                pageSize.width, pageSize.height, imgW, imgH, SCALE);

            int slideNum = 0;
            for (XSLFSlide slide : pptx.getSlides()) {
                slideNum++;
                BufferedImage img = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = img.createGraphics();

                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // Draw slide background manually (POI's slide.draw() doesn't always render it)
                g2d.scale(SCALE, SCALE);
                Color bgColor = Color.WHITE;
                try {
                    XSLFBackground bg = slide.getBackground();
                    if (bg != null && bg.getFillColor() != null) {
                        bgColor = bg.getFillColor();
                    }
                } catch (Exception ignored) {}
                g2d.setColor(bgColor);
                g2d.fillRect(0, 0, pageSize.width, pageSize.height);

                slide.draw(g2d);
                g2d.dispose();

                File outFile = outputDir.resolve(baseName + "-slide" + slideNum + ".png").toFile();
                ImageIO.write(img, "png", outFile);
                System.out.println("  Slide " + slideNum + " -> " + outFile.getName());
            }

            System.out.println("Done: " + slideNum + " slides rendered to " + outputDir);
        }
    }
}
