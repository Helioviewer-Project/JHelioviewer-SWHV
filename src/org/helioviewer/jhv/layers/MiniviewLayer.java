package org.helioviewer.jhv.layers;

import java.awt.Color;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayFrame;
import org.helioviewer.jhv.display.DisplayLayout;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.opengl.GL;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.opengl.GLSLShape;

import org.json.JSONObject;

public final class MiniviewLayer extends AbstractLayer {

    public static final int MIN_SCALE = 5;
    public static final int MAX_SCALE = 15;
    private int scale = 10;

    private static final byte[] colorCircle = Colors.bytes(Color.RED, 0.2);
    private static final byte[] colorRectangle = Colors.bytes(Color.GREEN, 0.2);
    private final GLSLShape circle = new GLSLShape(false);
    private final GLSLShape rectangle = new GLSLShape(false);

    private Viewport miniViewport = DisplayLayout.viewport(0, 0, 0, 100, 100, 100);

    @Override
    public void serialize(JSONObject jo) {
        jo.put("scale", scale);
    }

    public MiniviewLayer(JSONObject jo) {
        if (jo != null)
            scale = Math.clamp(jo.optInt("scale", scale), MIN_SCALE, MAX_SCALE);
        else
            setEnabled(true);
        reshapeViewport();
    }

    public void reshapeViewport() {
        int vpw = Display.fullViewport.width;
        int offset = (int) (vpw * 0.01);
        int size = (int) (vpw * 0.01 * scale);
        miniViewport = DisplayLayout.viewport(0, offset, offset, size, size, Display.fullViewport.height);
    }

    public void renderBackground() {
        rectangle.renderShape(GL.TRIANGLE_STRIP);
        circle.renderShape(GL.TRIANGLE_STRIP);
    }

    @Override
    public void remove() {
        dispose();
    }

    @Override
    public String getName() {
        return "Miniview";
    }

    @Override
    public void init() {
        circle.init();
        GLHelper.initCircleFront(circle, 0, 0, 1, 18, colorCircle);
        rectangle.init();
        GLHelper.initRectangleFront(rectangle, -30, -30, 60, 60, colorRectangle);
    }

    @Override
    public void dispose() {
        circle.dispose();
        rectangle.dispose();
    }

    public Viewport getViewport() {
        return miniViewport;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int _scale) {
        scale = _scale;
        reshapeViewport();
        DisplayFrame.display();
    }

}
