package org.helioviewer.jhv.viewmodel.view.jp2view;

import java.awt.EventQueue;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.AbstractView;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;

// This class is responsible for reading and decoding of JPEG2000 images
public class JP2View extends AbstractView {

    protected JP2Image _jp2Image;

    private int targetFrame = 0;
    private int trueFrame = -1;

    private int frameCount = 0;
    private long frameCountStart;
    private float frameRate;

    private MetaData[] metaData;
    private int maximumFrame;

    public void setJP2Image(JP2Image newJP2Image) {
        _jp2Image = newJP2Image;

        metaData = _jp2Image.metaData;
        maximumFrame = metaData.length - 1;
        frameCountStart = System.currentTimeMillis();
    }

    public String getXMLMetaData() {
        return _jp2Image.getXML(trueFrame + 1);
    }

    private volatile boolean isAbolished = false;

    @Override
    public void abolish() {
        if (isAbolished)
            return;
        isAbolished = true;

        if (_jp2Image != null) {
            _jp2Image.abolish();
            _jp2Image = null;
        }
    }

    // if instance was built before cancelling
    @Override
    protected void finalize() throws Throwable {
        try {
            abolish();
        } finally {
            super.finalize();
        }
    }

    /**
     * This function is used as a callback function which is called by
     * {@link J2KRender} when it has finished decoding an image.
     */
    void setImageData(ImageData newImageData) {
        int frame = newImageData.getMetaData().getFrameNumber();
        if (frame != trueFrame) {
            trueFrame = frame;
            ++frameCount;
        }

        if (dataHandler != null) {
            dataHandler.handleData(newImageData);
        }
    }

    @Override
    public float getCurrentFramerate() {
        long currentTime = System.currentTimeMillis();
        long delta = currentTime - frameCountStart;

        if (delta > 1000) {
            frameRate = 1000 * frameCount / (float) delta;
            frameCount = 0;
            frameCountStart = currentTime;
        }

        return frameRate;
    }

    @Override
    public boolean isMultiFrame() {
        return maximumFrame > 0;
    }

    @Override
    public int getMaximumFrameNumber() {
        return maximumFrame;
    }

    @Override
    public int getCurrentFrameNumber() {
        return targetFrame;
    }

    // to be accessed only from Layers
    @Override
    public JHVDate getNextTime(AnimationMode mode, int deltaT) {
        int next = targetFrame + 1;
        switch (mode) {
        case STOP:
            if (next > maximumFrame) {
                return null;
            }
            break;
        case SWING:
            if (targetFrame == maximumFrame) {
                Layers.setAnimationMode(AnimationMode.SWINGDOWN);
                return metaData[targetFrame - 1].getViewpoint().time;
            }
            break;
        case SWINGDOWN:
            if (targetFrame == 0) {
                Layers.setAnimationMode(AnimationMode.SWING);
                return metaData[1].getViewpoint().time;
            }
            return metaData[targetFrame - 1].getViewpoint().time;
        default: // LOOP
            if (next > maximumFrame) {
                return metaData[0].getViewpoint().time;
            }
        }
        return metaData[next].getViewpoint().time;
    }

    @Override
    public void setFrame(JHVDate time) {
        int frame = getFrameNumber(time);
        if (frame != targetFrame) {
            if (frame > _jp2Image.getStatusCache().getImageCachedPartiallyUntil())
                return;
            targetFrame = frame;
        }
    }

    private int getFrameNumber(JHVDate time) {
        int frame = -1;
        long lastDiff, currentDiff = -Long.MAX_VALUE;
        do {
            lastDiff = currentDiff;
            currentDiff = metaData[++frame].getViewpoint().time.milli - time.milli;
        } while (currentDiff < 0 && frame < maximumFrame);

        if (-lastDiff < currentDiff) {
            return frame - 1;
        } else {
            return frame;
        }
    }

    @Override
    public JHVDate getFrameTime(int frame) {
        if (frame < 0) {
            frame = 0;
        } else if (frame > maximumFrame) {
            frame = maximumFrame;
        }
        return metaData[frame].getViewpoint().time;
    }

    @Override
    public JHVDate getFirstTime() {
        return metaData[0].getViewpoint().time;
    }

    @Override
    public JHVDate getLastTime() {
        return metaData[maximumFrame].getViewpoint().time;
    }

    @Override
    public JHVDate getFrameTime(JHVDate time) {
        return metaData[getFrameNumber(time)].getViewpoint().time;
    }

    @Override
    public MetaData getMetaData(JHVDate time) {
        return metaData[getFrameNumber(time)];
    }

    @Override
    public void render(Camera camera, Viewport vp, double factor) {
        _jp2Image.signalRender(this, camera, vp, camera == null ? null : camera.getViewpoint(), targetFrame, factor);
    }

    void signalRenderFromReader(JP2ImageParameter params) {
        if (isAbolished || params.frame != targetFrame)
            return;
        EventQueue.invokeLater(() -> _jp2Image.execute(this, params, false));
    }

    @Override
    public String getName() {
        return _jp2Image.getName();
    }

    @Override
    public URI getURI() {
        return _jp2Image.getURI();
    }

    @Override
    public LUT getDefaultLUT() {
        return _jp2Image.getDefaultLUT();
    }

    @Override
    public AtomicBoolean getImageCacheStatus(int frame) {
        return _jp2Image.getVisibleStatus(frame);
    }

    @Override
    public boolean isDownloading() {
        return _jp2Image.isDownloading();
    }

}
