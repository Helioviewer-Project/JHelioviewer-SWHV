package org.helioviewer.viewmodel.view.jp2view;

import java.awt.EventQueue;
import java.net.URI;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.base.Region;
import org.helioviewer.base.math.GL3DVec2d;
import org.helioviewer.base.time.ImmutableDateTime;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.RenderListener;
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
import org.helioviewer.viewmodel.view.jp2view.kakadu.KakaduConstants;

/**
 * Implementation of View for JPG2000 images.
 * <p>
 * This class represents the gateway to the heart of the helioviewer project. It
 * is responsible for reading and decoding JPG2000 images. Therefore, it manages
 * two threads: one thread for communicating with the JPIP server, the other one
 * for decoding the images.
 *
 */
public class JHVJP2View extends AbstractView implements RenderListener {

    static private class RejectExecution implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            // System.out.println(Thread.currentThread().getName());
        }
    }

    private final BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<Runnable>(1);
    private final RejectedExecutionHandler rejectedExecutionHandler = new RejectExecution(); // new ThreadPoolExecutor.CallerRunsPolicy();
    private final int numOfThread = 1;
    private final ExecutorService exec = new ThreadPoolExecutor(numOfThread, numOfThread, 10000L, TimeUnit.MILLISECONDS, blockingQueue, new JHVThread.NamedThreadFactory("Render"), rejectedExecutionHandler);

    protected Region targetRegion;

    // Member related to JP2
    protected JP2Image _jp2Image;

    private int targetFrame;
    private int trueFrame;

    private int frameCount = 0;
    private long frameCountStart;
    private float frameRate;

    protected final int[] localIntBuffer = new int[KakaduConstants.MAX_RENDER_SAMPLES];

    private boolean stopRender = false;

    public JHVJP2View() {

        Displayer.addRenderListener(this);
        frameCountStart = System.currentTimeMillis();
    }

    /**
     * Sets the JPG2000 image used by this class.
     *
     * This functions sets up the whole infrastructure needed for using the
     * image, including the two threads.
     *
     * <p>
     * Thus, this functions also works as a constructor.
     *
     * @param newJP2Image
     */
    public void setJP2Image(JP2Image newJP2Image) throws Exception {
        if (_jp2Image != null) {
            throw new Exception("JP2 image already set");
        }

        _jp2Image = newJP2Image;
        _jp2Image.addReference();

        MetaData metaData = _jp2Image.metaDataList[0];
        targetRegion = new Region(metaData.getPhysicalLowerLeft(), metaData.getPhysicalSize());

        metaDataArray = _jp2Image.metaDataList;

        _jp2Image.startReader(this, calculateParameter(_jp2Image, targetRegion, 0));
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
        private JHVJP2View view;

        public Runnable init(JHVJP2View view) {
            this.view = view;
            return this;
        }

        @Override
        public void run() {
            JHVThread.BagThread t = (JHVThread.BagThread) Thread.currentThread();
            J2KRender.JHV_Kdu_compositor compositorObj = (J2KRender.JHV_Kdu_compositor) t.getVar();

            if (compositorObj != null) {
                try {
                    compositorObj.destroy();
                } catch (Exception e) {
                }
                t.setVar(null);
            }


            EventQueue.invokeLater(new Runnable() {
                private JHVJP2View view;

                @Override
                public void run() {
                    view.abolishExternal();
                }

                public Runnable init(JHVJP2View view) {
                    this.view = view;
                    return this;
                }
            }.init(this.view));
        }
    }

    // Destroy the resources associated with this object
    @Override
    public void abolish() {
        AbolishThread thread = new AbolishThread();
        stopRender = true;
        thread.init(this);
        exec.submit(thread);
    }

    public void abolishExternal() {
        Displayer.removeRenderListener(this);
        _jp2Image.abolish();
        _jp2Image = null;
    }

    /**
     * Recalculates the image parameters.
     *
     * This function maps between the set of parameters used within the view
     * chain and the set of parameters used within the jp2-package.
     *
     * <p>
     * To achieve this, calculates the set of parameters used within the
     * jp2-package according to the given requirements from the view chain.
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
     * <p>
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

    /**
     * Before actually setting the new frame number, checks whether that is
     * necessary. If the frame number has changed, also triggers an update of
     * the image.
     *
     * @param frame
     */
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
        _jp2Image.readerSignal.signal(imageViewParams);

        J2KRender task = new J2KRender(this, imageViewParams);
        {
            blockingQueue.poll();
            blockingQueue.add(task);
        }
        exec.submit(task, Boolean.TRUE);
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
