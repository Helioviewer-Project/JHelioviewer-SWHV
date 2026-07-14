package org.helioviewer.jhv.io;

import java.io.File;
import java.net.URI;
import java.nio.Buffer;
import java.util.List;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.base.ArrayUtils;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.header.Standard;
import nom.tam.image.compression.hdu.CompressedImageHDU;

// Raw PUNCH archive FITS carry no HV_DMIN/HV_DMAX, so FITSImage would normalize every frame to its
// own sampled percentile range -> brightness flicker across a movie. This samples a bounded subset
// of the frames and derives ONE robust display range; the caller applies it to the whole layer
// (View.setRange) so every frame decodes against the same range. Off the EDT; the progress callback
// lets the dialog show how far the sampling fetch has got. No files are written.
final class PunchRange {

    private static final int MAX_SAMPLE_FRAMES = 8; // a few frames are plenty for a robust range
    private static final int SAMPLE = 4;            // coarse grid, like FITSImage.sampleImage
    private static final int BIN = 1024;
    private static final double LO_PCT = 0.005;     // robust percentiles (0.5% / 99.5%)
    private static final double HI_PCT = 0.995;

    // Returns a shared [min, max] display range, or null if it couldn't be derived (caller then
    // keeps the default per-frame auto). Fetches only a small, evenly-spread subset, in parallel.
    @Nullable
    static float[] compute(List<URI> remoteUris) {
        int n = remoteUris.size();
        if (n < 2)
            return null;

        int step = Math.max(1, n / MAX_SAMPLE_FRAMES);
        List<float[]> samples = IntStream.range(0, n).filter(i -> i % step == 0).limit(MAX_SAMPLE_FRAMES)
                .parallel().mapToObj(i -> {
                    try {
                        File file = NetFileCache.get(remoteUris.get(i)).file();
                        return sampleFrame(file);
                    } catch (Exception e) {
                        Log.warn("PUNCH range: sample failed " + remoteUris.get(i), e);
                        return new float[0];
                    }
                }).filter(s -> s.length > 0).toList();

        if (samples.size() < 2)
            return null;

        int poolLen = samples.stream().mapToInt(s -> s.length).sum();
        if (poolLen < 2)
            return null;
        float[] pool = new float[poolLen];
        int at = 0;
        for (float[] s : samples) {
            System.arraycopy(s, 0, pool, at, s.length);
            at += s.length;
        }

        int kLo = Math.clamp((int) (LO_PCT * poolLen), 0, poolLen - 1);
        int kHi = Math.clamp((int) (HI_PCT * poolLen), 0, poolLen - 1);
        float dmin = ArrayUtils.selectKth(pool, 0, poolLen - 1, kLo);
        float dmax = ArrayUtils.selectKth(pool, 0, poolLen - 1, kHi);
        if (dmin >= dmax)
            dmax = dmin + 1;
        Log.info("PUNCH range: min=" + dmin + " max=" + dmax + " from " + samples.size() + '/' + n + " frames");
        return new float[]{dmin, dmax};
    }

    private static BasicHDU<?> imageHDU(Fits fits) throws Exception {
        BasicHDU<?>[] hdus = fits.read(); // read once; the stream is consumed
        for (BasicHDU<?> hdu : hdus) {
            if (hdu instanceof CompressedImageHDU)
                return hdu;
        }
        for (BasicHDU<?> hdu : hdus) {
            if (hdu instanceof ImageHDU ihdu && ihdu.getAxes() != null)
                return ihdu;
        }
        throw new Exception("No image found");
    }

    private static Header imageHeader(BasicHDU<?> hdu) throws Exception {
        return hdu instanceof CompressedImageHDU chdu ? chdu.getImageHeader() : hdu.getHeader();
    }

    // Subsample one frame to a coarse grid, apply BZERO/BSCALE, drop BLANK/non-finite/zero
    private static float[] sampleFrame(File file) throws Exception {
        try (Fits f = new Fits(file)) {
            BasicHDU<?> hdu = imageHDU(f);
            Header header = imageHeader(hdu);
            if (header.getIntValue("NAXIS", 0) != 2)
                return new float[0];
            int height = header.getIntValue("NAXIS2", 0);
            int width = header.getIntValue("NAXIS1", 0);
            if (width <= 0 || height <= 0)
                return new float[0];

            boolean hasBlank = header.containsKey(Standard.BLANK);
            long blank = hasBlank ? header.getLongValue(Standard.BLANK) : 0;
            double bzero = header.getDoubleValue(Standard.BZERO, 0);
            double bscale = header.getDoubleValue(Standard.BSCALE, 1);
            if (!Double.isFinite(bzero) || !Double.isFinite(bscale))
                return new float[0];

            Object pixels = flatPixels(hdu, height, width);
            int stepW = Math.max(SAMPLE * width / BIN, 1);
            int stepH = Math.max(SAMPLE * height / BIN, 1);
            float[] out = new float[((height + stepH - 1) / stepH) * ((width + stepW - 1) / stepW)];
            int len = 0;
            for (int j = 0; j < height; j += stepH) {
                int line = width * j;
                for (int i = 0; i < width; i += stepW) {
                    double v = switch (pixels) {
                        case short[] d -> (hasBlank && d[line + i] == (short) blank) ? Double.NaN : bzero + d[line + i] * bscale;
                        case int[] d -> (hasBlank && d[line + i] == (int) blank) ? Double.NaN : bzero + d[line + i] * bscale;
                        case long[] d -> (hasBlank && d[line + i] == blank) ? Double.NaN : bzero + d[line + i] * bscale;
                        case float[] d -> bzero + d[line + i] * bscale;
                        case double[] d -> bzero + d[line + i] * bscale;
                        default -> throw new Exception("Unknown pixel type: " + pixels.getClass().getSimpleName());
                    };
                    if (Double.isFinite(v) && v != 0)
                        out[len++] = (float) v;
                }
            }
            float[] trimmed = new float[len];
            System.arraycopy(out, 0, trimmed, 0, len);
            return trimmed;
        }
    }

    @SuppressWarnings("deprecation")
    private static Object flatPixels(BasicHDU<?> hdu, int height, int width) throws Exception {
        if (hdu instanceof CompressedImageHDU chdu) {
            Buffer buffer = chdu.getUncompressedData();
            if (!buffer.hasArray() || buffer.arrayOffset() != 0 || buffer.position() != 0 || buffer.remaining() < height * width)
                throw new Exception("Unsupported compressed FITS pixel buffer");
            return buffer.array();
        }
        if (hdu instanceof ImageHDU ihdu)
            return ihdu.getData().getTiler().getTile(new int[]{0, 0}, new int[]{height, width});
        throw new Exception("Unsupported FITS HDU: " + hdu.getClass().getSimpleName());
    }

    private PunchRange() {}
}
