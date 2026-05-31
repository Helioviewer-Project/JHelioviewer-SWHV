package org.helioviewer.jhv.view.j2k;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.math.MathUtils;

class J2KParams {

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

    static final class Decode {

        final int frame;
        final SubImage subImage;
        final int level;
        final float factor;
        private final int factorBits;
        private final int hash;

        Decode(int _frame, SubImage _subImage, int _level, float _factor) {
            frame = _frame;
            subImage = _subImage;
            level = _level;
            factor = _factor;
            factorBits = Float.floatToIntBits(_factor);

            int ret = 17;
            ret = 31 * ret + frame;
            ret = 31 * ret + subImage.x;
            ret = 31 * ret + subImage.y;
            ret = 31 * ret + subImage.w;
            ret = 31 * ret + subImage.h;
            ret = 31 * ret + level;
            ret = 31 * ret + factorBits;
            hash = ret;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Decode other
                    && frame == other.frame
                    && subImage.x == other.subImage.x
                    && subImage.y == other.subImage.y
                    && subImage.w == other.subImage.w
                    && subImage.h == other.subImage.h
                    && level == other.level
                    && factorBits == other.factorBits;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    record Read(J2KView view, J2KSource.Remote source, Decode decodeParams, Position viewpoint, boolean priority) {}

}
