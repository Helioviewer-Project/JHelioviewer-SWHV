package org.helioviewer.jhv.viewmodel.view.jp2view;

import java.awt.EventQueue;
import java.net.URI;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.filters.lut.LUT;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.threads.JHVThread;
import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatus.CacheStatus;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.AbstractView;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;

/**
 * Implementation of View for JPG2000 images.
 * <p>
 * This class represents the gateway to the heart of the helioviewer project. It
 * is responsible for reading and decoding JPG2000 images.
 */
public class JP2View extends AbstractView {

    private final ArrayBlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<Runnable>(1);
    private final ExecutorService executor = new ThreadPoolExecutor(1, 1, 10000L, TimeUnit.MILLISECONDS, blockingQueue,
                                                    new JHVThread.NamedThreadFactory("Render"), new ThreadPoolExecutor.DiscardPolicy());

    private void queueSubmitTask(Runnable task) {
        blockingQueue.poll();
        executor.execute(task);
    }

    // Member related to JP2
    protected JP2Image _jp2Image;

    private int targetFrame = 0;
    private int trueFrame = -1;

    private int frameCount = 0;
    private long frameCountStart;
    private float frameRate;

    private boolean stopRender = false;

    private MetaData[] metaDataArray;
    private int maximumFrame;

    /**
     * Sets the JPG2000 image used by this class.
     *
     * This functions sets up the whole infrastructure needed for using the
     * image.
     *
     * @param newJP2Image
     */
    public void setJP2Image(JP2Image newJP2Image) {
        _jp2Image = newJP2Image;

        metaDataArray = _jp2Image.metaDataList;
        maximumFrame = metaDataArray.length - 1;

        _jp2Image.startReader(this);
        frameCountStart = System.currentTimeMillis();
    }

    @Override
    public String getName() {
        return _jp2Image.getName();
    }

    public String getXMLMetaData() {
        return _jp2Image.getXML(trueFrame + 1);
    }

    @Override
    public URI getUri() {
        return _jp2Image.getURI();
    }

    @Override
    public URI getDownloadURI() {
        return _jp2Image.getDownloadURI();
    }

    private static class AbolishThread extends Thread {
        private JP2View view;

        public Runnable init(JP2View view) {
            this.view = view;
            return this;
        }

        @Override
        public void run() {
            J2KRender.threadEnv.destroy();

            EventQueue.invokeLater(new Runnable() {
                private JP2View view;

                @Override
                public void run() {
                    view._jp2Image.abolish();
                    view._jp2Image = null;
                }

                public Runnable init(JP2View view) {
                    this.view = view;
                    return this;
                }
            }.init(this.view));
        }
    }

    private volatile boolean isAbolished = false;

    @Override
    public void abolish() {
        isAbolished = true;
        stopRender = true;

        AbolishThread thread = new AbolishThread();
        thread.init(this);
        executor.execute(thread);
        executor.shutdown();
    }

    // if instance was built before cancelling
    @Override
    protected void finalize() {
        if (!isAbolished) {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    abolish();
                }
            });
        }
    }

    /**
     * Sets the new image data for the given region.
     *
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
    public CacheStatus getImageCacheStatus(int frame) {
        return _jp2Image.getImageCacheStatus().getImageStatus(frame);
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
    public JHVDate getNextTime(AnimationMode mode) {
        int next = targetFrame + 1;
        switch (mode) {
            case STOP:
                if (next > maximumFrame)
                    return null;
                break;
            case SWING:
                if (targetFrame == maximumFrame) {
                    Layers.setAnimationMode(AnimationMode.SWINGDOWN);
                    return metaDataArray[targetFrame - 1].getViewpoint().time;
                }
                break;
            case SWINGDOWN:
                if (targetFrame == 0) {
                    Layers.setAnimationMode(AnimationMode.SWING);
                    return metaDataArray[1].getViewpoint().time;
                }
                return metaDataArray[targetFrame - 1].getViewpoint().time;
            default: // LOOP
                if (next > maximumFrame)
                    return metaDataArray[0].getViewpoint().time;
        }
        return metaDataArray[next].getViewpoint().time;
    }

    @Override
    public void setFrame(JHVDate time) {
        int frame = getFrameNumber(time);
        if (frame != targetFrame) {
            CacheStatus status = _jp2Image.getImageCacheStatus().getImageStatus(frame);
            if (status != CacheStatus.PARTIAL && status != CacheStatus.COMPLETE) {
            //    _jp2Image.signalReader(calculateParameter(_jp2Image, v, frame, false)); // wake up reader
                return;
            }
            targetFrame = frame;
        }
    }

    private int getFrameNumber(JHVDate time) {
        int frame = -1;
        long lastDiff, currentDiff = -Long.MAX_VALUE;
        do {
            lastDiff = currentDiff;
            currentDiff = metaDataArray[++frame].getViewpoint().time.milli - time.milli;
        } while (currentDiff < 0 && frame < maximumFrame);

        if (-lastDiff < currentDiff) {
            return frame - 1;
        } else {
            return frame;
        }
    }

    @Override
    public JHVDate getFrameTime(int frame) {
        return metaDataArray[frame].getViewpoint().time;
    }

    @Override
    public JHVDate getFirstTime() {
        return metaDataArray[0].getViewpoint().time;
    }

    @Override
    public JHVDate getLastTime() {
        return metaDataArray[maximumFrame].getViewpoint().time;
    }

    @Override
    public JHVDate getFrameTime(JHVDate time) {
        return metaDataArray[getFrameNumber(time)].getViewpoint().time;
    }

    @Override
    public MetaData getMetaData(JHVDate time) {
        return metaDataArray[getFrameNumber(time)];
    }

////

    private Camera camera;
    private Viewport vp;
    private Position.Q viewpoint;

    @Override
    public void render(Camera _camera, Viewport _vp, double factor) {
        vp = _vp;
        camera = _camera;
        if (camera != null) // Callisto
            viewpoint = camera.getViewpoint();

        signalRender(_jp2Image, false, factor);
    }

    void signalRenderFromReader(JP2Image jp2Image) {
        signalRender(jp2Image, true, 1);
    }

    protected void signalRender(JP2Image jp2Image, boolean fromReader, double factor) {
        // from reader on EDT, might come after abolish
        if (stopRender == true || jp2Image == null)
            return;

        JP2ImageParameter imageViewParams = jp2Image.calculateParameter(camera, vp, viewpoint, targetFrame, fromReader);
        if (imageViewParams == null)
            return;

        if (!(this instanceof JP2ViewCallisto)) {
            int maxDim = Math.max(imageViewParams.subImage.width, imageViewParams.subImage.height);
            double adj = 1;
            if (JHVGlobals.GoForTheBroke && maxDim > JHVGlobals.hiDpiCutoff && Layers.isMoviePlaying()) {
                adj = JHVGlobals.hiDpiCutoff / (double) maxDim;
            }
            factor = Math.min(factor, adj);
        }

        queueSubmitTask(new J2KRender(this, imageViewParams, (float) factor));
    }

    @Override
    public LUT getDefaultLUT() {
        return _jp2Image.getDefaultLUT();
    }

}
