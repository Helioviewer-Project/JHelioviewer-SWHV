package org.helioviewer.viewmodel.view.jp2view.image;

public class SubImage {

    public final int x;
    public final int y;
    public final int width;
    public final int height;

    public SubImage(int _x, int _y, int _width, int _height) {
        if (_x < 0)
            x = -_x;
        else
            x = _x;

        if (_y < 0)
            y = -_y;
        else
            y = _y;

        if (_width < 0)
            width = -_width;
        else
            width = _width;

        if (_height < 0)
            height = -_height;
        else
            height = _height;
    }

    /** Returns the number of pixels in the ROI */
    public int getNumPixels() {
        return width * height;
    }

}
