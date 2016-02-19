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
        return calculateParameter(0, factor);
    }

    private JP2ImageParameter calculateParameter(int frame, double factor) {
        ResolutionLevel res = getResolutionSet(frame).getResolutionLevel(0);
        SubImage subImage = new SubImage(region.x, region.y, (int) Math.ceil(region.width), (int) Math.ceil(region.height), res.width, res.height);

        JP2ImageParameter imageViewParams = new JP2ImageParameter(this, null, subImage, res, frame, factor);
        signalReader(imageViewParams);

        return imageViewParams;
    }

}
