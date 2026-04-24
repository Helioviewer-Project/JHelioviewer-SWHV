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

    private static final float LINE_HEIGHT = 1.1f;

    public static void drawTextFloat(Viewport vp, List<String> lines, int x, int y) {
        if (lines.isEmpty())
            return;

        TextRenderer renderer = getRenderer(TEXT_SIZE_NORMAL);
        int textSize = physicalSize(TEXT_SIZE_NORMAL);
        float textScaleFactor = textSize / renderer.getFontSize();
        int lineStep = (int) (textSize * LINE_HEIGHT);

        float width = 0;
        for (String line : lines) {
            width = Math.max(width, renderer.measureWidth(line) * textScaleFactor);
        }
        float height = textSize * LINE_HEIGHT * lines.size();

        int textX = x;
        int textY = y;
        if (textX + width > vp.width)
            textX -= (int) (textX + width - vp.width);
        if (textY + height - textSize > vp.height)
            textY -= (int) (textY + height - textSize - vp.height);

        renderer.beginRendering(vp.width, vp.height);
        int baselineY = vp.height - textY;
        for (String line : lines) {
            renderer.setColor(shadowColor);
            renderer.draw(line, textX + shadowOffset[0], baselineY + shadowOffset[1], 0, textScaleFactor);
            renderer.setColor(Colors.LightGrayFloat);
            renderer.draw(line, textX, baselineY, 0, textScaleFactor);
            baselineY -= lineStep;
        }
        renderer.endRendering();
    }

}
