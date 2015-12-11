package org.helioviewer.jhv.viewmodel.view;

import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.filters.lut.LUT;
import org.helioviewer.jhv.renderable.components.RenderableImageLayer;
import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatus.CacheStatus;
import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatusLocal;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.ImageDataHandler;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

public abstract class AbstractView implements View {

    private RenderableImageLayer imageLayer;
    private ImageCacheStatusLocal cacheStatus;
    protected ImageData imageData = null;
    protected MetaData _metaData;

    @Override
    public void abolish() {
    }

    @Override
    public void render(Camera camera, Viewport vp, double factor) {
        imageData.setViewpoint(camera.getViewpoint());
        if (dataHandler != null) {
            dataHandler.handleData(imageData);
        }
    }

    @Override
    public CacheStatus getImageCacheStatus(int frame) {
        if (cacheStatus == null) {
            cacheStatus = new ImageCacheStatusLocal(getMaximumFrameNumber());
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
    public void setFrame(JHVDate time) {
    }

    @Override
    public JHVDate getFrameTime(JHVDate time) {
        return getFirstTime();
    }

    @Override
    public JHVDate getFirstTime() {
        return _metaData.getViewpoint().time;
    }

    @Override
    public JHVDate getLastTime() {
        return getFirstTime();
    }

    @Override
    public JHVDate getFrameTime(int frame) {
        return getFirstTime();
    }

    @Override
    public MetaData getMetaData(JHVDate time) {
        return _metaData;
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

}
