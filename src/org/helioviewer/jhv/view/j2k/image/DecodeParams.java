package org.helioviewer.jhv.view.j2k.image;

import org.helioviewer.jhv.imagedata.SubImage;
import org.helioviewer.jhv.position.Position;
import org.helioviewer.jhv.view.j2k.J2KView;

public class DecodeParams {

    public final J2KView view;
    public final Position viewpoint; // sync with camera & between layers
    public final boolean complete; // cache the decoded data
    public final SubImage subImage;
    public final ResolutionSet.ResolutionLevel resolution;
    public final int frame;
    public final double factor;
    private final int hash;

    public DecodeParams(J2KView _view, Position _viewpoint, boolean _complete, SubImage _roi, ResolutionSet.ResolutionLevel _resolution, int _frame, double _factor) {
        view = _view;
        viewpoint = _viewpoint;
        complete = _complete;
        subImage = _roi;
        resolution = _resolution;
        frame = _frame;
        factor = _factor;
        hash = computeHash(view, subImage, resolution, frame, factor);
    }

    private static int computeHash(J2KView v, SubImage s, ResolutionSet.ResolutionLevel r, int fr, double f) { // viewpoint, complete don't participate
        int result = 1;
        result = 31 * result + v.hashCode();
        result = 31 * result + s.hashCode();
        result = 31 * result + r.hashCode();
        result = 31 * result + fr;
        long tmp = Double.doubleToLongBits(f);
        return 31 * result + (int) (tmp ^ (tmp >>> 32));
    }

    @Override
    public final boolean equals(Object o) { // viewpoint, complete don't participate
        if (this == o)
            return true;
        if (!(o instanceof DecodeParams))
            return false;
        DecodeParams p = (DecodeParams) o;
        return view == p.view && frame == p.frame && factor == p.factor && subImage.equals(p.subImage) && resolution.equals(p.resolution);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return "[Frame=" + frame + ' ' + subImage + ' ' + resolution + " Factor=" + factor + ']';
    }

}
