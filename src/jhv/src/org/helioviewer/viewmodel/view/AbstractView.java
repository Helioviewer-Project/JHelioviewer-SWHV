package org.helioviewer.viewmodel.view;

import org.helioviewer.base.Region;
import org.helioviewer.base.Viewport;
import org.helioviewer.base.datetime.ImmutableDateTime;
import org.helioviewer.jhv.gui.filters.lut.LUT;
import org.helioviewer.jhv.renderable.components.RenderableImageLayer;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.view.cache.ImageCacheStatus;

import com.jogamp.opengl.GL2;

public abstract class AbstractView implements View {

    private RenderableImageLayer imageLayer;

    protected ImageData imageData;
    protected Viewport viewport;
    protected Region region;

    // <!-- Defaults
    @Override
    public void abolish() {
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public ImageCacheStatus getImageCacheStatus() {
        return null;
    }

    @Override
    public float getActualFramerate() {
        return 0;
    }

    @Override
    public boolean isMultiFrame() {
        return false;
    }

    @Override
    public int getCurrentFrameNumber() {
        return 0;
    }

    @Override
    public int getMaximumFrameNumber() {
        return 0;
    }

    @Override
    public int getMaximumAccessibleFrameNumber() {
        return 0;
    }

    @Override
    public void setFrame(int frame) {
    }

    @Override
    public int getFrame(ImmutableDateTime time) {
        return 0;
    }

    @Override
    public ImmutableDateTime getFrame(int frame) {
        return getMetaData().getDateObs();
    }

    @Override
    public ImageData getImageData() {
        return imageData;
    }

    @Override
    public ImageData getBaseDifferenceImageData() {
        return imageData;
    }

    @Override
    public ImageData getPreviousImageData() {
        return imageData;
    }

    @Override
    public boolean setRegion(Region r) {
        return false;
    }

    @Override
    public boolean setViewport(Viewport v) {
        return false;
    }

    @Override
    public LUT getDefaultLUT() {
        return null;
    }

    // -->

    public void setImageLayer(RenderableImageLayer _imageLayer) {
        imageLayer = _imageLayer;
    }

    public RenderableImageLayer getImageLayer() {
        return imageLayer;
    }

    protected AbstractViewDataHandler dataHandler;

    public void setDataHandler(AbstractViewDataHandler _dataHandler) {
        dataHandler = _dataHandler;
        dataHandler.handleData(this, imageData);
    }

    public void removeDataHandler() {
        dataHandler = null;
    }

}
