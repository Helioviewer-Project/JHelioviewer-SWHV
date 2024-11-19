package org.helioviewer.jhv.imagedata;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ForkJoinPool;

import org.helioviewer.jhv.math.MathUtils;

@SuppressWarnings("serial")
public class ImageFilter {

    public enum Type {
        None("No filter", null), MGN("Multi-scale Gaussian normalization", new FilterMGN()), WOW("Wavelet-optimized whitening", new FilterWOW());

        public final String description;
        final Algorithm algorithm;

        Type(String _description, Algorithm _algorithm) {
            description = _description;
            algorithm = _algorithm;
        }

    }

    interface Algorithm {
        float[] filter(float[] data, int width, int height);
    }

    private static final float BDIV = 1 / 255f, SDIV = 1 / 65535f;

    private interface ConvertOp {
        void accept(Object in, Object out, int start, int end);
    }

    private static class ConvertTask extends RecursiveAction {

        private final Object in;
        private final Object out;
        private final int start;
        private final int end;
        private final ConvertOp op;

        ConvertTask(Object in, Object out, int start, int end, ConvertOp op) {
            this.in = in;
            this.out = out;
            this.start = start;
            this.end = end;
            this.op = op;
        }

        @Override
        protected void compute() {
            if (end - start <= ArrayOp.THRESHOLD) {
                op.accept(in, out, start, end);
            } else {
                int mid = (start + end) / 2;
                invokeAll(
                        new ConvertTask(in, out, start, mid, op),
                        new ConvertTask(in, out, mid, end, op));
            }
        }

    }

    private static final ConvertOp b2f = (ain, aout, start, end) -> {
        byte[] in = (byte[]) ain;
        float[] out = (float[]) aout;
        for (int i = start; i < end; ++i)
            out[i] = ((in[i] + 256) & 0xFF) * BDIV;
    };

    private static final ConvertOp s2f = (ain, aout, start, end) -> {
        short[] in = (short[]) ain;
        float[] out = (float[]) aout;
        for (int i = start; i < end; ++i)
            out[i] = ((in[i] + 65536) & 0xFFFF) * SDIV;
    };

    private static final ConvertOp f2b = (ain, aout, start, end) -> {
        float[] in = (float[]) ain;
        byte[] out = (byte[]) aout;
        for (int i = start; i < end; ++i)
            out[i] = (byte) MathUtils.clip(in[i] * 255 + .5f, 0, 255);
    };

    private static final ConvertOp f2s = (ain, aout, start, end) -> {
        float[] in = (float[]) ain;
        short[] out = (short[]) aout;
        for (int i = start; i < end; ++i)
            out[i] = (short) MathUtils.clip(in[i] * 65535 + .5f, 0, 65535);
    };

    private static ByteBuffer filter(ByteBuffer buf, int width, int height, Algorithm algorithm) {
        int length = width * height;
        byte[] array = buf.array(); // always backed by array
        ForkJoinPool pool = ForkJoinPool.commonPool();

        float[] data = new float[length];
        pool.invoke(new ConvertTask(array, data, 0, length, b2f));

        float[] image = algorithm.filter(data, width, height);

        byte[] out = new byte[length];
        pool.invoke(new ConvertTask(image, out, 0, length, f2b));

        return ByteBuffer.wrap(out);
    }

    private static ShortBuffer filter(ShortBuffer buf, int width, int height, Algorithm algorithm) {
        int length = width * height;
        short[] array = buf.array(); // always backed by array
        ForkJoinPool pool = ForkJoinPool.commonPool();

        float[] data = new float[length];
        pool.invoke(new ConvertTask(array, data, 0, length, s2f));

        float[] image = algorithm.filter(data, width, height);

        short[] out = new short[length];
        pool.invoke(new ConvertTask(image, out, 0, length, f2s));

        return ShortBuffer.wrap(out);
    }

    static Buffer filter(Buffer buf, int width, int height, Type type) throws Exception {
        if (buf instanceof ByteBuffer)
            return filter((ByteBuffer) buf, width, height, type.algorithm);
        else if (buf instanceof ShortBuffer)
            return filter((ShortBuffer) buf, width, height, type.algorithm);
        else
            throw new Exception("Unimplemented data type filtering");
    }

}
