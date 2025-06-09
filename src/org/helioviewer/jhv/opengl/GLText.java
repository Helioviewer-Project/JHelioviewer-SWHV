package org.helioviewer.jhv.opengl;

import java.awt.Font;
import java.util.List;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.opengl.text.JhvTextRenderer;

import com.jogamp.opengl.GL3;

public class GLText {

    private static final int MIN = 10;
    private static final int MAX = 144;
    private static final int STEP = 1;
    private static final int SIZE = (MAX - MIN) / STEP + 1;
    private static final JhvTextRenderer[] renderers = new JhvTextRenderer[SIZE];

    public static final float[] shadowColor = {0.1f, 0.1f, 0.1f, 0.75f};
    public static final int[] shadowOffset = {2, -2};

    public static JhvTextRenderer getRenderer(int size) {
        size = (int) (size * GLInfo.pixelScale[1]);

        int idx = (size - MIN) / STEP;
        if (idx < 0)
            idx = 0;
        else if (idx >= SIZE)
            idx = SIZE - 1;

        if (renderers[idx] == null) {
            Font font = UIGlobals.canvasFont.deriveFont((float) (idx * STEP + MIN));
            renderers[idx] = new JhvTextRenderer(font, true, true);
            // precache for grid text
            renderers[idx].draw3D("-0123456789.", 0, 0, 0, 0);
        }
        return renderers[idx];
    }

    public static void dispose(GL3 gl) {
        for (int i = 0; i < SIZE; i++) {
            if (renderers[i] != null) {
                renderers[i].dispose(gl);
                renderers[i] = null;
            }
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
        float fontSize = renderer.getFont().getSize2D();

        double boundW = 0;
        int ct = 0;
        for (String txt : txts) {
            double w = renderer.getBounds(txt).getWidth();
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
