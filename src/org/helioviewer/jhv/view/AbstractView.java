package org.helioviewer.jhv.view;

import java.util.concurrent.atomic.AtomicBoolean;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.ImageDataHandler;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.time.JHVDate;

public abstract class AbstractView implements View {

    private static final AtomicBoolean fullCache = new AtomicBoolean(true);

    private ImageLayer imageLayer;
    protected ImageData imageData = null;
    protected MetaData _metaData;

    protected APIRequest apiRequest;

    @Override
    public APIRequest getAPIRequest() {
        return apiRequest;
    }

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
    public AtomicBoolean getFrameCacheStatus(int frame) {
        return fullCache;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public int getCurrentFramerate() {
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
    public JHVDate getNextTime(AnimationMode mode, int deltaT) {
        return null;
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
    public void setImageLayer(ImageLayer _imageLayer) {
        imageLayer = _imageLayer;
    }

    @Override
    public ImageLayer getImageLayer() {
        return imageLayer;
    }

    protected ImageDataHandler dataHandler;

    @Override
    public void setDataHandler(ImageDataHandler _dataHandler) {
        dataHandler = _dataHandler;
    }

    @Override
    public boolean isDownloading() {
        return false;
    }

}
