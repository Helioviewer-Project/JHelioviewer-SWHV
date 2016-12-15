package org.helioviewer.jhv.viewmodel.view.jp2view;

import java.awt.Rectangle;
import java.net.URI;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.viewmodel.imagedata.SubImage;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;

public class JP2ImageCallisto extends JP2Image {

    public JP2ImageCallisto(URI _uri, URI _downloadURI) throws Exception {
        super(_uri, _downloadURI);
    }

    public void setRegion(Rectangle r) {
        region = r;
    }

    private Rectangle region;

    @Override
    protected JP2ImageParameter calculateParameter(Camera camera, Viewport vp, Position.Q p, int frame, double factor) {
        ResolutionLevel res = getResolutionLevel(frame, 0);
        SubImage subImage = new SubImage(region.x, region.y, region.width, region.height, res.width, res.height);

        JP2ImageParameter params = new JP2ImageParameter(this, null, subImage, res, frame, getNumComponents(frame), factor);
        signalReader(params);

        return params;
    }

}
