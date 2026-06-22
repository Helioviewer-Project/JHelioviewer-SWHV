package org.helioviewer.jhv.view.uri;

import java.io.File;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.ArrayUtils;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.thread.ParallelRange;

import com.google.common.escape.Escaper;
import com.google.common.xml.XmlEscapers;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.header.Standard;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.util.Cursor;

// essentially static; local or network cache
public final class FITSImage implements URIImageReader {

    private static final int BAD_PIXEL = Integer.MIN_VALUE;

    @Override
    public URIImageReader.Image readImage(File file) throws Exception {
        try (Fits f = new Fits(file)) {
            BasicHDU<?> hdu = findHDU(f);
            return new URIImageReader.Image(getHeaderAsXML(imageHeader(hdu)), readHDU(hdu, ImageFilter.NONE, null), null);
        }
    }

    @Override
    public ImageBuffer readImageBuffer(File file, ImageFilter filter, @Nullable float[] clip) throws Exception {
        try (Fits f = new Fits(file)) {
            return readHDU(findHDU(f), filter, clip);
        }
    }

    public ImageBuffer readImageBuffer(InputStream input) throws Exception {
        try (Fits f = new Fits(input)) {
            return readHDU(findHDU(f), ImageFilter.NONE, null);
        }
    }

    private static BasicHDU<?> findHDU(Fits fits) throws Exception {
        BasicHDU<?>[] hdus = fits.read();
        // this is cumbersome
        for (BasicHDU<?> hdu : hdus) {
            if (hdu instanceof CompressedImageHDU) {
                return hdu;
            }
        }
        for (BasicHDU<?> hdu : hdus) {
            if (hdu instanceof ImageHDU ihdu && ihdu.getAxes() != null /* might be an extension */) {
                return ihdu;
            }
        }
        throw new Exception("No image found");
    }

    private static Header imageHeader(BasicHDU<?> hdu) throws Exception {
        if (hdu instanceof CompressedImageHDU chdu) {
            return chdu.getImageHeader();
        } else {
            return hdu.getHeader();
        }
    }

    private static float floatPixel(double v, double bzero, double bscale) {
        if (Double.isNaN(v)) {
            return BAD_PIXEL;
        } else if (Double.isInfinite(v)) {
            return Float.MAX_VALUE;
        } else {
            return (float) (bzero + v * bscale);
        }
    }

    // private static final int SAMPLE = 8;
    private static final int SAMPLE = 4;
    private static final int MIN_SAMPLES = 10;

    private record SampleBuffer(float[] values, int length) {}

    private static SampleBuffer sampleImage(Object pixels, boolean hasBlank, long blank, double bzero, double bscale, int width, int height) throws Exception {
        int stepW = Math.max(SAMPLE * width / 1024, 1);
        int stepH = Math.max(SAMPLE * height / 1024, 1);
        int sampleRows = (height + stepH - 1) / stepH;
        int sampleCols = (width + stepW - 1) / stepW;
        float[] samples = new float[sampleRows * sampleCols];
        int sampleLen = 0;
        switch (pixels) {
            case short[] inData -> {
                for (int j = 0; j < height; j += stepH) {
                    int line = width * j;
                    for (int i = 0; i < width; i += stepW) {
                        short raw = inData[line + i];
                        float v = (hasBlank && raw == (short) blank) ? BAD_PIXEL : (float) (bzero + raw * bscale);
                        if (v != BAD_PIXEL && v != Float.MAX_VALUE) {
                            samples[sampleLen++] = v;
                        }
                    }
                }
            }
            case int[] inData -> {
                for (int j = 0; j < height; j += stepH) {
                    int line = width * j;
                    for (int i = 0; i < width; i += stepW) {
                        int raw = inData[line + i];
                        float v = (hasBlank && raw == (int) blank) ? BAD_PIXEL : (float) (bzero + raw * bscale);
                        if (v != BAD_PIXEL && v != Float.MAX_VALUE) {
                            samples[sampleLen++] = v;
                        }
                    }
                }
            }
            case long[] inData -> {
                for (int j = 0; j < height; j += stepH) {
                    int line = width * j;
                    for (int i = 0; i < width; i += stepW) {
                        long raw = inData[line + i];
                        float v = (hasBlank && raw == blank) ? BAD_PIXEL : (float) (bzero + raw * bscale);
                        if (v != BAD_PIXEL && v != Float.MAX_VALUE) {
                            samples[sampleLen++] = v;
                        }
                    }
                }
            }
            case float[] inData -> {
                for (int j = 0; j < height; j += stepH) {
                    int line = width * j;
                    for (int i = 0; i < width; i += stepW) {
                        float v = floatPixel(inData[line + i], bzero, bscale);
                        if (v != BAD_PIXEL && v != Float.MAX_VALUE) {
                            samples[sampleLen++] = v;
                        }
                    }
                }
            }
            case double[] inData -> {
                for (int j = 0; j < height; j += stepH) {
                    int line = width * j;
                    for (int i = 0; i < width; i += stepW) {
                        float v = floatPixel(inData[line + i], bzero, bscale);
                        if (v != BAD_PIXEL && v != Float.MAX_VALUE) {
                            samples[sampleLen++] = v;
                        }
                    }
                }
            }
            default -> throw new Exception("Unknown pixel type: " + pixels.getClass().getSimpleName());
        }
        return new SampleBuffer(samples, sampleLen);
    }

    private interface NormalizedMapping {
        double map(double x);
    }

    private static final int SCALE_LOOKUP_SIZE = 1 << 16;
    private static final int RAW_SHORT_LOOKUP_SIZE = 1 << Short.SIZE;
    private static final short HALF_FLOAT_ZERO = Float.floatToFloat16(0f);
    private static final short HALF_FLOAT_ONE = Float.floatToFloat16(1f);

    private record NormalizedLookup(short[] values) {
        private short mapIndex(double index) {
            if (!(index > 0)) {
                return HALF_FLOAT_ZERO;
            }
            if (index >= SCALE_LOOKUP_SIZE) {
                return HALF_FLOAT_ONE;
            }
            return values[(int) index];
        }
    }

    private static NormalizedLookup normalizedLookup(NormalizedMapping mapping) {
        short[] values = new short[SCALE_LOOKUP_SIZE];

        for (int i = 0; i < values.length; i++) {
            double x = (i + .5) / SCALE_LOOKUP_SIZE;
            values[i] = mapNormalizedToHalfFloat(x, mapping);
        }
        return new NormalizedLookup(values);
    }

    private static NormalizedMapping normalizedMapping(FITSViewState.Data state, float range) {
        return switch (state.scalingMode()) {
            case Gamma -> {
                double gamma = state.gamma();
                yield x -> Math.pow(x, gamma);
            }
            case Beta -> {
                double k = range * state.beta();
                double scale = 1. / MathUtils.asinh(k);
                yield x -> scale * MathUtils.asinh(x * k);
            }
            case Alpha -> {
                double alpha = state.alpha();
                double scale = 1. / Math.log1p(alpha);
                yield x -> scale * Math.log1p(x * alpha);
            }
        };
    }

    private static short mapNormalizedToHalfFloat(double x, NormalizedMapping mapping) {
        if (!(x > 0)) {
            return HALF_FLOAT_ZERO;
        }
        if (x >= 1) {
            return HALF_FLOAT_ONE;
        }
        return Float.floatToFloat16((float) Math.clamp(mapping.map(x), 0, 1));
    }

    private static short[] rawShortToHalfFloat(boolean hasBlank, long blank, double bzero, double bscale, float min, double toUnit,
                                               NormalizedMapping mapping) {
        short[] values = new short[RAW_SHORT_LOOKUP_SIZE];

        for (int i = 0; i < values.length; i++) {
            short raw = (short) i;
            float v = (hasBlank && raw == (short) blank) ? BAD_PIXEL : (float) (bzero + raw * bscale);
            // sampling may have missed extremes
            values[i] = mapNormalizedToHalfFloat((v - min) * toUnit, mapping);
        }
        return values;
    }

    private static void convertPixels(Object pixels, ShortBuffer outData, boolean hasBlank, long blank, double bzero, double bscale,
                                      int width, int height, float min, float max, FITSViewState.Data state) throws Exception {
        float range = max - min;
        double toUnit = 1. / range;
        double toIndex = SCALE_LOOKUP_SIZE / (double) range;
        NormalizedMapping mapping = normalizedMapping(state, range);

        final boolean identity = bzero == 0.0 && bscale == 1.0; // hope HotSpot can generate a fast path
        switch (pixels) {
            case short[] inData -> {
                short[] values = rawShortToHalfFloat(hasBlank, blank, bzero, bscale, min, toUnit, mapping);
                ParallelRange.run(height, (from, to) -> {
                    for (int j = from; j < to; j++) {
                        int inLine = width * j;
                        int outLine = width * (height - 1 - j);

                        for (int i = 0, outIdx = outLine; i < width; i++, outIdx++) {
                            outData.put(outIdx, values[inData[inLine + i] & 0xFFFF]);
                        }
                    }
                });
            }
            case int[] inData -> {
                NormalizedLookup lookup = normalizedLookup(mapping);
                ParallelRange.run(height, (from, to) -> {
                    for (int j = from; j < to; j++) {
                        int inLine = width * j;
                        int outLine = width * (height - 1 - j);

                        for (int i = 0, outIdx = outLine; i < width; i++, outIdx++) {
                            int raw = inData[inLine + i];
                            float v = (hasBlank && raw == (int) blank) ? BAD_PIXEL : identity ? (float) raw : (float) (bzero + raw * bscale);
                            outData.put(outIdx, lookup.mapIndex((v - min) * toIndex));
                        }
                    }
                });
            }
            case long[] inData -> {
                NormalizedLookup lookup = normalizedLookup(mapping);
                ParallelRange.run(height, (from, to) -> {
                    for (int j = from; j < to; j++) {
                        int inLine = width * j;
                        int outLine = width * (height - 1 - j);

                        for (int i = 0, outIdx = outLine; i < width; i++, outIdx++) {
                            long raw = inData[inLine + i];
                            float v = (hasBlank && raw == blank) ? BAD_PIXEL : identity ? (float) raw : (float) (bzero + raw * bscale);
                            outData.put(outIdx, lookup.mapIndex((v - min) * toIndex));
                        }
                    }
                });
            }
            case float[] inData -> {
                NormalizedLookup lookup = normalizedLookup(mapping);
                ParallelRange.run(height, (from, to) -> {
                    for (int j = from; j < to; j++) {
                        int inLine = width * j;
                        int outLine = width * (height - 1 - j);

                        for (int i = 0, outIdx = outLine; i < width; i++, outIdx++) {
                            float raw = inData[inLine + i];
                            float v = Float.isNaN(raw) ? BAD_PIXEL : Float.isInfinite(raw) ? Float.MAX_VALUE : identity ? raw : (float) (bzero + raw * bscale);
                            outData.put(outIdx, lookup.mapIndex((v - min) * toIndex));
                        }
                    }
                });
            }
            case double[] inData -> {
                NormalizedLookup lookup = normalizedLookup(mapping);
                ParallelRange.run(height, (from, to) -> {
                    for (int j = from; j < to; j++) {
                        int inLine = width * j;
                        int outLine = width * (height - 1 - j);

                        for (int i = 0, outIdx = outLine; i < width; i++, outIdx++) {
                            double raw = inData[inLine + i];
                            float v = Double.isNaN(raw) ? BAD_PIXEL : Double.isInfinite(raw) ? Float.MAX_VALUE : identity ? (float) raw : (float) (bzero + raw * bscale);
                            outData.put(outIdx, lookup.mapIndex((v - min) * toIndex));
                        }
                    }
                });
            }
            default -> throw new Exception("Unknown pixel type: " + pixels.getClass().getSimpleName());
        }
    }

    private static int[] imageAxes(Header header) throws Exception {
        int nAxis = header.getIntValue("NAXIS", 0);
        if (nAxis != 2)
            throw new Exception("Only 2D FITS files supported");
        int[] axes = {header.getIntValue("NAXIS2", 0), header.getIntValue("NAXIS1", 0)};
        if (axes[0] <= 0 || axes[1] <= 0)
            throw new Exception("Only 2D FITS files supported");
        return axes;
    }

    private static ImageBuffer readHDU(BasicHDU<?> hdu, ImageFilter filter, @Nullable float[] clip) throws Exception {
        Header header = imageHeader(hdu);
        int[] axes = imageAxes(header);
        return readPixels(header, axes, readFlatPixels(hdu, axes), filter, clip);
    }

    @SuppressWarnings("deprecation")
    private static Object readFlatPixels(BasicHDU<?> hdu, int[] axes) throws Exception {
        if (hdu instanceof CompressedImageHDU chdu) {
            return unwrapPixelBuffer(chdu.getUncompressedData(), axes[0] * axes[1]);
        } else if (hdu instanceof ImageHDU ihdu) {
            return ihdu.getData().getTiler().getTile(new int[]{0, 0}, axes);
        } else {
            throw new Exception("Unsupported FITS HDU: " + hdu.getClass().getSimpleName());
        }
    }

    private static Object unwrapPixelBuffer(Buffer buffer, int expectedPixels) throws Exception {
        if (!buffer.hasArray() || buffer.arrayOffset() != 0 || buffer.position() != 0 || buffer.remaining() < expectedPixels) {
            throw new Exception("Unsupported compressed FITS pixel buffer: " + buffer.getClass().getSimpleName());
        }
        return buffer.array();
    }

    private static ImageBuffer readPixels(Header header, int[] axes, Object pixels, ImageFilter filter, @Nullable float[] clip) throws Exception {
        int height = axes[0];
        int width = axes[1];

        if (pixels instanceof byte[] inData) {
            ImageBuffer.WriteBuffer outBuffer = ImageBuffer.createWriteBuffer(width, height, ImageBuffer.Format.Gray8, filter);
            ByteBuffer outData = outBuffer.byteBuffer();
            for (int j = 0; j < height; j++) {
                outData.put(width * (height - 1 - j), inData, width * j, width);
            }
            return outBuffer.finish();
        }

        boolean hasBlank = header.containsKey(Standard.BLANK);
        long blank = hasBlank ? header.getLongValue(Standard.BLANK) : 0;
        double bzero = header.getDoubleValue(Standard.BZERO, 0);
        double bscale = header.getDoubleValue(Standard.BSCALE, 1);
        if (!Double.isFinite(bzero) || !Double.isFinite(bscale))
            throw new Exception("Invalid FITS BZERO/BSCALE");
        FITSViewState.Data state = FITSViewState.data();

        float min;
        float max;
        if (clip != null) {
            // Per-layer fixed display range — shared across a multi-frame layer's frames (e.g.
            // a PUNCH movie) so they normalize identically instead of strobing per-frame.
            min = clip[0];
            max = clip[1];
        } else {
            min = header.getFloatValue("HV_DMIN", Float.MAX_VALUE);
            max = header.getFloatValue("HV_DMAX", Float.MAX_VALUE);
            if (min == Float.MAX_VALUE || max == Float.MAX_VALUE) {
                if (state.clippingMode() == FITSViewState.ClippingMode.Range) {
                    min = (float) state.clippingMin();
                    max = (float) state.clippingMax();
                } else {
                    SampleBuffer sampleData = sampleImage(pixels, hasBlank, blank, bzero, bscale, width, height);
                    int sampleLen = sampleData.length();
                    if (sampleLen < MIN_SAMPLES) // couldn't find enough acceptable samples, return blank image
                        return ImageBuffer.createWriteBuffer(width, height, ImageBuffer.Format.Gray8, filter).finish();

                    if (state.clippingMode().percentile() > 0) {
                        double percentile = state.clippingMode().percentile();
                        int kMin = Math.clamp((int) (percentile * sampleLen), 0, sampleLen - 1);
                        int kMax = Math.clamp((int) ((1 - percentile) * sampleLen), 0, sampleLen - 1);
                        float[] values = sampleData.values();
                        min = ArrayUtils.selectKth(values, 0, sampleLen - 1, kMin);
                        max = ArrayUtils.selectKth(values, 0, sampleLen - 1, kMax);
                    } else {
                        Arrays.sort(sampleData.values(), 0, sampleLen);
                        ZScale.ZScaleRange range = ZScale.zscale(sampleData.values(), sampleLen, state.zContrast());
                        min = range.low();
                        max = range.high();
                    }
                }
            }
        }
        if (min >= max) {
            max = min + 1;
        }
        // System.out.println(">>> " + min + ' ' + max);

        ImageBuffer.WriteBuffer outBuffer = ImageBuffer.createWriteBuffer(width, height, ImageBuffer.Format.Gray16F, filter);
        convertPixels(pixels, outBuffer.shortBuffer(), hasBlank, blank, bzero, bscale, width, height, min, max, state);
        return outBuffer.finish();
    }

    private static final String nl = System.lineSeparator();
    private static final Escaper XML_CONTENT_ESCAPER = XmlEscapers.xmlContentEscaper();
    private static final Escaper XML_ATTRIBUTE_ESCAPER = XmlEscapers.xmlAttributeEscaper();

    private static String getHeaderAsXML(Header header) {
        StringBuilder builder = new StringBuilder("<meta>" + nl + "<fits>" + nl);

        for (Cursor<String, HeaderCard> iter = header.iterator(); iter.hasNext(); ) {
            HeaderCard headerCard = iter.next();
            String key = headerCard.getKey().trim();
            if ("END".equals(key))
                continue;
            key = key.isEmpty() ? "COMMENT" : key.replace("$", "-"); // allow illegal keyword character in FITS saved by IDL

            String value = headerCard.getValue();
            String val = value == null ? "" : XML_CONTENT_ESCAPER.escape(value);
            String comment = headerCard.getComment();
            String com = comment == null ? "" : " comment=\"" + XML_ATTRIBUTE_ESCAPER.escape(comment) + "\"";

            builder.append('<').append(key).append(com).append('>').append(val).append("</").append(key).append('>').append(nl);
        }
        builder.append("</fits>").append(nl).append("</meta>");
        return builder.toString();
    }

}
