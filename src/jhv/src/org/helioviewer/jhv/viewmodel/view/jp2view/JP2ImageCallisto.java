package org.helioviewer.jhv.viewmodel.view.jp2view;

import java.awt.Rectangle;
import java.net.URI;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.Viewpoint;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.SubImage;

public class JP2ImageCallisto extends JP2Image {

    public JP2ImageCallisto(URI _uri, URI _downloadURI) throws Exception {
        super(_uri, _downloadURI);
    }

    public void setViewport(Rectangle v) {
        viewport = v;
    }

    public void setRegion(Rectangle r) {
        region = r;
    }

    private Rectangle region;
    private Rectangle viewport;

    @Override
    protected JP2ImageParameter calculateParameter(Camera camera, Viewport vp, Viewpoint v, int frame, boolean fromReader) {
        return calculateParameter(0);
    }

    private JP2ImageParameter calculateParameter(int frame) {
        ResolutionSet resolutionSet = getResolutionSet(frame);
        int maxHeight = resolutionSet.getResolutionLevel(0).getResolutionBounds().height;
        int maxWidth = resolutionSet.getResolutionLevel(0).getResolutionBounds().width;

        ResolutionLevel res = resolutionSet.getPreviousResolutionLevel((int) Math.ceil(viewport.width / (double) region.width * maxWidth),
                                                                   2 * (int) Math.ceil(viewport.height / (double) region.height * maxHeight));
        Rectangle rect = res.getResolutionBounds();

        SubImage subImage = new SubImage((int) (region.x / (double) maxWidth * rect.width), (int) (region.y / (double) maxHeight * rect.height),
                                         (int) Math.ceil(region.width / (double) maxWidth * rect.width), (int) Math.ceil(region.height / (double) maxHeight * rect.height), rect);

        JP2ImageParameter imageViewParams = new JP2ImageParameter(this, null, subImage, res, frame);
        signalReader(imageViewParams);

        return imageViewParams;
    }

}
