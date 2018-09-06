package org.helioviewer.jhv.base;

import java.util.Objects;

public class Region {

    public final double width;
    public final double height;

    public final double llx;
    public final double lly;
    public final double urx;
    public final double ury;
    public final double ulx;
    public final double uly;

    public Region(double newLLX, double newLLY, double newWidth, double newHeight) {
        width = newWidth;
        height = newHeight;

        llx = newLLX;
        lly = newLLY;

        urx = llx + width;
        ury = lly + height;

        ulx = llx;
        uly = lly + height;
    }

    public static Region scale(Region r, double f) {
        return new Region(r.llx * f, r.lly * f, r.width * f, r.height * f);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Region))
            return false;
        Region r = (Region) o;
        return width == r.width && height == r.height && llx == r.llx && lly == r.lly;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height, llx, lly);
    }

    @Override
    public String toString() {
        return "[Region: Corner: [" + llx + ',' + lly + "] Size: [" + width + ',' + height + ']';
    }

}
