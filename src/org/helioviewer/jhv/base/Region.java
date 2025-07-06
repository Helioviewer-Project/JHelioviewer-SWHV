package org.helioviewer.jhv.base;

public class Region {

    public static final Region DEFAULT = new Region(-0.5, -0.5, 1, 1);

    public final double width;
    public final double height;

    public final double llx;
    public final double lly;
    public final double urx;
    public final double ury;
    public final double ulx;
    // public final double uly;
    public final float[] glslArray;

    public Region(double newLLX, double newLLY, double newWidth, double newHeight) {
        width = newWidth;
        height = newHeight;

        llx = newLLX;
        lly = newLLY;

        urx = llx + width;
        ury = lly + height;

        ulx = llx;
        // uly = lly + height;
        glslArray = new float[]{(float) (llx / width), (float) (lly / height), (float) (1. / width), (float) (1. / height)};
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
        int result = Double.hashCode(width);
        result = 31 * result + Double.hashCode(height);
        result = 31 * result + Double.hashCode(llx);
        return 31 * result + Double.hashCode(lly);
    }

    @Override
    public String toString() {
        return "[Region: Corner: [" + llx + ',' + lly + "] Size: [" + width + ',' + height + ']';
    }

}
