package com.html2ppt.model;

/**
 * Computed layout result from Yoga layout engine.
 * Contains the absolute position and dimensions in points.
 */
public record LayoutResult(double x, double y, double width, double height) {

    public static final LayoutResult ZERO = new LayoutResult(0, 0, 0, 0);

    public LayoutResult withSize(double width, double height) {
        return new LayoutResult(this.x, this.y, width, height);
    }

    public LayoutResult withPosition(double x, double y) {
        return new LayoutResult(x, y, this.width, this.height);
    }
}
