import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.ImageProcessingSettings;
import org.helioviewer.jhv.view.uri.FITSImage;
import org.helioviewer.jhv.view.uri.FastRiceProvider;

import nom.tam.fits.compression.algorithm.api.ICompressorControl;
import nom.tam.fits.compression.algorithm.rice.RiceCompressOption;
import nom.tam.fits.compression.algorithm.rice.RiceCompressor;
import nom.tam.fits.compression.algorithm.rice.RiceQuantizeCompressOption;
import nom.tam.fits.compression.provider.CompressorProvider;
import nom.tam.fits.header.Compression;
import nom.tam.util.type.PrimitiveTypes;

public final class FastRiceVerifier {

    private static final List<String> failures = new ArrayList<>();

    private static final Random RND = new Random(0x5a17);

    private FastRiceVerifier() {}

    public static void main(String[] args) throws Exception {
        Path nomTamRoot = args.length == 0 ? Path.of(System.getProperty("user.home"), "git", "nom-tam-fits") : Path.of(args[0]);
        Path resources = nomTamRoot.resolve("src/test/resources/nom/tam/image/comp");
        if (!Files.isDirectory(resources))
            throw new IllegalArgumentException("nom-tam-fits test resources not found: " + resources);

        System.out.println("nom-tam classes: " + RiceCompressor.class.getProtectionDomain().getCodeSource().getLocation());
        run("service provider", FastRiceVerifier::verifyServiceLoader);
        run("byte fixture", () -> verifyByteFixture(resources));
        run("short fixture", () -> verifyShortFixture(resources));
        run("int fixture", () -> verifyIntFixture(resources));
        run("float fixture", () -> verifyFloatFixture(resources));
        run("double fixture", () -> verifyDoubleFixture(resources));
        run("synthetic integers", FastRiceVerifier::verifySyntheticIntegerCases);
        run("short refill boundaries", FastRiceVerifier::verifyShortRefillBoundaries);
        run("int refill boundaries", FastRiceVerifier::verifyIntRefillBoundaries);
        run("constructed short tiles", () -> verifyConstructedRice(Short.BYTES));
        run("constructed int tiles", () -> verifyConstructedRice(Integer.BYTES));
        verifySyntheticQuantizedCases();
        verifyEncodedWidths();
        verifyKnownQuantizedValues();
        Path provided = nomTamRoot.resolve("src/test/resources/nom/tam/image/provided");
        for (String name : List.of("m13_rice.fits", "m13_gzip.fits", "m13_plio.fits"))
            run("FITS loader " + name, () -> verifyImage(provided.resolve("m13.fits"), provided.resolve(name)));

        if (!failures.isEmpty())
            throw new AssertionError(failures.size() + " failed checks: " + String.join(", ", failures));
        System.out.println("OK FastRiceVerifier");
    }

    private static void verifyServiceLoader() {
        for (Class<?> type : List.of(short.class, int.class, float.class, double.class)) {
            String quantization = type == float.class || type == double.class ? Compression.ZQUANTIZ_SUBTRACTIVE_DITHER_1 : null;
            ICompressorControl control = control(type, quantization);
            if (!control.getClass().getName().contains("FastRiceProvider"))
                throw new AssertionError("FastRiceProvider is not active for " + type.getName());
        }
        FastRiceProvider provider = new FastRiceProvider();
        if (provider.createCompressorControl(null, Compression.ZCMPTYPE_RICE_1, byte.class) != null)
            throw new AssertionError("FastRice must delegate byte output");
        if (provider.createCompressorControl("unknown", Compression.ZCMPTYPE_RICE_1, float.class) != null)
            throw new AssertionError("FastRice must delegate unknown quantization");
    }

    private interface Check { void run() throws Exception; }

    private static void run(String name, Check check) {
        try {
            check.run();
        } catch (Exception | AssertionError e) {
            failures.add(name);
            System.err.println("FAIL " + name + ": " + e);
        }
    }

    private static void verifyByteFixture(Path resources) throws Exception {
        byte[] expected = readBare(resources, "test100Data8.bin");
        byte[] compressed = readRise(resources, "test100Data8.rise");

        ByteBuffer out = ByteBuffer.allocate(expected.length);
        control(byte.class).decompress(ByteBuffer.wrap(compressed), out, riceOption(PrimitiveTypes.BYTE.size(), 32));
        assertArrayEquals("nom-tam byte fixture", expected, out.array());
    }

    private static void verifyShortFixture(Path resources) throws Exception {
        short[] expected = toShorts(readBare(resources, "test100Data16.bin"));
        byte[] compressed = readRise(resources, "test100Data16.rise");
        RiceCompressOption option = riceOption(PrimitiveTypes.SHORT.size(), 32);

        ShortBuffer heapOut = ShortBuffer.allocate(expected.length);
        control(short.class).decompress(ByteBuffer.wrap(compressed), heapOut, option);
        assertArrayEquals("nom-tam short fixture heap", expected, heapOut.array());

        ShortBuffer directOut = ByteBuffer.allocateDirect(expected.length * Short.BYTES).order(ByteOrder.BIG_ENDIAN).asShortBuffer();
        control(short.class).decompress(ByteBuffer.wrap(compressed), directOut, option);
        directOut.flip();
        short[] actual = new short[expected.length];
        directOut.get(actual);
        assertArrayEquals("nom-tam short fixture direct", expected, actual);
    }

    private static void verifyIntFixture(Path resources) throws Exception {
        int[] expected = toInts(readBare(resources, "test100Data32.bin"));
        byte[] compressed = readRise(resources, "test100Data32.rise");

        IntBuffer out = IntBuffer.allocate(expected.length);
        control(int.class).decompress(ByteBuffer.wrap(compressed), out, riceOption(PrimitiveTypes.INT.size(), 32));
        assertArrayEquals("nom-tam int fixture", expected, out.array());
    }

    private static void verifyFloatFixture(Path resources) throws Exception {
        float[] input = toFloats(readBare(resources, "test100Data-32.bin"));
        RiceQuantizeCompressOption option = quantizeOption(input.length);
        RiceCompressor.FloatRiceCompressor compressor = new RiceCompressor.FloatRiceCompressor(option);

        ByteBuffer compressed = ByteBuffer.allocate(input.length * Float.BYTES);
        requireCompressed(compressor.compress(FloatBuffer.wrap(input), compressed));
        compressed.flip();

        FloatBuffer nomTam = FloatBuffer.allocate(input.length);
        new RiceCompressor.FloatRiceCompressor(option).decompress(compressed.duplicate(), nomTam);

        FloatBuffer fast = FloatBuffer.allocate(input.length);
        control(float.class, Compression.ZQUANTIZ_SUBTRACTIVE_DITHER_1).decompress(compressed.duplicate(), fast, option);
        assertArrayEquals("nom-tam float fixture", nomTam.array(), fast.array());
    }

    private static void verifyDoubleFixture(Path resources) throws Exception {
        double[] input = toDoubles(readBare(resources, "test100Data-64.bin"));
        RiceQuantizeCompressOption option = quantizeOption(input.length);
        RiceCompressor.DoubleRiceCompressor compressor = new RiceCompressor.DoubleRiceCompressor(option);

        ByteBuffer compressed = ByteBuffer.allocate(input.length * Double.BYTES);
        requireCompressed(compressor.compress(DoubleBuffer.wrap(input), compressed));
        compressed.flip();

        DoubleBuffer nomTam = DoubleBuffer.allocate(input.length);
        new RiceCompressor.DoubleRiceCompressor(option).decompress(compressed.duplicate(), nomTam);

        DoubleBuffer fast = DoubleBuffer.allocate(input.length);
        control(double.class, Compression.ZQUANTIZ_SUBTRACTIVE_DITHER_1).decompress(compressed.duplicate(), fast, option);
        assertArrayEquals("nom-tam double fixture", nomTam.array(), fast.array());
    }

    private static void verifySyntheticIntegerCases() {
        int cases = 0;
        for (int block : new int[]{16, 32}) {
            for (int length : new int[]{1, 2, 15, 16, 17, 31, 32, 33, 127, 128, 129, 3040}) {
                for (int kind = 0; kind < 3; kind++) {
                    verifyByteSynthetic(block, byteData(length, kind));
                    verifyShortSynthetic(block, shortData(length, kind), true);
                    verifyShortSynthetic(block, shortData(length, kind), false);
                    verifyIntSynthetic(block, intData(length, kind));
                    cases += 4;
                }
            }
        }
        System.out.println("synthetic integer cases=" + cases);
    }

    private static void verifyByteSynthetic(int block, byte[] input) {
        RiceCompressOption option = riceOption(PrimitiveTypes.BYTE.size(), block);
        ByteBuffer compressed = ByteBuffer.allocate(input.length * 8 + 128);
        new RiceCompressor.ByteRiceCompressor(option).compress(ByteBuffer.wrap(input), compressed);
        compressed.flip();

        ByteBuffer nomTam = ByteBuffer.allocate(input.length);
        new RiceCompressor.ByteRiceCompressor(option).decompress(compressed.duplicate(), nomTam);

        ByteBuffer fast = ByteBuffer.allocate(input.length);
        control(byte.class).decompress(compressed.duplicate(), fast, option);
        assertArrayEquals("synthetic byte", nomTam.array(), fast.array());
    }

    private static void verifyShortSynthetic(int block, short[] input, boolean heapOutput) {
        RiceCompressOption option = riceOption(PrimitiveTypes.SHORT.size(), block);
        ByteBuffer compressed = ByteBuffer.allocate(input.length * 8 + 128);
        new RiceCompressor.ShortRiceCompressor(option).compress(ShortBuffer.wrap(input), compressed);
        compressed.flip();

        ShortBuffer nomTam = ShortBuffer.allocate(input.length);
        new RiceCompressor.ShortRiceCompressor(option).decompress(compressed.duplicate(), nomTam);

        ShortBuffer fast = heapOutput ? ShortBuffer.allocate(input.length)
                : ByteBuffer.allocateDirect(input.length * Short.BYTES).order(ByteOrder.BIG_ENDIAN).asShortBuffer();
        control(short.class).decompress(compressed.duplicate(), fast, option);
        fast.flip();
        short[] actual = new short[input.length];
        fast.get(actual);
        assertArrayEquals("synthetic short", nomTam.array(), actual);
    }

    private static void verifyShortRefillBoundaries() {
        Random random = new Random(0x71ce);
        for (int block : new int[]{16, 32}) {
            for (int length = 1; length <= 256; length++) {
                short[] expected = new short[length];
                for (int i = 0; i < length; i++) {
                    // Alternate constant, Rice-coded, and direct-coded blocks.
                    expected[i] = switch (i / block % 3) {
                        case 0 -> 123;
                        case 1 -> (short) (123 + random.nextInt(16));
                        default -> (i & 1) == 0 ? 0 : Short.MAX_VALUE;
                    };
                }
                RiceCompressOption option = riceOption(Short.BYTES, block);
                ByteBuffer compressed = ByteBuffer.allocate(length * 4 + 128);
                requireCompressed(new RiceCompressor.ShortRiceCompressor(option).compress(ShortBuffer.wrap(expected), compressed));
                compressed.flip();
                ShortBuffer reference = ShortBuffer.allocate(length);
                new RiceCompressor.ShortRiceCompressor(option).decompress(compressed.duplicate(), reference);
                assertArrayEquals("short refill upstream reference", expected, reference.array());

                // Nonzero array offset and position, with inaccessible padding after the limit.
                ByteBuffer padded = ByteBuffer.allocate(compressed.remaining() + 32);
                padded.position(7);
                ByteBuffer input = padded.slice();
                input.position(3);
                input.put(compressed);
                input.limit(input.position());
                input.position(3);
                ShortBuffer output = ShortBuffer.allocate(length);
                control(short.class).decompress(input, output, option);
                assertArrayEquals("short refill block=" + block + " length=" + length, expected, output.array());
                if (input.position() != input.limit() || output.position() != length)
                    throw new AssertionError("incorrect buffer position after short refill");
            }
        }

        // Encoded directly so the compressor cannot choose a different block code.
        for (int zeros : new int[]{0, 1, 7, 8, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 127, 128, 511, 8191, 65535}) {
            int length = (22 + zeros + 7) / 8;
            ByteBuffer input = ByteBuffer.allocate(length + 32);
            input.put(2, (byte) 0x10); // First value 0, fs = 0.
            for (int bit : new int[]{20 + zeros, 21 + zeros}) {
                int index = bit / 8;
                input.put(index, (byte) (input.get(index) | 1 << (7 - bit % 8)));
            }
            input.limit(length);
            ShortBuffer output = ShortBuffer.allocate(2);
            control(short.class).decompress(input, output, riceOption(Short.BYTES, 32));
            short value = (short) ((zeros & 1) == 0 ? zeros / 2 : -(zeros + 1) / 2);
            assertArrayEquals("short unary zeros=" + zeros, new short[]{value, value}, output.array());
            if (input.position() != input.limit() || output.position() != 2)
                throw new AssertionError("incorrect buffer position after short unary run");
        }
    }

    private static void verifyIntRefillBoundaries() {
        Random random = new Random(0x71ce);
        for (int block : new int[]{16, 32}) {
            for (int length = 1; length <= 256; length++) {
                int[] expected = new int[length];
                for (int i = 0; i < length; i++) {
                    // Alternate constant, Rice-coded, and direct-coded blocks.
                    expected[i] = switch (i / block % 3) {
                        case 0 -> 123;
                        case 1 -> 123 + random.nextInt(16);
                        default -> (i & 1) == 0 ? 0 : Integer.MAX_VALUE;
                    };
                }
                RiceCompressOption option = riceOption(Integer.BYTES, block);
                ByteBuffer compressed = ByteBuffer.allocate(length * 8 + 128);
                requireCompressed(new RiceCompressor.IntRiceCompressor(option).compress(IntBuffer.wrap(expected), compressed));
                compressed.flip();
                IntBuffer reference = IntBuffer.allocate(length);
                new RiceCompressor.IntRiceCompressor(option).decompress(compressed.duplicate(), reference);
                assertArrayEquals("int refill upstream reference", expected, reference.array());

                // Nonzero array offset and position, with inaccessible padding after the limit.
                ByteBuffer padded = ByteBuffer.allocate(compressed.remaining() + 32);
                padded.position(7);
                ByteBuffer input = padded.slice();
                input.position(3);
                input.put(compressed);
                input.limit(input.position());
                input.position(3);
                IntBuffer output = IntBuffer.allocate(length + 5).position(5).slice();
                control(int.class).decompress(input, output, option);
                assertArrayEquals("int refill block=" + block + " length=" + length, expected,
                        Arrays.copyOfRange(output.array(), output.arrayOffset(), output.arrayOffset() + length));
                if (input.position() != input.limit() || output.position() != length)
                    throw new AssertionError("incorrect buffer position after int refill");
            }
        }

        // Encoded directly so the compressor cannot choose a different block code.
        for (int zeros : new int[]{0, 1, 7, 8, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 127, 128, 511, 8191, 65535}) {
            int length = (39 + zeros + 7) / 8;
            ByteBuffer input = ByteBuffer.allocate(length + 32);
            input.put(4, (byte) 0x08); // First value 0, fs = 0.
            for (int bit : new int[]{37 + zeros, 38 + zeros}) {
                int index = bit / 8;
                input.put(index, (byte) (input.get(index) | 1 << (7 - bit % 8)));
            }
            input.limit(length);
            IntBuffer output = IntBuffer.allocate(2);
            control(int.class).decompress(input, output, riceOption(Integer.BYTES, 32));
            int value = (zeros & 1) == 0 ? zeros / 2 : -(zeros + 1) / 2;
            assertArrayEquals("int unary zeros=" + zeros, new int[]{value, value}, output.array());
            if (input.position() != input.limit() || output.position() != 2)
                throw new AssertionError("incorrect buffer position after int unary run");
        }
    }

    private static void verifyConstructedRice(int bytePix) {
        Random random = new Random(6843);
        int bits = bytePix * Byte.SIZE;
        int fsBits = bytePix == Short.BYTES ? 4 : 5;
        int fsMax = bytePix == Short.BYTES ? 14 : 25;
        int[] zeroRuns = {0, 1, 7, 8, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 127, 128, 511, 8191, 65535};
        ICompressorControl fast = control(bytePix == Short.BYTES ? short.class : int.class);
        for (int test = 0; test < 2000; test++) {
            int block = (test & 1) == 0 ? 16 : 32;
            int length = 1 + random.nextInt(700);
            int[] expected = new int[length];
            BitWriter writer = new BitWriter();
            int last = bytePix == Short.BYTES ? (short) random.nextInt() : random.nextInt();
            writer.write(last, bits);
            for (int i = 0; i < length; ) {
                // Visit every code and mix constant, direct, and Rice blocks in the same tile.
                int fs = (test + i / block) % (fsMax + 2) - 1;
                writer.write(fs + 1, fsBits);
                int end = Math.min(i + block, length);
                for (; i < end; i++) {
                    int diff = 0;
                    if (fs == fsMax) {
                        diff = bytePix == Short.BYTES ? random.nextInt(1 << Short.SIZE) : random.nextInt();
                        writer.write(diff, bits);
                    } else if (fs >= 0) {
                        int zeros = zeroRuns[(test + i) % zeroRuns.length] >>> fs;
                        int remainder = random.nextInt() & ((1 << fs) - 1);
                        for (int j = 0; j < zeros; j++)
                            writer.write(0, 1);
                        writer.write(1, 1);
                        writer.write(remainder, fs);
                        diff = zeros << fs | remainder;
                    }
                    // Compute expected signed differences without using either decoder.
                    last += (diff & 1) == 0 ? diff >>> 1 : ~(diff >>> 1);
                    expected[i] = bytePix == Short.BYTES ? (short) last : last;
                }
            }
            byte[] compressed = writer.finish();
            RiceCompressOption option = riceOption(bytePix, block);
            ByteBuffer referenceInput = ByteBuffer.wrap(compressed);
            Buffer reference = bytePix == Short.BYTES ? ShortBuffer.allocate(length) : IntBuffer.allocate(length);
            if (reference instanceof ShortBuffer shorts)
                new RiceCompressor.ShortRiceCompressor(option).decompress(referenceInput, shorts);
            else
                new RiceCompressor.IntRiceCompressor(option).decompress(referenceInput, (IntBuffer) reference);

            ByteBuffer storage = ByteBuffer.allocate(compressed.length + 32);
            Arrays.fill(storage.array(), (byte) 0xa5);
            ByteBuffer input = storage.position(7).slice();
            input.position(3).put(compressed).flip().position(3);
            Buffer output;
            if (bytePix == Short.BYTES) {
                ShortBuffer backing = ShortBuffer.allocate(length + 12);
                Arrays.fill(backing.array(), (short) 0x5a5a);
                output = backing.position(5).limit(5 + length).slice();
            } else {
                IntBuffer backing = IntBuffer.allocate(length + 12);
                Arrays.fill(backing.array(), 0x5a5a);
                output = backing.position(5).limit(5 + length).slice();
            }
            fast.decompress(input, output, option);
            for (int i = 0; i < length; i++) {
                int upstream = reference instanceof ShortBuffer shorts ? shorts.get(i) : ((IntBuffer) reference).get(i);
                int actual = output instanceof ShortBuffer shorts ? shorts.get(i) : ((IntBuffer) output).get(i);
                if (upstream != expected[i] || actual != expected[i])
                    throw new AssertionError("constructed BYTEPIX=" + bytePix + " tile=" + test + " pixel=" + i);
            }
            for (int i = 0; i < length + 12; i++) {
                int actual = output instanceof ShortBuffer shorts ? shorts.array()[i] : ((IntBuffer) output).array()[i];
                if ((i < 5 || i >= 5 + length) && actual != 0x5a5a)
                    throw new AssertionError("overwritten output guard in constructed tile " + test);
            }
            if (input.position() - 3 != compressed.length || referenceInput.position() != compressed.length || output.position() != length)
                throw new AssertionError("incorrect buffer position in constructed tile " + test);
        }
        System.out.println("constructed BYTEPIX=" + bytePix + " tiles=2000");
    }

    private static final class BitWriter {

        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private int bits;
        private int count;

        private void write(int value, int length) {
            for (int i = length - 1; i >= 0; i--) {
                bits = bits << 1 | (value >>> i & 1);
                if (++count == Byte.SIZE) {
                    bytes.write(bits);
                    count = 0;
                }
            }
        }

        private byte[] finish() {
            if (count > 0)
                bytes.write(bits << (Byte.SIZE - count));
            return bytes.toByteArray();
        }
    }

    private static void verifyIntSynthetic(int block, int[] input) {
        RiceCompressOption option = riceOption(PrimitiveTypes.INT.size(), block);
        ByteBuffer compressed = ByteBuffer.allocate(input.length * 8 + 128);
        new RiceCompressor.IntRiceCompressor(option).compress(IntBuffer.wrap(input), compressed);
        compressed.flip();

        IntBuffer nomTam = IntBuffer.allocate(input.length);
        new RiceCompressor.IntRiceCompressor(option).decompress(compressed.duplicate(), nomTam);

        IntBuffer fast = IntBuffer.allocate(input.length);
        control(int.class).decompress(compressed.duplicate(), fast, option);
        assertArrayEquals("synthetic int", nomTam.array(), fast.array());
    }

    private static void verifySyntheticQuantizedCases() {
        int cases = 0;
        for (int length : new int[]{32, 33, 127, 256}) {
            for (boolean dither : new boolean[]{false, true}) {
                run("synthetic float length=" + length + " dither=" + dither, () -> verifyFloatSynthetic(length, dither));
                run("synthetic double length=" + length + " dither=" + dither, () -> verifyDoubleSynthetic(length, dither));
                cases += 2;
            }
        }
        System.out.println("synthetic quantized cases=" + cases);
    }

    private static void verifyFloatSynthetic(int length, boolean dither) {
        RiceQuantizeCompressOption option = quantizeOption(length);
        option.setDither(dither);
        RiceCompressor.FloatRiceCompressor compressor = new RiceCompressor.FloatRiceCompressor(option);
        ByteBuffer compressed = ByteBuffer.allocate(length * 16 + 512);
        requireCompressed(compressor.compress(FloatBuffer.wrap(floatData(length)), compressed));
        compressed.flip();

        FloatBuffer nomTam = FloatBuffer.allocate(length);
        new RiceCompressor.FloatRiceCompressor(option).decompress(compressed.duplicate(), nomTam);

        FloatBuffer fast = FloatBuffer.allocate(length);
        control(float.class, Compression.ZQUANTIZ_SUBTRACTIVE_DITHER_1).decompress(compressed.duplicate(), fast, option);
        assertArrayEquals("synthetic float", nomTam.array(), fast.array());
    }

    private static void verifyDoubleSynthetic(int length, boolean dither) {
        RiceQuantizeCompressOption option = quantizeOption(length);
        option.setDither(dither);
        RiceCompressor.DoubleRiceCompressor compressor = new RiceCompressor.DoubleRiceCompressor(option);
        ByteBuffer compressed = ByteBuffer.allocate(length * 16 + 512);
        requireCompressed(compressor.compress(DoubleBuffer.wrap(doubleData(length)), compressed));
        compressed.flip();

        DoubleBuffer nomTam = DoubleBuffer.allocate(length);
        new RiceCompressor.DoubleRiceCompressor(option).decompress(compressed.duplicate(), nomTam);

        DoubleBuffer fast = DoubleBuffer.allocate(length);
        control(double.class, Compression.ZQUANTIZ_SUBTRACTIVE_DITHER_1).decompress(compressed.duplicate(), fast, option);
        assertArrayEquals("synthetic double", nomTam.array(), fast.array());
    }

    private static void requireCompressed(boolean compressed) {
        if (!compressed)
            throw new AssertionError("upstream compression declined the input");
    }

    private static void verifyEncodedWidths() {
        for (int bytePix : new int[]{1, 2, 4}) {
            for (Class<?> type : List.of(byte.class, short.class, int.class)) {
                for (boolean directInput : new boolean[]{false, true}) {
                    for (boolean directOutput : new boolean[]{false, true})
                        run("BYTEPIX=" + bytePix + " output=" + type.getName() + " directInput=" + directInput + " directOutput=" + directOutput,
                                () -> verifyEncodedWidth(bytePix, type, directInput, directOutput));
                }
            }
        }
    }

    private static void verifyEncodedWidth(int bytePix, Class<?> type, boolean directInput, boolean directOutput) {
        // All values fit every encoding width. Width is a property of the stream, not the output buffer.
        int[] values = new int[65];
        for (int i = 0; i < values.length; i++)
            values[i] = (i * 17) % 127;
        RiceCompressOption option = riceOption(bytePix, 32);
        ByteBuffer encoded = ByteBuffer.allocate(2048);
        requireCompressed(new RiceCompressor.IntRiceCompressor(option).compress(IntBuffer.wrap(values), encoded));
        encoded.flip();
        ByteBuffer storage = directInput ? ByteBuffer.allocateDirect(encoded.remaining() + 7) : ByteBuffer.allocate(encoded.remaining() + 7);
        storage.position(7);
        storage.put(encoded).flip().position(7);
        ByteBuffer input = storage.slice();
        Buffer decoded;
        if (directOutput) {
            ByteBuffer output = ByteBuffer.allocateDirect(values.length * Integer.BYTES);
            decoded = type == byte.class ? output.limit(values.length)
                    : type == short.class ? output.asShortBuffer().limit(values.length) : output.asIntBuffer();
        } else {
            decoded = type == byte.class ? ByteBuffer.allocate(values.length)
                    : type == short.class ? ShortBuffer.allocate(values.length) : IntBuffer.allocate(values.length);
        }
        if (input.hasArray() == directInput || decoded.hasArray() == directOutput)
            throw new AssertionError("incorrect heap/direct buffer setup");
        control(type).decompress(input, decoded, option);
        if (input.position() != input.limit() || decoded.position() != values.length)
            throw new AssertionError("incorrect buffer position after width test");
        for (int i = 0; i < values.length; i++) {
            int actual = decoded instanceof ByteBuffer bytes ? bytes.get(i)
                    : decoded instanceof ShortBuffer shorts ? shorts.get(i) : ((IntBuffer) decoded).get(i);
            if (actual != values[i])
                throw new AssertionError("pixel " + i + ": expected " + values[i] + ", got " + actual);
        }
    }

    private static void verifyKnownQuantizedValues() {
        // Pin the corrected reconstruction independently of the upstream floating-point decoder.
        // Seed 1 starts at the first Park-Miller random value, 16807 / 2147483647.
        double r0 = 16807.0 / 2147483647;
        double r1 = 282475249.0 / 2147483647;
        double r2 = 1622650073.0 / 2147483647;
        double r4 = 1144108930.0 / 2147483647;
        for (Class<?> type : List.of(float.class, double.class)) {
            for (boolean upstream : new boolean[]{false, true}) {
                String label = (upstream ? "upstream " : "JHV provider ") + type.getName();
                run(label + " no dither", () -> verifyKnownValues(type, upstream, false, false,
                        new int[]{0, 1, 2}, new double[]{10, 12, 14}));
                run(label + " fractional precision", () -> verifyKnownValues(type, upstream, true, false,
                        new int[]{0}, new double[]{10 + (0.5 - r0) * 2}));
                run(label + " dither2 zero marker", () -> verifyKnownValues(type, upstream, true, true,
                        new int[]{Integer.MIN_VALUE + 1, 0}, new double[]{0, 10 + (0.5 - r1) * 2}));
                run(label + " dither1 nulls", () -> verifyKnownValues(type, upstream, true, false,
                        new int[]{0, Integer.MIN_VALUE, 1}, new double[]{10 + (0.5 - r0) * 2, Double.NaN, 10 + (1.5 - r2) * 2}));
                run(label + " dither2 zeros/nulls", () -> verifyKnownValues(type, upstream, true, true,
                        new int[]{0, Integer.MIN_VALUE, 1, Integer.MIN_VALUE + 1, 2},
                        new double[]{10 + (0.5 - r0) * 2, Double.NaN, 10 + (1.5 - r2) * 2, 0, 10 + (2.5 - r4) * 2}));
            }
        }
    }

    private static void verifyKnownValues(Class<?> type, boolean upstream, boolean dither, boolean dither2, int[] values, double[] expected) {
        RiceQuantizeCompressOption option = quantizeOption(values.length);
        option.setSeed(1);
        option.setDither(dither);
        option.setDither2(dither2);
        option.setCheckNull(true);
        option.setBNull(Integer.MIN_VALUE);
        option.setBScale(2);
        option.setBZero(10);
        ByteBuffer compressed = ByteBuffer.allocate(1024);
        requireCompressed(new RiceCompressor.IntRiceCompressor(option.unwrap(RiceCompressOption.class)).compress(IntBuffer.wrap(values), compressed));
        compressed.flip();
        Buffer out = type == float.class ? FloatBuffer.allocate(values.length) : DoubleBuffer.allocate(values.length);
        if (upstream) {
            if (out instanceof FloatBuffer floats)
                new RiceCompressor.FloatRiceCompressor(option).decompress(compressed, floats);
            else
                new RiceCompressor.DoubleRiceCompressor(option).decompress(compressed, (DoubleBuffer) out);
        } else {
            control(type, dither2 ? Compression.ZQUANTIZ_SUBTRACTIVE_DITHER_2 : Compression.ZQUANTIZ_SUBTRACTIVE_DITHER_1).decompress(compressed, out, option);
        }
        for (int i = 0; i < values.length; i++) {
            double actual = out instanceof FloatBuffer floats ? floats.get(i) : ((DoubleBuffer) out).get(i);
            double wanted = type == float.class ? (float) expected[i] : expected[i];
            if (Double.doubleToLongBits(actual) != Double.doubleToLongBits(wanted))
                throw new AssertionError("pixel " + i + ": expected " + wanted + ", got " + actual);
        }
    }

    private static void verifyImage(Path reference, Path compressed) throws Exception {
        ImageBuffer expected = null, actual = null;
        try {
            ImageProcessingSettings.FITSParameters state = new ImageProcessingSettings(() -> {}).fitsParameters();
            expected = FITSImage.decode(reference.toFile(), ImageFilter.NONE, state, state.clipRange(FITSImage.readInfo(reference.toFile()).clipSet()));
            actual = FITSImage.decode(compressed.toFile(), ImageFilter.NONE, state, state.clipRange(FITSImage.readInfo(compressed.toFile()).clipSet()));
            if (expected.width != actual.width || expected.height != actual.height || expected.format != actual.format || !expected.buffer.equals(actual.buffer))
                throw new AssertionError("compressed and uncompressed images differ");
        } finally {
            Method free = ImageBuffer.class.getDeclaredMethod("free");
            free.setAccessible(true);
            if (expected != null) free.invoke(expected);
            if (actual != null) free.invoke(actual);
        }
    }

    private static byte[] readBare(Path resources, String name) throws IOException {
        return Files.readAllBytes(resources.resolve("bare").resolve(name));
    }

    private static byte[] readRise(Path resources, String name) throws IOException {
        return Files.readAllBytes(resources.resolve("rise").resolve(name));
    }

    private static RiceCompressOption riceOption(int bytePix, int blockSize) {
        return new RiceCompressOption().setBytePix(bytePix).setBlockSize(blockSize);
    }

    private static RiceQuantizeCompressOption quantizeOption(int length) {
        RiceQuantizeCompressOption option = new RiceQuantizeCompressOption();
        option.setDither(true);
        option.setSeed(8864L);
        option.setQlevel(4);
        option.setCheckNull(false);
        if (length == 10000) {
            option.setTileHeight(100);
            option.setTileWidth(100);
        } else {
            option.setTileHeight(1);
            option.setTileWidth(length);
        }
        return option;
    }

    private static ICompressorControl control(Class<?> baseType) {
        return control(baseType, null);
    }

    private static ICompressorControl control(Class<?> baseType, String quantAlgorithm) {
        ICompressorControl control = CompressorProvider.findCompressorControl(quantAlgorithm, Compression.ZCMPTYPE_RICE_1, baseType);
        if (control == null)
            throw new AssertionError("No compressor control for " + baseType.getName());
        return control;
    }

    private static short[] toShorts(byte[] bytes) {
        short[] values = new short[bytes.length / Short.BYTES];
        ByteBuffer.wrap(bytes).asShortBuffer().get(values);
        return values;
    }

    private static int[] toInts(byte[] bytes) {
        int[] values = new int[bytes.length / Integer.BYTES];
        ByteBuffer.wrap(bytes).asIntBuffer().get(values);
        return values;
    }

    private static float[] toFloats(byte[] bytes) {
        float[] values = new float[bytes.length / Float.BYTES];
        ByteBuffer.wrap(bytes).asFloatBuffer().get(values);
        return values;
    }

    private static double[] toDoubles(byte[] bytes) {
        double[] values = new double[bytes.length / Double.BYTES];
        ByteBuffer.wrap(bytes).asDoubleBuffer().get(values);
        return values;
    }

    private static byte[] byteData(int length, int kind) {
        byte[] values = new byte[length];
        for (int i = 0; i < length; i++) {
            values[i] = switch (kind) {
                case 0 -> 42;
                case 1 -> (byte) (i * 3 - 100);
                default -> (byte) RND.nextInt();
            };
        }
        return values;
    }

    private static short[] shortData(int length, int kind) {
        short[] values = new short[length];
        for (int i = 0; i < length; i++) {
            values[i] = switch (kind) {
                case 0 -> -1234;
                case 1 -> (short) (i * 7 - 20000);
                default -> (short) RND.nextInt();
            };
        }
        return values;
    }

    private static int[] intData(int length, int kind) {
        int[] values = new int[length];
        for (int i = 0; i < length; i++) {
            values[i] = switch (kind) {
                case 0 -> 0x76543210;
                case 1 -> i * 100003 - 0x40000000;
                default -> RND.nextInt();
            };
        }
        return values;
    }

    private static float[] floatData(int length) {
        float[] values = new float[length];
        for (int i = 0; i < length; i++)
            values[i] = (float) (Math.sin(i * .17) * 1000. + i - 50.);
        return values;
    }

    private static double[] doubleData(int length) {
        double[] values = new double[length];
        for (int i = 0; i < length; i++)
            values[i] = Math.cos(i * .13) * 1000. + i * .25 - 100.;
        return values;
    }

    private static void assertArrayEquals(String label, byte[] expected, byte[] actual) {
        if (!Arrays.equals(expected, actual))
            throw new AssertionError(label);
    }

    private static void assertArrayEquals(String label, short[] expected, short[] actual) {
        if (!Arrays.equals(expected, actual))
            throw new AssertionError(label);
    }

    private static void assertArrayEquals(String label, int[] expected, int[] actual) {
        if (!Arrays.equals(expected, actual))
            throw new AssertionError(label);
    }

    private static void assertArrayEquals(String label, float[] expected, float[] actual) {
        if (!Arrays.equals(expected, actual))
            throw new AssertionError(label);
    }

    private static void assertArrayEquals(String label, double[] expected, double[] actual) {
        if (!Arrays.equals(expected, actual))
            throw new AssertionError(label);
    }
}
