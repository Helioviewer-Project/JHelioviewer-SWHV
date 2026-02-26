package org.helioviewer.jhv.view.j2k;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.math.MathUtils;

class J2KParams {

    static record SubImage(int x, int y, int w, int h) {

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

    static record Decode(int serial, int frame, SubImage subImage, int level, float factor) {
        private static int computeHash(int s, int fr, SubImage si, int l, float f) {
            int result = s;
            result = 31 * result + fr;
            result = 31 * result + si.hashCode();
            result = 31 * result + l;
            int factorHash = (f == 0f) ? 0 : Float.floatToRawIntBits(f); // collapse +0.0f and -0.0f
            return 31 * result + factorHash;
        }

        @Override
        public final boolean equals(Object o) {
            if (this == o)
                return true;
            if (o instanceof Decode p)
                return serial == p.serial() && frame == p.frame() && level == p.level() && factor == p.factor() && subImage.equals(p.subImage());
            return false;
        }

        @Override
        public int hashCode() {
            return computeHash(serial, frame, subImage, level, factor);
        }

        @Override
        public String toString() {
            return "[Frame=" + frame + ' ' + subImage + " Level=" + level + " Factor=" + factor + ']';
        }

    }

    static class Read {

        final J2KView view;
        final Decode decodeParams;
        final Position viewpoint; // sync with camera & between layers
        final boolean complete; // cache the decoded data
        boolean priority;

        Read(J2KView _view, Decode _decodeParams, Position _viewpoint, boolean _complete, boolean _priority) {
            view = _view;
            decodeParams = _decodeParams;
            viewpoint = _viewpoint;
            complete = _complete;
            priority = _priority;
        }

    }

}
