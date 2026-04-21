package org.helioviewer.jhv.opengl;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.opengl.text.JhvTextRenderer;

import org.lwjgl.system.MemoryUtil;

public class GLText {
    private static final FontRenderContext BOUNDS_FRC = new FontRenderContext(null, true, true);

    private static final int MIN = 10;
    private static final int MAX = 144;
    private static final int STEP = 1;
    private static final int SIZE = (MAX - MIN) / STEP + 1;
    private static final Font[] fonts = new Font[SIZE];
    private static final JhvTextRenderer[] renderers = new JhvTextRenderer[SIZE];
    private static ByteBuffer canvasFontData;

    public static final float[] shadowColor = {0.1f, 0.1f, 0.1f, 0.75f};
    public static final int[] shadowOffset = {2, -2};

    public static JhvTextRenderer getRenderer(int size) {
        int idx = rendererIndex(size);

        if (renderers[idx] == null) {
            Font font = rendererFont(idx);
            renderers[idx] = new JhvTextRenderer(font.getSize2D(), getCanvasFontData());
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
            fonts[i] = null;
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

    private static Font rendererFont(int idx) {
        if (fonts[idx] == null)
            fonts[idx] = UIGlobals.canvasFont.deriveFont((float) (idx * STEP + MIN));
        return fonts[idx];
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

    private static final int TEXT_SIZE_NORMAL = 14;

    private static final int LEFT_MARGIN_TEXT = 0;//10;
    private static final int RIGHT_MARGIN_TEXT = 0;//10;
    private static final int TOP_MARGIN_TEXT = 0;//5;
    private static final int BOTTOM_MARGIN_TEXT = 0;//5;

    public static void drawTextFloat(Viewport vp, List<String> txts, int pt_x, int pt_y) {
        if (txts.isEmpty())
            return;

        JhvTextRenderer renderer = getRenderer(TEXT_SIZE_NORMAL);
        Font font = rendererFont(rendererIndex(TEXT_SIZE_NORMAL));
        float fontSize = renderer.getFontSize();

        double boundW = 0;
        int ct = 0;
        for (String txt : txts) {
            double w = getBounds(font, txt).getWidth();
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

    private static Rectangle2D getBounds(Font font, String str) {
        GlyphVector glyphs = font.createGlyphVector(BOUNDS_FRC, str);
        return glyphs.getVisualBounds();
    }
}
