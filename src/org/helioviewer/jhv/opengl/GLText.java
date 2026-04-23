package org.helioviewer.jhv.opengl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.opengl.text.TextFonts;
import org.helioviewer.jhv.opengl.text.TextRenderer;

public final class GLText {
    private static final int[] RENDERER_SIZES = {10, 12, 14, 16, 20, 24, 32, 48, 64, 80};
    private static final Map<Integer, TextRenderer> renderers = new HashMap<>();

    public static final float[] shadowColor = {0.1f, 0.1f, 0.1f, 0.75f};
    public static final int[] shadowOffset = {2, -2};

    private GLText() {}

    public static TextRenderer getRenderer(int size) {
        int logicalBucketSize = logicalBucketSize(size);
        int physicalSize = physicalSize(logicalBucketSize);
        TextRenderer renderer = renderers.get(physicalSize);
        if (renderer != null)
            return renderer;

        renderer = new TextRenderer(physicalSize, TextFonts.fontData());
        renderers.put(physicalSize, renderer);
        return renderer;
    }

    public static void dispose() {
        for (TextRenderer renderer : renderers.values())
            renderer.dispose();
        renderers.clear();
        TextFonts.dispose();
    }

    private static int logicalBucketSize(int logicalSize) {
        for (int rendererSize : RENDERER_SIZES) {
            if (logicalSize <= rendererSize)
                return rendererSize;
        }
        return RENDERER_SIZES[RENDERER_SIZES.length - 1];
    }

    private static int physicalSize(int logicalSize) {
        return logicalSize * (Display.pixelScale[1] < 1.5f ? 1 : 2);
    }

    private static final int TEXT_SIZE_NORMAL = 14;

    public static void drawTextFloat(Viewport vp, List<String> txts, int pt_x, int pt_y) {
        if (txts.isEmpty())
            return;

        TextRenderer renderer = getRenderer(TEXT_SIZE_NORMAL);
        int physicalTextSize = physicalSize(TEXT_SIZE_NORMAL);
        float textScaleFactor = physicalTextSize / renderer.getFontSize();

        double boundW = 0;
        for (String txt : txts) {
            double w = renderer.measureWidth(txt) * textScaleFactor;
            if (boundW < w)
                boundW = w;
        }

        float w = (float) boundW;
        float h = (float) (physicalTextSize * 1.1 * txts.size());
        int textInit_x = pt_x;
        int textInit_y = pt_y;

        // Correct if out of view
        if (w + pt_x > vp.width) {
            textInit_x -= (int) (w + pt_x - vp.width);
        }
        if (h + pt_y - physicalTextSize > vp.height) {
            textInit_y -= (int) (h + pt_y - physicalTextSize - vp.height);
        }

        int deltaY = 0, dY = (int) (physicalTextSize * 1.1);
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
