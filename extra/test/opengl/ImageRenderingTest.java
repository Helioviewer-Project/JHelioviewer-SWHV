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
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.image.ImageFilter;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.math.Mat2;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.metadata.Region;
import org.helioviewer.jhv.opengl.angle.AngleRenderer;
import org.helioviewer.jhv.time.JHVTime;
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
            ImageBuffer image = ImageBuffer.fromShorts(TEXTURE_SIZE, TEXTURE_SIZE, ImageBuffer.Format.Gray16F, samples, ImageFilter.NONE);
            ImageBuffer prior = ImageBuffer.fromShorts(TEXTURE_SIZE, TEXTURE_SIZE, ImageBuffer.Format.Gray16F, previous, ImageFilter.NONE);
            // Re-upload to exercise the streaming PBO path, not just initial allocation.
            texture.upload(prior);
            texture.upload(image);
            difference.upload(prior);
            ByteBuffer white = BufferUtils.newByteBuffer(1);
            white.put(0, (byte) 255);
            mask.upload2D(GLTexture.Format.R8, 1, 1, GL.NEAREST, white);
            float[] pv = new float[6];
            WcsHeader wcs = new WcsHeader(WcsHeader.Projection.TAN, pv, 215, Vec2.ZERO, Mat2.IDENTITY);
            Region region = new Region(-2, -2, 4, 4);
            float[] crval = {0, 0};
            GLSLImageShader.bindImages(region, Mat2.IDENTITY, crval, wcs, 215, 0, Quat.ZERO, Quat.ZERO,
                    region, Mat2.IDENTITY, crval, wcs, 215, 0, Quat.ZERO, Quat.ZERO);
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
        } finally {
            mask.delete();
            lut.delete();
            difference.delete();
            texture.delete();
            renderer.destroy();
        }
        System.out.println("ImageRenderingTest passed");
    }
}
