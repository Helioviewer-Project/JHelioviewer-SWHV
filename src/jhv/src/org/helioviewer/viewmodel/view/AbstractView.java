package org.helioviewer.viewmodel.view;

import org.helioviewer.base.time.ImmutableDateTime;
import org.helioviewer.jhv.gui.filters.lut.LUT;
import org.helioviewer.jhv.renderable.components.RenderableImageLayer;
import org.helioviewer.viewmodel.imagecache.ImageCacheStatus;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.metadata.MetaData;

public abstract class AbstractView implements View {

    private RenderableImageLayer imageLayer;

    protected MetaData[] metaDataArray = new MetaData[1];
    protected ImageData imageData;

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
    public void setFrame(int frame) {
    }

    @Override
    public int getFrame(ImmutableDateTime time) {
        return 0;
    }

    @Override
    public MetaData getMetaData(ImmutableDateTime time) {
        return metaDataArray[getFrame(time)];
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
