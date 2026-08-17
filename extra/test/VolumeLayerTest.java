package org.helioviewer.jhv.opengl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.TimeZone;

import javax.imageio.ImageIO;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.layers.VolumeLayer;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.angle.AngleRenderer;
import org.helioviewer.jhv.opengl.volume.FitsVolumeLoader;
import org.helioviewer.jhv.opengl.volume.VolumeData;

import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.ImageHDU;

public final class VolumeLayerTest {

    private static final int GRID_SIZE = 256;
    private static final int RENDER_SIZE = 1024;
    private static final double CORNER = -3;
    private static final double EXTENT = 6;
    private static final String OBSERVATION_TIME = "2026-08-17T00:00:00.000";
    private static final double SOLAR_RADIUS_METERS = 695_700_000.;
    private static final double OBSERVER_DISTANCE_METERS = 151_470_458_469.;
    private static final double OBSERVER_CARRINGTON_LONGITUDE = 168.63517776981791;
    private static final double OBSERVER_CARRINGTON_LATITUDE = 6.722914954331527;
    private static final String[] AXIS_TYPES = {"SOLX", "SOLY", "SOLZ"};

    public static void main(String[] args) throws Exception {
        System.setProperty("user.timezone", TimeZone.getDefault().getID());
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Locale.setDefault(Locale.US);

        Path outputDirectory = args.length == 0 ? Path.of("extra/test/out") : Path.of(args[0]);
        if (args.length > 1)
            throw new IllegalArgumentException("usage: VolumeLayerTest [output-directory]");

        Path volume8Path = Path.of("extra/test/data/synthetic-corona-8.fits");
        Path volume16Path = Path.of("extra/test/data/synthetic-corona-16.fits");
        Path image8Path = outputDirectory.resolve("synthetic-corona-volume-8.png");
        Path image16Path = outputDirectory.resolve("synthetic-corona-volume-16.png");
        Path temporaryDirectory = Files.createTempDirectory("jhv-volume-test-");
        temporaryDirectory.toFile().deleteOnExit();

        VolumeData data8 = FitsVolumeLoader.load(volume8Path);
        VolumeData data16 = FitsVolumeLoader.load(volume16Path);
        validateVolume(data8, VolumeData.Format.R8, "8-bit");
        validateVolume(data16, VolumeData.Format.R16F, "16-bit");
        check(!isValid(data8, 151 + GRID_SIZE * (113 + GRID_SIZE * 205)), "synthetic BITPIX=8 BLANK sample");
        compareQuantization(data8, data16);
        checkSample(data16, 151, 113, 205);
        checkFixedStoredRanges(temporaryDirectory);
        checkBlankRanges(temporaryDirectory);
        checkUnsupportedBitpix(temporaryDirectory);
        checkStandardWcs(temporaryDirectory);

        render(new VolumeLayer(volume8Path), image8Path, new VolumeLayer(volume16Path), image16Path);
        System.out.println("VolumeLayerTest passed");
        System.out.println("8-bit synthetic volume: " + volume8Path.toAbsolutePath().normalize());
        System.out.println("16-bit synthetic volume: " + volume16Path.toAbsolutePath().normalize());
        System.out.println("8-bit rendered image: " + image8Path.toAbsolutePath().normalize());
        System.out.println("16-bit rendered image: " + image16Path.toAbsolutePath().normalize());
    }

    private static void writeFits(Path path, Object values, double bscale, double bzero) throws Exception {
        writeFits(path, values, bscale, bzero, null);
    }

    private static void writeFits(Path path, Object values, double bscale, double bzero, Long blank) throws Exception {
        Path absolute = path.toAbsolutePath().normalize();
        Files.createDirectories(absolute.getParent());
        Files.deleteIfExists(absolute);
        absolute.toFile().deleteOnExit();

        ImageHDU hdu = (ImageHDU) Fits.makeHDU(values);
        Header header = hdu.getHeader();
        header.addValue("OBJECT", "synthetic coronal density", null);
        header.addValue("DATE-OBS", OBSERVATION_TIME, null);
        header.addValue("BSCALE", bscale, "physical value per stored integer step");
        header.addValue("BZERO", bzero, "physical value represented by stored zero");
        if (blank != null)
            header.addValue("BLANK", blank, "undefined stored value");
        header.addValue("WCSNAME", "Heliocentric Cartesian", "coordinate system name");
        header.addValue("RSUN_REF", SOLAR_RADIUS_METERS, 1, "[m] assumed physical solar radius");
        header.addValue("DSUN_OBS", OBSERVER_DISTANCE_METERS, 1, "[m] distance from centre of Sun to observer");
        header.addValue("CRLN_OBS", OBSERVER_CARRINGTON_LONGITUDE, "[deg] Carrington longitude of observer");
        header.addValue("CRLT_OBS", OBSERVER_CARRINGTON_LATITUDE, "[deg] Carrington latitude of observer");
        double spacing = EXTENT / GRID_SIZE;
        for (int axis = 1; axis <= 3; axis++) {
            header.addValue("CTYPE" + axis, AXIS_TYPES[axis - 1], "heliocentric Cartesian coordinate");
            header.addValue("CUNIT" + axis, "solRad", "coordinate unit");
            header.addValue("CRPIX" + axis, (GRID_SIZE + 1) / 2., "[pixel] reference pixel");
            header.addValue("CRVAL" + axis, 0, "[solRad] coordinate at reference pixel");
            header.addValue("CDELT" + axis, spacing, "[solRad/pixel] coordinate increment");
        }

        try (Fits fits = new Fits()) {
            fits.addHDU(hdu);
            fits.write(absolute.toFile());
        }
    }

    private static float syntheticDensity(double x, double y, double z) {
        double radius = Math.sqrt(x * x + y * y + z * z);
        if (radius <= 1 || radius >= 3)
            return 0;

        double corona = Math.exp(-1.4 * (radius - 1));
        double equatorialSheet = Math.exp(-12 * z * z) * Math.exp(-0.7 * (radius - 1));
        double plume = Math.exp(-8 * (x - 0.55) * (x - 0.55) - 8 * (y + 0.35) * (y + 0.35)) *
                Math.exp(-0.8 * (z - 1.3) * (z - 1.3));
        double arcRadius = Math.sqrt((x + 0.8) * (x + 0.8) + z * z);
        double arc = Math.exp(-35 * (arcRadius - 1.1) * (arcRadius - 1.1) - 8 * y * y) * (z > 0 ? 1 : 0);
        return (float) Math.min(1, 0.25 * corona + 0.65 * equatorialSheet + 0.8 * plume + arc);
    }

    private static void validateVolume(VolumeData data, VolumeData.Format format, String description) {
        check(data.width() == GRID_SIZE && data.height() == GRID_SIZE && data.depth() == GRID_SIZE,
                description + " volume dimensions");
        Vec3 centre = new Vec3(
                data.corner().x + 0.5 * (data.axisX().x + data.axisY().x + data.axisZ().x),
                data.corner().y + 0.5 * (data.axisX().y + data.axisY().y + data.axisZ().y),
                data.corner().z + 0.5 * (data.axisX().z + data.axisY().z + data.axisZ().z));
        checkVector(centre, 0, 0, 0, description + " volume centre");
        checkClose(data.axisX().length(), EXTENT, description + " volume X extent");
        checkClose(data.axisY().length(), EXTENT, description + " volume Y extent");
        checkClose(data.axisZ().length(), EXTENT, description + " volume Z extent");
        checkClose(Vec3.dot(data.axisX(), data.axisY()), 0, description + " volume X/Y orthogonality");
        checkClose(Vec3.dot(data.axisX(), data.axisZ()), 0, description + " volume X/Z orthogonality");
        checkClose(Vec3.dot(data.axisY(), data.axisZ()), 0, description + " volume Y/Z orthogonality");
        check(data.sampleUnits().isEmpty(), description + " sample units");
        check(data.minimum() >= 0 && data.maximum() > 0.99f, description + " scalar range");
        check(data.format() == format, description + " texture format");
        check((format == VolumeData.Format.R8) == (data.validityMask() != null), description + " validity mask");
    }

    private static void compareQuantization(VolumeData data8, VolumeData data16) {
        ByteBuffer samples8 = (ByteBuffer) data8.samples();
        ShortBuffer samples16 = (ShortBuffer) data16.samples();
        ByteBuffer mask8 = data8.validityMask();
        ByteBuffer mask16 = data16.validityMask();
        float maximumDifference = 0;
        int sampleCount = data8.width() * data8.height() * data8.depth();
        for (int i = 0; i < sampleCount; i++) {
            if ((mask8 != null && mask8.get(i) == 0) || (mask16 != null && mask16.get(i) == 0))
                continue;
            float value8 = (samples8.get(i) & 0xff) / 255f;
            float value16 = Float.float16ToFloat(samples16.get(i));
            maximumDifference = Math.max(maximumDifference, Math.abs(value8 - value16));
        }
        check(maximumDifference <= 1.0 / 255 + 1e-6, "8-bit and 16-bit decoded values disagree: " + maximumDifference);
    }

    private static void checkSample(VolumeData data, int x, int y, int z) {
        double spacing = EXTENT / GRID_SIZE;
        double first = CORNER + 0.5 * spacing;
        float expected = syntheticDensity(first + x * spacing, first + y * spacing, first + z * spacing);
        float actual = normalizedSample(data, x + GRID_SIZE * (y + GRID_SIZE * z));
        float fitsQuantized = Math.round(65535 * expected) / 65535f;
        float expectedHalf = Float.float16ToFloat(Float.floatToFloat16(fitsQuantized));
        check(Math.abs(actual - expectedHalf) < 1e-6, "FITS cube axis ordering is wrong: " + actual + " != " + expectedHalf);
    }

    private static float normalizedSample(VolumeData data, int index) {
        return switch (data.format()) {
            case R8 -> (((ByteBuffer) data.samples()).get(index) & 0xff) / 255f;
            case R16F -> Float.float16ToFloat(((ShortBuffer) data.samples()).get(index));
        };
    }

    private static boolean isValid(VolumeData data, int index) {
        ByteBuffer mask = data.validityMask();
        return mask == null || mask.get(index) != 0;
    }

    private static void checkFixedStoredRanges(Path outputDirectory) throws Exception {
        byte[][][] bytes = new byte[2][2][2];
        bytes[0][0][0] = 64;
        VolumeData positiveBytes = writeAndLoad(outputDirectory.resolve("synthetic-corona-byte-range.fits"), bytes, 2, 10);
        VolumeData negativeBytes = writeAndLoad(outputDirectory.resolve("synthetic-corona-byte-range-negative.fits"), bytes, -2, 10);
        checkClose(normalizedSample(positiveBytes, 0), 64.0 / 255, "positive-BSCALE BITPIX=8 sample");
        checkClose(normalizedSample(negativeBytes, 0), 191.0 / 255, "negative-BSCALE BITPIX=8 sample");
        checkClose(positiveBytes.minimum(), 10, "positive-BSCALE BITPIX=8 minimum");
        checkClose(positiveBytes.maximum(), 520, "positive-BSCALE BITPIX=8 maximum");
        checkClose(negativeBytes.minimum(), -500, "negative-BSCALE BITPIX=8 minimum");
        checkClose(negativeBytes.maximum(), 10, "negative-BSCALE BITPIX=8 maximum");

        short[][][] shorts = new short[2][2][2];
        shorts[0][0][0] = 16384;
        VolumeData positiveShorts = writeAndLoad(outputDirectory.resolve("synthetic-corona-short-range.fits"), shorts, 2, 10);
        VolumeData negativeShorts = writeAndLoad(outputDirectory.resolve("synthetic-corona-short-range-negative.fits"), shorts, -2, 10);
        float positiveValue = Float.float16ToFloat(Float.floatToFloat16((16384 - Short.MIN_VALUE) / 65535f));
        float negativeValue = Float.float16ToFloat(Float.floatToFloat16((Short.MAX_VALUE - 16384) / 65535f));
        checkClose(normalizedSample(positiveShorts, 0), positiveValue, "positive-BSCALE BITPIX=16 sample");
        checkClose(normalizedSample(negativeShorts, 0), negativeValue, "negative-BSCALE BITPIX=16 sample");
        checkClose(positiveShorts.minimum(), -65526, "positive-BSCALE BITPIX=16 minimum");
        checkClose(positiveShorts.maximum(), 65544, "positive-BSCALE BITPIX=16 maximum");
        checkClose(negativeShorts.minimum(), -65524, "negative-BSCALE BITPIX=16 minimum");
        checkClose(negativeShorts.maximum(), 65546, "negative-BSCALE BITPIX=16 maximum");
    }

    private static VolumeData writeAndLoad(Path path, Object values, double bscale, double bzero) throws Exception {
        writeFits(path, values, bscale, bzero);
        return FitsVolumeLoader.load(path);
    }

    private static void checkBlankRanges(Path outputDirectory) throws Exception {
        Path bytePath = outputDirectory.resolve("synthetic-corona-byte-invalid-blank.fits");
        writeFits(bytePath, new byte[1][1][1], 1, 0, 256L);
        checkLoadRejected(bytePath, "FITS BLANK is outside the BITPIX=8 stored-value range");

        Path shortPath = outputDirectory.resolve("synthetic-corona-short-invalid-blank.fits");
        writeFits(shortPath, new short[1][1][1], 1, 0, 32768L);
        checkLoadRejected(shortPath, "FITS BLANK is outside the BITPIX=16 stored-value range");

        Path allBlankPath = outputDirectory.resolve("synthetic-corona-all-blank.fits");
        writeFits(allBlankPath, new byte[][][]{{{7}}}, 1, 0, 7L);
        checkLoadRejected(allBlankPath, "volume contains no defined samples");

        Path mixedBytePath = outputDirectory.resolve("synthetic-corona-byte-blank.fits");
        VolumeData mixedBytes = writeAndLoad(mixedBytePath, new byte[][][]{{{7, 8}}}, 1, 0, 7L);
        check(!isValid(mixedBytes, 0) && isValid(mixedBytes, 1), "BITPIX=8 BLANK validity mask");
        checkClose(normalizedSample(mixedBytes, 0), 0, "BITPIX=8 BLANK stored texture value");
        checkClose(normalizedSample(mixedBytes, 1), 8.0 / 255, "BITPIX=8 defined stored texture value");

        Path mixedShortPath = outputDirectory.resolve("synthetic-corona-short-blank.fits");
        VolumeData mixedShorts = writeAndLoad(mixedShortPath, new short[][][]{{{-23, 17}}}, -2, 10, -23L);
        check(!isValid(mixedShorts, 0) && isValid(mixedShorts, 1), "BITPIX=16 BLANK validity mask");
        checkClose(normalizedSample(mixedShorts, 0), 0, "BITPIX=16 BLANK stored texture value");
        float definedShort = Float.float16ToFloat(Float.floatToFloat16((Short.MAX_VALUE - 17) / 65535f));
        checkClose(normalizedSample(mixedShorts, 1), definedShort, "BITPIX=16 defined stored texture value");
    }

    private static VolumeData writeAndLoad(Path path, Object values, double bscale, double bzero, long blank) throws Exception {
        writeFits(path, values, bscale, bzero, blank);
        return FitsVolumeLoader.load(path);
    }

    private static void checkUnsupportedBitpix(Path outputDirectory) throws Exception {
        Object[] unsupportedCubes = {new int[2][2][2], new long[2][2][2], new float[2][2][2], new double[2][2][2]};
        for (Object cube : unsupportedCubes) {
            String type = cube.getClass().getComponentType().getComponentType().getComponentType().getSimpleName();
            Path path = outputDirectory.resolve("synthetic-corona-unsupported-" + type + ".fits");
            writeFits(path, cube, 1, 0);
            checkLoadRejected(path, "volume BITPIX must be 8 or 16");
        }
    }

    private static void checkStandardWcs(Path outputDirectory) throws Exception {
        VolumeData cd = writeAndLoadStandardWcs(outputDirectory.resolve("synthetic-corona-standard-cd.fits"), false);
        VolumeData pc = writeAndLoadStandardWcs(outputDirectory.resolve("synthetic-corona-standard-pc.fits"), true);
        // Reference values from Astropy WCS followed by SunPy Heliocentric-to-Carrington conversion,
        // using the WCS observer for the destination Carrington frame.
        checkVector(cd.corner(), 0.1552360253653355, -0.2891054049173499, 0.0761222127657105, "CD volume corner");
        checkVector(cd.axisX(), 0.0145252448496349, 0.0064427542834685, -0.0141247364329747, "CD volume X axis");
        checkVector(cd.axisY(), 0.0176485115557656, 0.0577247289474022, 0.0147440058266052, "CD volume Y axis");
        checkVector(cd.axisZ(), 0.0661579441105812, 0.0012564172176435, 0.1014571231941672, "CD volume Z axis");
        checkVector(pc.corner(), cd.corner().x, cd.corner().y, cd.corner().z, "PC volume corner");
        checkVector(pc.axisX(), cd.axisX().x, cd.axisX().y, cd.axisX().z, "PC volume X axis");
        checkVector(pc.axisY(), cd.axisY().x, cd.axisY().y, cd.axisY().z, "PC volume Y axis");
        checkVector(pc.axisZ(), cd.axisZ().x, cd.axisZ().y, cd.axisZ().z, "PC volume Z axis");
    }

    private static VolumeData writeAndLoadStandardWcs(Path path, boolean pc) throws Exception {
        byte[][][] values = new byte[4][3][2];
        ImageHDU hdu = (ImageHDU) Fits.makeHDU(values);
        Header header = hdu.getHeader();
        header.addValue("DATE-OBS", "2026-08-17T00:00:00.000", null);
        header.addValue("RSUN_REF", 695_700_000., 1, "[m] assumed physical solar radius");
        header.addValue("DSUN_OBS", 149_597_870_700., 1, "[m] distance from centre of Sun to observer");
        header.addValue("CRLN_OBS", 35, "[deg] Carrington longitude of observer");
        header.addValue("CRLT_OBS", -7, "[deg] Carrington latitude of observer");
        double[][] matrix = {
                {0.01, 0.002, -0.001},
                {0.003, 0.02, 0.004},
                {-0.002, 0.005, 0.03}
        };
        double[] referencePixel = {2, 3, 4};
        double[] referenceValue = {0.1, -0.2, 0.3};
        double[] scale = {0.01, 0.02, 0.03};
        String[] units = pc ? new String[]{"m", "km", "Mm"} : new String[]{"solRad", "solRad", "solRad"};
        double[] solarRadiiPerUnit = pc ? new double[]{1 / 695_700_000., 1_000 / 695_700_000., 1_000_000 / 695_700_000.}
                : new double[]{1, 1, 1};
        int[] components = pc ? new int[]{2, 0, 1} : new int[]{0, 1, 2};
        for (int row = 0; row < 3; row++) {
            int axis = row + 1;
            int component = components[row];
            header.addValue("CTYPE" + axis, AXIS_TYPES[component], "heliocentric Cartesian coordinate");
            header.addValue("CUNIT" + axis, units[row], "coordinate unit");
            header.addValue("CRPIX" + axis, referencePixel[row], "[pixel] reference pixel");
            header.addValue("CRVAL" + axis, referenceValue[component] / solarRadiiPerUnit[row], '[' + units[row] + "] coordinate at reference pixel");
            if (pc)
                header.addValue("CDELT" + axis, scale[component] / solarRadiiPerUnit[row],
                        '[' + units[row] + "/pixel] coordinate increment");
            for (int column = 0; column < 3; column++)
                header.addValue((pc ? "PC" : "CD") + axis + '_' + (column + 1),
                        pc ? matrix[component][column] / scale[component] : matrix[component][column], null);
        }
        Path absolute = path.toAbsolutePath().normalize();
        Files.createDirectories(absolute.getParent());
        Files.deleteIfExists(absolute);
        absolute.toFile().deleteOnExit();
        try (Fits fits = new Fits()) {
            fits.addHDU(hdu);
            fits.write(absolute.toFile());
        }
        return FitsVolumeLoader.load(absolute);
    }

    private static void checkLoadRejected(Path path, String expectedMessage) throws Exception {
        try {
            FitsVolumeLoader.load(path);
            throw new AssertionError("invalid FITS volume was accepted: " + path);
        } catch (IOException e) {
            check(e.getMessage().contains(expectedMessage), "unexpected FITS rejection: " + e.getMessage());
        }
    }

    private static void render(VolumeLayer layer8, Path output8, VolumeLayer layer16, Path output16) throws Exception {
        Platform.init();
        Directories.createPersistentDirs();
        Log.init();
        Directories.createCacheDirs();
        AppInit.loadSpice();

        AngleRenderer renderer = AngleRenderer.pbuffer(RENDER_SIZE, RENDER_SIZE);
        try {
            GLRenderer.reshape(RENDER_SIZE, RENDER_SIZE);
            Viewport vp = Display.getViewport(0);
            MapView mv = GLRenderer.getMapView();
            GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            GL.glClearColor(0, 0, 0, 1);
            Transform.ortho(vp.aspect, 7, 0, 0, Quat.createXY(Math.toRadians(18), Math.toRadians(-28)));
            renderLayer(layer8, output8, mv, vp);
            renderLayer(layer16, output16, mv, vp);
            checkColorAndOpacity(layer16, mv, vp);
        } finally {
            layer8.dispose();
            layer16.dispose();
            renderer.destroy();
        }
    }

    private static void renderLayer(VolumeLayer layer, Path output, MapView mv, Viewport vp) throws Exception {
        layer.init();
        layer.init();
        GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);
        layer.render(mv, vp);
        GLException.checkErrors("VolumeLayerTest.render");

        ByteBuffer pixels = BufferUtils.newByteBuffer(RENDER_SIZE * RENDER_SIZE * 4);
        GL.glReadPixels(0, 0, RENDER_SIZE, RENDER_SIZE, GL.RGBA, GL.UNSIGNED_BYTE, pixels);
        BufferedImage image = new BufferedImage(RENDER_SIZE, RENDER_SIZE, BufferedImage.TYPE_INT_RGB);
        int visiblePixels = 0;
        int brightPixels = 0;
        for (int y = 0; y < RENDER_SIZE; y++) {
            for (int x = 0; x < RENDER_SIZE; x++) {
                int offset = 4 * (y * RENDER_SIZE + x);
                int red = pixels.get(offset) & 0xff;
                int green = pixels.get(offset + 1) & 0xff;
                int blue = pixels.get(offset + 2) & 0xff;
                if ((red | green | blue) != 0)
                    visiblePixels++;
                if (red > 100)
                    brightPixels++;
                image.setRGB(x, RENDER_SIZE - 1 - y, red << 16 | green << 8 | blue);
            }
        }
        check(visiblePixels > 80_000, "too little volume data was rendered");
        check(brightPixels > 2_000, "volume rendering has no bright structure");

        layer.dispose();
        layer.init();
        GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);
        layer.render(mv, vp);
        ByteBuffer reinitializedPixels = BufferUtils.newByteBuffer(RENDER_SIZE * RENDER_SIZE * 4);
        GL.glReadPixels(0, 0, RENDER_SIZE, RENDER_SIZE, GL.RGBA, GL.UNSIGNED_BYTE, reinitializedPixels);
        check(reinitializedPixels.equals(pixels), "volume rendering changed after GL reinitialization");

        Path absoluteOutput = output.toAbsolutePath().normalize();
        Files.createDirectories(absoluteOutput.getParent());
        check(ImageIO.write(image, "png", absoluteOutput.toFile()), "PNG writer unavailable");
    }

    private static void checkColorAndOpacity(VolumeLayer layer, MapView mv, Viewport vp) {
        layer.setLUT(LUT.spectral());
        layer.setOpacity(0.5);
        GL.glClearColor(0, 0, 0, 0);
        GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);
        layer.render(mv, vp);
        ByteBuffer fullPixels = BufferUtils.newByteBuffer(RENDER_SIZE * RENDER_SIZE * 4);
        GL.glReadPixels(0, 0, RENDER_SIZE, RENDER_SIZE, GL.RGBA, GL.UNSIGNED_BYTE, fullPixels);

        layer.setCrop(0, 0, 0.5);
        GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);
        layer.render(mv, vp);

        ByteBuffer pixels = BufferUtils.newByteBuffer(RENDER_SIZE * RENDER_SIZE * 4);
        GL.glReadPixels(0, 0, RENDER_SIZE, RENDER_SIZE, GL.RGBA, GL.UNSIGNED_BYTE, pixels);
        int coloredPixels = 0;
        long fullAlpha = 0;
        long croppedAlpha = 0;
        for (int offset = 0; offset < pixels.limit(); offset += 4) {
            int red = pixels.get(offset) & 0xff;
            int green = pixels.get(offset + 1) & 0xff;
            int blue = pixels.get(offset + 2) & 0xff;
            int alpha = pixels.get(offset + 3) & 0xff;
            fullAlpha += fullPixels.get(offset + 3) & 0xff;
            croppedAlpha += alpha;
            check(red <= alpha + 1 && green <= alpha + 1 && blue <= alpha + 1, "volume output is not premultiplied");
            check(alpha <= 128, "volume opacity was not applied to alpha");
            if (alpha != 0 && (red != green || green != blue))
                coloredPixels++;
        }
        check(coloredPixels > 2_000, "color table did not produce colored volume pixels");
        check(croppedAlpha < fullAlpha, "cropping did not reduce the integrated volume");
        GL.glClearColor(0, 0, 0, 1);
    }

    private static void checkClose(double actual, double expected, String description) {
        check(Math.abs(actual - expected) < 1e-6, description + ": " + actual + " != " + expected);
    }

    private static void checkVector(Vec3 actual, double x, double y, double z, String description) {
        checkClose(actual.x, x, description + " X");
        checkClose(actual.y, y, description + " Y");
        checkClose(actual.z, z, description + " Z");
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

    private VolumeLayerTest() {}
}
