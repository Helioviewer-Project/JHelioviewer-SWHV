package org.helioviewer.jhv.opengl;

import java.util.List;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.opengl.text.TextFonts;
import org.helioviewer.jhv.opengl.text.TextRenderer;

public class GLText {
    private static final int MIN = 10;
    private static final int MAX = 144;
    private static final int STEP = 2;
    private static final int SIZE = (MAX - MIN) / STEP + 1;
    private static final TextRenderer[] renderers = new TextRenderer[SIZE];

    public static final float[] shadowColor = {0.1f, 0.1f, 0.1f, 0.75f};
    public static final int[] shadowOffset = {2, -2};

    public static TextRenderer getRenderer(int size) {
        int idx = rendererIndex(size);

        if (renderers[idx] == null) {
            renderers[idx] = new TextRenderer(rendererSize(idx), TextFonts.loadCanvasFontData());
            renderers[idx].precache("-0123456789.");
        }
        return renderers[idx];
    }

    public static void dispose() {
        for (int i = 0; i < renderers.length; i++) {
            if (renderers[i] != null) {
                renderers[i].dispose();
                renderers[i] = null;
            }
        }
        TextFonts.dispose();
    }

    private static int rendererIndex(int size) {
        size = physicalSize(size);
        if (size <= MIN)
            return 0;
        if (size >= MAX)
            return SIZE - 1;
        return (size - MIN + STEP - 1) / STEP;
    }

    private static float rendererSize(int idx) {
        return idx * STEP + MIN;
    }

    private static int physicalSize(int logicalSize) {
        return (int) (logicalSize * Display.pixelScale[1]);
    }

    private static final int TEXT_SIZE_NORMAL = 14;

    private static final int LEFT_MARGIN_TEXT = 0;//10;
    private static final int RIGHT_MARGIN_TEXT = 0;//10;
    private static final int TOP_MARGIN_TEXT = 0;//5;
    private static final int BOTTOM_MARGIN_TEXT = 0;//5;

    public static void drawTextFloat(Viewport vp, List<String> txts, int pt_x, int pt_y) {
        if (txts.isEmpty())
            return;

        TextRenderer renderer = getRenderer(TEXT_SIZE_NORMAL);
        int textSize = physicalSize(TEXT_SIZE_NORMAL);
        float textScaleFactor = textSize / renderer.getFontSize();

        double boundW = 0;
        int ct = 0;
        for (String txt : txts) {
            double w = renderer.measureWidth(txt) * textScaleFactor;
            if (boundW < w)
                boundW = w;
            ct++;
        }

        float w = (float) (boundW + LEFT_MARGIN_TEXT + RIGHT_MARGIN_TEXT);
        float h = (float) (textSize * 1.1 * ct + BOTTOM_MARGIN_TEXT + TOP_MARGIN_TEXT);
        int textInit_x = pt_x;
        int textInit_y = pt_y;

        // Correct if out of view
        if (w + pt_x - LEFT_MARGIN_TEXT > vp.width) {
            textInit_x -= (int) (w + pt_x - LEFT_MARGIN_TEXT - vp.width);
        }
        if (h + pt_y - textSize - TOP_MARGIN_TEXT > vp.height) {
            textInit_y -= (int) (h + pt_y - textSize - TOP_MARGIN_TEXT - vp.height);
        }
        // float left = textInit_x - LEFT_MARGIN_TEXT;
        // float bottom = textInit_y - textSize - TOP_MARGIN_TEXT;

        int deltaY = 0, dY = (int) (textSize * 1.1);
        renderer.beginRendering(vp.width, vp.height);
        for (String txt : txts) {
            renderer.setColor(shadowColor);
            renderer.draw(txt, textInit_x + shadowOffset[0], vp.height - textInit_y + shadowOffset[1] - deltaY, 0, textScaleFactor);
            renderer.setColor(Colors.LightGrayFloat);
            renderer.draw(txt, textInit_x, vp.height - textInit_y - deltaY, 0, textScaleFactor);
            deltaY += dY;
        }
        renderer.endRendering();
    }

}
