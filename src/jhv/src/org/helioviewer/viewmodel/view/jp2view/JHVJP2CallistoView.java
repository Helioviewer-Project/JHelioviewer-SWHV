package org.helioviewer.viewmodel.view.jp2view;

import org.helioviewer.base.Region;
import org.helioviewer.base.Viewport;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;
import org.helioviewer.viewmodel.view.jp2view.image.SubImage;

public class JHVJP2CallistoView extends JHVJP2View {

    private Viewport viewport = new Viewport(86400, 380);

    public JHVJP2CallistoView() {
        Displayer.removeRenderListener(this);
    }

    public JP2Image getJP2Image() {
        return _jp2Image;
    }

    public void setViewport(Viewport v) {
        viewport = v;
    }

    public boolean setRegion(Region r) {
        targetRegion = r;
        signalRender(_jp2Image);
        return true;
    }

    @Override
    public void render() {
        System.out.println(">>> Should not be called");
        Thread.dumpStack();
    }

    @Override
    void setSubimageData(ImageData newImageData, JP2ImageParameter params) {
        if (dataHandler != null) {
            dataHandler.handleData(this, newImageData);
        }
    }

    @Override
    protected JP2ImageParameter calculateParameter(JP2Image jp2Image, Region r, int frameNumber) {
        ResolutionSet set = jp2Image.getResolutionSet();
        int maxHeight = set.getResolutionLevel(0).getResolutionBounds().height;
        int maxWidth = set.getResolutionLevel(0).getResolutionBounds().width;
        ResolutionLevel res = set.getClosestResolutionLevel((int) Math.ceil(viewport.getWidth() / r.getWidth() * maxWidth), 2 * (int) Math.ceil(viewport.getHeight() / r.getHeight() * maxHeight));

        SubImage subImage = new SubImage((int) (r.getLowerLeftCorner().x / maxWidth * res.getResolutionBounds().width), (int) (r.getLowerLeftCorner().y / maxHeight * res.getResolutionBounds().height), (int) (r.getWidth() / maxWidth * res.getResolutionBounds().width), (int) (r.getHeight() / maxHeight * res.getResolutionBounds().height));
        return new JP2ImageParameter(jp2Image, subImage, res, frameNumber);
    }

}
