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
import org.helioviewer.jhv.view.uri.FITSImage;
import org.helioviewer.jhv.view.uri.FITSViewState;
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
        ICompressorControl control = CompressorProvider.findCompressorControl(null, Compression.ZCMPTYPE_RICE_1, short.class);
        if (control == null || !control.getClass().getName().contains("FastRiceProvider"))
            throw new AssertionError("FastRiceProvider is not active through ServiceLoader: " + control);
        FastRiceProvider provider = new FastRiceProvider();
        for (Class<?> type : List.of(byte.class, int.class, float.class, double.class)) {
            String quantization = type == float.class || type == double.class ? Compression.ZQUANTIZ_SUBTRACTIVE_DITHER_1 : null;
            if (provider.createCompressorControl(quantization, Compression.ZCMPTYPE_RICE_1, type) != null)
                throw new AssertionError("FastRice must delegate " + type.getName());
            ICompressorControl fallback = control(type, quantization);
            if (fallback.getClass().getName().contains("FastRiceProvider"))
                throw new AssertionError("upstream fallback was not selected for " + type.getName());
        }
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
                for (boolean direct : new boolean[]{false, true})
                    run("BYTEPIX=" + bytePix + " output=" + type.getName() + " direct=" + direct,
                            () -> verifyEncodedWidth(bytePix, type, direct));
            }
        }
    }

    private static void verifyEncodedWidth(int bytePix, Class<?> type, boolean direct) {
        // All values fit every encoding width. Width is a property of the stream, not the output buffer.
        int[] values = new int[65];
        for (int i = 0; i < values.length; i++)
            values[i] = (i * 17) % 127;
        RiceCompressOption option = riceOption(bytePix, 32);
        ByteBuffer encoded = ByteBuffer.allocate(2048);
        requireCompressed(new RiceCompressor.IntRiceCompressor(option).compress(IntBuffer.wrap(values), encoded));
        encoded.flip();
        ByteBuffer storage = direct ? ByteBuffer.allocateDirect(encoded.remaining() + 7) : ByteBuffer.allocate(encoded.remaining() + 7);
        storage.position(7);
        storage.put(encoded).flip().position(7);
        ByteBuffer input = storage.slice();
        ByteBuffer output = direct ? ByteBuffer.allocateDirect(values.length * 4) : ByteBuffer.allocate(values.length * 4);
        Buffer decoded = type == byte.class ? output.limit(values.length)
                : type == short.class ? output.asShortBuffer().limit(values.length) : output.asIntBuffer();
        control(type).decompress(input, decoded, option);
        if (decoded.position() != values.length)
            throw new AssertionError("incorrect output position: " + decoded.position());
        for (int i = 0; i < values.length; i++) {
            int actual = decoded instanceof ByteBuffer bytes ? bytes.get(i)
                    : decoded instanceof ShortBuffer shorts ? shorts.get(i) : ((IntBuffer) decoded).get(i);
            if (actual != values[i])
                throw new AssertionError("pixel " + i + ": expected " + values[i] + ", got " + actual);
        }
    }

    private static void verifyKnownQuantizedValues() {
        // Pin the existing 1.22 reconstruction contract independently of the upstream floating-point decoder.
        // Seed 1 starts at the first Park-Miller random value, 16807 / 2147483647.
        double r0 = 16807.0 / 2147483647;
        double r1 = 282475249.0 / 2147483647;
        double r2 = 1622650073.0 / 2147483647;
        double r4 = 1144108930.0 / 2147483647;
        for (Class<?> type : List.of(float.class, double.class)) {
            for (boolean upstream : new boolean[]{false, true}) {
                String label = (upstream ? "upstream " : "JHV provider ") + type.getName();
                run(label + " no dither", () -> verifyKnownValues(type, upstream, false, false,
                        new int[]{0, 1, 2}, new double[]{11, 13, 15}));
                run(label + " fractional precision", () -> verifyKnownValues(type, upstream, true, false,
                        new int[]{0}, new double[]{10 + (0.5 - r0) * 2}));
                run(label + " dither2 zero marker", () -> verifyKnownValues(type, upstream, true, true,
                        new int[]{Integer.MIN_VALUE + 2, 0}, new double[]{0, 10 + (0.5 - r1) * 2}));
                run(label + " dither1 nulls", () -> verifyKnownValues(type, upstream, true, false,
                        new int[]{0, Integer.MIN_VALUE, 1}, new double[]{10 + (0.5 - r0) * 2, Double.NaN, 10 + (1.5 - r2) * 2}));
                run(label + " dither2 zeros/nulls", () -> verifyKnownValues(type, upstream, true, true,
                        new int[]{0, Integer.MIN_VALUE, 1, Integer.MIN_VALUE + 2, 2},
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
            FITSImage reader = new FITSImage(new FITSViewState(() -> {}).data());
            expected = reader.readImageBuffer(reference.toFile(), ImageFilter.NONE);
            actual = reader.readImageBuffer(compressed.toFile(), ImageFilter.NONE);
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
