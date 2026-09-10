package org.helioviewer.jhv.view.j2k;

import java.nio.ByteBuffer;
import java.util.concurrent.Callable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.image.DecodedImage;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.Region;

import org.lwjgl.system.MemoryUtil;

import kdu_jni.KduException;
import kdu_jni.Kdu_compositor_buf;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_global;
import kdu_jni.Kdu_quality_limiter;
import kdu_jni.Kdu_region_compositor;
import kdu_jni.Kdu_thread_env;

record J2KDecoder(J2KSource src, J2KParams.Decode params, int numComps, ImageFilter.Type filterType, MetaData metaData,
                  double factorX, double factorY) implements Callable<DecodedImage> {
    // Maximum of samples to process per rendering iteration
    private static final int MAX_RENDER_SAMPLES = 256 * 1024;
    private static final int[] firstComponent = {0};
    private static final Kdu_quality_limiter qualityLow = new Kdu_quality_limiter(2f / 256);
    private static final Kdu_quality_limiter qualityHigh = new Kdu_quality_limiter(1f / 256);

    private static final ThreadLocal<DecodeScratch> localScratch = ThreadLocal.withInitial(DecodeScratch::new);

    private static final class DecodeScratch {
        final Kdu_dims empty = new Kdu_dims();
        final Kdu_dims requestedRegion = new Kdu_dims();
        final Kdu_dims newRegion = new Kdu_dims();
        final int[] srcStride = new int[1];
    }

    @Override
    @SuppressWarnings("try")
    public DecodedImage call() throws KduException {
        try (J2KSource.Use ignored = src.use()) {
            if (src.isJP2())
                src.open();
            try {
                return decode();
            } finally {
                if (src.isJP2())
                    src.close();
            }
        }
    }

    private DecodedImage decode() throws KduException {
        // Measurements for compositor on 4k AIA tiles were up to about 10x faster than
        // kdu_region_decompressor/region output (~23ms vs ~200ms). Compositor stays on
        // Kakadu's optimized 32-bit path; region uses general channel-buffer conversion.
        Kdu_region_compositor compositor = new Kdu_region_compositor();
        Kdu_thread_env environment = null;
        try {
            environment = new Kdu_thread_env();
            environment.Create();
            int numThreads = Math.min(8, Kdu_global.Kdu_get_num_processors());
            for (int i = 1; i < numThreads; i++)
                environment.Add_thread();

            compositor.Create(src.jpxSource());
            compositor.Set_surface_initialization_mode(false);
            compositor.Set_quality_limiting(params.factor < 1 ? qualityLow : qualityHigh, -1, -1);
            compositor.Set_thread_env(environment, null);

            J2KParams.SubImage subImage = params.subImage;
            int frame = params.frame;
            DecodeScratch scratch = localScratch.get();

            Kdu_dims empty = scratch.empty;
            empty.From_u32(0, 0, 0, 0);
            if (numComps < 3) {
                // alpha tbd
                compositor.Add_primitive_ilayer(frame, firstComponent, Kdu_global.KDU_WANT_CODESTREAM_COMPONENTS, empty, empty);
            } else {
                compositor.Add_ilayer(frame, empty, empty);
            }

            compositor.Set_scale(false, false, false, 1f / (1 << params.level), params.factor);

            Kdu_dims requestedRegion = scratch.requestedRegion;
            requestedRegion.From_u32(subImage.x(), subImage.y(), subImage.w(), subImage.h());
            compositor.Set_buffer_surface(requestedRegion);

            Kdu_compositor_buf compositorBuf = compositor.Get_composition_buffer(empty, true); // modifies empty
            Kdu_dims actualRegion = compositorBuf.Get_rendering_region();

            Kdu_coords actualPos = actualRegion.Access_pos();
            int actualX = actualPos.Get_x(), actualY = actualPos.Get_y();

            Kdu_coords actualSize = actualRegion.Access_size();
            int actualWidth = actualSize.Get_x(), actualHeight = actualSize.Get_y();

            int[] srcStride = scratch.srcStride;
            long addr = compositorBuf.Get_buf(srcStride, false);
            ByteBuffer nativeBuffer = MemoryUtil.memByteBuffer(addr, Math.toIntExact(4L * srcStride[0] * actualHeight));

            boolean gray = numComps < 3;
            // Assume Kakadu's 4-byte compositor output already matches our RGBA byte upload layout.
            ImageBuffer.Format format = gray ? ImageBuffer.Format.Gray8 : ImageBuffer.Format.RGBA32;
            Region imageRegion = metaData.roiToRegion(actualX, actualY, actualWidth, actualHeight,
                    factorX / params.factor, factorY / params.factor);
            ImageFilter filter = ImageFilter.of(filterType, imageRegion, metaData);
            ImageBuffer.WriteBuffer outBuffer = ImageBuffer.createWriteBuffer(actualWidth, actualHeight, format, filter);
            ByteBuffer outByteBuffer = outBuffer.byteBuffer();

            // With surface initialization disabled, only the completed buffer is ready to copy.
            while (!compositor.Is_processing_complete()) {
                if (!compositor.Process(MAX_RENDER_SAMPLES, scratch.newRegion))
                    throw new KduException("JPEG 2000 rendering failed, invalid scale code "
                            + compositor.Check_invalid_scale_code());
            }
            if (gray) {
                gatherGray(nativeBuffer, outByteBuffer, srcStride[0], actualWidth, actualHeight);
            } else {
                for (int row = 0; row < actualHeight; row++)
                    outByteBuffer.put(4 * row * actualWidth, nativeBuffer, 4 * row * srcStride[0], 4 * actualWidth);
            }
            return new DecodedImage(outBuffer.finish(), imageRegion);
        } finally {
            // Kakadu's destructor stops processing and releases layers and buffers.
            try {
                compositor.Native_destroy();
            } finally {
                destroyThreadEnv(environment);
            }
        }
    }

    private static void gatherGray(ByteBuffer src, ByteBuffer dst, int srcStride, int width, int height) {
        for (int row = 0; row < height; row++) {
            int dstIdx = row * width;
            int srcByte = 4 * row * srcStride;
            int i = 0;
            for (; i <= width - 4; i += 4, srcByte += 16) { // doesn't help much, more is definitely harmful
                dst.put(dstIdx + i, src.get(srcByte));
                dst.put(dstIdx + i + 1, src.get(srcByte + 4));
                dst.put(dstIdx + i + 2, src.get(srcByte + 8));
                dst.put(dstIdx + i + 3, src.get(srcByte + 12));
            }
            for (; i < width; ++i, srcByte += 4)
                dst.put(dstIdx + i, src.get(srcByte));
        }
    }

    private static void destroyThreadEnv(Kdu_thread_env kte) {
        if (kte == null) return;
        try {
            kte.Destroy();
        } catch (KduException e) {
            Log.warn("Failed to destroy Kakadu thread environment", e);
        } finally {
            kte.Native_destroy();
        }
    }

}
