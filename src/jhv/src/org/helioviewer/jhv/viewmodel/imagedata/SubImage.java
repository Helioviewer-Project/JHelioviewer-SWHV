package org.helioviewer.jhv.viewmodel.imagedata;

public class SubImage {

    public final int x;
    public final int y;
    public final int width;
    public final int height;

    // minimum 1 pixel, clip to full image size
    public SubImage(int x, int y, int w, int h, int fwidth, int fheight) {
        x = Math.min(Math.max(x, 0), fwidth - 1);
        y = Math.min(Math.max(y, 0), fheight - 1);
        w = Math.min(Math.max(w, 1), fwidth);
        h = Math.min(Math.max(h, 1), fheight);

        w = Math.min(w, fwidth - x);
        h = Math.min(h, fheight - y);

        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SubImage) {
            SubImage s = (SubImage) o;
            return x == s.x && y == s.y && width == s.width && height == s.height;
        }
        return false;
    }

    @Override
    public int hashCode() {
        assert false : "hashCode not designed";
        return 42;
    }

    @Override
    public String toString() {
        return "[x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + "]";
    }

}
