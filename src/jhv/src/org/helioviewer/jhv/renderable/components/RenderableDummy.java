package org.helioviewer.jhv.renderable.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.geom.Rectangle2D;

import javax.swing.SwingWorker;

import org.helioviewer.jhv.camera.GL3DViewport;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.renderable.gui.Renderable;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

public class RenderableDummy implements Renderable {

    private Font font;
    private float oldFontSize = -1;
    private static final double vpScale = 0.04;
    private TextRenderer textRenderer;
    private SwingWorker<?,?> worker;

    private final String name = "Loading...";

    public RenderableDummy(SwingWorker<?,?> _worker) {
        worker = _worker;
    }

    @Override
    public void render(GL2 gl, GL3DViewport vp) {
        float fontSize = (int) (vp.getHeight() * vpScale);
        if (textRenderer == null || fontSize != oldFontSize) {
            oldFontSize = fontSize;
            font = UIGlobals.UIFontRoboto.deriveFont(fontSize);
            if (textRenderer != null) {
                textRenderer.dispose();
            }
            textRenderer = new TextRenderer(font, true, true);
            textRenderer.setUseVertexArrays(true);
            textRenderer.setSmoothing(false);
            textRenderer.setColor(Color.WHITE);
        }

        int delta = (int) (vp.getHeight() * 0.01);
        textRenderer.beginRendering(vp.getWidth(), vp.getHeight(), true);
        Rectangle2D rect = textRenderer.getBounds(name);
        textRenderer.draw(name, (int) (vp.getWidth() - rect.getWidth() - delta), (int) (vp.getHeight() - rect.getHeight() - delta));
        textRenderer.endRendering();
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
        worker.cancel(true);
    }

    @Override
    public Component getOptionsPanel() {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public void setVisible(boolean b) {
    }

    @Override
    public String getTimeString() {
        return null;
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void init(GL2 gl) {
    }

    @Override
    public void dispose(GL2 gl) {
        if (textRenderer != null) {
            textRenderer.dispose();
            textRenderer = null;
        }
        oldFontSize = -1;
    }

    @Override
    public void renderMiniview(GL2 gl, GL3DViewport vp) {
    }

}
