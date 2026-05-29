import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

import org.helioviewer.jhv.view.uri.FastRiceProvider;

import nom.tam.fits.compression.algorithm.api.ICompressorControl;
import nom.tam.fits.compression.algorithm.rice.RiceCompressOption;
import nom.tam.fits.compression.algorithm.rice.RiceCompressor.ByteRiceCompressor;
import nom.tam.fits.compression.algorithm.rice.RiceCompressor.DoubleRiceCompressor;
import nom.tam.fits.compression.algorithm.rice.RiceCompressor.FloatRiceCompressor;
import nom.tam.fits.compression.algorithm.rice.RiceCompressor.IntRiceCompressor;
import nom.tam.fits.compression.algorithm.rice.RiceCompressor.ShortRiceCompressor;
import nom.tam.fits.compression.algorithm.rice.RiceQuantizeCompressOption;
import nom.tam.fits.compression.provider.CompressorProvider;
import nom.tam.fits.header.Compression;
import nom.tam.util.type.PrimitiveTypes;

public final class FastRiceVerifier {

    private static final Random RND = new Random(0x5a17);

    private FastRiceVerifier() {}

    public static void main(String[] args) throws Exception {
        Path nomTamRoot = args.length == 0 ? Path.of(System.getProperty("user.home"), "git", "nom-tam-fits") : Path.of(args[0]);
        Path resources = nomTamRoot.resolve("src/test/resources/nom/tam/image/comp");
        if (!Files.isDirectory(resources))
            throw new IllegalArgumentException("nom-tam-fits test resources not found: " + resources);

        verifyServiceLoader();
        verifyNomTamFixtures(resources);
        verifySyntheticIntegerCases();
        verifySyntheticQuantizedCases();

        System.out.println("OK FastRiceVerifier");
    }

    private static void verifyServiceLoader() {
        ICompressorControl control = CompressorProvider.findCompressorControl(null, Compression.ZCMPTYPE_RICE_1, short.class);
        if (control == null || !control.getClass().getName().contains("FastRiceProvider"))
            throw new AssertionError("FastRiceProvider is not active through ServiceLoader: " + control);
    }

    private static void verifyNomTamFixtures(Path resources) throws Exception {
        verifyByteFixture(resources);
        verifyShortFixture(resources);
        verifyIntFixture(resources);
        verifyFloatFixture(resources);
        verifyDoubleFixture(resources);
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
        FloatRiceCompressor compressor = new FloatRiceCompressor(option);

        ByteBuffer compressed = ByteBuffer.allocate(input.length * Float.BYTES);
        compressor.compress(FloatBuffer.wrap(input), compressed);
        compressed.flip();

        FloatBuffer nomTam = FloatBuffer.allocate(input.length);
        new FloatRiceCompressor(option).decompress(compressed.duplicate(), nomTam);

        FloatBuffer fast = FloatBuffer.allocate(input.length);
        control(float.class, Compression.ZQUANTIZ_SUBTRACTIVE_DITHER_1).decompress(compressed.duplicate(), fast, option);
        assertArrayEquals("nom-tam float fixture", nomTam.array(), fast.array());
    }

    private static void verifyDoubleFixture(Path resources) throws Exception {
        double[] input = toDoubles(readBare(resources, "test100Data-64.bin"));
        RiceQuantizeCompressOption option = quantizeOption(input.length);
        DoubleRiceCompressor compressor = new DoubleRiceCompressor(option);

        ByteBuffer compressed = ByteBuffer.allocate(input.length * Double.BYTES);
        compressor.compress(DoubleBuffer.wrap(input), compressed);
        compressed.flip();

        DoubleBuffer nomTam = DoubleBuffer.allocate(input.length);
        new DoubleRiceCompressor(option).decompress(compressed.duplicate(), nomTam);

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
        new ByteRiceCompressor(option).compress(ByteBuffer.wrap(input), compressed);
        compressed.flip();

        ByteBuffer nomTam = ByteBuffer.allocate(input.length);
        new ByteRiceCompressor(option).decompress(compressed.duplicate(), nomTam);

        ByteBuffer fast = ByteBuffer.allocate(input.length);
        control(byte.class).decompress(compressed.duplicate(), fast, option);
        assertArrayEquals("synthetic byte", nomTam.array(), fast.array());
    }

    private static void verifyShortSynthetic(int block, short[] input, boolean heapOutput) {
        RiceCompressOption option = riceOption(PrimitiveTypes.SHORT.size(), block);
        ByteBuffer compressed = ByteBuffer.allocate(input.length * 8 + 128);
        new ShortRiceCompressor(option).compress(ShortBuffer.wrap(input), compressed);
        compressed.flip();

        ShortBuffer nomTam = ShortBuffer.allocate(input.length);
        new ShortRiceCompressor(option).decompress(compressed.duplicate(), nomTam);

        ShortBuffer fast = heapOutput ? ShortBuffer.allocate(input.length)
                : ByteBuffer.allocateDirect(input.length * Short.BYTES).order(ByteOrder.BIG_ENDIAN).asShortBuffer();
        control(short.class).decompress(compressed.duplicate(), fast, option);
        fast.flip();
        short[] actual = new short[input.length];
        fast.get(actual);
        assertArrayEquals("synthetic short", nomTam.array(), actual);
    }

    private static void verifyIntSynthetic(int block, int[] input) {
        RiceCompressOption option = riceOption(PrimitiveTypes.INT.size(), block);
        ByteBuffer compressed = ByteBuffer.allocate(input.length * 8 + 128);
        new IntRiceCompressor(option).compress(IntBuffer.wrap(input), compressed);
        compressed.flip();

        IntBuffer nomTam = IntBuffer.allocate(input.length);
        new IntRiceCompressor(option).decompress(compressed.duplicate(), nomTam);

        IntBuffer fast = IntBuffer.allocate(input.length);
        control(int.class).decompress(compressed.duplicate(), fast, option);
        assertArrayEquals("synthetic int", nomTam.array(), fast.array());
    }

    private static void verifySyntheticQuantizedCases() {
        int cases = 0;
        for (int length : new int[]{32, 33, 127, 256}) {
            for (boolean dither : new boolean[]{false, true}) {
                verifyFloatSynthetic(length, dither);
                verifyDoubleSynthetic(length, dither);
                cases += 2;
            }
        }
        System.out.println("synthetic quantized cases=" + cases);
    }

    private static void verifyFloatSynthetic(int length, boolean dither) {
        RiceQuantizeCompressOption option = quantizeOption(length);
        option.setDither(dither);
        FloatRiceCompressor compressor = new FloatRiceCompressor(option);
        ByteBuffer compressed = ByteBuffer.allocate(length * 16 + 512);
        compressor.compress(FloatBuffer.wrap(floatData(length)), compressed);
        compressed.flip();

        FloatBuffer nomTam = FloatBuffer.allocate(length);
        new FloatRiceCompressor(option).decompress(compressed.duplicate(), nomTam);

        FloatBuffer fast = FloatBuffer.allocate(length);
        control(float.class, Compression.ZQUANTIZ_SUBTRACTIVE_DITHER_1).decompress(compressed.duplicate(), fast, option);
        assertArrayEquals("synthetic float", nomTam.array(), fast.array());
    }

    private static void verifyDoubleSynthetic(int length, boolean dither) {
        RiceQuantizeCompressOption option = quantizeOption(length);
        option.setDither(dither);
        DoubleRiceCompressor compressor = new DoubleRiceCompressor(option);
        ByteBuffer compressed = ByteBuffer.allocate(length * 16 + 512);
        compressor.compress(DoubleBuffer.wrap(doubleData(length)), compressed);
        compressed.flip();

        DoubleBuffer nomTam = DoubleBuffer.allocate(length);
        new DoubleRiceCompressor(option).decompress(compressed.duplicate(), nomTam);

        DoubleBuffer fast = DoubleBuffer.allocate(length);
        control(double.class, Compression.ZQUANTIZ_SUBTRACTIVE_DITHER_1).decompress(compressed.duplicate(), fast, option);
        assertArrayEquals("synthetic double", nomTam.array(), fast.array());
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
        ICompressorControl control = new FastRiceProvider().createCompressorControl(quantAlgorithm, Compression.ZCMPTYPE_RICE_1, baseType);
        if (control == null)
            throw new AssertionError("No FastRice control for " + baseType.getName());
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
