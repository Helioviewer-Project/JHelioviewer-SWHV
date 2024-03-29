package org.helioviewer.jhv.view.j2k.image;

import org.helioviewer.jhv.math.MathUtils;

public class SubImage {

    public final int x;
    public final int y;
    public final int w;
    public final int h;
    private final int hash;

    private static final int QUANTA = 32;

    // roundoff to quanta, minimum 1 pixel, clip to full image size
    public SubImage(int xx, int yy, int ww, int hh, int fwidth, int fheight) {
        xx = MathUtils.roundDownTo(xx, QUANTA);
        yy = MathUtils.roundDownTo(yy, QUANTA);
        ww = MathUtils.roundUpTo(ww + QUANTA, QUANTA);
        hh = MathUtils.roundUpTo(hh + QUANTA, QUANTA);

        xx = MathUtils.clip(xx, 0, fwidth - 1);
        yy = MathUtils.clip(yy, 0, fheight - 1);
        ww = MathUtils.clip(ww, 1, fwidth);
        hh = MathUtils.clip(hh, 1, fheight);

        ww = MathUtils.clip(ww, fwidth - xx);
        hh = MathUtils.clip(hh, fheight - yy);

        x = xx;
        y = yy;
        w = ww;
        h = hh;
        hash = computeHash(x, y, w, h);
    }

    private static int computeHash(int _x, int _y, int _w, int _h) {
        int result = _x;
        result = 31 * result + _y;
        result = 31 * result + _w;
        return 31 * result + _h;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (o instanceof SubImage s)
            return x == s.x && y == s.y && w == s.w && h == s.h;
        return false;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return "[x=" + x + " y=" + y + " w=" + w + " h=" + h + ']';
    }

}
