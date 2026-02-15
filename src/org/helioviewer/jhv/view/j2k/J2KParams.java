package org.helioviewer.jhv.view.j2k;

import org.helioviewer.jhv.astronomy.Position;

class J2KParams {

    static class Decode {

        final int serial;
        final int frame;
        final SubImage subImage;
        final int level;
        final float factor;
        final boolean complete; // cache the decoded data
        final Position viewpoint; // sync with camera & between layers
        private final int hash;

        Decode(int _serial, int _frame, SubImage _subImage, int _level, float _factor, boolean _complete, Position _viewpoint) {
            serial = _serial;
            frame = _frame;
            subImage = _subImage;
            level = _level;
            factor = _factor;
            complete = _complete;
            viewpoint = _viewpoint;
            hash = computeHash(serial, frame, subImage, level, factor);
        }

        private static int computeHash(int s, int fr, SubImage si, int l, float f) { // viewpoint, complete don't participate
            int result = s;
            result = 31 * result + fr;
            result = 31 * result + si.hashCode();
            result = 31 * result + l;
            int factorHash = (f == 0f) ? 0 : Float.floatToRawIntBits(f); // collapse +0.0f and -0.0f
            return 31 * result + factorHash;
        }

        @Override
        public final boolean equals(Object o) { // viewpoint, complete don't participate
            if (this == o)
                return true;
            if (o instanceof Decode p)
                return serial == p.serial && frame == p.frame && level == p.level && factor == p.factor && subImage.equals(p.subImage);
            return false;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return "[Frame=" + frame + ' ' + subImage + " Level=" + level + " Factor=" + factor + ']';
        }

    }

    static class Read {

        final J2KView view;
        final Decode decodeParams;
        boolean priority;

        Read(J2KView _view, Decode _decodeParams, boolean _priority) {
            view = _view;
            decodeParams = _decodeParams;
            priority = _priority;
        }

    }

}
