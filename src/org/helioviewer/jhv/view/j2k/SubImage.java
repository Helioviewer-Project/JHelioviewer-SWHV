package org.helioviewer.jhv.view.j2k;

import org.helioviewer.jhv.math.MathUtils;

record SubImage(int x, int y, int w, int h) {

    private static final int QUANTA = 32;

    // roundoff to quanta, minimum 1 pixel, clip to full image size
    SubImage(int xx, int yy, int ww, int hh, int fwidth, int fheight) {
        xx = MathUtils.roundDownTo(xx, QUANTA);
        yy = MathUtils.roundDownTo(yy, QUANTA);
        ww = MathUtils.roundUpTo(ww + QUANTA, QUANTA);
        hh = MathUtils.roundUpTo(hh + QUANTA, QUANTA);

        xx = Math.clamp(xx, 0, fwidth - 1);
        yy = Math.clamp(yy, 0, fheight - 1);
        ww = Math.clamp(ww, 1, fwidth);
        hh = Math.clamp(hh, 1, fheight);

        ww = Math.min(ww, fwidth - xx);
        hh = Math.min(hh, fheight - yy);

        this(xx, yy, ww, hh);
    }

}
