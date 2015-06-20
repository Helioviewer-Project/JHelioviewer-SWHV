package org.helioviewer.viewmodel.view;

import org.helioviewer.base.Region;
import org.helioviewer.base.Viewport;
import org.helioviewer.base.datetime.ImmutableDateTime;
import org.helioviewer.jhv.gui.filters.lut.LUT;
import org.helioviewer.jhv.renderable.components.RenderableImageLayer;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.cache.ImageCacheStatus;

public abstract class AbstractView implements View {

    private RenderableImageLayer imageLayer;

    protected MetaData[] metaDataArray = new MetaData[1];
    protected ImageData imageData;
    protected Viewport viewport;
    protected Region region;

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
    public ImmutableDateTime getFrameDateTime(int frame) {
        if (frame <= 0)
            return metaDataArray[0].getDateObs();
        if (frame >= getMaximumFrameNumber())
            return metaDataArray[getMaximumFrameNumber()].getDateObs();
        return metaDataArray[frame].getDateObs();
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

    @Override
    public void setImageLayer(RenderableImageLayer _imageLayer) {
        imageLayer = _imageLayer;
    }

    @Override
    public RenderableImageLayer getImageLayer() {
        return imageLayer;
    }

    protected ViewDataHandler dataHandler;

    @Override
    public void setDataHandler(ViewDataHandler _dataHandler) {
        dataHandler = _dataHandler;
        dataHandler.handleData(this, imageData);
    }

    @Override
    public void removeDataHandler() {
        dataHandler = null;
    }

}
