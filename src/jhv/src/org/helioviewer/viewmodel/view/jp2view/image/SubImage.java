package org.helioviewer.viewmodel.view.jp2view.image;

import java.awt.Rectangle;

import org.helioviewer.base.interval.Interval;

/**
 * A very simple region of interest class. Basically just an immutable (for
 * thread safety) rectangle class with less functionality. At the moment the
 * information contained in this class is just x,y,width, and height. This data
 * is actually kept in two separate formats... int's and range/domain
 * Interval's. The intervals are probably the better choice, but I have gone
 * through and eliminated the direct calls to the member variables yet.
 *
 * @author caplins
 *
 */
public class SubImage {

    /** Holds the domain of the ROI */
    public final Interval<Integer> domain;

    /** Holds the range of the ROI */
    public final Interval<Integer> range;

    /** The same data as in the range and domain */
    public int x;

    public int y;

    public int width;

    public int height;

    /**
     * Constructor taking primitive ints.
     *
     * @param _x
     * @param _y
     * @param _width
     * @param _height
     */
    public SubImage(int _x, int _y, int _width, int _height) {
        x = _x;
        if (x < 0) {
            x = 0;
        }
        y = _y;
        if (y < 0) {
            y = 0;
        }
        width = _width;
        if (width < 0) {
            width = -width;
        }
        height = _height;
        if (height < 0) {
            height = -height;
        }
        domain = new Interval<Integer>(x, x + width);
        range = new Interval<Integer>(y, y + height);
    }

    /**
     * Constructor taking Intervals.
     *
     * @param _domain
     * @param _range
     */
    public SubImage(Interval<Integer> _domain, Interval<Integer> _range) {
        domain = _domain;
        range = _range;
        x = domain.getStart();
        width = domain.getEnd() - domain.getStart();
        y = range.getStart();
        height = range.getEnd() - range.getStart();
    }

    /**
     * Constructor taking a Rectangle
     *
     * @param _rect
     */
    public SubImage(Rectangle _rect) {
        this(_rect.x, _rect.y, _rect.width, _rect.height);
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
            return domain.equals(roi.domain) && range.equals(roi.range);
        }
    }

    /** Returns the number of pixels in the ROI */
    public int getNumPixels() {
        return width * height;
    }

    /** Overridden toString method */
    @Override
    public String toString() {
        String ret = "x=" + domain.getStart() + "   ";
        ret += "y=" + range.getStart() + "   ";
        ret += "width=" + (domain.getEnd() - domain.getStart()) + "   ";
        ret += "height=" + (range.getEnd() - range.getStart()) + "   ";
        return ret;
    }

}
