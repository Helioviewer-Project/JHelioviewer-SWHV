package org.helioviewer.jhv.viewmodel.view.jp2view;

import java.awt.EventQueue;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import kdu_jni.KduException;
import kdu_jni.Kdu_compositor_buf;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_global;
import kdu_jni.Kdu_ilayer_ref;
import kdu_jni.Kdu_region_compositor;
import kdu_jni.Kdu_thread_env;

import org.helioviewer.jhv.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.SubImage;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduConstants;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduUtils;

class J2KRender implements Runnable {

    private static final ThreadLocal<int[]> bufferLocal = new ThreadLocal<int[]>() {
        @Override
        protected int[] initialValue() {
            return new int[KakaduConstants.MAX_RENDER_SAMPLES];
        }
    };

    private static final int[] firstComponent = new int[] { 0 };

    /** A reference to the JP2Image this object is owned by. */
    private final JP2Image parentImageRef;

    /** A reference to the JP2View this object is owned by. */
    private final JP2View parentViewRef;

    private final JP2ImageParameter currParams;

    J2KRender(JP2View _parentViewRef, JP2ImageParameter _currParams) {
        parentViewRef = _parentViewRef;

        currParams = _currParams;

        parentImageRef = currParams.jp2Image;
    }

    private void renderLayer(Kdu_region_compositor compositor) throws KduException {

        int numLayer = currParams.compositionLayer;

        // compositor.Refresh();
        compositor.Remove_ilayer(new Kdu_ilayer_ref(), true);

        Kdu_dims dimsRef1 = new Kdu_dims(), dimsRef2 = new Kdu_dims();
        int numComponents = parentImageRef.getNumComponents();
        // parentImageRef.deactivateColorLookupTable(numLayer);
        if (numComponents < 3) {
            // alpha tbd
            compositor.Add_primitive_ilayer(numLayer, firstComponent, KakaduConstants.KDU_WANT_CODESTREAM_COMPONENTS, dimsRef1, dimsRef2);
        } else {
            compositor.Add_ilayer(numLayer, dimsRef1, dimsRef2);
        }

        parentImageRef.updateResolutionSet(compositor, numLayer);

        compositor.Set_scale(false, false, false, currParams.resolution.getZoomPercent());

        SubImage roi = currParams.subImage;
        Kdu_dims requestedBufferedRegion = KakaduUtils.roiToKdu_dims(roi);
        compositor.Set_buffer_surface(requestedBufferedRegion);

        Kdu_dims actualBufferedRegion = new Kdu_dims();
        Kdu_compositor_buf compositorBuf = compositor.Get_composition_buffer(actualBufferedRegion);

        Kdu_coords actualOffset = new Kdu_coords();
        actualOffset.Assign(actualBufferedRegion.Access_pos());

        Kdu_dims newRegion = new Kdu_dims();

        int[] intBuffer = null;
        byte[] byteBuffer = null;

        if (numComponents < 3) {
            byteBuffer = new byte[roi.getNumPixels()];
        } else {
            intBuffer = new int[roi.getNumPixels()];
        }

        int[] localIntBuffer = bufferLocal.get();
        while (!compositor.Is_processing_complete()) {
            compositor.Process(KakaduConstants.MAX_RENDER_SAMPLES, newRegion);
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

            if (numComponents < 3) {
                for (int row = 0; row < newHeight; row++, destIdx += roi.width, srcIdx += newWidth) {
                    for (int col = 0; col < newWidth; ++col) {
                        byteBuffer[destIdx + col] = (byte) (localIntBuffer[srcIdx + col] & 0xFF);
                    }
                }
            } else {
                for (int row = 0; row < newHeight; row++, destIdx += roi.width, srcIdx += newWidth) {
                    System.arraycopy(localIntBuffer, srcIdx, intBuffer, destIdx, newWidth);
                }
            }
        }

        if (compositorBuf != null) {
            compositorBuf.Native_destroy();
        }

        ImageData imdata = null;
        if (numComponents < 3) {
            imdata = new SingleChannelByte8ImageData(roi.width, roi.height, ByteBuffer.wrap(byteBuffer));
        } else {
            imdata = new ARGBInt32ImageData(false, roi.width, roi.height, IntBuffer.wrap(intBuffer));
        }
        setImageData(imdata, currParams);
    }

    private void setImageData(ImageData newImdata, JP2ImageParameter newParams) {
        EventQueue.invokeLater(new Runnable() {
            private ImageData theImdata;
            private JP2ImageParameter theParams;

            @Override
            public void run() {
                parentViewRef.setSubimageData(theImdata, theParams);
            }

            public Runnable init(ImageData imdata, JP2ImageParameter params) {
                theImdata = imdata;
                theParams = params;
                return this;
            }
        }.init(newImdata, newParams));
    }

    @Override
    public void run() {
        try {
            renderLayer(parentImageRef.getCompositor(threadEnv.get()));
        } catch (Exception e) {
            // reboot the compositor
            try {
                parentImageRef.destroyCompositor();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    static final ThreadEnvLocal threadEnv = new ThreadEnvLocal();

    static class ThreadEnvLocal extends ThreadLocal<Kdu_thread_env> {
        @Override
        protected Kdu_thread_env initialValue() {
            try {
                return createThreadEnv();
            } catch (KduException e) {
                e.printStackTrace();
            }
            return null;
        }

        public void destroy() {
            destroyThreadEnv(get());
            set(null);
        }
    }

    private static Kdu_thread_env createThreadEnv() throws KduException {
        Kdu_thread_env threadEnv = new Kdu_thread_env();
        // System.out.println(">>>> threadEnv create " + threadEnv);
        threadEnv.Create();
        int numThreads = Kdu_global.Kdu_get_num_processors();
        for (int i = 1; i < numThreads; i++)
            threadEnv.Add_thread();
        return threadEnv;
    }

    private static void destroyThreadEnv(Kdu_thread_env threadEnv) {
        if (threadEnv != null) {
            // System.out.println(">>>> threadEnv destroy " + threadEnv);
            threadEnv.Native_destroy();
        }
    }

}
