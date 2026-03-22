package com.html2ppt.tools;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SlideRendererTest {

    @Test
    void rendersSlidesToExplicitOutputDirectory(@TempDir Path tempDir) throws Exception {
        Path pptx = createPresentation(tempDir.resolve("slides.pptx"));
        Path outputDir = tempDir.resolve("images");

        SlideRenderer.main(new String[]{pptx.toString(), outputDir.toString()});

        assertThat(outputDir.resolve("slides-slide1.png")).exists();
    }

    @Test
    void rendersSlidesNextToInputWhenOutputDirOmitted(@TempDir Path tempDir) throws Exception {
        Path pptx = createPresentation(tempDir.resolve("inline-output.pptx"));

        SlideRenderer.main(new String[]{pptx.toString()});

        assertThat(tempDir.resolve("inline-output-slide1.png")).exists();
    }

    private Path createPresentation(Path output) throws IOException {
        try (XMLSlideShow pptx = new XMLSlideShow()) {
            pptx.setPageSize(new Dimension(720, 405));
            XSLFSlide slide = pptx.createSlide();
            slide.getBackground().setFillColor(new Color(0xEE, 0xF2, 0xFF));

            XSLFTextBox textBox = slide.createTextBox();
            textBox.setAnchor(new java.awt.geom.Rectangle2D.Double(60, 60, 300, 80));
            textBox.setText("Slide Renderer");

            try (var out = Files.newOutputStream(output)) {
                pptx.write(out);
            }
        }
        return output;
    }
}
