package org.helioviewer.jhv.view.j2k.image;

import java.util.Objects;

import org.helioviewer.jhv.imagedata.SubImage;
import org.helioviewer.jhv.math.Quat;

public class DecodeParams {

    public final int serialNo; // sync between layers
    public final Quat q; // sync with camera
    public final boolean complete; // cache the decoded data
    public final SubImage subImage;
    public final ResolutionSet.ResolutionLevel resolution;
    public final int frame;
    public final double factor;

    public DecodeParams(int _serialNo, Quat _q, boolean _complete, SubImage _roi, ResolutionSet.ResolutionLevel _resolution, int _frame, double _factor) {
        serialNo = _serialNo;
        q = _q;
        complete = _complete;
        subImage = _roi;
        resolution = _resolution;
        frame = _frame;
        factor = _factor;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DecodeParams))
            return false;
        DecodeParams p = (DecodeParams) o;
        return frame == p.frame && factor == p.factor && subImage.equals(p.subImage) && resolution.equals(p.resolution); // serialNo, q, complete don't participate
    }

    @Override
    public int hashCode() {
        return Objects.hash(frame, factor, subImage, resolution); // serialNo, q, complete don't participate
    }

    @Override
    public String toString() {
        return "[Frame=" + frame + ' ' + subImage + ' ' + resolution + " Factor=" + factor + ']';
    }

}
