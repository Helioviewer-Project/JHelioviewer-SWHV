package org.helioviewer.viewmodel.view.jp2view;

import java.awt.EventQueue;
import java.awt.Rectangle;
import java.net.URI;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.base.Region;
import org.helioviewer.base.math.GL3DVec2d;
import org.helioviewer.base.time.ImmutableDateTime;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.filters.lut.LUT;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.threads.JHVThread;
import org.helioviewer.viewmodel.imagecache.ImageCacheStatus.CacheStatus;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.ViewROI;
import org.helioviewer.viewmodel.view.jp2view.JP2Image.ReaderMode;
import org.helioviewer.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;
import org.helioviewer.viewmodel.view.jp2view.image.SubImage;

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
    private ExecutorService executor;

    private void queueSubmitTask(Runnable task) {
        blockingQueue.poll();
        executor.submit(task);
    }

    // Member related to JP2
    protected JP2Image _jp2Image;

    private boolean hiResImage = false;
    private static final int hiDpiCutoff = 1024;

    private Date targetMasterTime;

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
        targetMasterTime = metaDataArray[0].getDateObs().getDate();

        _jp2Image.startReader(this);

        Rectangle fullFrame = _jp2Image.getResolutionSet().getResolutionLevel(0).getResolutionBounds();
        if (JHVGlobals.GoForTheBroke && fullFrame.width * fullFrame.height > hiDpiCutoff * hiDpiCutoff)
            hiResImage = true;

        int numOfThread = 1;
        executor = new ThreadPoolExecutor(numOfThread, numOfThread, 10000L, TimeUnit.MILLISECONDS, blockingQueue, new JHVThread.NamedThreadFactory("Render " + _jp2Image.getName(0)), new ThreadPoolExecutor.DiscardPolicy()/* rejectedExecutionHandler */);
        frameCountStart = System.currentTimeMillis();

        render();
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

    private class AbolishThread extends Thread {
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
                    executor.shutdown();
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
        executor.submit(thread);
    }

    // if instance was built before cancelling
    @Override
    public void finalize() {
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
    protected JP2ImageParameter calculateParameter(JP2Image jp2Image, Date masterTime, int frameNumber) {
        GL3DCamera camera = Displayer.getViewport().getCamera();
        MetaData m = jp2Image.metaDataList[frameNumber];
        Region r = ViewROI.updateROI(camera, masterTime, m);

        double mWidth = m.getPhysicalSize().x;
        double mHeight = m.getPhysicalSize().y;
        double rWidth = r.getWidth();
        double rHeight = r.getHeight();

        double ratio = 2 * camera.getCameraWidth() / Displayer.getViewport().getHeight();
        int totalHeight = (int) (mHeight / ratio);

        ResolutionLevel res;
        if (hiResImage && totalHeight > hiDpiCutoff && Layers.isMoviePlaying())
            res = jp2Image.getResolutionSet().getPreviousResolutionLevel(totalHeight, totalHeight);
        else
            res = jp2Image.getResolutionSet().getNextResolutionLevel(totalHeight, totalHeight);

        int viewportImageWidth = res.getResolutionBounds().width;
        int viewportImageHeight = res.getResolutionBounds().height;

        double currentMeterPerPixel = mWidth / viewportImageWidth;
        int imageWidth = (int) Math.round(rWidth / currentMeterPerPixel);
        int imageHeight = (int) Math.round(rHeight / currentMeterPerPixel);

        GL3DVec2d rUpperLeft = r.getUpperLeftCorner();
        GL3DVec2d mUpperLeft = m.getPhysicalUpperLeft();
        double displacementX = rUpperLeft.x - mUpperLeft.x;
        double displacementY = rUpperLeft.y - mUpperLeft.y;

        int imagePositionX = (int) Math.round(displacementX / mWidth * viewportImageWidth);
        int imagePositionY = -(int) Math.round(displacementY / mHeight * viewportImageHeight);

        SubImage subImage = new SubImage(imagePositionX, imagePositionY, imageWidth, imageHeight, res.getResolutionBounds());
        JP2ImageParameter newImageViewParams = new JP2ImageParameter(jp2Image, masterTime, subImage, res, frameNumber);

        if (jp2Image.getImageCacheStatus().getImageStatus(frameNumber) == CacheStatus.COMPLETE && newImageViewParams.equals(oldImageViewParams)) {
            Displayer.display();
            return null;
        }
        oldImageViewParams = newImageViewParams;

        return newImageViewParams;
    }

    /*
     * NOTE: The following section is for communications with the two threads,
     * J2KReader and J2KRender. Thus, the visibility is set to "default" (also
     * known as "package"). These functions should not be used by any other
     * class.
     */

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
            frameRate = 1000.f * frameCount / delta;
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
    public void setFrame(int frame, Date masterTime) {
        if (frame != targetFrame && frame >= 0 && frame <= _jp2Image.getMaximumFrameNumber()) {
            CacheStatus status = _jp2Image.getImageCacheStatus().getImageStatus(frame);
            if (status != CacheStatus.PARTIAL && status != CacheStatus.COMPLETE)
                return;

            targetFrame = frame;
            targetMasterTime = masterTime;

            if (_jp2Image.getReaderMode() != ReaderMode.ONLYFIREONCOMPLETE) {
                render();
            }
        }
    }

    // to be accessed only from Layers
    @Override
    public int getFrame(ImmutableDateTime time) {
        int frame = -1;
        long timeMillis = time.getMillis();
        long lastDiff, currentDiff = -Long.MAX_VALUE;
        do {
            lastDiff = currentDiff;
            currentDiff = metaDataArray[++frame].getDateObs().getMillis() - timeMillis;
        } while (currentDiff < 0 && frame < _jp2Image.getMaximumFrameNumber());

        if (-lastDiff < currentDiff) {
            return frame - 1;
        } else {
            return frame;
        }
    }

    @Override
    public void render() {
        signalRender(_jp2Image);
    }

    void signalRenderFromReader(JP2Image jp2Image) {
        signalRender(jp2Image);
    }

    void signalRender(JP2Image jp2Image) {
        // from reader on EDT, might come after abolish
        if (stopRender == true || jp2Image == null)
            return;

        JP2ImageParameter imageViewParams = calculateParameter(jp2Image, targetMasterTime, targetFrame);
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
