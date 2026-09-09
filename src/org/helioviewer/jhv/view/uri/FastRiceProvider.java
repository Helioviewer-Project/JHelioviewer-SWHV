package org.helioviewer.jhv.view.uri;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Logger;

import nom.tam.fits.compression.algorithm.api.ICompressOption;
import nom.tam.fits.compression.algorithm.api.ICompressorControl;
import nom.tam.fits.compression.algorithm.rice.RiceCompressOption;
import nom.tam.fits.compression.algorithm.rice.RiceCompressor;
import nom.tam.fits.compression.provider.api.ICompressorProvider;
import nom.tam.fits.header.Compression;

public final class FastRiceProvider implements ICompressorProvider {

    private static final int BITS_PER_BYTE = 8;
    private static final int BYTE_MASK = 0xff;
    private static final int FS_BITS_FOR_SHORT = 4;
    private static final int FS_MAX_FOR_SHORT = 14;
    private static final Logger LOG = Logger.getLogger(FastRiceProvider.class.getName());

    public FastRiceProvider() {}

    @Override
    public ICompressorControl createCompressorControl(String quantAlgorithm, String compressionAlgorithm, Class<?> baseType) {
        if (!Compression.ZCMPTYPE_RICE_1.equalsIgnoreCase(compressionAlgorithm) &&
                !Compression.ZCMPTYPE_RICE_ONE.equalsIgnoreCase(compressionAlgorithm)) {
            return null;
        }

        return quantAlgorithm == null && baseType == short.class ? new Control() : null;
    }

    private static final class Control implements ICompressorControl {

        @Override
        public boolean compress(Buffer in, ByteBuffer out, ICompressOption option) {
            return false;
        }

        @Override
        public void decompress(ByteBuffer in, Buffer out, ICompressOption option) {
            RiceCompressOption rice = option.unwrap(RiceCompressOption.class);
            // Encoded width can differ from the short output type.
            if (rice.getBytePix() == Short.BYTES)
                decodeShort(in, (ShortBuffer) out, rice);
            else
                new RiceCompressor.ShortRiceCompressor(rice).decompress(in, (ShortBuffer) out);
        }

        @Override
        public ICompressOption option() {
            return new RiceCompressOption();
        }
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

        private void decodeShortArray(int last, short[] out, int offset, int length) {
            if (inArray == null) {
                decodeShort(this, last, out, offset, length);
                return;
            }

            byte[] input = inArray;
            int position = inPosition;
            int limit = inArrayOffset + in.limit();
            long bitBuffer = bits;
            int bitCount = nbits;

            for (int i = 0; i < length; ) {
                bitCount -= fsBits;
                while (bitCount < 0) {
                    bitBuffer = bitBuffer << BITS_PER_BYTE | input[position++] & BYTE_MASK;
                    bitCount += BITS_PER_BYTE;
                }

                int fs = (int) ((bitBuffer >>> bitCount) - 1L);
                bitBuffer &= (1L << bitCount) - 1L;

                int end = Math.min(i + blockSize, length);
                if (fs < 0) {
                    for (; i < end; i++) {
                        out[offset + i] = (short) last;
                    }
                } else if (fs == fsMax) {
                    for (; i < end; i++) {
                        while (bitCount < bBits) {
                            bitBuffer = bitBuffer << BITS_PER_BYTE | input[position++] & BYTE_MASK;
                            bitCount += BITS_PER_BYTE;
                        }
                        bitCount -= bBits;
                        int diff = (int) (bitBuffer >>> bitCount);
                        bitBuffer &= (1L << bitCount) - 1L;

                        last += map(diff);
                        out[offset + i] = (short) last;
                    }
                } else {
                    for (; i < end; i++) {
                        // Refill ahead when four bytes remain, without crossing the input limit.
                        if (bitCount < Short.SIZE && position <= limit - Integer.BYTES) {
                            int word = (input[position] & BYTE_MASK) << 24 | (input[position + 1] & BYTE_MASK) << 16
                                    | (input[position + 2] & BYTE_MASK) << 8 | input[position + 3] & BYTE_MASK;
                            bitBuffer = bitBuffer << Integer.SIZE | Integer.toUnsignedLong(word);
                            position += Integer.BYTES;
                            bitCount += Integer.SIZE;
                        }
                        while (bitBuffer == 0) {
                            bitCount += BITS_PER_BYTE;
                            bitBuffer = input[position++] & BYTE_MASK;
                        }
                        int nzero = bitCount - (Long.SIZE - Long.numberOfLeadingZeros(bitBuffer));
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

            // Leave prefetched whole bytes unread, matching the byte-at-a-time decoder.
            inPosition = position - bitCount / BITS_PER_BYTE;
            bitBuffer >>>= bitCount / BITS_PER_BYTE * BITS_PER_BYTE;
            bitCount %= BITS_PER_BYTE;
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

}
