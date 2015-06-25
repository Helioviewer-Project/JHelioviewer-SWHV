package org.helioviewer.viewmodel.view.jp2view.image;

/**
 * A very simple region of interest class. Basically just an immutable (for
 * thread safety) rectangle class with less functionality. At the moment the
 * information contained in this class is just x,y,width, and height.
 * @author caplins
 *
 */
public class SubImage {

    public final int x;
    public final int y;
    public final int width;
    public final int height;

    public SubImage(int _x, int _y, int _width, int _height) {
        if (_x < 0)
            x = 0;
        else
            x = _x;

        if (_y < 0)
            y = 0;
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
