package org.helioviewer.jhv.base;

public class Region {

    public final double width;
    public final double height;

    public final double llx;
    public final double lly;
    public final double urx;
    public final double ury;
    public final double ulx;
    // public final double uly;

    public Region(double newLLX, double newLLY, double newWidth, double newHeight) {
        width = newWidth;
        height = newHeight;

        llx = newLLX;
        lly = newLLY;

        urx = llx + width;
        ury = lly + height;

        ulx = llx;
        // uly = lly + height;
    }

    public static Region scale(Region r, double f) {
        return new Region(r.llx * f, r.lly * f, r.width * f, r.height * f);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (o instanceof Region r)
            return Double.doubleToLongBits(width) == Double.doubleToLongBits(r.width) &&
                    Double.doubleToLongBits(height) == Double.doubleToLongBits(r.height) &&
                    Double.doubleToLongBits(llx) == Double.doubleToLongBits(r.llx) &&
                    Double.doubleToLongBits(lly) == Double.doubleToLongBits(r.lly);
        return false;
    }

    @Override
    public int hashCode() {
        int result = 1;
        long tmp = Double.doubleToLongBits(width);
        result = 31 * result + (int) (tmp ^ (tmp >>> 32));
        tmp = Double.doubleToLongBits(height);
        result = 31 * result + (int) (tmp ^ (tmp >>> 32));
        tmp = Double.doubleToLongBits(llx);
        result = 31 * result + (int) (tmp ^ (tmp >>> 32));
        tmp = Double.doubleToLongBits(lly);
        return 31 * result + (int) (tmp ^ (tmp >>> 32));
    }

    @Override
    public String toString() {
        return "[Region: Corner: [" + llx + ',' + lly + "] Size: [" + width + ',' + height + ']';
    }

}
