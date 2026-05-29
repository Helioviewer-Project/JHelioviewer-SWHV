package org.helioviewer.jhv.view.uri;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Logger;

import nom.tam.fits.compression.algorithm.api.ICompressOption;
import nom.tam.fits.compression.algorithm.api.ICompressorControl;
import nom.tam.fits.compression.algorithm.quant.QuantizeOption;
import nom.tam.fits.compression.algorithm.quant.RandomSequence;
import nom.tam.fits.compression.algorithm.rice.RiceCompressOption;
import nom.tam.fits.compression.provider.api.ICompressorProvider;
import nom.tam.fits.header.Compression;

public final class FastRiceProvider implements ICompressorProvider {

    private static final int BITS_PER_BYTE = 8;
    private static final int BYTE_MASK = 0xff;
    private static final int FS_BITS_FOR_BYTE = 3;
    private static final int FS_BITS_FOR_SHORT = 4;
    private static final int FS_BITS_FOR_INT = 5;
    private static final int FS_MAX_FOR_BYTE = 6;
    private static final int FS_MAX_FOR_SHORT = 14;
    private static final int FS_MAX_FOR_INT = 25;
    private static final int RANDOM_MULTIPLICATOR = 500;
    private static final int ZERO_VALUE = Integer.MIN_VALUE + 2;
    private static final Logger LOG = Logger.getLogger(FastRiceProvider.class.getName());

    // @formatter:off
    private static final int[] NONZERO_COUNT = {0, 1, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
            6, 6, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
            7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 8, 8, 8, 8, 8, 8, 8, 8,
            8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
            8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
            8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
            8, 8, 8, 8, 8, 8, 8, 8, 8};
    // @formatter:on

    public FastRiceProvider() {}

    @Override
    public ICompressorControl createCompressorControl(String quantAlgorithm, String compressionAlgorithm, Class<?> baseType) {
        if (!Compression.ZCMPTYPE_RICE_1.equalsIgnoreCase(compressionAlgorithm) &&
                !Compression.ZCMPTYPE_RICE_ONE.equalsIgnoreCase(compressionAlgorithm)) {
            return null;
        }

        if (quantAlgorithm != null && (baseType == float.class || baseType == double.class)) {
            return new Control(baseType, true);
        }
        if (quantAlgorithm == null && (baseType == byte.class || baseType == short.class || baseType == int.class)) {
            return new Control(baseType, false);
        }
        return null;
    }

    private static final class Control implements ICompressorControl {

        private final Class<?> baseType;
        private final boolean quantized;

        private Control(Class<?> _baseType, boolean _quantized) {
            baseType = _baseType;
            quantized = _quantized;
        }

        @Override
        public boolean compress(Buffer in, ByteBuffer out, ICompressOption option) {
            return false;
        }

        @Override
        public void decompress(ByteBuffer in, Buffer out, ICompressOption option) {
            if (quantized) {
                QuantizeOption quantize = option.unwrap(QuantizeOption.class);
                RiceCompressOption rice = option.unwrap(RiceCompressOption.class);
                if (baseType == float.class) {
                    decodeFloat(in, (FloatBuffer) out, quantize, rice);
                } else {
                    decodeDouble(in, (DoubleBuffer) out, quantize, rice);
                }
            } else if (baseType == byte.class) {
                decodeByte(in, (ByteBuffer) out, option.unwrap(RiceCompressOption.class));
            } else if (baseType == short.class) {
                decodeShort(in, (ShortBuffer) out, option.unwrap(RiceCompressOption.class));
            } else {
                decodeInt(in, (IntBuffer) out, option.unwrap(RiceCompressOption.class));
            }
        }

        @Override
        public ICompressOption option() {
            RiceCompressOption rice = new RiceCompressOption();
            if (quantized) {
                return new QuantizeOption(rice);
            }
            return rice;
        }
    }

    private static void decodeByte(ByteBuffer in, ByteBuffer out, RiceCompressOption option) {
        Decoder decoder = new Decoder(in, option, FS_BITS_FOR_BYTE, FS_MAX_FOR_BYTE);
        int last = decoder.firstByte();
        int length = out.limit();
        for (int i = 0; i < length;) {
            int fs = decoder.readFs();
            int end = decoder.blockEnd(i, length);
            if (fs < 0) {
                for (; i < end; i++) {
                    out.put((byte) last);
                }
            } else if (fs == decoder.fsMax) {
                for (; i < end; i++) {
                    last += map(decoder.readDirect());
                    out.put((byte) last);
                }
            } else {
                for (; i < end; i++) {
                    last += map(decoder.readRice(fs));
                    out.put((byte) last);
                }
            }
        }
        decoder.finish();
    }

    private static void decodeShort(ByteBuffer in, ShortBuffer out, RiceCompressOption option) {
        Decoder decoder = new Decoder(in, option, FS_BITS_FOR_SHORT, FS_MAX_FOR_SHORT);
        int last = decoder.firstShort();
        int length = out.limit();
        if (out.hasArray()) {
            short[] data = out.array();
            int offset = out.arrayOffset() + out.position();
            decoder.decodeShortArray(last, data, offset, length);
            out.position(length);
        } else {
            decodeShort(decoder, last, out, length);
        }
        decoder.finish();
    }

    private static void decodeShort(Decoder decoder, int last, short[] out, int offset, int length) {
        for (int i = 0; i < length;) {
            int fs = decoder.readFs();
            int end = decoder.blockEnd(i, length);
            if (fs < 0) {
                for (; i < end; i++) {
                    out[offset + i] = (short) last;
                }
            } else if (fs == decoder.fsMax) {
                for (; i < end; i++) {
                    last += map(decoder.readDirect());
                    out[offset + i] = (short) last;
                }
            } else {
                for (; i < end; i++) {
                    last += map(decoder.readRice(fs));
                    out[offset + i] = (short) last;
                }
            }
        }
    }

    private static void decodeShort(Decoder decoder, int last, ShortBuffer out, int length) {
        for (int i = 0; i < length;) {
            int fs = decoder.readFs();
            int end = decoder.blockEnd(i, length);
            if (fs < 0) {
                for (; i < end; i++) {
                    out.put((short) last);
                }
            } else if (fs == decoder.fsMax) {
                for (; i < end; i++) {
                    last += map(decoder.readDirect());
                    out.put((short) last);
                }
            } else {
                for (; i < end; i++) {
                    last += map(decoder.readRice(fs));
                    out.put((short) last);
                }
            }
        }
    }

    private static void decodeInt(ByteBuffer in, IntBuffer out, RiceCompressOption option) {
        Decoder decoder = new Decoder(in, option, FS_BITS_FOR_INT, FS_MAX_FOR_INT);
        int last = decoder.firstInt();
        int length = out.limit();
        for (int i = 0; i < length;) {
            int fs = decoder.readFs();
            int end = decoder.blockEnd(i, length);
            if (fs < 0) {
                for (; i < end; i++) {
                    out.put(last);
                }
            } else if (fs == decoder.fsMax) {
                for (; i < end; i++) {
                    last += map(decoder.readDirect());
                    out.put(last);
                }
            } else {
                for (; i < end; i++) {
                    last += map(decoder.readRice(fs));
                    out.put(last);
                }
            }
        }
        decoder.finish();
    }

    private static void decodeFloat(ByteBuffer in, FloatBuffer out, QuantizeOption quantize, RiceCompressOption option) {
        Decoder decoder = new Decoder(in, option, FS_BITS_FOR_INT, FS_MAX_FOR_INT);
        int last = decoder.firstInt();
        int length = out.limit();
        Quantizer quantizer = new Quantizer(quantize);
        for (int i = 0; i < length;) {
            int fs = decoder.readFs();
            int end = decoder.blockEnd(i, length);
            if (fs < 0) {
                for (; i < end; i++) {
                    out.put((float) quantizer.toDouble(last));
                    quantizer.nextPixel();
                }
            } else if (fs == decoder.fsMax) {
                for (; i < end; i++) {
                    last += map(decoder.readDirect());
                    out.put((float) quantizer.toDouble(last));
                    quantizer.nextPixel();
                }
            } else {
                for (; i < end; i++) {
                    last += map(decoder.readRice(fs));
                    out.put((float) quantizer.toDouble(last));
                    quantizer.nextPixel();
                }
            }
        }
        decoder.finish();
    }

    private static void decodeDouble(ByteBuffer in, DoubleBuffer out, QuantizeOption quantize, RiceCompressOption option) {
        Decoder decoder = new Decoder(in, option, FS_BITS_FOR_INT, FS_MAX_FOR_INT);
        int last = decoder.firstInt();
        int length = out.limit();
        Quantizer quantizer = new Quantizer(quantize);
        for (int i = 0; i < length;) {
            int fs = decoder.readFs();
            int end = decoder.blockEnd(i, length);
            if (fs < 0) {
                for (; i < end; i++) {
                    out.put(quantizer.toDouble(last));
                    quantizer.nextPixel();
                }
            } else if (fs == decoder.fsMax) {
                for (; i < end; i++) {
                    last += map(decoder.readDirect());
                    out.put(quantizer.toDouble(last));
                    quantizer.nextPixel();
                }
            } else {
                for (; i < end; i++) {
                    last += map(decoder.readRice(fs));
                    out.put(quantizer.toDouble(last));
                    quantizer.nextPixel();
                }
            }
        }
        decoder.finish();
    }

    private static int map(int diff) {
        return (diff >>> 1) ^ -(diff & 1);
    }

    private static final class Decoder {

        private final ByteBuffer in;
        private final byte[] inArray;
        private final int inArrayOffset;
        private final int blockSize;
        private final int bBits;
        private final int fsBits;
        private final int fsMax;
        private long bits;
        private int nbits;
        private int inPosition;

        private Decoder(ByteBuffer _in, RiceCompressOption option, int _fsBits, int _fsMax) {
            in = _in;
            if (_in.hasArray()) {
                inArray = _in.array();
                inArrayOffset = _in.arrayOffset();
                inPosition = inArrayOffset + _in.position();
            } else {
                inArray = null;
                inArrayOffset = 0;
            }
            blockSize = option.getBlockSize();
            bBits = 1 << _fsBits;
            fsBits = _fsBits;
            fsMax = _fsMax;
        }

        private int firstByte() {
            int first = getByte();
            initBits();
            return first;
        }

        private int firstShort() {
            int first = getByte() << BITS_PER_BYTE | getByte();
            initBits();
            return first;
        }

        private int firstInt() {
            int first = getByte() << 24 | getByte() << 16 | getByte() << BITS_PER_BYTE | getByte();
            initBits();
            return first;
        }

        private void initBits() {
            bits = getByte();
            nbits = BITS_PER_BYTE;
        }

        private int blockEnd(int index, int length) {
            return Math.min(index + blockSize, length);
        }

        private int readFs() {
            nbits -= fsBits;
            while (nbits < 0) {
                bits = bits << BITS_PER_BYTE | getByte();
                nbits += BITS_PER_BYTE;
            }
            int fs = (int) ((bits >>> nbits) - 1L);
            bits &= (1L << nbits) - 1L;
            return fs;
        }

        private int readDirect() {
            int k = bBits - nbits;
            long diff = bits << k;
            for (k -= BITS_PER_BYTE; k >= 0; k -= BITS_PER_BYTE) {
                bits = getByte();
                diff |= bits << k;
            }
            if (nbits > 0) {
                bits = getByte();
                diff |= bits >>> -k;
                bits &= (1L << nbits) - 1L;
            } else {
                bits = 0;
            }
            return (int) diff;
        }

        private int readRice(int fs) {
            while (bits == 0) {
                nbits += BITS_PER_BYTE;
                bits = getByte();
            }
            int nzero = nbits - NONZERO_COUNT[(int) (bits & BYTE_MASK)];
            nbits -= nzero + 1;
            bits ^= 1L << nbits;
            nbits -= fs;
            while (nbits < 0) {
                bits = bits << BITS_PER_BYTE | getByte();
                nbits += BITS_PER_BYTE;
            }
            int diff = (int) (nzero << fs | bits >>> nbits);
            bits &= (1L << nbits) - 1L;
            return diff;
        }

        private void decodeShortArray(int last, short[] out, int offset, int length) {
            if (inArray == null) {
                decodeShort(this, last, out, offset, length);
                return;
            }

            byte[] input = inArray;
            int position = inPosition;
            long bitBuffer = bits;
            int bitCount = nbits;
            int block = blockSize;
            int fsWidth = fsBits;
            int directBits = bBits;
            int maxFs = fsMax;

            for (int i = 0; i < length;) {
                bitCount -= fsWidth;
                while (bitCount < 0) {
                    bitBuffer = bitBuffer << BITS_PER_BYTE | input[position++] & BYTE_MASK;
                    bitCount += BITS_PER_BYTE;
                }

                int fs = (int) ((bitBuffer >>> bitCount) - 1L);
                bitBuffer &= (1L << bitCount) - 1L;

                int end = Math.min(i + block, length);
                if (fs < 0) {
                    for (; i < end; i++) {
                        out[offset + i] = (short) last;
                    }
                } else if (fs == maxFs) {
                    for (; i < end; i++) {
                        int k = directBits - bitCount;
                        long diff = bitBuffer << k;
                        for (k -= BITS_PER_BYTE; k >= 0; k -= BITS_PER_BYTE) {
                            bitBuffer = input[position++] & BYTE_MASK;
                            diff |= bitBuffer << k;
                        }
                        if (bitCount > 0) {
                            bitBuffer = input[position++] & BYTE_MASK;
                            diff |= bitBuffer >>> -k;
                            bitBuffer &= (1L << bitCount) - 1L;
                        } else {
                            bitBuffer = 0;
                        }

                        last += map((int) diff);
                        out[offset + i] = (short) last;
                    }
                } else {
                    for (; i < end; i++) {
                        while (bitBuffer == 0) {
                            bitCount += BITS_PER_BYTE;
                            bitBuffer = input[position++] & BYTE_MASK;
                        }
                        int nzero = bitCount - NONZERO_COUNT[(int) (bitBuffer & BYTE_MASK)];
                        bitCount -= nzero + 1;
                        bitBuffer ^= 1L << bitCount;

                        bitCount -= fs;
                        while (bitCount < 0) {
                            bitBuffer = bitBuffer << BITS_PER_BYTE | input[position++] & BYTE_MASK;
                            bitCount += BITS_PER_BYTE;
                        }

                        int diff = (int) (nzero << fs | bitBuffer >>> bitCount);
                        bitBuffer &= (1L << bitCount) - 1L;

                        last += map(diff);
                        out[offset + i] = (short) last;
                    }
                }
            }

            inPosition = position;
            bits = bitBuffer;
            nbits = bitCount;
        }

        private int getByte() {
            if (inArray != null) {
                return inArray[inPosition++] & BYTE_MASK;
            }
            return in.get() & BYTE_MASK;
        }

        private void finish() {
            if (inArray != null) {
                in.position(inPosition - inArrayOffset);
            }
            if (in.limit() > in.position()) {
                LOG.warning("decompressing left over some extra bytes got: " + in.limit() + " but needed only " + in.position());
            }
        }
    }

    private static final class Quantizer {

        private final QuantizeOption option;
        private final boolean dither;
        private final boolean checkZero;
        private final boolean checkNull;
        private final boolean nullIsNaN;
        private final int nullIndicator;
        private final double bScale;
        private final double bZero;
        private final double nullValue;
        private int iseed;
        private int nextRandom;

        private Quantizer(QuantizeOption _option) {
            option = _option;
            dither = option.isDither() || option.isDither2();
            checkZero = option.isCheckZero() || option.isDither2();
            checkNull = option.isCheckNull();
            Integer bNull = option.getBNull();
            nullIndicator = bNull == null ? Integer.MIN_VALUE + 1 : bNull;
            nullValue = option.getNullValue();
            nullIsNaN = Double.isNaN(nullValue);
            bScale = option.getBScale();
            bZero = option.getBZero();
            if (dither) {
                initialize(option.getSeed() + option.getTileIndex());
            }
        }

        private void initialize(long seed) {
            iseed = (int) ((seed - 1) % RandomSequence.length());
            initI1();
        }

        private void initI1() {
            nextRandom = (int) (RandomSequence.get(iseed) * RANDOM_MULTIPLICATOR);
        }

        private double nextRandom() {
            return RandomSequence.get(nextRandom);
        }

        private void nextPixel() {
            if (!dither) {
                return;
            }
            nextRandom++;
            if (nextRandom >= RandomSequence.length()) {
                iseed++;
                if (iseed >= RandomSequence.length()) {
                    iseed = 0;
                }
                initI1();
            }
        }

        private double toDouble(int pixel) {
            if (checkNull && pixel == nullIndicator) {
                return nullIsNaN ? Double.NaN : nullValue;
            }
            if (checkZero && pixel == ZERO_VALUE) {
                return 0.;
            }
            if (dither) {
                return (pixel - nextRandom() + .5) * bScale + bZero;
            }
            return (pixel + .5) * bScale + bZero;
        }
    }
}
