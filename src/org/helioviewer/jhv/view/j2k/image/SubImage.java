package org.helioviewer.jhv.view.j2k.image;

import org.helioviewer.jhv.math.MathUtils;

public class SubImage {

    public final int x;
    public final int y;
    public final int width;
    public final int height;

    private static final int QUANTA = 32;

    // roundoff to quanta, minimum 1 pixel, clip to full image size
    public SubImage(int x, int y, int w, int h, int fwidth, int fheight) {
        x = MathUtils.roundDownTo(x, QUANTA);
        y = MathUtils.roundDownTo(y, QUANTA);
        w = MathUtils.roundUpTo(w + QUANTA, QUANTA);
        h = MathUtils.roundUpTo(h + QUANTA, QUANTA);

        x = MathUtils.clip(x, 0, fwidth - 1);
        y = MathUtils.clip(y, 0, fheight - 1);
        w = MathUtils.clip(w, 1, fwidth);
        h = MathUtils.clip(h, 1, fheight);

        w = MathUtils.clip(w, fwidth - x);
        h = MathUtils.clip(h, fheight - y);

        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SubImage))
            return false;
        SubImage s = (SubImage) o;
        return x == s.x && y == s.y && width == s.width && height == s.height;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + x;
        result = 31 * result + y;
        result = 31 * result + width;
        return 31 * result + height;
    }

    @Override
    public String toString() {
        return "[x=" + x + " y=" + y + " w=" + width + " h=" + height + ']';
    }

}
