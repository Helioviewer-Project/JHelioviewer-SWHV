package org.helioviewer.jhv.viewmodel.view.jp2view.image;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.viewmodel.imagedata.SubImage;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;

public class JP2ImageParameter {

    public final JP2Image jp2Image;
    public final Position.Q viewpoint;
    public final SubImage subImage;
    public final ResolutionLevel resolution;
    public final int compositionLayer;
    public final int components;
    public final double factor;

    public JP2ImageParameter(JP2Image _jp2Image, Position.Q _p, SubImage _roi, ResolutionLevel _resolution, int _compositionLayer, int _components, double _factor) {
        jp2Image = _jp2Image;
        viewpoint = _p;
        subImage = _roi;
        resolution = _resolution;
        compositionLayer = _compositionLayer;
        components = _components;
        factor = _factor;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JP2ImageParameter))
            return false;

        JP2ImageParameter p = (JP2ImageParameter) o;
        return components == p.components && // can't happen otherwise
               compositionLayer == p.compositionLayer && factor == p.factor &&
               jp2Image.equals(p.jp2Image) && viewpoint.equals(p.viewpoint) &&
               subImage.equals(p.subImage) && resolution.equals(p.resolution);
    }

    @Override
    public int hashCode() {
        assert false : "hashCode not designed";
        return 42;
    }

    @Override
    public String toString() {
        return "ImageViewParams[ " + jp2Image + " " + viewpoint + " " + subImage + " " + resolution + " [LayerNum=" + compositionLayer + " NumComponents=" + components + "] " + factor + "]";
    }

}
