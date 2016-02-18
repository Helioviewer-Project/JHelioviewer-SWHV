package org.helioviewer.jhv.viewmodel.view.jp2view;

import java.awt.Rectangle;
import java.net.URI;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.viewmodel.imagedata.SubImage;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;
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
    protected JP2ImageParameter calculateParameter(Camera camera, Viewport vp, Position.Q p, int frame, boolean fromReader) {
        return calculateParameter(0);
    }

    private JP2ImageParameter calculateParameter(int frame) {
        ResolutionSet resolutionSet = getResolutionSet(frame);
        int maxHeight = resolutionSet.getResolutionLevel(0).height;
        int maxWidth = resolutionSet.getResolutionLevel(0).width;

        ResolutionLevel res = resolutionSet.getResolutionLevel(0);

        SubImage subImage = new SubImage((int) (region.x / (double) maxWidth * res.width), (int) (region.y / (double) maxHeight * res.height), (int) Math.ceil(region.width / (double) maxWidth * res.width), (int) Math.ceil(region.height / (double) maxHeight * res.height), res.width, res.height);

        JP2ImageParameter imageViewParams = new JP2ImageParameter(this, null, subImage, res, frame);
        signalReader(imageViewParams);

        return imageViewParams;
    }

}
