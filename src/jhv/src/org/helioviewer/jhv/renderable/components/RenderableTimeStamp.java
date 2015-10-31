package org.helioviewer.jhv.renderable.components;

import java.awt.Color;
import java.awt.Component;

import org.helioviewer.jhv.camera.GL3DViewport;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

public class RenderableTimeStamp extends AbstractRenderable {

    private float oldFontSize = -1;
    private static final double vpScale = 0.035;
    private TextRenderer textRenderer;

    private static final String name = "Timestamp";

    public RenderableTimeStamp() {
    }

    @Override
    public void render(GL2 gl, GL3DViewport vp) {
    }

    @Override
    public void renderFloat(GL2 gl, GL3DViewport vp) {
        if (!isVisible[vp.getIndex()])
            return;

        float fontSize = (int) (vp.getHeight() * vpScale);
        if (textRenderer == null || fontSize != oldFontSize) {
            oldFontSize = fontSize;
            if (textRenderer != null) {
                textRenderer.dispose();
            }
            textRenderer = new TextRenderer(UIGlobals.UIFontRoboto.deriveFont(fontSize), true, true);
            textRenderer.setUseVertexArrays(true);
            textRenderer.setSmoothing(false);
            textRenderer.setColor(Color.WHITE);
        }

        String text = Layers.getLastUpdatedTimestamp().toString();
        if (Displayer.multiview) {
            RenderableImageLayer im = ImageViewerGui.getRenderableContainer().getViewportRenderableImageLayer(vp.getIndex());
            if (im != null) {
                text = im.getTimeString();
            }
        }

        int delta = (int) (vp.getHeight() * 0.01);
        textRenderer.beginRendering(vp.getWidth(), vp.getHeight(), true);
        textRenderer.draw(text, delta, delta);
        textRenderer.endRendering();
    }

    @Override
    public void init(GL2 gl) {
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
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
    public String getTimeString() {
        return null;
    }

    @Override
    public boolean isDeletable() {
        return false;
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
