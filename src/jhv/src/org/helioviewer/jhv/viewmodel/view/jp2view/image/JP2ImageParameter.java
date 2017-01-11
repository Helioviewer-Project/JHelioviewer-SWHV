package org.helioviewer.jhv.viewmodel.view.jp2view.image;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.viewmodel.imagedata.SubImage;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;

public class JP2ImageParameter {

    public final Position.Q viewpoint;
    public final SubImage subImage;
    public final ResolutionLevel resolution;
    public final int frame;
    public final double factor;
    public boolean priority;

    public JP2ImageParameter(Position.Q _p, SubImage _roi, ResolutionLevel _resolution, int _frame, double _factor, boolean _priority) {
        viewpoint = _p;
        subImage = _roi;
        resolution = _resolution;
        frame = _frame;
        factor = _factor;
        priority = _priority;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JP2ImageParameter))
            return false;

        JP2ImageParameter p = (JP2ImageParameter) o;
        return frame == p.frame && factor == p.factor &&
               viewpoint.equals(p.viewpoint) && subImage.equals(p.subImage) && resolution.equals(p.resolution);
    }

    @Override
    public int hashCode() {
        assert false : "hashCode not designed";
        return 42;
    }

    @Override
    public String toString() {
        return "ImageViewParams[ " + viewpoint + " " + subImage + " " + resolution + " [LayerNum=" + frame + "] " + factor + "]";
    }

}
