package org.helioviewer.jhv.opengl;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.opengl.angle.AngleRenderer;
import org.helioviewer.jhv.opengl.text.SdfTextRenderer;

import org.joml.Vector3f;
import org.json.JSONArray;
import org.json.JSONObject;

public final class SdfTextRenderingTest {
    private static final int SIZE = 512;
    private static final float SCALE = 0.125f;

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();
        AngleRenderer renderer = AngleRenderer.pbuffer(SIZE, SIZE);
        try {
            SdfTextRenderer text = GLText.renderer();
            JSONObject atlas;
            try (InputStream input = FileUtils.getResource("/sdf/atlas-128.json")) {
                atlas = JSONUtils.get(input);
            }
            float size = atlas.getJSONObject("atlas").getFloat("size");
            Map<Integer, Float> advances = new HashMap<>();
            JSONArray glyphs = atlas.getJSONArray("glyphs");
            for (int i = 0; i < glyphs.length(); i++) {
                JSONObject glyph = glyphs.getJSONObject(i);
                advances.put(glyph.getInt("unicode"), glyph.getFloat("advance") * size);
            }
            // String keys and JSON values form a reference independent of glyph/table indices.
            Map<String, Float> pairs = new HashMap<>();
            JSONArray kerning = atlas.getJSONArray("kerning");
            for (int i = 0; i < kerning.length(); i++) {
                JSONObject pair = kerning.getJSONObject(i);
                pairs.put(pair.getInt("unicode1") + ":" + pair.getInt("unicode2"), pair.getFloat("advance") * size);
            }
            Field field = SdfTextRenderer.class.getDeclaredField("coordBuf");
            field.setAccessible(true);
            BufCoord buffer = (BufCoord) field.get(text);
            for (int left : advances.keySet()) {
                for (int right : advances.keySet())
                    checkText(text, buffer, new String(Character.toChars(left)) + new String(Character.toChars(right)), advances, pairs);
            }
            for (String value : new String[]{"", " ", "AV To -A", "2026-01-15T12:00:00 Z", "A\u2603V", "T\uD83D\uDE80o"})
                checkText(text, buffer, value, advances, pairs);

            GLRenderer.reshape(SIZE, SIZE);
            for (boolean spatial : new boolean[]{false, true}) {
                ByteBuffer actual = render(text, "AV To -A 2026-01-15", advances, pairs, spatial, false);
                ByteBuffer expected = render(text, "AV To -A 2026-01-15", advances, pairs, spatial, true);
                if (!actual.equals(expected))
                    throw new AssertionError("Rendered text differs from independently positioned glyphs: spatial=" + spatial);
                boolean visible = false;
                for (int i = 0; i < actual.remaining(); i += 4)
                    visible |= actual.get(i) != 0;
                if (!visible)
                    throw new AssertionError("No text pixels: spatial=" + spatial);
            }
            GLException.checkErrors("SdfTextRenderingTest");
            System.out.println("SdfTextRenderingTest passed: " + advances.size() * advances.size() + " glyph pairs, widths, both draw paths and framebuffer references");
        } finally {
            renderer.destroy();
        }
    }

    private static int resolve(int codePoint, Map<Integer, Float> advances) {
        return advances.containsKey(codePoint) ? codePoint : '?';
    }

    private static float adjustment(int left, int right, Map<String, Float> pairs) {
        return pairs.getOrDefault(left + ":" + right, 0f);
    }

    private static void checkText(SdfTextRenderer text, BufCoord buffer, String value, Map<Integer, Float> advances, Map<String, Float> pairs) {
        float width = 0;
        int previous = -1;
        for (int codePoint : value.codePoints().toArray()) {
            int current = resolve(codePoint, advances);
            if (previous >= 0)
                width += adjustment(previous, current, pairs);
            width += advances.get(current);
            previous = current;
        }
        if (text.measureWidth(value) != width)
            throw new AssertionError("Incorrect width: " + value);
        for (boolean spatial : new boolean[]{false, true}) {
            buffer.clear();
            draw(text, value, 0, 0, spatial);
            FloatBuffer actual = buffer.toBuffer();
            float[] coordinates = new float[actual.remaining()];
            actual.get(coordinates);
            buffer.clear();
            drawReference(text, value, 0, 0, advances, pairs, spatial);
            FloatBuffer expected = buffer.toBuffer();
            if (coordinates.length != expected.remaining())
                throw new AssertionError("Incorrect vertex count: " + value);
            for (float coordinate : coordinates) {
                if (coordinate != expected.get())
                    throw new AssertionError("Incorrect glyph position: " + value + ", spatial=" + spatial);
            }
            buffer.clear();
        }
    }

    private static void draw(SdfTextRenderer text, String value, float x, float y, boolean spatial) {
        if (spatial)
            text.draw(value, new Vector3f(x, y, 0), new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), SCALE);
        else
            text.draw(value, x, y, 0, SCALE);
    }

    private static void drawReference(SdfTextRenderer text, String value, float x, float y, Map<Integer, Float> advances, Map<String, Float> pairs, boolean spatial) {
        float pen = 0;
        int previous = -1;
        for (int codePoint : value.codePoints().toArray()) {
            int current = resolve(codePoint, advances);
            if (previous >= 0)
                pen += adjustment(previous, current, pairs) * SCALE;
            draw(text, new String(Character.toChars(current)), x + pen, y, spatial);
            pen += advances.get(current) * SCALE;
            previous = current;
        }
    }

    private static ByteBuffer render(SdfTextRenderer text, String value, Map<Integer, Float> advances, Map<String, Float> pairs, boolean spatial, boolean reference) {
        GL.glViewport(0, 0, SIZE, SIZE);
        GL.glClearColor(0, 0, 0, 0);
        GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);
        Transform.ortho2D(1, SIZE, -SIZE / 2., -SIZE / 2.);
        if (spatial)
            text.begin3DRendering();
        else
            text.beginRendering(SIZE, SIZE);
        if (reference)
            drawReference(text, value, 0, 64, advances, pairs, spatial);
        else
            draw(text, value, 0, 64, spatial);
        if (spatial)
            text.end3DRendering();
        else
            text.endRendering();
        ByteBuffer pixels = BufferUtils.newByteBuffer(SIZE * SIZE * 4);
        GL.glReadPixels(0, 0, SIZE, SIZE, GL.RGBA, GL.UNSIGNED_BYTE, pixels);
        GLException.checkErrors("SdfTextRenderingTest.render");
        return pixels;
    }
}
