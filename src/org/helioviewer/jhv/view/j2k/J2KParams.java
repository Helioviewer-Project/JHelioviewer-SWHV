package org.helioviewer.jhv.view.j2k;

class J2KParams {

    record SubImage(int x, int y, int w, int h) {}

    static final class Decode {

        final int frame;
        final SubImage subImage;
        final int level;
        private final int hash;

        Decode(int _frame, SubImage _subImage, int _level) {
            frame = _frame;
            subImage = _subImage;
            level = _level;

            int ret = 17;
            ret = 31 * ret + frame;
            ret = 31 * ret + subImage.x;
            ret = 31 * ret + subImage.y;
            ret = 31 * ret + subImage.w;
            ret = 31 * ret + subImage.h;
            ret = 31 * ret + level;
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
                    && level == other.level;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    record Read(J2KView view, Decode decodeParams, boolean priority) {}

}
