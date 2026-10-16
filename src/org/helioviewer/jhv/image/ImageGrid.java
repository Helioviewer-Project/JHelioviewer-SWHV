package org.helioviewer.jhv.image;

// Buffer samples in original image pixels: upper-left edge (0,0), centers at half-integers.
// level identifies a pyramid level; it also determines the spacing for the supported decoders.
public record ImageGrid(int level, int width, int height, double firstX, double firstY) {

    public ImageGrid(int _level, int _width, int _height) {
        this(_level, _width, _height, .5, .5);
    }

    public double step() {
        return Math.scalb(1., level);
    }

}
