package org.helioviewer.jhv.view.jp2view.image;

import java.util.Objects;

import org.helioviewer.jhv.imagedata.SubImage;

public class DecodeParams {

    public final SubImage subImage;
    public final ResolutionSet.ResolutionLevel resolution;
    public final int frame;
    public final double factor;

    public DecodeParams(SubImage _roi, ResolutionSet.ResolutionLevel _resolution, int _frame, double _factor) {
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
        return frame == p.frame && factor == p.factor && subImage.equals(p.subImage) && resolution.equals(p.resolution);
    }

    @Override
    public int hashCode() {
        return Objects.hash(frame, factor, subImage, resolution);
    }

    @Override
    public String toString() {
        return "ImageParams[ " + subImage + ' ' + resolution + " [LayerNum=" + frame + "] " + factor + ']';
    }

}
