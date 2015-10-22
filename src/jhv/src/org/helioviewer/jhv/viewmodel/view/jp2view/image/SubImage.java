package org.helioviewer.jhv.viewmodel.view.jp2view.image;

import java.awt.Rectangle;

public class SubImage {

    public final int x;
    public final int y;
    public final int width;
    public final int height;

    // minimum 1 pixel, fit into rectangle
    public SubImage(int x, int y, int w, int h, Rectangle r) {
        w = Math.min(Math.max(w, 1), r.width);
        h = Math.min(Math.max(h, 1), r.height);
        x = Math.min(Math.max(x, 0), r.width - 1);
        y = Math.min(Math.max(y, 0), r.height - 1);

        w = Math.min(w, r.width - x);
        h = Math.min(h, r.height - y);

        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
    }

    /** Overridden equals method. */
    @Override
    public boolean equals(Object _obj) {
        if (_obj == null)
            return false;
        else if (!(_obj instanceof SubImage))
            return false;
        else {
            SubImage roi = SubImage.class.cast(_obj);
            return x == roi.x && y == roi.y && width == roi.width && height == roi.height;
        }
    }

    /** Returns the number of pixels in the ROI */
    public int getNumPixels() {
        return width * height;
    }

    /** Overridden toString method */
    @Override
    public String toString() {
        String ret = "x=" + x + "   ";
        ret += "y=" + y + "   ";
        ret += "width=" + width + "   ";
        ret += "height=" + height + "   ";
        return ret;
    }

}
