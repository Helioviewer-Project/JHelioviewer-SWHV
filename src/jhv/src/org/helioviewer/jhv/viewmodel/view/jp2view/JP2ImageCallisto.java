package org.helioviewer.jhv.viewmodel.view.jp2view;

import java.awt.Rectangle;
import java.net.URI;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.camera.Viewpoint;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.SubImage;

public class JP2ImageCallisto extends JP2Image {

    public JP2ImageCallisto(URI _uri, URI _downloadURI) throws Exception {
        super(_uri, _downloadURI);
    }

    protected void setViewport(Rectangle v) {
        viewport = v;
    }

    protected void setRegion(Region r) {
        region = r;
    }

    private Region region;
    private Rectangle viewport = new Rectangle(86400, 380);

    @Override
    protected JP2ImageParameter calculateParameter(Viewpoint v, int frameNumber, boolean fromReader) {
        int maxHeight = resolutionSet.getResolutionLevel(0).getResolutionBounds().height;
        int maxWidth = resolutionSet.getResolutionLevel(0).getResolutionBounds().width;

        ResolutionLevel res = resolutionSet.getPreviousResolutionLevel((int) Math.ceil(viewport.width / region.width * maxWidth),
                                                                   2 * (int) Math.ceil(viewport.height / region.height * maxHeight));
        Rectangle rect = res.getResolutionBounds();

        SubImage subImage = new SubImage((int) (region.llx / maxWidth * rect.width), (int) (region.lly / maxHeight * rect.height),
                                         (int) Math.ceil(region.width / maxWidth * rect.width), (int) Math.ceil(region.height / maxHeight * rect.height), rect);

        JP2ImageParameter imageViewParams = new JP2ImageParameter(this, v, subImage, res, frameNumber);
        signalReader(imageViewParams);

        return imageViewParams;
    }

}
