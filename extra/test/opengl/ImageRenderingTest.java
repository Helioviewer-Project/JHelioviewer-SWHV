package org.helioviewer.jhv.opengl;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.TimeZone;

import javax.imageio.ImageIO;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.GridType;
import org.helioviewer.jhv.display.MapMode;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.image.DecodedImage;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.ImageGrid;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.math.Mat2;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.opengl.angle.AngleRenderer;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.wcs.ImageGeometry;
import org.helioviewer.jhv.wcs.WcsHeader;

public final class ImageRenderingTest {
    private static final int TEXTURE_SIZE = 64;

    public static void main(String[] args) throws Exception {
        if (args.length > 1)
            throw new IllegalArgumentException("usage: ImageRenderingTest [output-directory]");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Locale.setDefault(Locale.US);
        Platform.init();
        Directories.createPersistentDirs();
        Log.init();
        Directories.createCacheDirs();
        AppInit.loadSpice();
        Position viewpoint = Sun.getEarth(new JHVTime("2026-01-15T12:00:00"));
        DisplayController.setRenderRequestHandler(_ -> {});
        DisplayController.setViewpointUpdate(_ -> viewpoint, DisplayController.ViewpointApplyMode.KEEP_TRANSFORM);
        AngleRenderer renderer = AngleRenderer.pbuffer(512, 512);
        GLStreamingTexture2D texture = new GLStreamingTexture2D(GLTexture.Unit.ZERO, GL.LINEAR);
        GLStreamingTexture2D difference = new GLStreamingTexture2D(GLTexture.Unit.TWO, GL.LINEAR);
        GLTexture lut = new GLTexture(GL.TEXTURE_2D, GLTexture.Unit.ONE);
        GLTexture mask = new GLTexture(GL.TEXTURE_2D, GLTexture.Unit.THREE);
        try {
            short[] samples = new short[TEXTURE_SIZE * TEXTURE_SIZE];
            short[] previous = new short[samples.length];
            for (int y = 0; y < TEXTURE_SIZE; y++) {
                for (int x = 0; x < TEXTURE_SIZE; x++) {
                    float value = 0.15f + 0.65f * x / (TEXTURE_SIZE - 1) + ((x / 4 + y / 4) % 2) * 0.15f;
                    samples[y * TEXTURE_SIZE + x] = Float.floatToFloat16(value);
                    previous[y * TEXTURE_SIZE + x] = Float.floatToFloat16(0.4f);
                }
            }
            ImageBuffer image = ImageBuffer.fromShorts(TEXTURE_SIZE, TEXTURE_SIZE, samples, ImageFilter.NONE);
            ImageBuffer prior = ImageBuffer.fromShorts(TEXTURE_SIZE, TEXTURE_SIZE, previous, ImageFilter.NONE);
            ImageBuffer odd = ImageBuffer.fromBytes(13, 11, new byte[13 * 11]);
            texture.upload(odd);
            lut.upload2D(GLTexture.Format.RGBA8, 256, 1, GL.NEAREST, LUT.gray().rgba());
            texture.upload(odd);
            if (GLException.checkErrors("ImageRenderingTest Gray8 13x11"))
                throw new AssertionError("Odd-width grayscale upload failed");
            // Re-upload to exercise the streaming PBO path, not just initial allocation.
            texture.upload(prior);
            texture.upload(image);
            difference.upload(prior);
            ByteBuffer white = BufferUtils.newByteBuffer(1);
            white.put(0, (byte) 255);
            mask.upload2D(GLTexture.Format.R8, 1, 1, GL.NEAREST, white);
            float[] pv = new float[6];
            WcsHeader wcs = new WcsHeader(WcsHeader.Projection.TAN, pv, 215, Vec2.ZERO);
            ImageGeometry geometry = new ImageGeometry(TEXTURE_SIZE, TEXTURE_SIZE, wcs,
                    new Mat2(4.0 / TEXTURE_SIZE, 0, 0, -4.0 / TEXTURE_SIZE), new Vec2(TEXTURE_SIZE / 2.0, TEXTURE_SIZE / 2.0));
            float[] crval = {0, 0};
            GLSLImageShader.bindImages(geometry, crval, 215, 0, Quat.ZERO, Quat.ZERO,
                    geometry, crval, 215, 0, Quat.ZERO, Quat.ZERO, Mat2.IDENTITY);
            for (int size : new int[]{128, 257, 512}) {
                GLRenderer.reshape(size, size);
                Viewport vp = Display.getViewport(0);
                GL.glViewport(0, 0, size, size);
                for (MapMode mode : MapMode.values()) {
                    MapScale scale = switch (mode) {
                        case Orthographic -> MapScale.ortho;
                        case HPC -> MapScale.hpc(0.6, 0.6);
                        case Latitudinal -> MapScale.lati;
                        case RadialWarp, RectWarp -> MapScale.boxCoxRadial(3, 0);
                    };
                    MapView view = MapView.create(Display.getCamera(), viewpoint, mode, GridType.Viewpoint, new MapScale[]{scale});
                    if (mode == MapMode.Orthographic)
                        Transform.ortho(1, 4, 0, 0, Quat.ZERO);
                    else
                        Transform.ortho2D(1, 1, 0, 0);
                    GLSLScreenShader.setView(view, vp);
                    for (String effect : new String[]{"gray", "inverted", "levels", "sharpen", "difference", "gamma", "mask", "blend"}) {
                        lut.upload2D(GLTexture.Format.RGBA8, 256, 1, GL.NEAREST,
                                effect.equals("inverted") ? LUT.gray().rgbaInv() : LUT.gray().rgba());
                        mask.bind();
                        difference.bind();
                        texture.bind();
                        boolean blend = effect.equals("blend");
                        GL.glClearColor(blend ? 0.2f : 0, blend ? 0.1f : 0, blend ? 0.3f : 0, 1);
                        GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);
                        float opacity = blend ? 0.5f : 1;
                        int diff = effect.equals("difference") ? 1 : 0;
                        GLSLImageShader.bindDisplay(new float[]{opacity, opacity, opacity, opacity},
                                1f / TEXTURE_SIZE, 1f / TEXTURE_SIZE, effect.equals("sharpen") ? -1 : 0, diff,
                                effect.equals("levels") ? -0.2f : 0, effect.equals("levels") ? 1.5f : 1,
                                effect.equals("gamma") ? 0.5f : 1, 1, 0, 0, 0, 0, 0, 0, -1, 0,
                                effect.equals("mask") ? 0.5f : 0, 100, 0, 1, 0);
                        GLSLImageShader.render(mode, pv, pv);
                        ByteBuffer pixels = BufferUtils.newByteBuffer(size * size * 4);
                        GL.glReadPixels(0, 0, size, size, GL.RGBA, GL.UNSIGNED_BYTE, pixels);
                        GLException.checkErrors("ImageRenderingTest " + mode + " " + effect);
                        BufferedImage rendered = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
                        int min = 255, max = 0;
                        for (int y = 0; y < size; y++) {
                            for (int x = 0; x < size; x++) {
                                int offset = 4 * (y * size + x);
                                int r = pixels.get(offset) & 255;
                                min = Math.min(min, r);
                                max = Math.max(max, r);
                                rendered.setRGB(x, size - y - 1, (r << 16) | ((pixels.get(offset + 1) & 255) << 8) | (pixels.get(offset + 2) & 255));
                            }
                        }
                        if (max - min < 20)
                            throw new AssertionError("Empty/flat image: " + mode + " " + effect);
                        if (args.length == 1) {
                            Path directory = Path.of(args[0]);
                            Files.createDirectories(directory);
                            if (!ImageIO.write(rendered, "png", directory.resolve(mode + "-" + size + "-" + effect + ".png").toFile()))
                                throw new AssertionError("PNG writer unavailable");
                        }
                    }
                }
            }
            sampling(texture, difference, lut, mask, viewpoint);
        } finally {
            mask.delete();
            lut.delete();
            difference.delete();
            texture.delete();
            renderer.destroy();
        }
        System.out.println("ImageRenderingTest passed");
    }

    private static void sampling(GLStreamingTexture2D texture, GLStreamingTexture2D difference,
                                 GLTexture lut, GLTexture mask, Position viewpoint) {
        int size = 257;
        Position centeredView = new Position(viewpoint.time, 215, 0, 0);
        GLRenderer.reshape(size, size);
        GL.glViewport(0, 0, size, size);
        lut.upload2D(GLTexture.Format.RGBA8, 256, 1, GL.NEAREST, LUT.gray().rgba());
        // Keep samples away from decoder boundary extension. The asymmetric detector
        // mask and slit must cover exactly the same screen pixels at every level.
        ByteBuffer detector = BufferUtils.newByteBuffer(8 * 8);
        for (int y = 3; y < 5; y++)
            for (int x = 2; x < 6; x++)
                detector.put(y * 8 + x, (byte) 255);
        mask.upload2D(GLTexture.Format.R8, 8, 8, GL.NEAREST, detector);
        float[] pv = new float[6];
        WcsHeader wcs = new WcsHeader(WcsHeader.Projection.TAN, pv, 215, Vec2.ZERO);
        for (int[] geometry : new int[][]{{128, 96, 0, 0}, {129, 97, 0, 0}, {129, 97, 3, 5}}) {
            ImageGeometry full = new ImageGeometry(geometry[0], geometry[1], wcs,
                    new Mat2(4.0 / geometry[0], 0, 0, -4.0 / geometry[1]), new Vec2(geometry[0] / 2.0, geometry[1] / 2.0));
            DecodedImage reference = ramp(full, geometry[2], geometry[3], 0);
            try {
                for (MapMode mode : MapMode.values()) {
                    MapScale scale = switch (mode) {
                        case Orthographic -> MapScale.ortho;
                        case HPC -> MapScale.hpc(0.6, 0.6);
                        case Latitudinal -> MapScale.lati;
                        case RadialWarp, RectWarp -> MapScale.boxCoxRadial(3, 0);
                    };
                    MapView view = MapView.create(Display.getCamera(), centeredView, mode, GridType.Viewpoint, new MapScale[]{scale});
                    if (mode == MapMode.Orthographic)
                        Transform.ortho(1, 4, 0, 0, Quat.ZERO);
                    else
                        Transform.ortho2D(1, 1, 0, 0);
                    GLSLScreenShader.setView(view, Display.getViewport(0));
                    for (int effect = 0; effect < 4; effect++) {
                        ByteBuffer expected = renderSampling(texture, difference, mask, reference, reference, mode, wcs, effect, size, Mat2.IDENTITY);
                        for (int level : new int[]{1, 3}) {
                            DecodedImage image = ramp(full, geometry[2], geometry[3], level);
                            DecodedImage prior = ramp(full, geometry[2], geometry[3], level + 1);
                            try {
                                ByteBuffer actual = renderSampling(texture, difference, mask, image, prior, mode, wcs, effect, size, Mat2.IDENTITY);
                                int visible = 0;
                                for (int i = 0; i < actual.capacity(); i += 4) {
                                    int a = actual.get(i) & 255, e = expected.get(i) & 255;
                                    if ((a == 0) != (e == 0) || Math.abs(a - e) > 2)
                                        throw new AssertionError("Sampling/mask/slit mismatch: " + mode + " effect " + effect
                                                + " grid " + full.grid + " origin " + geometry[2] + "," + geometry[3] + " level " + level + " pixel " + i / 4 + ": " + a + " vs " + e);
                                    if (e != 0) visible++;
                                }
                                if (visible < 20)
                                    throw new AssertionError("Sampling test rendered no useful pixels: " + mode);
                            } finally {
                                prior.release();
                                image.release();
                            }
                        }
                    }
                }
            } finally {
                reference.release();
            }
        }
        transforms(texture, difference, mask, wcs, centeredView, size);
        sharpening(texture, difference, mask, wcs, centeredView, size);
        wrapping(texture, difference, mask, centeredView, size);
        System.out.println("PASS: reduced-grid alignment, fixed detector masks and slits, CAR/CEA wrapping, and nonlinear sharpening against a CPU reference");
    }

    private static void transforms(GLStreamingTexture2D texture, GLStreamingTexture2D difference, GLTexture mask,
                                   WcsHeader wcs, Position viewpoint, int size) {
        MapView view = MapView.create(Display.getCamera(), viewpoint, MapMode.Orthographic, GridType.Viewpoint, new MapScale[]{MapScale.ortho});
        Transform.ortho(1, 4, 0, 0, Quat.ZERO);
        GLSLScreenShader.setView(view, Display.getViewport(0));
        Vec2 referencePixel = new Vec2(61.25, 45.75);
        ImageGeometry referenceGeometry = new ImageGeometry(129, 97, wcs,
                new Mat2(.04, .01, .005, -.06), referencePixel);
        // Rotate both plane axes by 90 degrees. Applying the same rotation to
        // the plane input must recover the reference, including its shear and unequal scales.
        ImageGeometry rotated = new ImageGeometry(129, 97, wcs,
                new Mat2(-.005, .06, .04, .01), referencePixel);
        Mat2 adjustment = new Mat2(0, -1, 1, 0);
        DecodedImage reference = ramp(referenceGeometry, 3, 5, 0);
        try {
            for (int level : new int[]{0, 3}) {
                DecodedImage image = ramp(rotated, 3, 5, level);
                DecodedImage prior = ramp(rotated, 3, 5, level + 1);
                try {
                    for (int effect = 0; effect < 4; effect++) {
                        ByteBuffer expected = renderSampling(texture, difference, mask, reference, reference,
                                MapMode.Orthographic, wcs, effect, size, Mat2.IDENTITY);
                        ByteBuffer actual = renderSampling(texture, difference, mask, image, prior,
                                MapMode.Orthographic, wcs, effect, size, adjustment);
                        int visible = 0;
                        for (int i = 0; i < actual.capacity(); i += 4) {
                            int a = actual.get(i) & 255, e = expected.get(i) & 255;
                            if ((a == 0) != (e == 0) || Math.abs(a - e) > 2)
                                throw new AssertionError("Rotated sampling mismatch: level " + level + " effect " + effect + " pixel " + i / 4);
                            if (e != 0) visible++;
                        }
                        if (visible < 20)
                            throw new AssertionError("Rotation test rendered no useful pixels");
                    }
                } finally {
                    prior.release();
                    image.release();
                }
            }
        } finally {
            reference.release();
        }
        System.out.println("PASS: Java shader binding composes user rotation, shear, unequal scales and reduced grids");
    }

    private static void wrapping(GLStreamingTexture2D texture, GLStreamingTexture2D difference, GLTexture mask,
                                 Position viewpoint, int size) {
        MapView view = MapView.create(Display.getCamera(), viewpoint, MapMode.Latitudinal, GridType.Viewpoint, new MapScale[]{MapScale.lati});
        Transform.ortho2D(1, 1, 0, 0);
        GLSLScreenShader.setView(view, Display.getViewport(0));
        for (WcsHeader.Projection projection : new WcsHeader.Projection[]{WcsHeader.Projection.CAR, WcsHeader.Projection.CEA}) {
            float[] pv = {0, 1, 0, 0, 0, 0};
            WcsHeader wcs = new WcsHeader(projection, pv, 1, Vec2.ZERO);
            double height = projection == WcsHeader.Projection.CAR ? Math.PI : 2;
            // The mask and slit admit only coordinates that started negative and wrapped.
            // Odd dimensions and a nonzero decoder origin also exercise scale and offset.
            ImageGeometry full = new ImageGeometry(129, 97, wcs,
                    new Mat2(2 * Math.PI / 129, 0, 0, -height / 97), new Vec2(-129.0 / 2, 97.0 / 2));
            DecodedImage reference = ramp(full, 3, 5, 0);
            DecodedImage image = ramp(full, 3, 5, 3);
            DecodedImage prior = ramp(full, 3, 5, 4);
            try {
                for (int effect = 0; effect < 4; effect++) {
                    ByteBuffer expected = renderSampling(texture, difference, mask, reference, reference, MapMode.Latitudinal, wcs, effect, size, Mat2.IDENTITY);
                    ByteBuffer actual = renderSampling(texture, difference, mask, image, prior, MapMode.Latitudinal, wcs, effect, size, Mat2.IDENTITY);
                    int visible = 0;
                    for (int i = 0; i < actual.capacity(); i += 4) {
                        int a = actual.get(i) & 255, e = expected.get(i) & 255;
                        if ((a == 0) != (e == 0) || Math.abs(a - e) > 2)
                            throw new AssertionError("Wrapped sampling mismatch: " + projection + " effect " + effect
                                    + " pixel " + i / 4 + ": " + a + " vs " + e);
                        if (e != 0) visible++;
                    }
                    if (visible < 20)
                        throw new AssertionError("Wrapping test rendered no useful pixels: " + projection);
                }
            } finally {
                prior.release();
                image.release();
                reference.release();
            }
        }
    }

    private static void sharpening(GLStreamingTexture2D texture, GLStreamingTexture2D difference, GLTexture mask,
                                   WcsHeader wcs, Position viewpoint, int size) {
        // Odd dimensions make the two decoded texture extents differ. At the center,
        // both grids have a sample; the comparison image's blur taps are half as far apart.
        ImageGeometry full = new ImageGeometry(129, 97, wcs, new Mat2(4.0 / 129, 0, 0, -4.0 / 97), new Vec2(64.5, 48.5));
        MapView view = MapView.create(Display.getCamera(), viewpoint, MapMode.Orthographic, GridType.Viewpoint, new MapScale[]{MapScale.ortho});
        Transform.ortho(1, 4, 0, 0, Quat.ZERO);
        GLSLScreenShader.setView(view, Display.getViewport(0));
        DecodedImage[] images = new DecodedImage[2];
        try {
            for (int i = 0; i < images.length; i++) {
                ImageGrid grid = new ImageGrid(3 + i, i == 0 ? 17 : 9, i == 0 ? 13 : 7);
                short[] pixels = new short[grid.width() * grid.height()];
                for (int y = 0; y < grid.height(); y++) {
                    for (int x = 0; x < grid.width(); x++) {
                        int parity = (x - grid.width() / 2 + y - grid.height() / 2) & 1;
                        pixels[y * grid.width() + x] = Float.floatToFloat16(parity == 0 ? .9f : .1f);
                    }
                }
                images[i] = new DecodedImage(ImageBuffer.fromShorts(grid.width(), grid.height(), pixels, ImageFilter.NONE), full.forGrid(grid));
            }
            // CPU reference: bilinear samples of the checkerboard at nine weighted taps.
            // Distances are in texels, independently of the shader's normalized coordinates.
            double[] weights = {.30613, .38774, .30613};
            double blur = 0, differenceBlur = 0;
            for (int y = -1; y <= 1; y++) {
                for (int x = -1; x <= 1; x++) {
                    double dx = x * 1.2004377, dy = y * 1.2004377;
                    double weight = weights[x + 1] * weights[y + 1];
                    double sample = checkerSample(dx, dy);
                    blur += weight * sample;
                    differenceBlur += weight * (sample - checkerSample(dx / 2, dy / 2));
                }
            }
            for (int effect : new int[]{1, 3}) {
                double value = effect == 1 ? 2 * checkerSample(0, 0) - blur : .5 - 2.5 * differenceBlur;
                int expected = (int) Math.clamp(Math.floor(value * 256), 0, 255);
                ByteBuffer actual = renderSampling(texture, difference, mask, images[0], images[1], MapMode.Orthographic, wcs, effect, size, Mat2.IDENTITY);
                int center = actual.get(4 * (size * (size / 2) + size / 2)) & 255;
                // Dithering adds at most one grayscale step; LUT quantization adds another.
                if (Math.abs(center - expected) > 2)
                    throw new AssertionError("Nonlinear sharpening differs from CPU reference: effect " + effect + ": " + center + " vs " + expected);
            }
        } finally {
            for (DecodedImage image : images)
                if (image != null)
                    image.release();
        }
    }

    private static double checkerSample(double x, double y) {
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y);
        double low = Float.float16ToFloat(Float.floatToFloat16(.1f));
        double high = Float.float16ToFloat(Float.floatToFloat16(.9f));
        double sign = ((ix + iy) & 1) == 0 ? 1 : -1;
        return (high + low) / 2 + (high - low) / 2 * sign * (1 - 2 * (x - ix)) * (1 - 2 * (y - iy));
    }

    private static DecodedImage ramp(ImageGeometry full, int originX, int originY, int reduction) {
        int step = 1 << reduction;
        int x0 = Math.ceilDiv(originX, step), y0 = Math.ceilDiv(originY, step);
        int width = Math.ceilDiv(originX + full.grid.width(), step) - x0;
        int height = Math.ceilDiv(originY + full.grid.height(), step) - y0;
        double firstX = x0 * step - originX + .5, firstY = y0 * step - originY + .5;
        short[] pixels = new short[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double px = firstX + x * step, py = firstY + y * step;
                pixels[y * width + x] = Float.floatToFloat16((float) (.1 + .35 * px / full.grid.width() + .3 * py / full.grid.height()));
            }
        }
        ImageGrid grid = new ImageGrid(reduction, width, height, firstX, firstY);
        return new DecodedImage(ImageBuffer.fromShorts(width, height, pixels, ImageFilter.NONE), full.forGrid(grid));
    }

    private static ByteBuffer renderSampling(GLStreamingTexture2D texture, GLStreamingTexture2D difference, GLTexture mask,
                                             DecodedImage image, DecodedImage prior, MapMode mode, WcsHeader wcs, int effect, int size, Mat2 adjustment) {
        texture.upload(image.imageBuffer());
        difference.upload(prior.imageBuffer());
        mask.bind();
        difference.bind();
        texture.bind();
        float[] crval = {0, 0};
        GLSLImageShader.bindImages(image.geometry(), crval, 215, 0, Quat.ZERO, Quat.ZERO,
                prior.geometry(), crval, 215, 0, Quat.ZERO, Quat.ZERO, adjustment);
        GLSLImageShader.bindDisplay(new float[]{1, 1, 1, 1}, 1f / image.imageBuffer().width, 1f / image.imageBuffer().height,
                (effect & 1) != 0 ? -1 : 0, effect >= 2 ? 1 : 0,
                0, 1, 1, 1, 0, 0, 0, 0, 0, 0, -1, 0, 0, 100, .375f, .625f, 0);
        GL.glClearColor(0, 0, 0, 1);
        GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);
        GLSLImageShader.render(mode, wcs.pv2, wcs.pv2);
        ByteBuffer pixels = BufferUtils.newByteBuffer(size * size * 4);
        GL.glReadPixels(0, 0, size, size, GL.RGBA, GL.UNSIGNED_BYTE, pixels);
        if (GLException.checkErrors("ImageRenderingTest sampling"))
            throw new AssertionError("Sampling render failed");
        return pixels;
    }
}
