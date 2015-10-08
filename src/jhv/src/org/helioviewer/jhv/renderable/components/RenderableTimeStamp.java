package org.helioviewer.jhv.renderable.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import org.helioviewer.base.time.TimeUtils;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.GL3DViewport;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.jhv.renderable.gui.Renderable;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

public class RenderableTimeStamp implements Renderable {

    private Font font;
    private final float baseFontSize = 16;
    private float fontSize;
    private TextRenderer textRenderer;

    private final String name = "Timestamp";
    private boolean isVisible = false;

    public RenderableTimeStamp() {
    }

    @Override
    public void render(GL2 gl, GL3DViewport vp) {
        if (!isVisible)
            return;

        int sx = GLInfo.pixelScale[0];
        int sy = GLInfo.pixelScale[1];
        if (textRenderer == null || fontSize != sy * baseFontSize) {
            fontSize = sy * baseFontSize;
            font = UIGlobals.UIFontRoboto.deriveFont(fontSize);
            if (textRenderer != null) {
                textRenderer.dispose();
            }
            textRenderer = new TextRenderer(font, true, true);
            textRenderer.setUseVertexArrays(true);
            textRenderer.setSmoothing(false);
            textRenderer.setColor(Color.WHITE);
        }

        textRenderer.beginRendering(vp.getWidth(), vp.getHeight(), true);
        textRenderer.draw(TimeUtils.utcDateFormat.format(Layers.getLastUpdatedTimestamp()), 5 * sx, 5 * sy);
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
    public boolean isVisible() {
        return this.isVisible;
    }

    @Override
    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
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
    }

    @Override
    public void renderMiniview(GL2 gl, GL3DViewport vp) {
    }

}
