package org.helioviewer.viewmodel.view.jp2view;

import kdu_jni.KduException;
import kdu_jni.Kdu_compositor_buf;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_region_compositor;

import org.helioviewer.base.logging.Log;
import org.helioviewer.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.viewmodel.view.MovieView;
import org.helioviewer.viewmodel.view.MovieView.AnimationMode;
import org.helioviewer.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.viewmodel.view.jp2view.image.SubImage;
import org.helioviewer.viewmodel.view.jp2view.kakadu.KakaduConstants;
import org.helioviewer.viewmodel.view.jp2view.kakadu.KakaduUtils;

/**
 * The J2KRender class handles all of the decompression, buffering, and
 * filtering of the image data. It essentially just waits for the shared object
 * in the JP2ImageView to signal it.
 *
 * @author caplins
 * @author Benjamin Wamsler
 * @author Desmond Amadigwe
 * @author Markus Langenberg
 */
class J2KRender implements Runnable {

    /**
     * There could be multiple reason that the Render object was signaled. This
     * enum lists them.
     */
    public enum RenderReasons {
        NEW_DATA, OTHER, MOVIE_PLAY
    };

    /** The thread that this object runs on. */
    private volatile Thread myThread;

    /** A boolean flag used for stopping the thread. */
    private volatile boolean stop;

    /** A reference to the JP2Image this object is owned by. */
    private final JP2Image parentImageRef;

    /** A reference to the JP2ImageView this object is owned by. */
    private final JHVJP2View parentViewRef;

    /** A reference to the compositor used by this JP2Image. */
    private final Kdu_region_compositor compositorRef;

    /** Used in run method to keep track of the current ImageViewParams */
    private JP2ImageParameter currParams = null;

    private int lastFrame = -1;

    private final static int NUM_BUFFERS = 1;

    /** An integer buffer used in the run method. */
    private int[] localIntBuffer = new int[0];
    private int[][] intBuffer = new int[NUM_BUFFERS][0];
    private int currentIntBuffer = 0;

    /** A byte buffer used in the run method. */
    private byte[][] byteBuffer = new byte[NUM_BUFFERS][0];
    private int currentByteBuffer = 0;

    /** Maximum of samples to process per rendering iteration */
    private final int MAX_RENDER_SAMPLES = 50000;

    /** It says if the render is going to play a movie instead of a single image */
    private boolean movieMode = false;

    private int movieSpeed = 20;
    private float actualMovieFramerate = 0.0f;
    private long lastSleepTime = 0;
    private int lastCompositionLayerRendered = -1;

    private NextFrameCandidateChooser nextFrameCandidateChooser;
    private FrameChooser frameChooser;

    /**
     * The constructor.
     *
     * @param _parentViewRef
     */
    J2KRender(JHVJP2View _parentViewRef) {
        parentViewRef = _parentViewRef;
        parentImageRef = parentViewRef.jp2Image;
        compositorRef = parentImageRef.getCompositorRef();

        nextFrameCandidateChooser = new NextFrameCandidateLoopChooser();
        frameChooser = new RelativeFrameChooser(1000 / 20);

        stop = false;
        myThread = null;
    }

    /** Starts the J2KRender thread. */
    void start() {
        myThread = new Thread(JHVJP2View.renderGroup, this, "J2KRender");
        stop = false;
        myThread.start();
    }

    /**
     * Stops the J2KRender thread.
     *
     * @param jp2Image
     */
    void abolish(JP2Image jp2Image) {
        stop = true;
        myThread.interrupt();
        try {
            myThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        myThread = null;

        localIntBuffer = null;
        intBuffer = null;
        byteBuffer = null;

        jp2Image.abolish();
    }

    public void setMovieMode(boolean val) {
        if (movieMode) {
            myThread.interrupt();
            System.gc();
        }

        movieMode = val;
        if (frameChooser instanceof AbsoluteFrameChooser) {
            ((AbsoluteFrameChooser) frameChooser).resetStartTime(currParams.compositionLayer);
        }
    }

    public void setMovieRelativeSpeed(int framesPerSecond /* > 0 */) {
        if (movieMode && lastSleepTime > 1000) {
            myThread.interrupt();
        }

        frameChooser = new RelativeFrameChooser(1000 / framesPerSecond);
    }

    public void setMovieAbsoluteSpeed(int secondsPerSecond /* > 0 */) {
        if (movieMode && lastSleepTime > 1000) {
            myThread.interrupt();
        }


        long[] obsMillis = new long[parentImageRef.getMaximumFrameNumber() + 1];
        for (int i = 0; i <= parentImageRef.getMaximumFrameNumber(); ++i) {
            obsMillis[i] = parentImageRef.metaDataList[i].getDateObs().getMillis() / secondsPerSecond;
        }

        frameChooser = new AbsoluteFrameChooser(obsMillis);
    }

    public void setAnimationMode(AnimationMode mode) {
        switch (mode) {
        case LOOP:
            nextFrameCandidateChooser = new NextFrameCandidateLoopChooser();
            break;
        case STOP:
            nextFrameCandidateChooser = new NextFrameCandidateStopChooser();
            break;
        case SWING:
            nextFrameCandidateChooser = new NextFrameCandidateSwingChooser();
            break;
        }
    }

    public float getActualMovieFramerate() {
        return actualMovieFramerate;
    }

    public boolean isMovieMode() {
        return movieMode;
    }

    private static final Object renderLock = new Object();

    private void renderLayer(int numLayer) {
        synchronized (renderLock) {
            parentImageRef.getLock().lock();

            try {

                // see TODO below
                // compositorRef.Refresh();
                // compositorRef.Remove_compositing_layer(-1, true);

                // not needed: the raw component is extracted from codestream
                // parentImageRef.deactivateColorLookupTable(numLayer);

                // TODO: figure out for getNumComponents() > 2
                // Kdu_dims dimsRef1 = new Kdu_dims(), dimsRef2 = new Kdu_dims();
                // compositorRef.Add_compositing_layer(numLayer, dimsRef1,
                // dimsRef2);

                if (lastCompositionLayerRendered != numLayer) {
                    lastCompositionLayerRendered = numLayer;
                    parentImageRef.updateResolutionSet(numLayer);
                }

                compositorRef.Set_surface_initialization_mode(false);
                compositorRef.Set_max_quality_layers(currParams.qualityLayers);
                compositorRef.Set_scale(false, false, false, currParams.resolution.getZoomPercent());
                if (parentImageRef.getNumComponents() <= 2) {
                    compositorRef.Set_single_component(numLayer, 0, KakaduConstants.KDU_WANT_CODESTREAM_COMPONENTS);
                }

                SubImage roi = currParams.subImage;
                Kdu_dims requestedBufferedRegion = KakaduUtils.roiToKdu_dims(roi);
                compositorRef.Set_buffer_surface(requestedBufferedRegion, 0);

                Kdu_dims actualBufferedRegion = new Kdu_dims();
                Kdu_compositor_buf compositorBuf = compositorRef.Get_composition_buffer(actualBufferedRegion);

                Kdu_coords actualOffset = new Kdu_coords();
                actualOffset.Assign(actualBufferedRegion.Access_pos());

                Kdu_dims newRegion = new Kdu_dims();

                if (parentImageRef.getNumComponents() < 2) {
                    currentByteBuffer = (currentByteBuffer + 1) % NUM_BUFFERS;
                    byteBuffer[currentByteBuffer] = new byte[roi.getNumPixels()];
                } else {
                    currentIntBuffer = (currentIntBuffer + 1) % NUM_BUFFERS;
                    intBuffer[currentIntBuffer] = new int[roi.getNumPixels()];
                }

                while (!compositorRef.Is_processing_complete()) {
                    compositorRef.Process(MAX_RENDER_SAMPLES, newRegion);
                    Kdu_coords newOffset = newRegion.Access_pos();
                    Kdu_coords newSize = newRegion.Access_size();

                    newOffset.Subtract(actualOffset);

                    int newWidth = newSize.Get_x();
                    int newHeight = newSize.Get_y();
                    int newPixels = newWidth * newHeight;

                    if (newPixels == 0) {
                        continue;
                    }

                    localIntBuffer = newPixels > localIntBuffer.length ? new int[newPixels << 1] : localIntBuffer;
                    compositorBuf.Get_region(newRegion, localIntBuffer);

                    int srcIdx = 0;
                    int destIdx = newOffset.Get_x() + newOffset.Get_y() * roi.width;

                    if (parentImageRef.getNumComponents() < 2) {
                        for (int row = 0; row < newHeight; row++, destIdx += roi.width, srcIdx += newWidth) {
                            for (int col = 0; col < newWidth; ++col) {
                                byteBuffer[currentByteBuffer][destIdx + col] = (byte) (localIntBuffer[srcIdx + col] & 0xFF);
                            }
                        }
                    } else {
                        for (int row = 0; row < newHeight; row++, destIdx += roi.width, srcIdx += newWidth) {
                            System.arraycopy(localIntBuffer, srcIdx, intBuffer[currentIntBuffer], destIdx, newWidth);
                        }
                    }
                }

                if (parentImageRef.getNumComponents() == 2) {
                    // extract alpha component
                    compositorRef.Set_single_component(numLayer, 1, KakaduConstants.KDU_WANT_CODESTREAM_COMPONENTS);
                    while (!compositorRef.Is_processing_complete()) {
                        compositorRef.Process(MAX_RENDER_SAMPLES, newRegion);
                        Kdu_coords newOffset = newRegion.Access_pos();
                        Kdu_coords newSize = newRegion.Access_size();

                        newOffset.Subtract(actualOffset);

                        int newWidth = newSize.Get_x();
                        int newHeight = newSize.Get_y();
                        int newPixels = newWidth * newHeight;

                        if (newPixels == 0) {
                            continue;
                        }

                        localIntBuffer = newPixels > localIntBuffer.length ? new int[newPixels << 1] : localIntBuffer;
                        compositorBuf.Get_region(newRegion, localIntBuffer);

                        int srcIdx = 0;
                        int destIdx = newOffset.Get_x() + newOffset.Get_y() * roi.width;

                        for (int row = 0; row < newHeight; row++, destIdx += roi.width, srcIdx += newWidth) {
                            for (int col = 0; col < newWidth; ++col) {
                                // long unsignedValue =
                                // (intBuffer[currentByteBuffer][destIdx + col] &
                                // 0xffffffffl) >> 24;
                                intBuffer[currentByteBuffer][destIdx + col] = (intBuffer[currentByteBuffer][destIdx + col] & 0x00FFFFFF) | ((localIntBuffer[srcIdx + col] & 0x00FF0000) << 8);
                            }
                        }
                    }
                }

                if (compositorBuf != null) {
                    compositorBuf.Native_destroy();
                }

            } catch (KduException e) {
                e.printStackTrace();
            } finally {
                parentImageRef.getLock().unlock();
            }
        }
    }

    // The method that decompresses and renders the image
    @Override
    public void run() {
        int numFrames = 0;
        lastFrame = -1;
        long tfrm, tmax = 0;
        long tnow, tini = System.currentTimeMillis();

        while (!stop) {
            try {
                parentViewRef.renderRequestedSignal.waitForSignal();
            } catch (InterruptedException ex) {
                continue;
            }

            currParams = parentViewRef.getImageViewParams();

            while (!Thread.interrupted() && !stop) {
                tfrm = System.currentTimeMillis();
                int curLayer = currParams.compositionLayer;

                if (parentViewRef instanceof MovieView) {
                    MovieView parent = (MovieView) parentViewRef;
                    if (parent.getMaximumAccessibleFrameNumber() < curLayer) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                        }
                        parentViewRef.renderRequestedSignal.signal(RenderReasons.NEW_DATA);
                        break;
                    }
                }

                renderLayer(curLayer);

                SubImage roi = currParams.subImage;
                int width = roi.width;
                int height = roi.height;

                if (parentImageRef.getNumComponents() < 2) {
                    if (roi.getNumPixels() == byteBuffer[currentByteBuffer].length) {
                        SingleChannelByte8ImageData imdata = new SingleChannelByte8ImageData(width, height, byteBuffer[currentByteBuffer]);
                        parentViewRef.setSubimageData(imdata, roi, curLayer, currParams.resolution.getZoomPercent(), false);
                    } else {
                        Log.warn("J2KRender: Params out of sync, skip frame");
                    }
                } else {
                    if (roi.getNumPixels() == intBuffer[currentIntBuffer].length) {
                        boolean singleChannel = false;
                        if (parentImageRef.getNumComponents() == 2) {
                            singleChannel = true;
                        }

                        ARGBInt32ImageData imdata = new ARGBInt32ImageData(singleChannel, width, height, intBuffer[currentIntBuffer]);
                        parentViewRef.setSubimageData(imdata, roi, curLayer, currParams.resolution.getZoomPercent(), false);
                    } else {
                        Log.warn("J2KRender: Params out of sync, skip frame");
                    }
                }

                if (!movieMode) {
                    break;
                } else {
                    currParams = parentViewRef.getImageViewParams();
                    numFrames += currParams.compositionLayer - lastFrame;
                    lastFrame = currParams.compositionLayer;
                    tmax = frameChooser.moveToNextFrame();
                    if (lastFrame > currParams.compositionLayer) {
                        lastFrame = -1;
                    }
                    tnow = System.currentTimeMillis();

                    if ((tnow - tini) >= 1000) {
                        actualMovieFramerate = (numFrames * 1000.0f) / (tnow - tini);
                        tini = tnow;
                        numFrames = 0;
                    }

                    lastSleepTime = tmax - (tnow - tfrm);
                    if (lastSleepTime > 0) {
                        try {
                            Thread.sleep(lastSleepTime);
                        } catch (InterruptedException ex) {
                            break;
                        }
                    } else {
                        Thread.yield();
                    }
                }
            }

            numFrames += currParams.compositionLayer - lastFrame;
            lastFrame = currParams.compositionLayer;
            if (lastFrame > currParams.compositionLayer) {
                lastFrame = -1;
            }
            tnow = System.currentTimeMillis();

            if ((tnow - tini) >= 1000) {
                actualMovieFramerate = (numFrames * 1000.0f) / (tnow - tini);
                tini = tnow;
                numFrames = 0;
            }
        }
    }

    private abstract class NextFrameCandidateChooser {

        protected int maximumFrameNumber;

        public NextFrameCandidateChooser() {
            maximumFrameNumber = parentImageRef.getMaximumFrameNumber();
        }

        protected void resetStartTime(int frameNumber) {
            if (frameChooser instanceof AbsoluteFrameChooser) {
                ((AbsoluteFrameChooser) frameChooser).resetStartTime(frameNumber);
            }
        }

        public abstract int getNextCandidate(int lastCandidate);
    }

    private class NextFrameCandidateLoopChooser extends NextFrameCandidateChooser {
        @Override
        public int getNextCandidate(int lastCandidate) {
            if (++lastCandidate > maximumFrameNumber) {
                System.gc();
                resetStartTime(0);
                return 0;
            }
            return lastCandidate;
        }
    }

    private class NextFrameCandidateStopChooser extends NextFrameCandidateChooser {
        @Override
        public int getNextCandidate(int lastCandidate) {
            if (++lastCandidate > maximumFrameNumber) {
                movieMode = false;
                resetStartTime(0);
                return 0;
            }
            return lastCandidate;
        }
    }

    private class NextFrameCandidateSwingChooser extends NextFrameCandidateChooser {

        private int currentDirection = 1;

        @Override
        public int getNextCandidate(int lastCandidate) {
            lastCandidate += currentDirection;
            if (lastCandidate < 0 && currentDirection == -1) {
                currentDirection = 1;
                resetStartTime(0);
                return 1;
            } else if (lastCandidate > maximumFrameNumber && currentDirection == 1) {
                currentDirection = -1;
                resetStartTime(maximumFrameNumber);
                return maximumFrameNumber - 1;
            }

            return lastCandidate;
        }
    }

    private interface FrameChooser {
        public long moveToNextFrame();
    }

    private class RelativeFrameChooser implements FrameChooser {

        private long dt;

        public RelativeFrameChooser(long _dt) {
            dt = _dt;
        }

        @Override
        public long moveToNextFrame() {
            currParams.compositionLayer = nextFrameCandidateChooser.getNextCandidate(currParams.compositionLayer);
            return dt;
        }
    }

    private class AbsoluteFrameChooser implements FrameChooser {

        private long[] obsMillis;
        private long absoluteStartTime;
        private long systemStartTime;

        public AbsoluteFrameChooser(long[] _obsMillis) {
            obsMillis = _obsMillis;
            resetStartTime(currParams.compositionLayer);
        }

        public void resetStartTime(int frameNumber) {
            absoluteStartTime = obsMillis[frameNumber];
            systemStartTime = System.currentTimeMillis();
        }

        @Override
        public long moveToNextFrame() {
            int lastCandidate, nextCandidate = currParams.compositionLayer;
            long lastDiff, nextDiff = -Long.MAX_VALUE;

            do {
                lastCandidate = nextCandidate;
                nextCandidate = nextFrameCandidateChooser.getNextCandidate(nextCandidate);

                lastDiff = nextDiff;
                nextDiff = Math.abs(obsMillis[nextCandidate] - absoluteStartTime) - (System.currentTimeMillis() - systemStartTime);
            } while (nextDiff < 0);

            if (-lastDiff < nextDiff) {
                currParams.compositionLayer = lastCandidate;
                return lastDiff;
            } else {
                currParams.compositionLayer = nextCandidate;
                return nextDiff;
            }
        }
    }

}
