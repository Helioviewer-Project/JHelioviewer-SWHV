package org.helioviewer.jhv.math;

public class Region {

    public final double width;
    public final double height;

    public final double llx;
    public final double lly;
    public final double urx;
    public final double ury;

    public Region(double newLLX, double newLLY, double newWidth, double newHeight) {
        width = newWidth;
        height = newHeight;

        llx = newLLX;
        lly = newLLY;

        urx = llx + width;
        ury = lly + height;
    }

    public static Region scale(Region r, double f) {
        return new Region(r.llx * f, r.lly * f, r.width * f, r.height * f);
    }

    @Override
    public String toString() {
        return "[Region: Corner: [" + llx + ',' + lly + "] Size: [" + width + ',' + height + ']';
    }

}
