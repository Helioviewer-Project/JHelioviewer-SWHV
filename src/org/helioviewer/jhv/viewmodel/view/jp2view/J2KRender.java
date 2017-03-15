package org.helioviewer.jhv.viewmodel.view.jp2view;

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
import org.helioviewer.jhv.viewmodel.imagedata.SubImage;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ImageParams;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduConstants;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduEngine;

class J2KRender implements Runnable {

    private static final int MAX_INACTIVE_LAYERS = 100;

    private static final ThreadLocal<int[]> bufferLocal = ThreadLocal.withInitial(() -> new int[KakaduConstants.MAX_RENDER_SAMPLES]);
    private static final ThreadLocal<Kdu_thread_env> threadLocal = ThreadLocal.withInitial(J2KRender::createThreadEnv);
    private static final ThreadLocal<KakaduEngine> engineLocal = new ThreadLocal<>();

    private static final int[] firstComponent = { 0 };

    // A reference to the JP2View this object is owned by
    private final JP2View viewRef;

    private final ImageParams params;

    private final boolean discard;

    J2KRender(JP2View _viewRef, ImageParams _currParams, boolean _discard) {
        viewRef = _viewRef;
        params = _currParams;
        discard = _discard;
    }

    private void renderLayer(Kdu_region_compositor compositor) throws KduException {
        if (discard)
            compositor.Refresh();
        else
            compositor.Cull_inactive_ilayers(MAX_INACTIVE_LAYERS);

        SubImage subImage = params.subImage;
        int frame = params.frame;
        int numComponents = viewRef.getNumComponents(frame);

        Kdu_ilayer_ref ilayer;
        Kdu_dims dimsRef1 = new Kdu_dims(), dimsRef2 = new Kdu_dims();

        if (numComponents < 3) {
            // alpha tbd
            ilayer = compositor.Add_primitive_ilayer(frame, firstComponent, KakaduConstants.KDU_WANT_CODESTREAM_COMPONENTS, dimsRef1, dimsRef2);
        } else {
            ilayer = compositor.Add_ilayer(frame, dimsRef1, dimsRef2);
        }

        compositor.Set_scale(false, false, false, 1f / (1 << params.resolution.level), (float) params.factor);
        Kdu_dims requestedRegion = jhvToKdu_dims(subImage.x, subImage.y, subImage.width, subImage.height);
        compositor.Set_buffer_surface(requestedRegion);

        Kdu_compositor_buf compositorBuf = compositor.Get_composition_buffer(new Kdu_dims());
        Kdu_dims actualRenderedRegion = compositorBuf.Get_rendering_region();

        // avoid gc
        Kdu_coords actualPos = actualRenderedRegion.Access_pos();
        Kdu_coords actualOffset = new Kdu_coords(actualPos.Get_x(), actualPos.Get_y());
        Kdu_coords actualSize = actualRenderedRegion.Access_size();
        int aWidth = actualSize.Get_x(), aHeight = actualSize.Get_y();

        Kdu_dims newRegion = new Kdu_dims();

        int[] intBuffer = null;
        byte[] byteBuffer = null;

        if (numComponents < 3) {
            byteBuffer = new byte[aWidth * aHeight];
        } else {
            intBuffer = new int[aWidth * aHeight];
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
            int dstIdx = newOffset.Get_x() + newOffset.Get_y() * aWidth;

            if (numComponents < 3) {
                for (int row = 0; row < newHeight; row++, dstIdx += aWidth, srcIdx += newWidth) {
                    for (int col = 0; col < newWidth; ++col) {
                        byteBuffer[dstIdx + col] = (byte) (localIntBuffer[srcIdx + col] & 0xFF);
                    }
                }
            } else {
                for (int row = 0; row < newHeight; row++, dstIdx += aWidth, srcIdx += newWidth) {
                    System.arraycopy(localIntBuffer, srcIdx, intBuffer, dstIdx, newWidth);
                }
            }
        }

        compositorBuf.Native_destroy();
        compositor.Remove_ilayer(ilayer, discard);

        ImageData data;
        if (numComponents < 3) {
            data = new SingleChannelByte8ImageData(aWidth, aHeight, ByteBuffer.wrap(byteBuffer));
        } else {
            data = new ARGBInt32ImageData(false, aWidth, aHeight, IntBuffer.wrap(intBuffer));
        }
        viewRef.setDataFromRender(params, data);
    }

    @Override
    public void run() {
        try {
            KakaduEngine kduEngine = engineLocal.get();
            if (kduEngine == null) {
                kduEngine = viewRef.getRenderEngine(threadLocal.get());
                engineLocal.set(kduEngine);
            }
            renderLayer(kduEngine.getCompositor());
        } catch (Exception e) {
            // reboot the compositor
            engineLocal.set(null);
            threadLocal.remove();
            e.printStackTrace();
        }
    }

    private static Kdu_dims jhvToKdu_dims(int x, int y, int width, int height) throws KduException {
        Kdu_dims dims = new Kdu_dims();
        Kdu_coords pos = dims.Access_pos();
        pos.Set_x(x);
        pos.Set_y(y);
        Kdu_coords siz = dims.Access_size();
        siz.Set_x(width);
        siz.Set_y(height);

        return dims;
    }

    private static Kdu_thread_env createThreadEnv() {
        try {
            Kdu_thread_env theThreadEnv = new Kdu_thread_env();
            theThreadEnv.Create();
            int numThreads = Kdu_global.Kdu_get_num_processors();
            for (int i = 1; i < numThreads; i++)
                theThreadEnv.Add_thread();
            // System.out.println(">>>> Kdu_thread_env create " + theThreadEnv);
            return theThreadEnv;
        } catch (KduException e) {
            e.printStackTrace();
        }
        return null;
    }

}
