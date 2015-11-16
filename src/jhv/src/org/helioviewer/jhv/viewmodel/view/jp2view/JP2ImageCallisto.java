package org.helioviewer.jhv.viewmodel.view.jp2view;

import java.awt.Rectangle;
import java.net.URI;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;
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
    protected JP2ImageParameter calculateParameter(JHVDate masterTime, int frameNumber, boolean fromReader) {
        double rWidth = region.getWidth();
        double rHeight = region.getHeight();

        int maxHeight = resolutionSet.getResolutionLevel(0).getResolutionBounds().height;
        int maxWidth = resolutionSet.getResolutionLevel(0).getResolutionBounds().width;

        ResolutionLevel res = resolutionSet.getPreviousResolutionLevel((int) Math.ceil(viewport.width / rWidth * maxWidth),
                                                                   2 * (int) Math.ceil(viewport.height / rHeight * maxHeight));
        Rectangle rect = res.getResolutionBounds();

        SubImage subImage = new SubImage((int) (region.getLLX() / maxWidth * rect.width), (int) (region.getLLY() / maxHeight * rect.height),
                                         (int) Math.ceil(rWidth / maxWidth * rect.width), (int) Math.ceil(rHeight / maxHeight * rect.height), rect);

        return new JP2ImageParameter(this, masterTime, subImage, res, 1, frameNumber);
    }

}
