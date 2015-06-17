package org.helioviewer.viewmodel.view.jp2view;

import kdu_jni.KduException;
import kdu_jni.Kdu_compositor_buf;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_region_compositor;

import org.helioviewer.base.logging.Log;
import org.helioviewer.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.viewmodel.view.MovieView;
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

    private float actualMovieFramerate = 0.0f;
    private int lastCompositionLayerRendered = -1;

    J2KRender(JHVJP2View _parentViewRef) {
        parentViewRef = _parentViewRef;
        parentImageRef = parentViewRef.jp2Image;
        compositorRef = parentImageRef.getCompositorRef();

        stop = false;
        myThread = null;
    }

    void start() {
        myThread = new Thread(JHVJP2View.renderGroup, this, "J2KRender");
        stop = false;
        myThread.start();
    }

    void abolish() {
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
    }

    public float getActualMovieFramerate() {
        return actualMovieFramerate;
    }

    private static final Object renderLock = new Object();

    private void renderLayer(int numLayer) {
        synchronized (renderLock) {
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
            }
        }
    }

    @Override
    public void run() {
        int numFrames = 0;
        lastFrame = -1;
        long tnow, tini = System.currentTimeMillis();

        while (!stop) {
            try {
                parentViewRef.renderRequestedSignal.waitForSignal();
            } catch (InterruptedException ex) {
                continue;
            }

            currParams = parentViewRef.imageViewParams;
            int curLayer = currParams.compositionLayer;

            if (parentViewRef instanceof MovieView) {
                MovieView parent = (MovieView) parentViewRef;
                if (parent.getMaximumAccessibleFrameNumber() < curLayer) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                    }
                    parentViewRef.renderRequestedSignal.signal(RenderReasons.NEW_DATA);
                    continue;
                }
            }

            renderLayer(curLayer);

            SubImage roi = currParams.subImage;
            int width = roi.width;
            int height = roi.height;
            ImageData imdata = null;

            if (parentImageRef.getNumComponents() < 2) {
                if (roi.getNumPixels() == byteBuffer[currentByteBuffer].length) {
                    imdata = new SingleChannelByte8ImageData(width, height, byteBuffer[currentByteBuffer]);
                }
            } else {
                if (roi.getNumPixels() == intBuffer[currentIntBuffer].length) {
                    boolean singleChannel = false;
                    if (parentImageRef.getNumComponents() == 2) {
                        singleChannel = true;
                    }
                    imdata = new ARGBInt32ImageData(singleChannel, width, height, intBuffer[currentIntBuffer]);
                }
            }

            if (imdata != null) {
                parentViewRef.setSubimageData(imdata, roi, curLayer, currParams.resolution.getZoomPercent());
            } else {
                Log.warn("J2KRender: Params out of sync, skip frame");
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

}
