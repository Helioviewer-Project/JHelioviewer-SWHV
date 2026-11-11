package org.helioviewer.jhv.source;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.ArrayUtils;
import org.helioviewer.jhv.image.ClipSet;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.ImageProcessingSettings;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.thread.ParallelRange;

record FITSData(int width, int height, boolean byteImage, @Nullable ClipSet.Range headerRange) {

    private static final int MIN_SAMPLES = 10;
    private static final int SCALE_LOOKUP_SIZE = 1 << 16;
    private static final short HALF_FLOAT_ZERO = Float.floatToFloat16(0f);
    private static final short HALF_FLOAT_ONE = Float.floatToFloat16(1f);

    ImageBuffer decode(float[] pixels, ImageFilter filter, ImageProcessingSettings.FITSParameters state, @Nullable ClipSet.Range clipRange) throws Exception {
        ClipSet.Range range = headerRange != null ? headerRange : clipRange;
        ImageBuffer.Format format = byteImage || range == null ? ImageBuffer.Format.Gray8 : ImageBuffer.Format.Gray16F;
        try (ImageBuffer.WriteBuffer outBuffer = ImageBuffer.createWriteBuffer(width, height, format)) {
            if (byteImage) {
                ByteBuffer output = outBuffer.byteBuffer();
                for (int y = 0; y < height; y++)
                    for (int x = 0; x < width; x++)
                        output.put(width * (height - 1 - y) + x, (byte) pixels[width * y + x]);
            } else if (range == null) {
                outBuffer.clearPixels();
            } else {
                float min = range.lower();
                float max = range.upper();
                if (min >= max)
                    max = min + 1;
                convertPixels(pixels, outBuffer.shortBuffer(), min, max, state);
            }
            return outBuffer.finish(filter);
        }
    }

    ClipSet calculateSampledClipSet(float[] samples) {
        int count = 0;
        for (float value : samples) {
            if (Float.isFinite(value))
                samples[count++] = value;
        }
        if (count < MIN_SAMPLES)
            return new ClipSet(null, null);
        return new ClipSet(percentileRange(samples, count, ImageProcessingSettings.ClippingMode.Percentile001.percentile()),
                percentileRange(samples, count, ImageProcessingSettings.ClippingMode.Percentile05.percentile()));
    }

    private void convertPixels(float[] pixels, ShortBuffer outData, float min, float max, ImageProcessingSettings.FITSParameters state) throws Exception {
        float range = max - min;
        double toIndex = SCALE_LOOKUP_SIZE / (double) range;
        NormalizedLookup lookup = normalizedLookup(normalizedMapping(state, range));
        ParallelRange.run(height, (from, to) -> {
            for (int j = from; j < to; j++) {
                int inLine = width * j;
                int outLine = width * (height - 1 - j);
                for (int i = 0; i < width; i++) {
                    float value = pixels[inLine + i];
                    outData.put(outLine + i, Float.isInfinite(value) ? HALF_FLOAT_ONE
                            : lookup.mapIndex((value - min) * toIndex));
                }
            }
        });
    }

    private static ClipSet.Range percentileRange(float[] values, int length, double percentile) {
        float lower = ArrayUtils.selectKth(values, 0, length - 1, Math.clamp((int) (percentile * length), 0, length - 1));
        float upper = ArrayUtils.selectKth(values, 0, length - 1, Math.clamp((int) ((1 - percentile) * length), 0, length - 1));
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
            values[i] = Float.floatToFloat16((float) Math.clamp(mapping.map(x), 0, 1));
        }
        return new NormalizedLookup(values);
    }

    private static NormalizedMapping normalizedMapping(ImageProcessingSettings.FITSParameters state, float range) {
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
}
