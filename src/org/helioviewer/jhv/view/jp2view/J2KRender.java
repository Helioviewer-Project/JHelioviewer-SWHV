package org.helioviewer.jhv.view.jp2view;

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

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.imagedata.ARGBInt32ImageData;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.Single8ImageData;
import org.helioviewer.jhv.imagedata.SubImage;
import org.helioviewer.jhv.view.jp2view.image.ImageParams;
import org.helioviewer.jhv.view.jp2view.kakadu.KakaduConstants;
import org.helioviewer.jhv.view.jp2view.kakadu.KakaduSource;

class J2KRender implements Runnable {

    private static final int MAX_INACTIVE_LAYERS = 200;

    private static final ThreadLocal<int[]> localArray = ThreadLocal.withInitial(() -> new int[KakaduConstants.MAX_RENDER_SAMPLES]);
    private static final ThreadLocal<Kdu_thread_env> localThread = ThreadLocal.withInitial(J2KRender::createThreadEnv);
    private static final ThreadLocal<KakaduSource> localSource = new ThreadLocal<>();

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
            ilayer = compositor.Add_primitive_ilayer(frame, firstComponent, Kdu_global.KDU_WANT_CODESTREAM_COMPONENTS, dimsRef1, dimsRef2);
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

        ByteBuffer byteBuffer = null;
        IntBuffer intBuffer = null;

        if (numComponents < 3) {
            byteBuffer = BufferUtils.newByteBuffer(aWidth * aHeight);
        } else {
            intBuffer = BufferUtils.newIntBuffer(aWidth * aHeight);
        }

        int[] intArray = localArray.get();
        Kdu_dims newRegion = new Kdu_dims();
        while (compositor.Process(KakaduConstants.MAX_RENDER_SAMPLES, newRegion)) {
            Kdu_coords newSize = newRegion.Access_size();
            int newWidth = newSize.Get_x();
            int newHeight = newSize.Get_y();
            if (newWidth * newHeight == 0)
                continue;

            Kdu_coords newOffset = newRegion.Access_pos();
            newOffset.Subtract(actualOffset);

            int theHeight = intArray.length / newWidth;

            Kdu_dims theRegion = new Kdu_dims();
            Kdu_coords theSize = theRegion.Access_size();
            theSize.Set_x(newWidth);
            theSize.Set_y(theHeight);
            theRegion.Access_pos().Assign(newOffset);

            while (!(theRegion = theRegion.Intersection(newRegion)).Is_empty()) { // slide with buffer top to bottom
                compositorBuf.Get_region(theRegion, intArray);

                Kdu_coords theOffset = theRegion.Access_pos();
                int yOff = theOffset.Get_y();
                int dstIdx = theOffset.Get_x() + yOff * aWidth;
                int height = theRegion.Access_size().Get_y();

                if (numComponents < 3) {
                    for (int row = 0, srcIdx = 0; row < height; row++, dstIdx += aWidth, srcIdx += newWidth) {
                        for (int col = 0; col < newWidth; ++col) {
                            byteBuffer.put(dstIdx + col, (byte) (intArray[srcIdx + col] & 0xFF));
                        }
                    }
                } else {
                    for (int row = 0, srcIdx = 0; row < height; row++, dstIdx += aWidth, srcIdx += newWidth) {
                        intBuffer.position(dstIdx);
                        intBuffer.put(intArray, srcIdx, newWidth);
                    }
                    intBuffer.rewind();
                }
                theOffset.Set_y(yOff + height);
            }
        }

        compositorBuf.Native_destroy();
        compositor.Remove_ilayer(ilayer, discard);

        ImageData data;
        if (numComponents < 3) {
            data = new Single8ImageData(aWidth, aHeight, byteBuffer);
        } else {
            data = new ARGBInt32ImageData(aWidth, aHeight, intBuffer);
        }
        viewRef.setDataFromRender(params, data);
    }

    @Override
    public void run() {
        try {
            KakaduSource kduSource = localSource.get();
            if (kduSource == null) {
                kduSource = viewRef.getRenderSource(localThread.get());
                localSource.set(kduSource);
            }
            renderLayer(kduSource.getCompositor());
        } catch (Exception e) {
            // reboot the compositor
            localSource.set(null);
            localThread.remove();
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
