package org.helioviewer.jhv.opengl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.opengl.text.JhvTextRenderer;

import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class GLText {
    private static final int MIN = 10;
    private static final int MAX = 144;
    private static final int STEP = 1;
    private static final int SIZE = (MAX - MIN) / STEP + 1;
    private static final JhvTextRenderer[] renderers = new JhvTextRenderer[SIZE];
    private static ByteBuffer canvasFontData;
    private static STBTTFontinfo canvasFontInfo;

    public static final float[] shadowColor = {0.1f, 0.1f, 0.1f, 0.75f};
    public static final int[] shadowOffset = {2, -2};

    public static JhvTextRenderer getRenderer(int size) {
        int idx = rendererIndex(size);

        if (renderers[idx] == null) {
            renderers[idx] = new JhvTextRenderer(rendererSize(idx), getCanvasFontData());
            // precache for grid text
            renderers[idx].draw3D("-0123456789.", 0, 0, 0, 0);
        }
        return renderers[idx];
    }

    public static void dispose() {
        for (int i = 0; i < SIZE; i++) {
            if (renderers[i] != null) {
                renderers[i].dispose();
                renderers[i] = null;
            }
        }
        if (canvasFontInfo != null) {
            canvasFontInfo.free();
            canvasFontInfo = null;
        }
        if (canvasFontData != null) {
            MemoryUtil.memFree(canvasFontData);
            canvasFontData = null;
        }
    }

    private static int rendererIndex(int size) {
        size = (int) (size * Display.pixelScale[1]);

        int idx = (size - MIN) / STEP;
        if (idx < 0)
            idx = 0;
        else if (idx >= SIZE)
            idx = SIZE - 1;
        return idx;
    }

    private static float rendererSize(int idx) {
        return idx * STEP + MIN;
    }

    private static ByteBuffer getCanvasFontData() {
        if (canvasFontData != null)
            return canvasFontData.duplicate();

        try (InputStream is = FileUtils.getResource("/fonts/DejaVuSansCondensed.ttf")) {
            byte[] bytes = is.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes).flip();
            canvasFontData = buffer;
            return canvasFontData.duplicate();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load canvas font", e);
        }
    }

    private static STBTTFontinfo getCanvasFontInfo() {
        if (canvasFontInfo != null)
            return canvasFontInfo;

        canvasFontInfo = STBTTFontinfo.create();
        if (!STBTruetype.stbtt_InitFont(canvasFontInfo, getCanvasFontData())) {
            canvasFontInfo.free();
            canvasFontInfo = null;
            throw new IllegalStateException("Failed to initialize canvas font");
        }
        return canvasFontInfo;
    }

    private static float measureWidth(int size, String str) {
        STBTTFontinfo fontInfo = getCanvasFontInfo();
        float scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, rendererSize(rendererIndex(size)));
        float width = 0;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer advanceWidth = stack.mallocInt(1);
            IntBuffer ignoredLeftBearing = stack.mallocInt(1);
            int len = str.length();
            for (int i = 0; i < len; i++) {
                int glyphIndex = STBTruetype.stbtt_FindGlyphIndex(fontInfo, str.charAt(i));
                if (glyphIndex == 0)
                    continue;
                STBTruetype.stbtt_GetGlyphHMetrics(fontInfo, glyphIndex, advanceWidth, ignoredLeftBearing);
                width += advanceWidth.get(0) * scale;
            }
        }

        return width;
    }

    private static final int TEXT_SIZE_NORMAL = 14;

    private static final int LEFT_MARGIN_TEXT = 0;//10;
    private static final int RIGHT_MARGIN_TEXT = 0;//10;
    private static final int TOP_MARGIN_TEXT = 0;//5;
    private static final int BOTTOM_MARGIN_TEXT = 0;//5;

    public static void drawTextFloat(Viewport vp, List<String> txts, int pt_x, int pt_y) {
        if (txts.isEmpty())
            return;

        JhvTextRenderer renderer = getRenderer(TEXT_SIZE_NORMAL);
        float fontSize = renderer.getFontSize();

        double boundW = 0;
        int ct = 0;
        for (String txt : txts) {
            double w = measureWidth(TEXT_SIZE_NORMAL, txt);
            if (boundW < w)
                boundW = w;
            ct++;
        }

        float w = (float) (boundW + LEFT_MARGIN_TEXT + RIGHT_MARGIN_TEXT);
        float h = (float) (fontSize * 1.1 * ct + BOTTOM_MARGIN_TEXT + TOP_MARGIN_TEXT);
        int textInit_x = pt_x;
        int textInit_y = pt_y;

        // Correct if out of view
        if (w + pt_x - LEFT_MARGIN_TEXT > vp.width) {
            textInit_x -= (int) (w + pt_x - LEFT_MARGIN_TEXT - vp.width);
        }
        if (h + pt_y - fontSize - TOP_MARGIN_TEXT > vp.height) {
            textInit_y -= (int) (h + pt_y - fontSize - TOP_MARGIN_TEXT - vp.height);
        }
        // float left = textInit_x - LEFT_MARGIN_TEXT;
        // float bottom = textInit_y - fontSize - TOP_MARGIN_TEXT;

        int deltaY = 0, dY = (int) (fontSize * 1.1);
        renderer.beginRendering(vp.width, vp.height);
        for (String txt : txts) {
            renderer.setColor(shadowColor);
            renderer.draw(txt, textInit_x + shadowOffset[0], vp.height - textInit_y + shadowOffset[1] - deltaY);
            renderer.setColor(Colors.LightGrayFloat);
            renderer.draw(txt, textInit_x, vp.height - textInit_y - deltaY);
            deltaY += dY;
        }
        renderer.endRendering();
    }

}
