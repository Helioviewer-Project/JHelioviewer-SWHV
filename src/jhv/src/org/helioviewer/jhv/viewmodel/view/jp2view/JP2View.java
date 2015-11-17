package org.helioviewer.jhv.viewmodel.view.jp2view;

import java.awt.EventQueue;
import java.net.URI;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.Viewport;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.filters.lut.LUT;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.threads.JHVThread;
import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatus.CacheStatus;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.AbstractView;
import org.helioviewer.jhv.viewmodel.view.ViewROI;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2Image.ReaderMode;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.SubImage;

/**
 * Implementation of View for JPG2000 images.
 * <p>
 * This class represents the gateway to the heart of the helioviewer project. It
 * is responsible for reading and decoding JPG2000 images.
 */
public class JP2View extends AbstractView {

    static private class RejectExecution implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            System.out.println(Thread.currentThread().getName());
        }
    }

    private final BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<Runnable>(1);
    private final RejectedExecutionHandler rejectedExecutionHandler = new RejectExecution(); // new ThreadPoolExecutor.CallerRunsPolicy();
    private final ExecutorService executor = new ThreadPoolExecutor(1, 1, 10000L, TimeUnit.MILLISECONDS, blockingQueue, new JHVThread.NamedThreadFactory("Render"), new ThreadPoolExecutor.DiscardPolicy()/* rejectedExecutionHandler */);

    private void queueSubmitTask(Runnable task) {
        blockingQueue.poll();
        executor.execute(task);
    }

    // Member related to JP2
    protected JP2Image _jp2Image;

    private JHVDate targetMasterTime;

    private int targetFrame = 0;
    private int trueFrame;

    private int frameCount = 0;
    private long frameCountStart;
    private float frameRate;

    private boolean stopRender = false;

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
        targetMasterTime = metaDataArray[0].getDateObs();

        _jp2Image.startReader(this);
        frameCountStart = System.currentTimeMillis();
    }

    @Override
    public String getName() {
        return _jp2Image.getName(0);
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

    private JP2ImageParameter oldImageViewParams;

    // Recalculates the image parameters used within the jp2-package
    // Reader signals only for CURRENTFRAME*
    protected JP2ImageParameter calculateParameter(JP2Image jp2Image, JHVDate masterTime, int frameNumber, boolean fromReader) {
        Camera camera = Displayer.getCamera();
        Viewport vp = Displayer.getViewport();

        MetaData m = jp2Image.metaDataList[frameNumber];
        Region mr = m.getPhysicalRegion();
        Region r = ViewROI.updateROI(camera, vp, masterTime, m);

        double mWidth = mr.getWidth();
        double mHeight = mr.getHeight();
        double rWidth = r.getWidth();
        double rHeight = r.getHeight();

        double ratio = 2 * camera.getWidth() / vp.getHeight();
        int totalHeight = (int) (mHeight / ratio);

        ResolutionLevel res = jp2Image.getResolutionSet().getNextResolutionLevel(totalHeight, totalHeight);
        int viewportImageWidth = res.getResolutionBounds().width;
        int viewportImageHeight = res.getResolutionBounds().height;

        double currentMeterPerPixel = mWidth / viewportImageWidth;
        int imageWidth = (int) Math.round(rWidth / currentMeterPerPixel);
        int imageHeight = (int) Math.round(rHeight / currentMeterPerPixel);

        double displacementX = r.getULX() - mr.getULX();
        double displacementY = r.getULY() - mr.getULY();

        int imagePositionX = (int) Math.round(displacementX / mWidth * viewportImageWidth);
        int imagePositionY = -(int) Math.round(displacementY / mHeight * viewportImageHeight);

        SubImage subImage = new SubImage(imagePositionX, imagePositionY, imageWidth, imageHeight, res.getResolutionBounds());

        float scaleAdj = 1;
        int maxDim = Math.max(imageWidth, imageHeight);
        if (JHVGlobals.GoForTheBroke && maxDim > JHVGlobals.hiDpiCutoff && Layers.isMoviePlaying()) {
            scaleAdj = JHVGlobals.hiDpiCutoff / (float) maxDim;
        }

        JP2ImageParameter newImageViewParams = new JP2ImageParameter(jp2Image, masterTime, subImage, res, scaleAdj, frameNumber);
        if (!fromReader && jp2Image.getImageCacheStatus().getImageStatus(frameNumber) == CacheStatus.COMPLETE && newImageViewParams.equals(oldImageViewParams)) {
            Displayer.display();
            return null;
        }
        oldImageViewParams = newImageViewParams;

        return newImageViewParams;
    }

    /**
     * Sets the new image data for the given region.
     *
     * This function is used as a callback function which is called by
     * {@link J2KRender} when it has finished decoding an image.
     *
     * @param newImageData
     *            New image data
     * @param params
     *            New JP2Image parameters
     * @param prevParams
     * @param prevData
     */

    void setSubimageData(ImageData newImageData, JP2ImageParameter params) {
        int frame = params.compositionLayer;
        MetaData metaData = params.jp2Image.metaDataList[frame];

        newImageData.setFrameNumber(frame);
        newImageData.setMetaData(metaData);
        newImageData.setMasterTime(params.masterTime);

        if (metaData instanceof HelioviewerMetaData) {
            newImageData.setRegion(((HelioviewerMetaData) metaData).roiToRegion(params.subImage, params.resolution.getZoomPercent()));
        }

        if (frame != trueFrame) {
            trueFrame = frame;
            ++frameCount;
        }

        if (dataHandler != null) {
            dataHandler.handleData(this, newImageData);
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
        return _jp2Image.isMultiFrame();
    }

    @Override
    public int getMaximumFrameNumber() {
        return _jp2Image.getMaximumFrameNumber();
    }

    @Override
    public int getCurrentFrameNumber() {
        return targetFrame;
    }

    // to be accessed only from Layers
    @Override
    public void setFrame(int frame, JHVDate masterTime) {
        if (frame != targetFrame && frame >= 0 && frame <= _jp2Image.getMaximumFrameNumber()) {
            CacheStatus status = _jp2Image.getImageCacheStatus().getImageStatus(frame);
            if (status != CacheStatus.PARTIAL && status != CacheStatus.COMPLETE) {
                _jp2Image.signalReader(calculateParameter(_jp2Image, masterTime, frame, false)); // wake up reader
                return;
            }

            targetFrame = frame;
            targetMasterTime = masterTime;

            if (_jp2Image.getReaderMode() != ReaderMode.ONLYFIREONCOMPLETE) {
                render(1);
            }
        }
    }

    // to be accessed only from Layers
    @Override
    public int getFrame(JHVDate time) {
        int frame = -1;
        long milli = time.getTime();
        long lastDiff, currentDiff = -Long.MAX_VALUE;
        do {
            lastDiff = currentDiff;
            currentDiff = metaDataArray[++frame].getDateObs().getTime() - milli;
        } while (currentDiff < 0 && frame < _jp2Image.getMaximumFrameNumber());

        if (-lastDiff < currentDiff) {
            return frame - 1;
        } else {
            return frame;
        }
    }

    @Override
    public void render(float factor) {
        signalRender(_jp2Image, false, factor);
    }

    void signalRenderFromReader(JP2Image jp2Image) {
        signalRender(jp2Image, true, 1);
    }

    protected void signalRender(JP2Image jp2Image, boolean fromReader, float factor) {
        // from reader on EDT, might come after abolish
        if (stopRender == true || jp2Image == null)
            return;

        JP2ImageParameter imageViewParams = calculateParameter(jp2Image, targetMasterTime, targetFrame, fromReader);
        if (imageViewParams == null)
            return;

        // ping reader
        jp2Image.signalReader(imageViewParams);

        queueSubmitTask(new J2KRender(this, imageViewParams));
    }

    @Override
    public LUT getDefaultLUT() {
        int[] builtIn = _jp2Image.getBuiltinLUT();
        if (builtIn != null) {
            return new LUT("built-in", builtIn/* , builtIn */);
        }
        return _jp2Image.getAssociatedLUT();
    }

}
