package org.helioviewer.jhv.view.jp2view;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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

import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.imagedata.ImageDataBuffer;
import org.helioviewer.jhv.imagedata.ImageData.ImageFormat;
import org.helioviewer.jhv.imagedata.SubImage;
import org.helioviewer.jhv.view.jp2view.image.DecodeParams;
import org.helioviewer.jhv.view.jp2view.image.ImageParams;
import org.helioviewer.jhv.view.jp2view.kakadu.KakaduConstants;

import org.lwjgl.system.MemoryUtil;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;

class J2KRender implements Runnable {

    private static final int[] firstComponent = {0};

    private static final RemovalListener<DecodeParams, ImageDataBuffer> removalListener = new RemovalListener<DecodeParams, ImageDataBuffer>() {
        @Override
        public void onRemoval(RemovalNotification<DecodeParams, ImageDataBuffer> removal) {
            System.out.println(">>> removed!");
            //ImageDataBuffer.vbo.delete(gl);
            GLContext context = ImageViewerGui.getGLWindow().getContext();
            context.makeCurrent();
            GL2 gl = (GL2) context.getGL();
            gl.glDeleteBuffers(1, new int[]{removal.getValue().bufferID}, 0);
            context.release();
        }
    };

    private static final ThreadLocal<Cache<DecodeParams, ImageDataBuffer>> decodeCache =
            ThreadLocal.withInitial(() -> CacheBuilder.newBuilder().softValues().removalListener(removalListener).build());

    private static final ThreadLocal<Kdu_thread_env> localThread = ThreadLocal.withInitial(J2KRender::createThreadEnv);
    private static final ThreadLocal<Kdu_region_compositor> localCompositor = new ThreadLocal<>();

    private final JP2View view;
    private final ImageParams imageParams;
    private final boolean discard;
    private final boolean abolish;

    J2KRender(JP2View _view, ImageParams _imageParams, boolean _discard, boolean _abolish) {
        view = _view;
        imageParams = _imageParams;
        discard = _discard;
        abolish = _abolish;
    }

    private ImageDataBuffer renderLayer(DecodeParams params) throws KduException {
        ImageDataBuffer ret = decodeCache.get().getIfPresent(params);
        if (ret != null)
            return ret;

        SubImage subImage = params.subImage;
        int frame = params.frame;
        int numComponents = view.getNumComponents(frame);

        Kdu_region_compositor compositor = getCompositor();
        Kdu_dims empty = new Kdu_dims();
        Kdu_ilayer_ref ilayer;
        if (numComponents < 3) {
            // alpha tbd
            ilayer = compositor.Add_primitive_ilayer(frame, firstComponent, Kdu_global.KDU_WANT_CODESTREAM_COMPONENTS, empty, empty);
        } else {
            ilayer = compositor.Add_ilayer(frame, empty, empty);
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

        int[] rowGap = new int[1];
        long addr = compositorBuf.Get_buf(rowGap, false);

        int bufferLength = numComponents < 3 ? actualWidth * actualHeight : 4 * actualWidth * actualHeight;
//        byte[] byteBuffer = new byte[bufferLength];

        GL2 gl;
        GLContext context = ImageViewerGui.getGLWindow().getContext();

        context.makeCurrent();
        gl = (GL2) context.getGL();

        int[] tmpId = new int[1];
        gl.glGenBuffers(1, tmpId, 0);
        int bufferID = tmpId[0];

        gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, bufferID);
        gl.glBufferData(GL2.GL_PIXEL_UNPACK_BUFFER, bufferLength, null, GL2.GL_STATIC_DRAW);
        ByteBuffer byteBuffer = gl.glMapBufferRange(GL2.GL_PIXEL_UNPACK_BUFFER, 0, bufferLength, GL2.GL_MAP_WRITE_BIT | GL2.GL_MAP_UNSYNCHRONIZED_BIT);
        gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, 0);

        context.release();

        Kdu_dims newRegion = new Kdu_dims();
        while (compositor.Process(KakaduConstants.MAX_RENDER_SAMPLES, newRegion)) {
            Kdu_coords newSize = newRegion.Access_size();
            int newWidth = newSize.Get_x();
            int newHeight = newSize.Get_y();
            if (newWidth * newHeight == 0)
                continue;

            Kdu_coords newOffset = newRegion.Access_pos();
            int newX = newOffset.Get_x() - actualX;
            int newY = newOffset.Get_y() - actualY;

            int dstIdx = newX + newY * actualWidth;
            int srcIdx = 0;

            if (numComponents < 3) {
                for (int row = 0; row < newHeight; row++, dstIdx += actualWidth, srcIdx += newWidth) {
                    for (int col = 0; col < newWidth; ++col) {
                        byteBuffer.put(dstIdx + col, MemoryUtil.memGetByte(addr + 4 * (srcIdx + col)));
                    }
                }
            } else {
                for (int row = 0; row < newHeight; row++, dstIdx += actualWidth, srcIdx += newWidth) {
                    for (int col = 0; col < newWidth; ++col) {
                        for (int idx = 0; idx < 4; ++idx)
                            byteBuffer.put(4 * (dstIdx + col) + idx, MemoryUtil.memGetByte(addr + 4 * (srcIdx + col) + idx));
                    }
                }
            }
        }
        compositor.Remove_ilayer(ilayer, true);

        context.makeCurrent();
        gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, bufferID);
        gl.glUnmapBuffer(GL2.GL_PIXEL_UNPACK_BUFFER);
        gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, 0);
        context.release();

        ImageFormat format = numComponents < 3 ? ImageFormat.Gray8 : ImageFormat.ARGB32;
        ret = new ImageDataBuffer(actualWidth, actualHeight, format, /*ByteBuffer.wrap(byteBuffer).order(ByteOrder.nativeOrder())*/null, bufferID);
        if (!discard)
            decodeCache.get().put(params, ret);

        return ret;
    }

    @Override
    public void run() {
        if (abolish) {
            abolish();
            return;
        }

        try {
            ImageDataBuffer data = renderLayer(imageParams.decodeParams);
            view.setDataFromRender(imageParams, data);
        } catch (Exception e) { // reboot the compositor
            Kdu_region_compositor krc = localCompositor.get();
            if (krc != null)
                destroyCompositor(krc);
            localCompositor.set(null);
            localThread.remove();
            e.printStackTrace();
        }
    }

    private Kdu_region_compositor getCompositor() throws KduException {
        Kdu_region_compositor krc = localCompositor.get();
        if (krc != null)
            return krc;

        Thread.currentThread().setName("Render " + view.getName());
        krc = createCompositor(view.getSource().getJpxSource());
        krc.Set_thread_env(localThread.get(), null);
        localCompositor.set(krc);
        return krc;
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
            decodeCache.get().invalidateAll();
        } catch (KduException e) {
            e.printStackTrace();
        }
    }

}
