package org.helioviewer.jhv.renderable.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;

import org.helioviewer.base.FileUtils;
import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.jhv.renderable.gui.Renderable;
import org.helioviewer.jhv.renderable.viewport.GL3DViewport;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

public class RenderableDummy implements Renderable {

    private Font font;
    private final float baseFontSize = 16;
    private float fontSize;
    private TextRenderer textRenderer;

    private final String name = "Layer loading...";

    public RenderableDummy() {
        InputStream is = FileUtils.getResourceInputStream("/fonts/RobotoCondensed-Regular.ttf");
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, is);
        } catch (FontFormatException e) {
            Log.warn("Font not loaded correctly, fallback to default");
            font = new Font("SansSerif", Font.PLAIN, 20);
        } catch (IOException e) {
            Log.warn("Font not loaded correctly, fallback to default");
            font = new Font("SansSerif", Font.PLAIN, 20);
        }
    }

    @Override
    public void render(GL2 gl, GL3DViewport vp) {
        int sx = GLInfo.pixelScale[0];
        int sy = GLInfo.pixelScale[1];
        if (textRenderer == null || fontSize != sy * baseFontSize) {
            fontSize = sy * baseFontSize;
            font = font.deriveFont(fontSize);
            if (textRenderer != null) {
                textRenderer.dispose();
            }
            textRenderer = new TextRenderer(font, true, true);
            textRenderer.setUseVertexArrays(true);
            textRenderer.setSmoothing(false);
            textRenderer.setColor(Color.WHITE);
        }

        textRenderer.beginRendering(sx * vp.getWidth(), sy * vp.getHeight(), true);
        Rectangle2D rect = textRenderer.getBounds(name);
        textRenderer.draw(name, sx * (vp.getWidth() - 5) - (int) rect.getWidth(), sy * (vp.getHeight() - 5) - (int) rect.getHeight());
        textRenderer.endRendering();
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
        return false;
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
    public boolean isActiveImageLayer() {
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
    }

    @Override
    public void renderMiniview(GL2 gl, GL3DViewport vp) {
    }

}
