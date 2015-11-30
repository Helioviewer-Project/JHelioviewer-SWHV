package org.helioviewer.jhv.viewmodel.view;

import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.camera.Viewpoint;
import org.helioviewer.jhv.gui.filters.lut.LUT;
import org.helioviewer.jhv.renderable.components.RenderableImageLayer;
import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatus.CacheStatus;
import org.helioviewer.jhv.viewmodel.imagecache.LocalImageCacheStatus;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.ImageDataHandler;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

public abstract class AbstractView implements View {

    private RenderableImageLayer imageLayer;
    private LocalImageCacheStatus cacheStatus;
    protected ImageData imageData = null;
    protected MetaData[] metaDataArray = new MetaData[1];

    @Override
    public void abolish() {
    }

    @Override
    public void render(double factor) {
        if (dataHandler != null) {
            dataHandler.handleData(imageData);
        }
    }

    @Override
    public CacheStatus getImageCacheStatus(int frame) {
        if (cacheStatus == null) {
            cacheStatus = new LocalImageCacheStatus(getMaximumFrameNumber());
        }
        return cacheStatus.getImageStatus(frame);
    }

    @Override
    public float getCurrentFramerate() {
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
    public void setFrame(int frame, Viewpoint v) {
        imageData.setViewpoint(v);
    }

    @Override
    public int getFrame(JHVDate time) {
        return 0;
    }

    @Override
    public MetaData getMetaData(JHVDate time) {
        return metaDataArray[getFrame(time)];
    }

    @Override
    public JHVDate getFrameDateTime(int frame) {
        if (frame <= 0)
            return metaDataArray[0].getDateObs();
        if (frame >= getMaximumFrameNumber())
            return metaDataArray[getMaximumFrameNumber()].getDateObs();
        return metaDataArray[frame].getDateObs();
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

    protected ImageDataHandler dataHandler;

    @Override
    public void setDataHandler(ImageDataHandler _dataHandler) {
        dataHandler = _dataHandler;
    }

    @Override
    public void removeDataHandler() {
        dataHandler = null;
    }

}
