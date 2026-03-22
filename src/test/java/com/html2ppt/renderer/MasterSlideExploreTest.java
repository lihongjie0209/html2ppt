package com.html2ppt.renderer;

import org.apache.poi.xslf.usermodel.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class MasterSlideExploreTest {
    @Test
    void exploreSlideMasters() {
        XMLSlideShow pptx = new XMLSlideShow();
        
        // List all slide masters
        List<XSLFSlideMaster> masters = pptx.getSlideMasters();
        assertThat(masters).hasSize(1);  // Default has 1 master
        
        XSLFSlideMaster master = masters.get(0);
        XSLFSlideLayout[] layouts = master.getSlideLayouts();
        assertThat(layouts.length).isGreaterThan(5);  // Should have several layouts
        
        // Test creating slide with title layout
        XSLFSlideLayout titleLayout = master.getLayout(SlideLayout.TITLE);
        assertThat(titleLayout).isNotNull();
        
        XSLFSlide slide = pptx.createSlide(titleLayout);
        assertThat(slide).isNotNull();
        
        // Check for title placeholder
        boolean hasTitle = false;
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                var type = textShape.getTextType();
                if (type == org.apache.poi.sl.usermodel.Placeholder.TITLE || 
                    type == org.apache.poi.sl.usermodel.Placeholder.CENTERED_TITLE) {
                    hasTitle = true;
                }
            }
        }
        assertThat(hasTitle).isTrue();
    }
    
    @Test
    void createSlideWithTitleAndContentLayout() {
        XMLSlideShow pptx = new XMLSlideShow();
        XSLFSlideMaster master = pptx.getSlideMasters().get(0);
        
        // TITLE_AND_CONTENT is the most common layout
        XSLFSlideLayout layout = master.getLayout(SlideLayout.TITLE_AND_CONTENT);
        assertThat(layout).isNotNull();
        
        XSLFSlide slide = pptx.createSlide(layout);
        
        // Fill placeholders
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                var type = textShape.getTextType();
                if (type == org.apache.poi.sl.usermodel.Placeholder.TITLE) {
                    textShape.setText("My Title");
                } else if (type == org.apache.poi.sl.usermodel.Placeholder.BODY) {
                    textShape.setText("Content goes here");
                }
            }
        }
        
        assertThat(pptx.getSlides()).hasSize(1);
    }
    
    @Test
    void blankLayoutHasNoPlaceholders() {
        XMLSlideShow pptx = new XMLSlideShow();
        XSLFSlideMaster master = pptx.getSlideMasters().get(0);
        
        XSLFSlideLayout blankLayout = master.getLayout(SlideLayout.BLANK);
        assertThat(blankLayout).isNotNull();
        
        XSLFSlide slide = pptx.createSlide(blankLayout);
        
        // Blank layout should have no text placeholders
        long textPlaceholders = slide.getShapes().stream()
            .filter(s -> s instanceof XSLFTextShape)
            .map(s -> (XSLFTextShape) s)
            .filter(ts -> ts.getTextType() != null)
            .count();
        
        assertThat(textPlaceholders).isEqualTo(0);
    }
}
