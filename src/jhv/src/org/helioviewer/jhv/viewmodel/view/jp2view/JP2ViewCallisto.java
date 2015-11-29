package org.helioviewer.jhv.viewmodel.view.jp2view;

import java.awt.Rectangle;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.SubImage;

public class JP2ViewCallisto extends JP2View {

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
        signalRender(_jp2Image, false, 1);
        return true;
    }

    @Override
    public void render(double factor) {
        // should be called only during setJP2Image
    }

    @Override
    protected JP2ImageParameter calculateParameter(JP2Image jp2Image, JHVDate masterTime, int frameNumber, boolean fromReader) {
        ResolutionSet set = jp2Image.getResolutionSet();
        int maxHeight = set.getResolutionLevel(0).getResolutionBounds().height;
        int maxWidth = set.getResolutionLevel(0).getResolutionBounds().width;

        ResolutionLevel res = set.getPreviousResolutionLevel((int) Math.ceil(viewport.width / region.width * maxWidth),
                                                         2 * (int) Math.ceil(viewport.height / region.height * maxHeight));
        Rectangle rect = res.getResolutionBounds();

        SubImage subImage = new SubImage((int) (region.llx / maxWidth * rect.width), (int) (region.lly / maxHeight * rect.height),
                                         (int) Math.ceil(region.width / maxWidth * rect.width), (int) Math.ceil(region.height / maxHeight * rect.height), rect);

        JP2ImageParameter imageViewParams = new JP2ImageParameter(jp2Image, masterTime, subImage, res, frameNumber);
        jp2Image.signalReader(imageViewParams);

        return imageViewParams;
    }

}
