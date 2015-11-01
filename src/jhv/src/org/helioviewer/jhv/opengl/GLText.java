package org.helioviewer.jhv.opengl;

import java.awt.Color;
import java.awt.Font;

import org.helioviewer.jhv.gui.UIGlobals;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

public class GLText {

    private static final int MIN = 10;
    private static final int MAX = 288;
    private static final int STEP = 2;
    private static final int SIZE = (MAX - MIN) / STEP + 1;
    private static final TextRenderer[] renderer = new TextRenderer[SIZE];

    public static void init(GL2 gl) {
    }

    public static TextRenderer getRenderer(int size) {
        int idx = (size - MIN) / STEP;
        if (idx < 0)
            idx = 0;
        else if (idx >= SIZE)
            idx = SIZE - 1;

        if (renderer[idx] == null) {
            Font font = UIGlobals.UIFontRoboto.deriveFont((float) (idx * STEP + MIN));
            renderer[idx] = new TextRenderer(font, true, true, null, true);
            renderer[idx].setUseVertexArrays(true);
            // textRenderer.setSmoothing(false);
            renderer[idx].setColor(Color.WHITE);
        }
        return renderer[idx];
    }

    public static void dispose(GL2 gl) {
        for (int i = 0; i < SIZE; i++) {
            if (renderer[i] != null) {
                renderer[i].dispose();
                renderer[i] = null;
            }
        }
    }

}
