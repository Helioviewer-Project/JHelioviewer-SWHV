package org.helioviewer.viewmodel.view.jp2view;

import java.awt.EventQueue;
import java.net.URI;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import kdu_jni.Kdu_region_compositor;

import org.helioviewer.base.Region;
import org.helioviewer.base.math.GL3DVec2d;
import org.helioviewer.base.time.ImmutableDateTime;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.filters.lut.LUT;
import org.helioviewer.jhv.opengl.GLInfo;
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
        blockingQueue.add(task);
        executor.submit(task);
    }

    protected Region targetRegion;

    // Member related to JP2
    protected JP2Image _jp2Image;

    private int targetFrame;
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

        MetaData metaData = _jp2Image.metaDataList[0];
        targetRegion = new Region(metaData.getPhysicalLowerLeft(), metaData.getPhysicalSize());
        targetFrame = 0;

        metaDataArray = _jp2Image.metaDataList;

        _jp2Image.startReader(this);

        int numOfThread = 1;
        executor = new ThreadPoolExecutor(numOfThread, numOfThread, 10000L, TimeUnit.MILLISECONDS, blockingQueue, new JHVThread.NamedThreadFactory("Render " + _jp2Image.getName(0)), new ThreadPoolExecutor.DiscardPolicy()/*rejectedExecutionHandler*/);
        render(); // for proper ROI computation (ViewROI)

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

    private class AbolishThread extends Thread {
        private JP2View view;

        public Runnable init(JP2View view) {
            this.view = view;
            return this;
        }

        @Override
        public void run() {
            JHVThread.BagThread t = (JHVThread.BagThread) Thread.currentThread();
            Kdu_region_compositor compositor = (Kdu_region_compositor) t.getVar();
            if (compositor != null) {
                try {
                    J2KRender.destroyCompositor(compositor);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                t.setVar(null);
            }

//            if (J2KRender.compositor != null) {
//                J2KRender.compositor.destroy();
//            }
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

    /**
     * Recalculates the image parameters used within the jp2-package
     *
     * @param v
     *            Viewport the image will be displayed in
     * @param r
     *            Physical region
     * @param frameNumber
     *            Frame number to show (has to be 0 for single images)
     * @return Set of parameters used within the jp2-package
     */
    protected JP2ImageParameter calculateParameter(JP2Image jp2Image, Region r, int frameNumber) {
        MetaData m = jp2Image.metaDataList[frameNumber];
        double mWidth = m.getPhysicalSize().x;
        double mHeight = m.getPhysicalSize().y;
        double rWidth = r.getWidth();
        double rHeight = r.getHeight();

        double ratio = (Displayer.getViewport().getCamera().getCameraWidth() * 2) / (Displayer.getViewport().getHeight() * GLInfo.pixelScale[1]);
        int totalHeight = (int) (mHeight / ratio);

        ResolutionLevel res = jp2Image.getResolutionSet().getClosestResolutionLevel(totalHeight, totalHeight);

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

        // clamp for esajpip
        imageWidth = Math.max(0, Math.min(viewportImageWidth, imageWidth));
        imageHeight = Math.max(0, Math.min(viewportImageHeight, imageHeight));
        imagePositionX = Math.max(0, Math.min(viewportImageWidth - 1, imagePositionX));
        imagePositionY = Math.max(0, Math.min(viewportImageHeight - 1, imagePositionY));

        imageWidth = Math.min(viewportImageHeight - imagePositionX, imageWidth);
        imageHeight = Math.min(viewportImageHeight - imagePositionY, imageHeight);

        SubImage subImage = new SubImage(imagePositionX, imagePositionY, imageWidth, imageHeight);

        return new JP2ImageParameter(jp2Image, subImage, res, frameNumber);
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

        if (metaData instanceof HelioviewerMetaData) {
            newImageData.setRegion(((HelioviewerMetaData) metaData).roiToRegion(params.subImage, params.resolution.getZoomPercent()));
        }

        if (frame != trueFrame) {
            trueFrame = frame;
            ++frameCount;
        }

        imageData = newImageData; // tbd
        if (dataHandler != null) {
            dataHandler.handleData(this, newImageData);
        }
    }

    @Override
    public CacheStatus getImageCacheStatus(int frame) {
        return _jp2Image.getImageCacheStatus().getImageStatus(frame);
    }

    @Override
    public float getActualFramerate() {
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
    public void setFrame(int frame) {
        if (frame != targetFrame && frame >= 0 && frame <= _jp2Image.getMaximumFrameNumber()) {
            CacheStatus status = _jp2Image.getImageCacheStatus().getImageStatus(frame);
            if (status != CacheStatus.PARTIAL && status != CacheStatus.COMPLETE)
                return;

            targetFrame = frame;

            if (_jp2Image.getReaderMode() != ReaderMode.ONLYFIREONCOMPLETE) {
                signalRender(_jp2Image);
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
        targetRegion = ViewROI.getSingletonInstance().updateROI(metaDataArray[targetFrame]);
        signalRender(_jp2Image);
    }

    void signalRender(JP2Image jp2Image) {
        // from reader on EDT, might come after abolish
        if (stopRender == true || jp2Image == null)
            return;

        JP2ImageParameter imageViewParams = calculateParameter(jp2Image, targetRegion, targetFrame);

        // ping reader
        _jp2Image.signalReader(imageViewParams);

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
