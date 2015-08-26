package org.helioviewer.jhv.renderable.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.helioviewer.base.FileUtils;
import org.helioviewer.base.datetime.TimeUtils;
import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.jhv.renderable.gui.Renderable;
import org.helioviewer.jhv.renderable.gui.RenderableType;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

public class RenderableTimeStamp implements Renderable {

    private Font font;
    private final float baseFontSize = 16;
    private float fontSize;
    private TextRenderer textRenderer;

    private final RenderableType renderableType;
    private final String name = "Timestamp";
    private boolean isVisible = false;

    public RenderableTimeStamp(RenderableType renderableType) {
        this.renderableType = renderableType;

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
    public void render(GL2 gl) {
        if (!isVisible)
            return;

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

        textRenderer.beginRendering(sx * Displayer.getViewportWidth(), sy * Displayer.getViewportHeight(), true);
        textRenderer.draw(TimeUtils.utcDateFormat.format(Displayer.getLastUpdatedTimestamp()), 5 * sx, 5 * sy);
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
    public RenderableType getType() {
        return renderableType;
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
    public boolean isActiveImageLayer() {
        return false;
    }

    @Override
    public void dispose(GL2 gl) {
        if (textRenderer != null) {
            textRenderer.dispose();
            textRenderer = null;
        }
    }

}
