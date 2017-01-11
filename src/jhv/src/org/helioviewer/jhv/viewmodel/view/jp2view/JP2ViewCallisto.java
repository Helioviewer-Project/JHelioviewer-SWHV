package org.helioviewer.jhv.viewmodel.view.jp2view;

import java.awt.Rectangle;
import java.net.URI;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.viewmodel.imagedata.SubImage;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;

public class JP2ViewCallisto extends JP2View {

    public JP2ViewCallisto(URI _uri) throws Exception {
        super(_uri);
    }

    public void setRegion(Rectangle r) {
        region = r;
    }

    private Rectangle region;

    @Override
    JP2ImageParameter calculateParameter(Camera camera, Viewport vp, Position.Q p, int frame, double factor) {
        ResolutionLevel res = getResolutionLevel(frame, 0);
        SubImage subImage = new SubImage(region.x, region.y, region.width, region.height, res.width, res.height);

        JP2ImageParameter params = new JP2ImageParameter(null, subImage, res, frame, factor, true);
        signalReader(params);

        return params;
    }

}
