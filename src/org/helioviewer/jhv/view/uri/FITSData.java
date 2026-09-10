package org.helioviewer.jhv.view.uri;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import javax.annotation.Nullable;

import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.thread.ParallelRange;
import org.helioviewer.jhv.view.ClipSet;

import nom.tam.fits.Header;

record FITSData(Header header, Object pixels, int width, int height, boolean hasBlank, long blank,
               double bzero, double bscale, @Nullable ClipSet.Range headerRange) {

    private static final int BAD_PIXEL = Integer.MIN_VALUE;
    private static final int SAMPLE = 4;
    private static final int MIN_SAMPLES = 10;
    private static final int SCALE_LOOKUP_SIZE = 1 << 16;
    private static final int RAW_SHORT_LOOKUP_SIZE = 1 << Short.SIZE;
    private static final short HALF_FLOAT_ZERO = Float.floatToFloat16(0f);
    private static final short HALF_FLOAT_ONE = Float.floatToFloat16(1f);

    @Nullable
    ClipSet calculateClipSet() throws Exception {
        if (pixels instanceof byte[] || headerRange != null)
            return null;
        SampleBuffer sample = sampleImage();
        if (sample.length() < MIN_SAMPLES)
            return new ClipSet(null, null);
        Arrays.sort(sample.values(), 0, sample.length());
        return new ClipSet(percentileRange(sample, FITSViewState.ClippingMode.Percentile001.percentile()),
                percentileRange(sample, FITSViewState.ClippingMode.Percentile05.percentile()));
    }

    ImageBuffer decode(ImageFilter filter, FITSViewState.Data state, @Nullable ClipSet clipSet) throws Exception {
        if (pixels instanceof byte[] inData) {
            ImageBuffer.WriteBuffer outBuffer = ImageBuffer.createWriteBuffer(width, height, ImageBuffer.Format.Gray8, filter);
            ByteBuffer outData = outBuffer.byteBuffer();
            for (int j = 0; j < height; j++) {
                outData.put(width * (height - 1 - j), inData, width * j, width);
            }
            return outBuffer.finish();
        }

        ClipSet.Range range = headerRange;
        if (range == null) {
            if (clipSet == null)
                clipSet = calculateClipSet();
            range = switch (state.clippingMode()) {
                case Percentile001 -> clipSet.percentile001();
                case Percentile05 -> clipSet.percentile05();
                case Range -> new ClipSet.Range((float) state.clippingMin(), (float) state.clippingMax());
            };
        }
        if (range == null)
            return ImageBuffer.createWriteBuffer(width, height, ImageBuffer.Format.Gray8, filter).clearPixels().finish();
        float min = range.lower();
        float max = range.upper();
        if (min >= max)
            max = min + 1;

        ImageBuffer.WriteBuffer outBuffer = ImageBuffer.createWriteBuffer(width, height, ImageBuffer.Format.Gray16F, filter);
        convertPixels(outBuffer.shortBuffer(), min, max, state);
        return outBuffer.finish();
    }

    private SampleBuffer sampleImage() throws Exception {
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

    private short[] rawShortToHalfFloat(float min, double toUnit, NormalizedMapping mapping) {
        short[] values = new short[RAW_SHORT_LOOKUP_SIZE];

        for (int i = 0; i < values.length; i++) {
            short raw = (short) i;
            float v = (hasBlank && raw == (short) blank) ? BAD_PIXEL : (float) (bzero + raw * bscale);
            // sampling may have missed extremes
            values[i] = mapNormalizedToHalfFloat((v - min) * toUnit, mapping);
        }
        return values;
    }

    private void convertPixels(ShortBuffer outData, float min, float max, FITSViewState.Data state) throws Exception {
        float range = max - min;
        double toUnit = 1. / range;
        double toIndex = SCALE_LOOKUP_SIZE / (double) range;
        NormalizedMapping mapping = normalizedMapping(state, range);

        final boolean identity = bzero == 0.0 && bscale == 1.0; // hope HotSpot can generate a fast path
        switch (pixels) {
            case short[] inData -> {
                short[] values = rawShortToHalfFloat(min, toUnit, mapping);
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

    private static float floatPixel(double v, double bzero, double bscale) {
        if (Double.isNaN(v)) {
            return BAD_PIXEL;
        } else if (Double.isInfinite(v)) {
            return Float.MAX_VALUE;
        } else {
            return (float) (bzero + v * bscale);
        }
    }

    private record SampleBuffer(float[] values, int length) {}

    private static ClipSet.Range percentileRange(SampleBuffer sample, double percentile) {
        int length = sample.length();
        float lower = sample.values()[Math.clamp((int) (percentile * length), 0, length - 1)];
        float upper = sample.values()[Math.clamp((int) ((1 - percentile) * length), 0, length - 1)];
        return new ClipSet.Range(lower, upper);
    }

    private interface NormalizedMapping {
        double map(double x);
    }

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
}
