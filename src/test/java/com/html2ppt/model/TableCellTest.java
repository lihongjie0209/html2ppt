package com.html2ppt.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class TableCellTest {

    @Test
    void defaultsColspanRowspanToOne() {
        var cell = new TableCell("Hello");
        assertThat(cell.colspan()).isEqualTo(1);
        assertThat(cell.rowspan()).isEqualTo(1);
    }

    @Test
    void twoArgConstructorDefaultsSpans() {
        var style = new ComputedStyle();
        style.set("color", "red");
        var cell = new TableCell("Hello", style);
        assertThat(cell.colspan()).isEqualTo(1);
        assertThat(cell.rowspan()).isEqualTo(1);
        assertThat(cell.style().get("color")).isEqualTo("red");
    }

    @Test
    void fullConstructorSetsAllFields() {
        var style = new ComputedStyle();
        var cell = new TableCell("Test", style, 3, 2);
        assertThat(cell.text()).isEqualTo("Test");
        assertThat(cell.colspan()).isEqualTo(3);
        assertThat(cell.rowspan()).isEqualTo(2);
    }

    @Test
    void recordEquality() {
        var style = new ComputedStyle();
        var cell1 = new TableCell("A", style, 2, 1);
        var cell2 = new TableCell("A", style, 2, 1);
        // Same style instance, same values
        assertThat(cell1).isEqualTo(cell2);
    }
    
    @Test
    void cellTextAndSpansAccessible() {
        var cell = new TableCell("Product", new ComputedStyle(), 2, 3);
        assertThat(cell.text()).isEqualTo("Product");
        assertThat(cell.colspan()).isEqualTo(2);
        assertThat(cell.rowspan()).isEqualTo(3);
    }
}
