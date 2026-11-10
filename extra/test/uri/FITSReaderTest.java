package org.helioviewer.jhv.source;

import java.io.IOException;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.image.ClipSet;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.ImageProcessingSettings;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.metadata.DetectorMask;

import org.json.JSONArray;
import org.json.JSONObject;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsFactory;
import nom.tam.image.compression.hdu.CompressedImageHDU;

public final class FITSReaderTest {

    private static final Path data = Path.of("extra/test/data/fits");

    public static void main(String[] args) throws Exception {
        Platform.init();
        Directories.createCacheDirs();
        FitsFactory.setUseHierarch(true);
        FitsFactory.setLongStringsEnabled(true);
        BufferPoolMXBean mapped = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class).stream()
                .filter(pool -> pool.getName().equals("mapped")).findFirst().orElseThrow();
        long maps = mapped.getCount();
        JSONArray fixtures = new JSONObject(Files.readString(data.resolve("manifest.json"))).getJSONArray("images");
        for (int i = 0; i < fixtures.length(); i++) {
            JSONObject fixture = fixtures.getJSONObject(i);
            Path path = data.resolve(fixture.getString("file"));
            int bitpix = fixture.getInt("bitpix"), width = fixture.getInt("width"), height = fixture.getInt("height");
            int hdu = fixture.getInt("hdu"), planes = fixture.getInt("planes");
            ByteBuffer expected = ByteBuffer.wrap(Files.readAllBytes(data.resolve(path.getFileName().toString().replace(".fits", ".raw"))))
                    .order(ByteOrder.LITTLE_ENDIAN);
            try (Fits headers = new Fits(path.toFile())) {
                for (BasicHDU<?> image : headers.read()) {
                    if (image.getData() != null && image.getData().getSize() > 0 && !image.getData().isDeferred())
                        throw new AssertionError("Header scan loaded pixels: " + path);
                }
            }
            for (int plane = 0; plane < planes; plane++) {
                float[] pixels = FITSReader.read(path, hdu, bitpix, width, height, plane, tileRows(path, hdu, height), 1, 1);
                compare(pixels, expected, plane * width * height, bitpix, width * height);
                float[] sampled = FITSReader.read(path, hdu, bitpix, width, height, plane, 1, 3, 4);
                for (int y = 0, index = 0; y < height; y += 4)
                    for (int x = 0; x < width; x += 3)
                        compareOne(sampled, index++, expected, plane * width * height + y * width + x, bitpix);
            }
            if (mapped.getCount() != maps)
                throw new AssertionError("Mapping leaked after " + path);
            System.out.println("PASS raw/subset planes: " + path.getFileName());
        }
        checkInfinities();
        checkScaling();
        checkSelection();
        checkCoupledCube();
        FITSSource ccor = new FITSSource(data.getParent().resolve("CCOR2_1A_20260902T211514_V00_NC.fits").toFile());
        try {
            // Astropy excludes the missing pixels that the old NTF decoder turned into zeros.
            if (!new ClipSet.Range(0f, 2.8012987e-8f).equals(ccor.clipSet().percentile05()))
                throw new AssertionError("CCOR2 missing pixels changed the 0.5% clipping range");
        } finally {
            ccor.close();
        }
        Path directory = Files.createTempDirectory("jhv-fits-Å-[0]+-");
        try {
            Path imagePath = directory.resolve("image Å [0](raw)+1.fits");
            Files.copy(data.resolve("integer32.fits"), imagePath);
            FITSSource named = new FITSSource(imagePath.toFile());
            named.close();
            Files.delete(imagePath);
            Path gzip = directory.resolve("image.fits.gz");
            try (GZIPOutputStream output = new GZIPOutputStream(Files.newOutputStream(gzip))) {
                Files.copy(data.resolve("integer32.fits"), output);
            }
            long before = stagedFiles();
            FITSSource source = new FITSSource(gzip.toFile());
            ImageProcessingSettings.FITSParameters settings = new ImageProcessingSettings(() -> {}).fitsParameters();
            try (ImageBufferOwner image = new ImageBufferOwner(source.decode(0, 0, ImageFilter.NONE, settings, settings.clipRange(source.clipSet())))) {
                if (image.buffer.width != 70 || image.buffer.height != 35)
                    throw new AssertionError("Image dimensions");
            }
            source.close();
            source.close();
            try {
                source.decode(0, 0, ImageFilter.NONE, settings, settings.clipRange(source.clipSet()));
                throw new AssertionError("Closed source accepted a decode");
            } catch (IllegalStateException expected) {
                // A closed source cannot reopen or map its staged input.
            }
            if (stagedFiles() != before)
                throw new AssertionError("gzip staging leaked");
            try {
                try (GZIPOutputStream output = new GZIPOutputStream(Files.newOutputStream(gzip))) {
                    output.write(new byte[4000]);
                }
                new FITSSource(gzip.toFile());
                throw new AssertionError("Invalid FITS input accepted");
            } catch (Exception expected) {
                if (stagedFiles() != before)
                    throw new AssertionError("Failed constructor leaked gzip staging");
            }
            Files.delete(gzip);
            Path brokenGzip = directory.resolve("broken.fits.gz");
            Files.write(brokenGzip, new byte[]{0x1f, (byte) 0x8b});
            try {
                new FITSSource(brokenGzip.toFile());
                throw new AssertionError("Broken gzip header accepted");
            } catch (IOException expected) {
                if (stagedFiles() != before)
                    throw new AssertionError("Broken gzip header leaked staging");
            }
            Files.delete(brokenGzip);
            Path truncated = directory.resolve("truncated.fits");
            Files.write(truncated, new byte[4000]);
            try {
                FITSReader.read(truncated, 1, 16, 70, 35, 0, 1, 1, 1);
                throw new AssertionError("Malformed input accepted");
            } catch (IllegalArgumentException expected) {
                if (mapped.getCount() != maps)
                    throw new AssertionError("Failed read leaked mapping");
            }
            Files.delete(truncated);
        } finally {
            try (Stream<Path> files = Files.list(directory)) {
                for (Path file : files.toList())
                    Files.deleteIfExists(file);
            }
            Files.delete(directory);
        }
        DetectorMask.loadBuiltins();
        if (DetectorMask.EUI_OCCULTED == DetectorMask.NONE)
            throw new AssertionError("Packaged EUI detector mask did not load");
        ImageBuffer mask = DetectorMask.EUI_OCCULTED.getImageBuffer();
        byte[] bytes = new byte[mask.width * mask.height];
        ((ByteBuffer) mask.buffer).get(0, bytes);
        // Astropy reference for the actual packaged mask, with JHV's Y reversal.
        if (!digest(bytes).equals("e7f7b98f09886bebd6188ad1e6ea215f4e91baa3b33a0fb656ae9bf2c763f456"))
            throw new AssertionError("Packaged EUI detector mask pixels/orientation");
        if (mapped.getCount() != maps)
            throw new AssertionError("Mapping leak");
        System.out.println("PASS paths, gzip/error cleanup, CCOR2 clipping, and detector mask");
    }

    private static void checkScaling() throws Exception {
        Path path = Files.createTempFile("jhv-scaled-pixels-", ".fits");
        try {
            try (Fits fits = new Fits()) {
                BasicHDU<?> image = Fits.makeHDU(new short[][]{{0, 1, 2, 3}, {4, 5, 6, 7}, {8, 9, 10, 11}, {12, 13, 14, 15}});
                image.getHeader().addValue("BZERO", 10, null);
                image.getHeader().addValue("BSCALE", -2, null);
                image.getHeader().addValue("BLANK", 2, null);
                fits.addHDU(image);
                fits.write(path.toFile());
            }
            float[] pixels = FITSReader.read(path, 1, 16, 4, 4, 0, 4, 1, 1);
            for (int i = 0; i < pixels.length; i++) {
                if (i == 2 ? !Float.isNaN(pixels[i]) : pixels[i] != 10 - 2 * i)
                    throw new AssertionError("CFITSIO scaling or integer BLANK conversion");
            }
            FITSData data = new FITSData(4, 4, false, null);
            ClipSet clips = data.calculateSampledClipSet(pixels);
            if (!new ClipSet.Range(-20, 10).equals(clips.percentile05()))
                throw new AssertionError("Clipping must use scaled finite pixels");
            float[] extremes = {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
                    -3e9f, (float) Integer.MIN_VALUE, -2e9f, -1e9f, 0, 1, 2, 3, 4, 5, Float.MAX_VALUE};
            clips = data.calculateSampledClipSet(extremes);
            if (!new ClipSet.Range(-3e9f, Float.MAX_VALUE).equals(clips.percentile05()))
                throw new AssertionError("Finite values must not collide with undefined-pixel sentinels");
        } finally {
            Files.delete(path);
        }
        System.out.println("PASS scaled float pixels, BLANK and finite-only clipping");
    }

    private static void checkInfinities() throws Exception {
        for (boolean wide : new boolean[]{false, true}) {
            Path path = Files.createTempFile("jhv-infinite-pixels-", ".fits");
            try {
                try (Fits fits = new Fits()) {
                    Object values = wide ? new double[][]{{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, .5}}
                            : new float[][]{{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, .5f}};
                    BasicHDU<?> image = Fits.makeHDU(values);
                    image.getHeader().addValue("HV_DMIN", 0.0, null);
                    image.getHeader().addValue("HV_DMAX", 1.0, null);
                    fits.addHDU(image);
                    fits.write(path.toFile());
                }
                float[] actual = FITSReader.read(path, 1, wide ? -64 : -32, 4, 1, 0, 1, 1, 1);
                if (!Float.isNaN(actual[0])
                        || actual[1] != Float.POSITIVE_INFINITY
                        || actual[2] != Float.NEGATIVE_INFINITY)
                    throw new AssertionError("Reader must preserve NaN and both infinity signs");
                FITSSource source = new FITSSource(path.toFile());
                try {
                    ImageProcessingSettings.FITSParameters settings = new ImageProcessingSettings(() -> {}).fitsParameters();
                    try (ImageBufferOwner owner = new ImageBufferOwner(source.decode(0, 0, ImageFilter.NONE, settings, null))) {
                        ShortBuffer pixels = (ShortBuffer) owner.buffer.buffer;
                        if (Float.float16ToFloat(pixels.get(0)) != 0 || Float.float16ToFloat(pixels.get(1)) != 1
                                || Float.float16ToFloat(pixels.get(2)) != 1)
                            throw new AssertionError("NaN must display low; both infinities must display high");
                    }
                } finally {
                    source.close();
                }
            } finally {
                Files.deleteIfExists(path);
            }
        }
        System.out.println("PASS float/double infinity preservation and full-white display");
    }

    private static void checkSelection() throws Exception {
        Path path = Files.createTempFile("jhv-fits-selection-", ".fits");
        try {
            try (Fits fits = new Fits()) {
                fits.addHDU(BasicHDU.getDummyHDU());
                BasicHDU<?> signal = Fits.makeHDU(new byte[][][]{{{1, 2}, {3, 4}}, {{11, 12}, {13, 14}}});
                signal.getHeader().addValue("EXTNAME", "Signal", null);
                fits.addHDU(signal);
                BasicHDU<?> uncertainty = Fits.makeHDU(new byte[][]{{21, 22}, {23, 24}});
                uncertainty.getHeader().addValue("EXTNAME", "Uncertainty", null);
                fits.addHDU(uncertainty);
                fits.write(path.toFile());
            }
            ImageProcessingSettings.FITSParameters settings = new ImageProcessingSettings(() -> {}).fitsParameters();
            for (int hdu = 1; hdu <= 2; hdu++) {
                for (int plane = 0; plane < (hdu == 1 ? 2 : 1); plane++) {
                    ImageProcessingSettings.FITSSelection selection = new ImageProcessingSettings.FITSSelection(hdu, plane);
                    FITSSource source = new FITSSource(path.toFile(), selection);
                    try {
                        if (source.frames() != 1 || source.images().size() != 2 || !source.selection().equals(selection)
                                || !source.xml(0).contains(hdu == 1 ? "Signal" : "Uncertainty"))
                            throw new AssertionError("HDU selection metadata or frame count");
                        try (ImageBufferOwner owner = new ImageBufferOwner(source.decode(0, 0, ImageFilter.NONE, settings, null))) {
                            ByteBuffer pixels = (ByteBuffer) owner.buffer.buffer;
                            int base = hdu == 1 ? plane * 10 : 20;
                            byte[] expected = {(byte) (base + 3), (byte) (base + 4), (byte) (base + 1), (byte) (base + 2)};
                            if (!pixels.equals(ByteBuffer.wrap(expected)))
                                throw new AssertionError("Selected HDU/plane pixels or Y reversal");
                        }
                    } finally {
                        source.close();
                    }
                }
            }
            FITSSource automatic = new FITSSource(path.toFile());
            try {
                if (!automatic.selection().equals(new ImageProcessingSettings.FITSSelection(1, 0)))
                    throw new AssertionError("Default cube selection");
            } finally {
                automatic.close();
            }
            for (ImageProcessingSettings.FITSSelection invalid : new ImageProcessingSettings.FITSSelection[]{new ImageProcessingSettings.FITSSelection(0, 0),
                    new ImageProcessingSettings.FITSSelection(1, -1), new ImageProcessingSettings.FITSSelection(1, 2), new ImageProcessingSettings.FITSSelection(2, 1)}) {
                try {
                    FITSSource source = new FITSSource(path.toFile(), invalid);
                    source.close();
                    throw new AssertionError("Invalid selection accepted: " + invalid);
                } catch (IllegalArgumentException expected) {
                    // Invalid selections fail before any pixels are decoded.
                }
            }
        } finally {
            Files.delete(path);
        }
        System.out.println("PASS source HDU/plane selection, metadata, defaults and bounds");
    }

    private static void checkCoupledCube() throws Exception {
        for (String key : new String[]{"PC1_3", "PC2_3", "CD1_3", "CD2_3"}) {
            Path path = Files.createTempFile("jhv-coupled-cube-", ".fits");
            try {
                try (Fits fits = new Fits()) {
                    BasicHDU<?> image = Fits.makeHDU(new byte[2][2][2]);
                    image.getHeader().addValue(key, 1.0, null);
                    fits.addHDU(image);
                    fits.write(path.toFile());
                }
                try {
                    FITSSource source = new FITSSource(path.toFile(), new ImageProcessingSettings.FITSSelection(0, 1));
                    source.close();
                    throw new AssertionError("Spatially coupled cube accepted: " + key);
                } catch (IllegalArgumentException expected) {
                    if (!expected.getMessage().contains("coupled"))
                        throw expected;
                }
            } finally {
                Files.delete(path);
            }
        }
        System.out.println("PASS spatially coupled cubes rejected before publishing incorrect geometry");
    }

    private static int tileRows(Path path, int hdu, int height) throws Exception {
        try (Fits fits = new Fits(path.toFile())) {
            BasicHDU<?> image = fits.read()[hdu - 1];
            return image instanceof CompressedImageHDU ? image.getHeader().getIntValue("ZTILE2", 1) : height;
        }
    }

    private static long stagedFiles() throws Exception {
        try (Stream<Path> files = Files.list(Directories.fileCacheDir.toPath())) {
            return files.filter(path -> path.getFileName().toString().startsWith("fits-")).count();
        }
    }

    private static void compare(float[] actual, ByteBuffer expected, int first, int bitpix, int count) {
        for (int i = 0; i < count; i++)
            compareOne(actual, i, expected, first + i, bitpix);
    }

    private static void compareOne(float[] actual, int index, ByteBuffer expected, int offset, int bitpix) {
        float value = switch (bitpix) {
            case 8 -> expected.get(offset) & 0xFF;
            case 16 -> expected.getShort(offset * 2);
            case 32 -> expected.getInt(offset * 4);
            case 64 -> expected.getLong(offset * 8);
            case -32 -> expected.getFloat(offset * 4);
            case -64 -> (float) expected.getDouble(offset * 8);
            default -> throw new AssertionError("Unsupported BITPIX");
        };
        if (Float.floatToIntBits(actual[index]) != Float.floatToIntBits(value))
            throw new AssertionError("Pixel " + offset + " differs for BITPIX " + bitpix);
    }

    private static String digest(byte[] pixels) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pixels));
    }

    private record ImageBufferOwner(ImageBuffer buffer) implements AutoCloseable {
        @Override
        public void close() throws Exception {
            Method free = ImageBuffer.class.getDeclaredMethod("free");
            free.setAccessible(true);
            free.invoke(buffer);
        }
    }
}
