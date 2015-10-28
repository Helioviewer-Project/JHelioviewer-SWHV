package org.helioviewer.jhv.viewmodel.view.jp2view;

import java.awt.Rectangle;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.SubImage;

public class JP2CallistoView extends JP2View {

    private Region region;
    private Rectangle viewport = new Rectangle(86400, 380);

    public JP2Image getJP2Image() {
        return _jp2Image;
    }

    public void setViewport(Rectangle v) {
        viewport = v;
    }

    public boolean setRegion(Region r) {
        region = r;
        signalRender(_jp2Image, false);
        return true;
    }

    @Override
    public void render() {
        // should be called only during setJP2Image
    }

    @Override
    void setSubimageData(ImageData newImageData, JP2ImageParameter params) {
        if (dataHandler != null) {
            dataHandler.handleData(this, newImageData);
        }
    }

    @Override
    protected JP2ImageParameter calculateParameter(JP2Image jp2Image, JHVDate masterTime, int frameNumber, boolean fromReader) {
        double rWidth = region.getWidth();
        double rHeight = region.getHeight();

        ResolutionSet set = jp2Image.getResolutionSet();
        int maxHeight = set.getResolutionLevel(0).getResolutionBounds().height;
        int maxWidth = set.getResolutionLevel(0).getResolutionBounds().width;

        ResolutionLevel res = set.getPreviousResolutionLevel((int) Math.ceil(viewport.width / rWidth * maxWidth),
                2 * (int) Math.ceil(viewport.height / rHeight * maxHeight));
        Rectangle rect = res.getResolutionBounds();

        SubImage subImage = new SubImage((int) (region.getLowerLeftCorner().x / maxWidth * rect.width),
                (int) (region.getLowerLeftCorner().y / maxHeight * rect.height),
                (int) Math.ceil(rWidth / maxWidth * rect.width),
                (int) Math.ceil(rHeight / maxHeight * rect.height), rect);

        return new JP2ImageParameter(jp2Image, masterTime, subImage, res, frameNumber);
    }

}
