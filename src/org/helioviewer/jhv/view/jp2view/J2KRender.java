package org.helioviewer.jhv.view.jp2view;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.annotation.Nullable;

import kdu_jni.Jpx_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_compositor_buf;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_global;
import kdu_jni.Kdu_ilayer_ref;
import kdu_jni.Kdu_quality_limiter;
import kdu_jni.Kdu_region_compositor;
import kdu_jni.Kdu_thread_env;

import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.ImageData.ImageFormat;
import org.helioviewer.jhv.imagedata.SubImage;
import org.helioviewer.jhv.view.jp2view.image.ImageParams;
import org.helioviewer.jhv.view.jp2view.kakadu.KakaduConstants;

class J2KRender implements Runnable {

    private static final int MAX_INACTIVE_LAYERS = 200;
    private static final int[] firstComponent = {0};

    private static final ThreadLocal<int[]> localArray = ThreadLocal.withInitial(() -> new int[KakaduConstants.MAX_RENDER_SAMPLES]);
    private static final ThreadLocal<Kdu_thread_env> localThread = ThreadLocal.withInitial(J2KRender::createThreadEnv);
    private static final ThreadLocal<Kdu_region_compositor> localCompositor = new ThreadLocal<>();

    private final JP2View view;
    private final ImageParams params;
    private final boolean discard;
    private final boolean abolish;

    J2KRender(JP2View _view, ImageParams _currParams, boolean _discard, boolean _abolish) {
        view = _view;
        params = _currParams;
        discard = _discard;
        abolish = _abolish;
    }

    private void renderLayer(Kdu_region_compositor compositor) throws KduException {
        if (discard)
            compositor.Refresh();
        else
            compositor.Cull_inactive_ilayers(MAX_INACTIVE_LAYERS);

        SubImage subImage = params.subImage;
        int frame = params.frame;
        int numComponents = view.getNumComponents(frame);

        Kdu_ilayer_ref ilayer;
        Kdu_dims empty = new Kdu_dims();
        if (numComponents < 3) {
            // alpha tbd
            ilayer = compositor.Add_primitive_ilayer(frame, firstComponent, Kdu_global.KDU_WANT_CODESTREAM_COMPONENTS, empty, empty);
        } else {
            ilayer = compositor.Add_ilayer(frame, new Kdu_dims(), new Kdu_dims());
        }

        compositor.Set_scale(false, false, false, 1f / (1 << params.resolution.level), (float) params.factor);

        Kdu_dims requestedRegion = new Kdu_dims();
        requestedRegion.From_u32(subImage.x, subImage.y, subImage.width, subImage.height);
        compositor.Set_buffer_surface(requestedRegion);

        Kdu_compositor_buf compositorBuf = compositor.Get_composition_buffer(empty, true); // modifies empty
        Kdu_dims actualRegion = compositorBuf.Get_rendering_region();
        Kdu_coords actualPos = actualRegion.Access_pos();
        int actualX = actualPos.Get_x(), actualY = actualPos.Get_y();
        Kdu_coords actualSize = actualRegion.Access_size();
        int actualWidth = actualSize.Get_x(), actualHeight = actualSize.Get_y();

        int[] intBuffer = null;
        byte[] byteBuffer = null;

        if (numComponents < 3) {
            byteBuffer = new byte[actualWidth * actualHeight];
        } else {
            intBuffer = new int[actualWidth * actualHeight];
        }

        int[] intArray = localArray.get();
        Kdu_dims newRegion = new Kdu_dims();
        Kdu_dims theRegion = new Kdu_dims();
        while (compositor.Process(KakaduConstants.MAX_RENDER_SAMPLES, newRegion)) {
            Kdu_coords newSize = newRegion.Access_size();
            int newWidth = newSize.Get_x();
            int newHeight = newSize.Get_y();
            if (newWidth * newHeight == 0)
                continue;

            Kdu_coords newOffset = newRegion.Access_pos();
            theRegion.From_u32(newOffset.Get_x() - actualX, newOffset.Get_y() - actualY, newWidth, intArray.length / newWidth);

            while (!(theRegion = theRegion.Intersection(newRegion)).Is_empty()) { // slide with buffer top to bottom
                compositorBuf.Get_region(theRegion, intArray);

                Kdu_coords theOffset = theRegion.Access_pos();
                int yOff = theOffset.Get_y();
                int dstIdx = theOffset.Get_x() + yOff * actualWidth;
                int height = theRegion.Access_size().Get_y();

                if (numComponents < 3) {
                    for (int row = 0, srcIdx = 0; row < height; row++, dstIdx += actualWidth, srcIdx += newWidth) {
                        for (int col = 0; col < newWidth; ++col) {
                            byteBuffer[dstIdx + col] = (byte) (intArray[srcIdx + col] & 0xFF);
                        }
                    }
                } else {
                    for (int row = 0, srcIdx = 0; row < height; row++, dstIdx += actualWidth, srcIdx += newWidth) {
                        System.arraycopy(intArray, srcIdx, intBuffer, dstIdx, newWidth);
                    }
                }
                theOffset.Set_y(yOff + height);
            }
        }

        compositorBuf.Native_destroy();
        compositor.Remove_ilayer(ilayer, discard);

        ImageData data;
        if (numComponents < 3) {
            data = new ImageData(actualWidth, actualHeight, ImageFormat.Gray8, ByteBuffer.wrap(byteBuffer));
        } else {
            data = new ImageData(actualWidth, actualHeight, ImageFormat.ARGB32, IntBuffer.wrap(intBuffer));
        }
        view.setDataFromRender(params, data);
    }

    @Override
    public void run() {
        if (abolish) {
            abolish();
            return;
        }

        Kdu_region_compositor krc = null;
        try {
            krc = localCompositor.get();
            if (krc == null) {
                Thread.currentThread().setName("Render " + view.getName());
                krc = createCompositor(view.getSource().getJpxSource());
                krc.Set_thread_env(localThread.get(), null);
                localCompositor.set(krc);
            }
            renderLayer(krc);
        } catch (Exception e) { // reboot the compositor
            if (krc != null)
                destroyCompositor(krc);
            localCompositor.set(null);
            localThread.remove();
            e.printStackTrace();
        }
    }

    @Nullable
    private static Kdu_thread_env createThreadEnv() {
        try {
            Kdu_thread_env kte = new Kdu_thread_env();
            kte.Create();
            int numThreads = Kdu_global.Kdu_get_num_processors();
            for (int i = 1; i < numThreads; i++)
                kte.Add_thread();
            // System.out.println(">>>> Kdu_thread_env create " + kte);
            return kte;
        } catch (KduException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Kdu_region_compositor createCompositor(Jpx_source jpx) throws KduException {
        Kdu_region_compositor krc = new Kdu_region_compositor();
        // System.out.println(">>>> compositor create " + krc + " " + Thread.currentThread().getName());
        krc.Create(jpx, KakaduConstants.CODESTREAM_CACHE_THRESHOLD);
        krc.Set_surface_initialization_mode(false);
        krc.Set_quality_limiting(new Kdu_quality_limiter(1f / 256), -1, -1);
        return krc;
    }

    private static void destroyCompositor(Kdu_region_compositor krc) {
        try {
            // System.out.println(">>>> compositor destroy " + krc + " " + Thread.currentThread().getName());
            krc.Halt_processing();
            krc.Remove_ilayer(new Kdu_ilayer_ref(), true);
            krc.Set_thread_env(null, null);
            krc.Native_destroy();
        } catch (KduException e) {
            e.printStackTrace();
        }
    }

    private static void abolish() {
        try {
            Kdu_region_compositor krc = localCompositor.get();
            if (krc != null) {
                destroyCompositor(krc);
                localCompositor.set(null);
            }
            Kdu_thread_env kte = localThread.get();
            if (kte != null) {
                kte.Destroy();
                localThread.set(null);
            }
        } catch (KduException e) {
            e.printStackTrace();
        }
    }

}
