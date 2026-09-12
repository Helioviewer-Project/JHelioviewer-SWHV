package org.helioviewer.jhv.opengl;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.TimeZone;

import javax.imageio.ImageIO;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.opengl.angle.AngleRenderer;

public final class ColoredVertexRenderingTest {

    private static final int SIZE = 256;
    private static final float VIEW_WIDTH = 4;
    private static final float POINT_SIZE = 32;
    private static final float POINT_CORNER_OFFSET = 15 * VIEW_WIDTH / SIZE;

    public static void main(String[] args) throws Exception {
        if (args.length > 1)
            throw new IllegalArgumentException("usage: ColoredVertexRenderingTest [output.png]");

        checkVertexData();
        initApplication();
        AngleRenderer renderer = AngleRenderer.pbuffer(SIZE, SIZE);
        GLSLLine line = new GLSLLine(true);
        GLSLShape shape = new GLSLShape(true);
        try {
            GLRenderer.reshape(SIZE, SIZE);
            line.init();
            shape.init();

            Viewport vp = Display.getViewport(0);
            GL.glViewport(vp.x, vp.yGL, vp.width, vp.height);
            GL.glClearColor(0, 0, 0, 0);
            GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);
            Transform.ortho(vp.aspect, VIEW_WIDTH, 0, 0, Quat.ZERO);

            drawTriangles(shape);
            drawPoints(shape);
            drawLines(line, vp);
            GLException.checkErrors("ColoredVertexRenderingTest.render");

            ByteBuffer pixels = readPixels();
            checkColor(pixels, -1.4f, 1.05f, Colors.Red.bytes(), "first triangle");
            checkColor(pixels, 1.4f, 1.05f, Colors.Green.bytes(), "triangle after replacement upload");
            checkColor(pixels, -1.4f, -1.35f, Colors.Cyan.bytes(), "cyan point");
            checkColor(pixels, 1.4f, -1.35f, Colors.Magenta.bytes(), "magenta point");
            checkBlack(pixels, -1.4f + POINT_CORNER_OFFSET, -1.35f + POINT_CORNER_OFFSET, "point bounding-box corner");
            checkColor(pixels, -0.9f, -0.15f, Colors.Yellow.bytes(), "joined polyline");
            checkColor(pixels, 0.45f, 0, Colors.Blue.bytes(), "first disconnected segment");
            checkColor(pixels, 1.35f, 0, Colors.Blue.bytes(), "second disconnected segment");
            checkBlack(pixels, 0.9f, -0.25f, "gap between disconnected segments");

            if (args.length == 1)
                writeImage(pixels, Path.of(args[0]));
        } finally {
            shape.dispose();
            line.dispose();
            renderer.destroy();
        }
        System.out.println("ColoredVertexRenderingTest passed");
    }

    private static void checkVertexData() {
        byte[] firstColor = {1, 2, 3, 4};
        byte[] secondColor = {5, 6, 7, 8};
        BufVertex vertices = new BufVertex();
        vertices.putVertex(1.25f, -2.5f, 3.75f, 1, firstColor);
        vertices.putVertex(1.25f, -2.5f, 3.75f, 1, secondColor);

        ByteBuffer buffer = vertices.toBuffer().duplicate().order(ByteOrder.nativeOrder());
        check(buffer.remaining() == 2 * BufVertex.BYTES_PER_VERTEX, "Incorrect interleaved buffer size");
        checkVertex(buffer, 0, 1.25f, -2.5f, 3.75f, 1, firstColor);
        checkVertex(buffer, 1, 1.25f, -2.5f, 3.75f, 1, secondColor);

        DirectBufVertex direct = new DirectBufVertex(vertices);
        check(direct.count() == vertices.getCount() && direct.buffer().equals(vertices.toBuffer()), "Incorrect retained vertex buffer");

        vertices.clear();
        vertices.startLine(1, 2, 3, 1, firstColor);
        vertices.putVertex(4, 5, 6, 1, secondColor);
        vertices.endLine();

        ByteBuffer retained = direct.buffer().duplicate().order(ByteOrder.nativeOrder());
        checkVertex(retained, 0, 1.25f, -2.5f, 3.75f, 1, firstColor);
        checkVertex(retained, 1, 1.25f, -2.5f, 3.75f, 1, secondColor);

        buffer = vertices.toBuffer().duplicate().order(ByteOrder.nativeOrder());
        check(buffer.remaining() == 4 * BufVertex.BYTES_PER_VERTEX, "Incorrect line buffer size");
        checkVertex(buffer, 0, 1, 2, 3, 1, Colors.Null);
        checkVertex(buffer, 1, 1, 2, 3, 1, firstColor);
        checkVertex(buffer, 2, 4, 5, 6, 1, secondColor);
        checkVertex(buffer, 3, 4, 5, 6, 1, Colors.Null);

        BufVertex grown = new BufVertex(64);
        for (int i = 0; i <= 64; i++)
            grown.putVertex(i, -i, i / 2f, 1, firstColor);
        buffer = grown.toBuffer().duplicate().order(ByteOrder.nativeOrder());
        check(buffer.remaining() == 65 * BufVertex.BYTES_PER_VERTEX, "Incorrect grown buffer size");
        checkVertex(buffer, 0, 0, 0, 0, 1, firstColor);
        checkVertex(buffer, 64, 64, -64, 32, 1, firstColor);
    }

    private static void checkVertex(ByteBuffer buffer, int index, float x, float y, float z, float w, byte[] color) {
        int offset = index * BufVertex.BYTES_PER_VERTEX;
        check(buffer.getFloat(offset) == x && buffer.getFloat(offset + 4) == y && buffer.getFloat(offset + 8) == z
                && buffer.getFloat(offset + 12) == w, "Incorrect vertex position");
        for (int i = 0; i < 4; i++)
            check(buffer.get(offset + 16 + i) == color[i], "Incorrect vertex color");
    }

    private static void initApplication() throws Exception {
        System.setProperty("user.timezone", TimeZone.getDefault().getID());
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Locale.setDefault(Locale.US);
        Platform.init();
        Directories.createPersistentDirs();
        Log.init();
        Directories.createCacheDirs();
        AppInit.loadSpice();
    }

    private static void drawTriangles(GLSLShape shape) {
        BufVertex vertices = new BufVertex();
        vertices.putVertex(-1.75f, 0.7f, 0, 1, Colors.Red.bytes());
        vertices.putVertex(-1.05f, 0.7f, 0, 1, Colors.Red.bytes());
        vertices.putVertex(-1.4f, 1.4f, 0, 1, Colors.Red.bytes());
        shape.uploadAndClear(vertices);
        shape.renderShape(GL.TRIANGLES);

        vertices.putVertex(1.05f, 0.7f, 0, 1, Colors.Green.bytes());
        vertices.putVertex(1.75f, 0.7f, 0, 1, Colors.Green.bytes());
        vertices.putVertex(1.4f, 1.4f, 0, 1, Colors.Green.bytes());
        shape.uploadAndClear(vertices);
        shape.renderShape(GL.TRIANGLES);
    }

    private static void drawPoints(GLSLShape shape) {
        BufVertex vertices = new BufVertex(2);
        vertices.putVertex(-1.4f, -1.35f, 0, POINT_SIZE, Colors.Cyan.bytes());
        vertices.putVertex(1.4f, -1.35f, 0, POINT_SIZE, Colors.Magenta.bytes());
        shape.uploadAndClear(vertices);
        shape.renderPoints(1);
    }

    private static void drawLines(GLSLLine line, Viewport vp) {
        BufVertex vertices = new BufVertex();
        polyline(vertices, Colors.Yellow.bytes(), -1.65f, 0.2f, -0.9f, -0.15f, -0.2f, 0.2f);
        polyline(vertices, Colors.Blue.bytes(), 0.2f, 0.25f, 0.7f, -0.25f);
        polyline(vertices, Colors.Blue.bytes(), 1.1f, -0.25f, 1.6f, 0.25f);
        line.upload(new DirectBufVertex(vertices));
        line.renderLine(vp, 0.025);
    }

    private static void polyline(BufVertex vertices, byte[] color, float... coordinates) {
        vertices.startLine(coordinates[0], coordinates[1], 0, 1, color);
        for (int i = 2; i < coordinates.length; i += 2)
            vertices.putVertex(coordinates[i], coordinates[i + 1], 0, 1, color);
        vertices.endLine();
    }

    private static ByteBuffer readPixels() {
        ByteBuffer pixels = BufferUtils.newByteBuffer(4 * SIZE * SIZE);
        GL.glReadPixels(0, 0, SIZE, SIZE, GL.RGBA, GL.UNSIGNED_BYTE, pixels);
        return pixels;
    }

    private static void checkColor(ByteBuffer pixels, float x, float y, byte[] expected, String label) {
        for (int channel = 0; channel < 3; channel++) {
            int value = maxChannel(pixels, x, y, channel);
            if (expected[channel] == 0)
                check(value <= 20, label + " has unexpected channel " + channel + " value " + value);
            else
                check(value >= 180, label + " channel " + channel + " is " + value);
        }
    }

    private static void checkBlack(ByteBuffer pixels, float x, float y, String label) {
        for (int channel = 0; channel < 3; channel++)
            check(maxChannel(pixels, x, y, channel) == 0, label + " is not empty");
    }

    private static int maxChannel(ByteBuffer pixels, float x, float y, int channel) {
        int centerX = Math.round(SIZE * (0.5f + x / VIEW_WIDTH));
        int centerY = Math.round(SIZE * (0.5f + y / VIEW_WIDTH));
        int maximum = 0;
        for (int py = centerY - 2; py <= centerY + 2; py++) {
            for (int px = centerX - 2; px <= centerX + 2; px++)
                maximum = Math.max(maximum, pixels.get(4 * (py * SIZE + px) + channel) & 0xff);
        }
        return maximum;
    }

    private static void writeImage(ByteBuffer pixels, Path output) throws Exception {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int offset = 4 * (y * SIZE + x);
                int rgb = (pixels.get(offset) & 0xff) << 16 | (pixels.get(offset + 1) & 0xff) << 8 | pixels.get(offset + 2) & 0xff;
                image.setRGB(x, SIZE - 1 - y, rgb);
            }
        }
        Path absolute = output.toAbsolutePath().normalize();
        if (absolute.getParent() != null)
            Files.createDirectories(absolute.getParent());
        check(ImageIO.write(image, "png", absolute.toFile()), "PNG writer unavailable");
        System.out.println("Rendered image: " + absolute);
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

    private ColoredVertexRenderingTest() {}
}
