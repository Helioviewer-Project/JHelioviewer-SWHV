package org.helioviewer.jhv.view.uri;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Logger;

import nom.tam.fits.compression.algorithm.api.ICompressOption;
import nom.tam.fits.compression.algorithm.api.ICompressor;
import nom.tam.fits.compression.algorithm.api.ICompressorControl;
import nom.tam.fits.compression.algorithm.quant.QuantizeOption;
import nom.tam.fits.compression.algorithm.quant.QuantizeProcessor;
import nom.tam.fits.compression.algorithm.rice.RiceCompressOption;
import nom.tam.fits.compression.algorithm.rice.RiceCompressor;
import nom.tam.fits.compression.provider.api.ICompressorProvider;
import nom.tam.fits.header.Compression;

public final class FastRiceProvider implements ICompressorProvider {

    private static final int BITS_PER_BYTE = 8;
    private static final int BYTE_MASK = 0xff;
    private static final int FS_BITS_FOR_SHORT = 4;
    private static final int FS_MAX_FOR_SHORT = 14;
    private static final int FS_BITS_FOR_INT = 5;
    private static final int FS_MAX_FOR_INT = 25;
    private static final VarHandle LONG_WORD = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);
    private static final Logger LOG = Logger.getLogger(FastRiceProvider.class.getName());

    public FastRiceProvider() {}

    @Override
    public ICompressorControl createCompressorControl(String quantAlgorithm, String compressionAlgorithm, Class<?> baseType) {
        if (!Compression.ZCMPTYPE_RICE_1.equalsIgnoreCase(compressionAlgorithm) &&
                !Compression.ZCMPTYPE_RICE_ONE.equalsIgnoreCase(compressionAlgorithm)) {
            return null;
        }

        if (quantAlgorithm == null && (baseType == short.class || baseType == int.class))
            return new Control(baseType);
        if ((baseType == float.class || baseType == double.class) &&
                (Compression.ZQUANTIZ_NO_DITHER.equalsIgnoreCase(quantAlgorithm) ||
                        Compression.ZQUANTIZ_SUBTRACTIVE_DITHER_1.equalsIgnoreCase(quantAlgorithm) ||
                        Compression.ZQUANTIZ_SUBTRACTIVE_DITHER_2.equalsIgnoreCase(quantAlgorithm)))
            return new Control(baseType);
        return null;
    }

    private static final class Control implements ICompressorControl {

        private final Class<?> baseType;

        private Control(Class<?> _baseType) {
            baseType = _baseType;
        }

        @Override
        public boolean compress(Buffer in, ByteBuffer out, ICompressOption option) {
            return false;
        }

        @Override
        public void decompress(ByteBuffer in, Buffer out, ICompressOption option) {
            RiceCompressOption rice = option.unwrap(RiceCompressOption.class);
            if (baseType == short.class) {
                // Encoded width can differ from the output type.
                if (rice.getBytePix() == Short.BYTES)
                    decodeShort(in, (ShortBuffer) out, rice);
                else
                    new RiceCompressor.ShortRiceCompressor(rice).decompress(in, (ShortBuffer) out);
            } else {
                IntCompressor decoder = new IntCompressor(rice);
                if (baseType == int.class)
                    decoder.decompress(in, (IntBuffer) out);
                else if (baseType == float.class)
                    new QuantizeProcessor.FloatQuantCompressor(option.unwrap(QuantizeOption.class), decoder).decompress(in, (FloatBuffer) out);
                else
                    new QuantizeProcessor.DoubleQuantCompressor(option.unwrap(QuantizeOption.class), decoder).decompress(in, (DoubleBuffer) out);
            }
        }

        @Override
        public ICompressOption option() {
            RiceCompressOption rice = new RiceCompressOption();
            return baseType == float.class || baseType == double.class ? new QuantizeOption(rice) : rice;
        }
    }

    private static final class IntCompressor implements ICompressor<IntBuffer> {

        private final RiceCompressOption option;

        private IntCompressor(RiceCompressOption _option) {
            option = _option;
        }

        @Override
        public boolean compress(IntBuffer in, ByteBuffer out) {
            return false;
        }

        @Override
        public void decompress(ByteBuffer in, IntBuffer out) {
            if (option.getBytePix() != Integer.BYTES || !in.hasArray() || !out.hasArray()) {
                new RiceCompressor.IntRiceCompressor(option).decompress(in, out);
                return;
            }
            Decoder decoder = new Decoder(in, option, FS_BITS_FOR_INT, FS_MAX_FOR_INT);
            int last = decoder.firstInt();
            decoder.decodeArray(last, out.array(), out.arrayOffset() + out.position(), out.limit());
            out.position(out.limit());
            decoder.finish();
        }
    }

    private static void decodeShort(ByteBuffer in, ShortBuffer out, RiceCompressOption option) {
        Decoder decoder = new Decoder(in, option, FS_BITS_FOR_SHORT, FS_MAX_FOR_SHORT);
        int last = decoder.firstShort();
        int length = out.limit();
        if (out.hasArray()) {
            short[] data = out.array();
            int offset = out.arrayOffset() + out.position();
            if (in.hasArray())
                decoder.decodeArray(last, data, offset, length);
            else
                decodeShort(decoder, last, data, offset, length);
            out.position(length);
        } else {
            decodeShort(decoder, last, out, length);
        }
        decoder.finish();
    }

    private static void decodeShort(Decoder decoder, int last, short[] out, int offset, int length) {
        for (int i = 0; i < length; ) {
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
        for (int i = 0; i < length; ) {
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

        private int firstShort() {
            int first = getByte() << BITS_PER_BYTE | getByte();
            initBits();
            return first;
        }

        private int firstInt() {
            int first = getByte() << 24 | getByte() << 16 | getByte() << 8 | getByte();
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
            int nzero = nbits - (32 - Integer.numberOfLeadingZeros((int) (bits & BYTE_MASK)));
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

        private void decodeArray(int last, Object out, int offset, int length) {
            short[] shortOutput = out instanceof short[] ? (short[]) out : null;
            int[] intOutput = shortOutput == null ? (int[]) out : null;
            byte[] input = inArray;
            int position = inPosition;
            int limit = inArrayOffset + in.limit();
            long bitBuffer = bits << (Long.SIZE - nbits);
            int bitCount = nbits;

            for (int i = 0; i < length; ) {
                while (bitCount < fsBits) {
                    bitBuffer |= (long) (input[position++] & BYTE_MASK) << (Long.SIZE - bitCount - BITS_PER_BYTE);
                    bitCount += BITS_PER_BYTE;
                }
                int fs = (int) (bitBuffer >>> (Long.SIZE - fsBits)) - 1;
                bitBuffer <<= fsBits;
                bitCount -= fsBits;

                int end = Math.min(i + blockSize, length);
                if (fs < 0) {
                    for (; i < end; i++) {
                        if (shortOutput != null)
                            shortOutput[offset + i] = (short) last;
                        else
                            intOutput[offset + i] = last;
                    }
                } else if (fs == fsMax) {
                    for (; i < end; i++) {
                        while (bitCount < bBits) {
                            bitBuffer |= (long) (input[position++] & BYTE_MASK) << (Long.SIZE - bitCount - BITS_PER_BYTE);
                            bitCount += BITS_PER_BYTE;
                        }
                        int diff = (int) (bitBuffer >>> (Long.SIZE - bBits));
                        bitBuffer <<= bBits;
                        bitCount -= bBits;
                        last += map(diff);
                        if (shortOutput != null)
                            shortOutput[offset + i] = (short) last;
                        else
                            intOutput[offset + i] = last;
                    }
                } else {
                    int remainderMask = (1 << fs) - 1;
                    for (; i < end; i++) {
                        if (bitCount < bBits && position <= limit - Long.BYTES) {
                            // Keep at most 63 counted bits so consuming a code never shifts by 64.
                            int bytes = (Long.SIZE - 1 - bitCount) / BITS_PER_BYTE;
                            bitBuffer |= (long) LONG_WORD.get(input, position) >>> bitCount;
                            position += bytes;
                            bitCount += bytes * BITS_PER_BYTE;
                        } else {
                            while (bitCount < bBits && position < limit) {
                                bitBuffer |= (long) (input[position++] & BYTE_MASK) << (Long.SIZE - bitCount - BITS_PER_BYTE);
                                bitCount += BITS_PER_BYTE;
                            }
                        }

                        int nzero = Long.numberOfLeadingZeros(bitBuffer);
                        int skipped = 0;
                        // The word load can look into the next byte without consuming it.
                        while (nzero >= bitCount) {
                            skipped += bitCount;
                            bitBuffer = (long) (input[position++] & BYTE_MASK) << (Long.SIZE - BITS_PER_BYTE);
                            bitCount = BITS_PER_BYTE;
                            nzero = Long.numberOfLeadingZeros(bitBuffer);
                        }
                        bitBuffer <<= nzero + 1;
                        bitCount -= nzero + 1;

                        while (bitCount < fs) {
                            bitBuffer |= (long) (input[position++] & BYTE_MASK) << (Long.SIZE - bitCount - BITS_PER_BYTE);
                            bitCount += BITS_PER_BYTE;
                        }
                        int diff = (skipped + nzero) << fs | ((int) (bitBuffer >>> (Long.SIZE - fs)) & remainderMask);
                        bitBuffer <<= fs;
                        bitCount -= fs;

                        last += map(diff);
                        if (shortOutput != null)
                            shortOutput[offset + i] = (short) last;
                        else
                            intOutput[offset + i] = last;
                    }
                }
            }

            // Leave prefetched whole bytes unread, matching the byte-at-a-time decoder.
            inPosition = position - bitCount / BITS_PER_BYTE;
            nbits = bitCount % BITS_PER_BYTE;
            bits = nbits == 0 ? 0 : bitBuffer >>> (Long.SIZE - nbits);
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

}
