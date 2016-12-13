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
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduConstants;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduEngine;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduUtils;

class J2KRender implements Runnable {

    private static final ThreadLocal<int[]> bufferLocal = ThreadLocal.withInitial(() -> new int[KakaduConstants.MAX_RENDER_SAMPLES]);
    private static final ThreadLocal<Kdu_thread_env> threadLocal = ThreadLocal.withInitial(J2KRender::createThreadEnv);
    private static final ThreadLocal<KakaduEngine> engineLocal = new ThreadLocal<KakaduEngine>();

    private static final int[] firstComponent = { 0 };

    // A reference to the JP2Image this object is owned by
    private final JP2Image parentImageRef;

    // A reference to the JP2View this object is owned by
    private final JP2View parentViewRef;

    private final JP2ImageParameter params;

    private final boolean discard;

    J2KRender(JP2View _parentViewRef, JP2ImageParameter _currParams, boolean _discard) {
        parentViewRef = _parentViewRef;
        params = _currParams;
        parentImageRef = params.jp2Image;
        discard = _discard;
    }

    private void renderLayer(Kdu_region_compositor compositor) throws KduException {
        int numLayer = params.compositionLayer;
        int numComponents = params.components;

        Kdu_ilayer_ref ilayer;
        Kdu_dims dimsRef1 = new Kdu_dims(), dimsRef2 = new Kdu_dims();

        if (numComponents < 3) {
            // alpha tbd
            ilayer = compositor.Add_primitive_ilayer(numLayer, firstComponent, KakaduConstants.KDU_WANT_CODESTREAM_COMPONENTS, dimsRef1, dimsRef2);
        } else {
            ilayer = compositor.Add_ilayer(numLayer, dimsRef1, dimsRef2);
        }

        compositor.Set_scale(false, false, false, params.resolution.scaleLevel, (float) params.factor);
        Kdu_dims requestedRegion = KakaduUtils.roiToKdu_dims(params.subImage);
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

        ImageData imdata;
        if (numComponents < 3) {
            imdata = new SingleChannelByte8ImageData(aWidth, aHeight, ByteBuffer.wrap(byteBuffer));
        } else {
            imdata = new ARGBInt32ImageData(false, aWidth, aHeight, IntBuffer.wrap(intBuffer));
        }
        setImageData(imdata);
    }

    private void setImageData(ImageData newImageData) {
        MetaData metaData = params.jp2Image.metaDataList[params.compositionLayer];
        newImageData.setMetaData(metaData);
        newImageData.setViewpoint(params.viewpoint);
        newImageData.setRegion(metaData.roiToRegion(params.subImage, params.resolution.factorX, params.resolution.factorY));

        EventQueue.invokeLater(() -> parentViewRef.setImageData(newImageData));
    }

    @Override
    public void run() {
        try {
            KakaduEngine kduEngine = engineLocal.get();
            if (kduEngine == null) {
                kduEngine = parentImageRef.getRenderEngine(threadLocal.get());
                engineLocal.set(kduEngine);
            }
            renderLayer(kduEngine.getCompositor());
        } catch (Exception e) {
            // reboot the compositor
            engineLocal.set(null);
            e.printStackTrace();
        }
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
