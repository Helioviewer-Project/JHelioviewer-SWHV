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
import org.helioviewer.jhv.display.MapMode;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.layers.GridLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.angle.AngleRenderer;
import org.helioviewer.jhv.time.JHVTime;

public final class GridRenderingTest {
    public static void main(String[] args) throws Exception {
        if (args.length > 1)
            throw new IllegalArgumentException("usage: GridRenderingTest [output-directory]");
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
        AngleRenderer renderer = AngleRenderer.pbuffer(1024, 1024);
        try {
            GridLayer grid = new GridLayer(null);
            Layers.add(grid);
            // The same logical canvas at 1x and 2x separates HiDPI from size effects.
            for (int[] size : new int[][]{{128, 128}, {256, 192}, {192, 384}, {512, 128}, {512, 512}}) {
                for (int scale : new int[]{1, 2}) {
                    int width = size[0] * scale;
                    int height = size[1] * scale;
                    Display.pixelScale[0] = scale;
                    Display.pixelScale[1] = scale;
                    GLRenderer.reshape(width, height);
                    for (MapMode mode : MapMode.values()) {
                        Display.mode = mode;
                        for (boolean labels : new boolean[]{false, true}) {
                            grid.setShowLabels(labels);
                            renderer.render(viewpoint);
                            ByteBuffer pixels = BufferUtils.newByteBuffer(width * height * 4);
                            GL.glReadPixels(0, 0, width, height, GL.RGBA, GL.UNSIGNED_BYTE, pixels);
                            GLException.checkErrors("GridRenderingTest " + mode);
                            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                            int colored = 0;
                            for (int y = 0; y < height; y++) {
                                for (int x = 0; x < width; x++) {
                                    int offset = 4 * (y * width + x);
                                    int r = pixels.get(offset) & 255;
                                    int g = pixels.get(offset + 1) & 255;
                                    int b = pixels.get(offset + 2) & 255;
                                    if (g > r + 10 && g > b + 10)
                                        colored++;
                                    image.setRGB(x, height - 1 - y, (r << 16) | (g << 8) | b);
                                }
                            }
                            if (colored == 0)
                                throw new AssertionError("No green grid pixels: " + mode + " " + width + "x" + height);
                            if (args.length == 1) {
                                Path directory = Path.of(args[0]);
                                Files.createDirectories(directory);
                                String name = mode + "-" + size[0] + "x" + size[1] + "-" + scale + "x-" + (labels ? "labels" : "lines");
                                if (!ImageIO.write(image, "png", directory.resolve(name + ".png").toFile()))
                                    throw new AssertionError("PNG writer unavailable");
                            }
                        }
                    }
                }
            }
        } finally {
            renderer.destroy();
        }
        System.out.println("GridRenderingTest passed");
    }
}
