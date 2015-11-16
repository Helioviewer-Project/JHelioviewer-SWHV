package org.helioviewer.jhv.base;

public class Region {

    private final double width;
    private final double height;

    private final double llx;
    private final double lly;
    private final double urx;
    private final double ury;
    private final double ulx;
    private final double uly;

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

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public double getLLX() {
        return llx;
    }

    public double getLLY() {
        return lly;
    }

    public double getURX() {
        return urx;
    }

    public double getURY() {
        return ury;
    }

    public double getULX() {
        return ulx;
    }

    public double getULY() {
        return uly;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Region)) {
            return false;
        }
        Region r = (Region) o;
        return width == r.width && height == r.height && llx == r.llx && lly == r.lly;
    }

    @Override
    public int hashCode() {
        assert false : "hashCode not designed";
        return 42;
    }

    @Override
    public String toString() {
        return "[Region: Corner: [" + llx + "," + lly + "] Size: [" + width + "," + height + "]";
    }

}
