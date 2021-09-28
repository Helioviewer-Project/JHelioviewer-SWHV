package org.helioviewer.jhv.view.j2k.image;

import org.helioviewer.jhv.astronomy.Position;

public class DecodeParams {

    public final int serial;
    public final int frame;
    public final SubImage subImage;
    public final int level;
    public final float factor;
    public final boolean complete; // cache the decoded data
    public final Position viewpoint; // sync with camera & between layers
    private final int hash;

    public DecodeParams(int _serial, int _frame, SubImage _subImage, int _level, float _factor, boolean _complete, Position _viewpoint) {
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
        int result = 31 + s;
        result = 31 * result + fr;
        result = 31 * result + si.hashCode();
        result = 31 * result + l;
        return 31 * result + Float.floatToRawIntBits(f);
    }

    @Override
    public final boolean equals(Object o) { // viewpoint, complete don't participate
        if (this == o)
            return true;
        if (o instanceof DecodeParams p)
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
