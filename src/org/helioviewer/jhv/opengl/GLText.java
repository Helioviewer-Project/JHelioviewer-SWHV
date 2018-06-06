package org.helioviewer.jhv.opengl;

import java.awt.Color;
import java.awt.Font;
import java.nio.FloatBuffer;
import java.util.List;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.UIGlobals;

import com.jogamp.opengl.GL2;

public class GLText {

    private static final int MIN = 10;
    private static final int MAX = 144;
    private static final int STEP = 1;
    private static final int SIZE = (MAX - MIN) / STEP + 1;
    private static final JhvTextRenderer[] renderer = new JhvTextRenderer[SIZE];

    private static final FloatBuffer color = BufferUtils.newFloatBuffer(16);

    static {
        BufferUtils.put4f(color, 0.33f, 0.33f, 0.33f, 0.9f);
        BufferUtils.put4f(color, 0.33f, 0.33f, 0.33f, 0.9f);
        BufferUtils.put4f(color, 0.33f, 0.33f, 0.33f, 0.9f);
        BufferUtils.put4f(color, 0.33f, 0.33f, 0.33f, 0.9f);
        color.rewind();
    }

    public static JhvTextRenderer getRenderer(int size) {
        size *= GLInfo.pixelScale[1];

        int idx = (size - MIN) / STEP;
        if (idx < 0)
            idx = 0;
        else if (idx >= SIZE)
            idx = SIZE - 1;

        if (renderer[idx] == null) {
            Font font = UIGlobals.UIFontRoboto.deriveFont((float) (idx * STEP + MIN));
            renderer[idx] = new JhvTextRenderer(font, true, true, null, true);
            renderer[idx].setUseVertexArrays(true);
            // renderer[idx].setSmoothing(false);
            renderer[idx].setColor(Color.WHITE);
            // precache for grid text
            renderer[idx].draw3D("-0123456789.", 0, 0, 0, 0);
        }
        return renderer[idx];
    }

    public static void dispose() {
        for (int i = 0; i < SIZE; i++) {
            if (renderer[i] != null) {
                renderer[i].dispose();
                renderer[i] = null;
            }
        }
    }

    public static final int TEXT_SIZE_NORMAL = 12;

    private static final int LEFT_MARGIN_TEXT = 10;
    private static final int RIGHT_MARGIN_TEXT = 10;
    private static final int TOP_MARGIN_TEXT = 5;
    private static final int BOTTOM_MARGIN_TEXT = 5;

    public static void drawText(GL2 gl, Viewport vp, List<String> txts, int pt_x, int pt_y) {
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
        float left = textInit_x - LEFT_MARGIN_TEXT;
        float bottom = textInit_y - fontSize - TOP_MARGIN_TEXT;

        renderer.beginRendering(vp.width, vp.height, true);

        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        FloatBuffer vertex = BufferUtils.newFloatBuffer(12);
        {
            BufferUtils.put3f(vertex, left, vp.height - bottom, 0);
            BufferUtils.put3f(vertex, left, vp.height - bottom - h, 0);
            BufferUtils.put3f(vertex, left + w, vp.height - bottom - h, 0);
            BufferUtils.put3f(vertex, left + w, vp.height - bottom, 0);
            vertex.rewind();

        }
        GLHelper.drawColorQuad(gl, vertex, color);
        gl.glPopMatrix();
        gl.glEnable(GL2.GL_TEXTURE_2D);

        gl.glColor3f(1, 1, 1);
        int deltaY = 0;
        for (String txt : txts) {
            renderer.draw(txt, textInit_x, vp.height - textInit_y - deltaY);
            deltaY += (int) (fontSize * 1.1);
        }
        renderer.endRendering();
        gl.glDisable(GL2.GL_TEXTURE_2D);
    }

}
