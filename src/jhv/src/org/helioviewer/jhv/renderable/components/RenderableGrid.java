package org.helioviewer.jhv.renderable.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Date;

import org.helioviewer.base.FileUtils;
import org.helioviewer.base.astronomy.Position;
import org.helioviewer.base.astronomy.Sun;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.GL3DMat4d;
import org.helioviewer.base.math.MathUtils;
import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.renderable.gui.Renderable;
import org.helioviewer.jhv.renderable.gui.RenderableType;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

public class RenderableGrid implements Renderable {
    private static final int SUBDIVISIONS = 120;

    private float lonstepDegrees = 13.2f;
    private float latstepDegrees = 20.f;
    private Font font;
    private TextRenderer textRenderer;
    //The height of the text in solar radii
    private final float textScale = 0.07f;
    private final boolean followCamera;
    private final Color firstColor = Color.RED;
    private final Color secondColor = Color.GREEN;
    private final RenderableType renderableType;
    private final Component optionsPanel;
    private final String name = "Grid";
    private boolean isVisible = true;

    public RenderableGrid(RenderableType renderableType, boolean followCamera) {
        this.renderableType = renderableType;
        this.followCamera = followCamera;

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

        optionsPanel = new RenderableGridOptionsPanel(this);
    }

    private float oldPixelsPerSolarRadiusDoubled = -1;
    private int positionBufferID;
    private int colorBufferID;

    @Override
    public void render(GL2 gl) {
        if (!isVisible)
            return;

        GL3DCamera activeCamera = Displayer.getActiveCamera();
        GL3DMat4d cameraMatrix = activeCamera.getLocalRotation().toMatrix();

        gl.glPushMatrix();
        gl.glMultMatrixd(cameraMatrix.transpose().m, 0);
        {
            gl.glColor3f(1, 1, 0);
            float pixelsPerSolarRadiusDoubled = (float) (textScale * Displayer.getViewportHeight() / activeCamera.getCameraWidth());
            if (textRenderer == null || pixelsPerSolarRadiusDoubled != oldPixelsPerSolarRadiusDoubled) {
                oldPixelsPerSolarRadiusDoubled = pixelsPerSolarRadiusDoubled;

                float cfontsize = pixelsPerSolarRadiusDoubled;
                cfontsize = cfontsize < 10 ? 10 : cfontsize;
                font = font.deriveFont(cfontsize);
                if (textRenderer != null) {
                    textRenderer.dispose();
                }
                textRenderer = new TextRenderer(font, true, true);
                textRenderer.setUseVertexArrays(true);
                //renderer.setSmoothing(true);
                textRenderer.setColor(Color.WHITE);
            }

            if (!followCamera) {
                drawText(gl);
            }

            gl.glDisable(GL2.GL_TEXTURE_2D);
            drawCircles(gl, cameraMatrix);
            gl.glEnable(GL2.GL_TEXTURE_2D);
        }
        gl.glPopMatrix();
    }

    private void drawCircles(GL2 gl, GL3DMat4d cameraMatrix) {
        gl.glLineWidth(0.5f);

        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, positionBufferID);
        gl.glVertexPointer(2, GL2.GL_FLOAT, 0, 0);
        {
            gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, colorBufferID);
            gl.glColorPointer(3, GL2.GL_FLOAT, 0, 0);

            gl.glRotatef(90, 0, 1, 0);

            gl.glPushMatrix();
            {
                float rotation = 0;
                while (rotation <= 90) {
                    gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, SUBDIVISIONS);
                    gl.glRotatef(lonstepDegrees, 0, 1, 0);
                    rotation += lonstepDegrees;
                }
            }
            gl.glPopMatrix();

            gl.glPushMatrix();
            {
                float rotation = 0;
                rotation -= lonstepDegrees;
                gl.glRotatef(-lonstepDegrees, 0, 1, 0);

                while (rotation >= -90) {
                    gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, SUBDIVISIONS);
                    gl.glRotatef(-lonstepDegrees, 0, 1, 0);
                    rotation -= lonstepDegrees;
                }
            }
            gl.glPopMatrix();

            gl.glPushMatrix();
            {
                float rotation = 0;
                gl.glRotatef(90, 1, 0, 0);

                gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, SUBDIVISIONS);
                while (rotation < 90) {
                    gl.glPushMatrix();
                    {
                        gl.glTranslatef(0, 0, (float) Math.sin(Math.PI / 180. * rotation));
                        float scale = (float) Math.cos(Math.PI / 180. * rotation);
                        gl.glScalef(scale, scale, scale);
                        gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, SUBDIVISIONS);
                    }
                    gl.glPopMatrix();
                    rotation += latstepDegrees;
                }

                rotation = latstepDegrees;
                while (rotation < 90) {
                    gl.glPushMatrix();
                    {
                        gl.glTranslatef(0, 0, -(float) Math.sin(Math.PI / 180. * rotation));
                        float scale = (float) Math.cos(Math.PI / 180. * rotation);
                        gl.glScalef(scale, scale, scale);
                        gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, SUBDIVISIONS);
                    }
                    gl.glPopMatrix();
                    rotation += latstepDegrees;
                }
                gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, SUBDIVISIONS);
            }
            gl.glPopMatrix();

            gl.glDisableClientState(GL2.GL_COLOR_ARRAY);

            // last: Earth circles - static color, undo rotation
            Date timestamp = Displayer.getLastUpdatedTimestamp();
            if (timestamp != null) {
                gl.glColor4f(1, 1, 0, 1);

                gl.glPushMatrix();
                gl.glRotatef(-90, 0, 1, 0);
                gl.glMultMatrixd(cameraMatrix.transpose().m, 0);
                {
                    Position.Latitudinal p = Sun.getEarth(timestamp);
                    gl.glRotatef(90 - (float) (p.lon * MathUtils.radeg), 0, 1, 0);
                    gl.glRotatef((float) -(p.lat * MathUtils.radeg), 0, 0, 1);
                    gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, SUBDIVISIONS);

                    gl.glRotatef(90, 1, 0, 0);
                    gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, SUBDIVISIONS);
                }
                gl.glPopMatrix();
            }
        }
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
    }

    private void drawText(GL2 gl) {
        float zdist = 0f;

        double size = Sun.Radius * 1.06;
        //The scale factor has to be divided by the current font size
        float textScaleFactor = this.textScale / font.getSize();
        //Adjust for font size in horizontal and vertical direction (centering the text approximately)
        float horizontalAdjustment = this.textScale / 2f;
        float verticalAdjustment = this.textScale / 3f;

        textRenderer.begin3DRendering();
        for (double phi = 0; phi <= 90; phi += latstepDegrees) {
            double angle = (90 - phi) * Math.PI / 180.;
            String txt = String.format("%.1f", phi);
            if (txt.substring(txt.length() - 1, txt.length()).equals("0")) {
                txt = txt.substring(0, txt.length() - 2);
            }
            textRenderer.draw3D(txt, (float) (Math.sin(angle) * size), (float) (Math.cos(angle) * size - verticalAdjustment), zdist, textScaleFactor);
            if (phi != 90) {
                textRenderer.draw3D(txt, (float) (-Math.sin(angle) * size - horizontalAdjustment), (float) (Math.cos(angle) * size - verticalAdjustment), zdist, textScaleFactor);
            }
        }
        for (double phi = -latstepDegrees; phi >= -90; phi -= latstepDegrees) {
            double angle = (90 - phi) * Math.PI / 180.;
            String txt = String.format("%.1f", phi);
            if (txt.substring(txt.length() - 1, txt.length()).equals("0")) {
                txt = txt.substring(0, txt.length() - 2);
            }
            textRenderer.draw3D(txt, (float) (Math.sin(angle) * size), (float) (Math.cos(angle) * size - verticalAdjustment), zdist, textScaleFactor);
            if (phi != -90) {
                textRenderer.draw3D(txt, (float) (-Math.sin(angle) * size - horizontalAdjustment), (float) (Math.cos(angle) * size - verticalAdjustment), zdist, textScaleFactor);
            }
        }
        textRenderer.end3DRendering();

        size = Sun.Radius * 1.02;

        for (double theta = 0; theta <= 180.; theta += lonstepDegrees) {
            String txt = String.format("%.1f", theta);
            if (txt.substring(txt.length() - 1, txt.length()).equals("0")) {
                txt = txt.substring(0, txt.length() - 2);
            }
            double angle = (90 - theta) * Math.PI / 180.;
            gl.glPushMatrix();
            {
                gl.glTranslatef((float) (Math.cos(angle) * size), 0, (float) (Math.sin(angle) * size));
                gl.glRotatef((float) theta, 0, 1, 0);

                textRenderer.begin3DRendering();
                textRenderer.draw3D(txt, 0, 0, 0, textScaleFactor);
                textRenderer.end3DRendering();
            }
            gl.glPopMatrix();
        }

        for (double theta = -lonstepDegrees; theta > -180.; theta -= lonstepDegrees) {
            String txt = String.format("%.1f", theta);
            if (txt.substring(txt.length() - 1, txt.length()).equals("0")) {
                txt = txt.substring(0, txt.length() - 2);
            }
            double angle = (90 - theta) * Math.PI / 180.;

            gl.glPushMatrix();
            {
                gl.glTranslatef((float) (Math.cos(angle) * size), 0, (float) (Math.sin(angle) * size));
                gl.glRotatef((float) theta, 0, 1, 0);

                textRenderer.begin3DRendering();
                textRenderer.draw3D(txt, 0, 0, 0, textScaleFactor);
                textRenderer.end3DRendering();
            }
            gl.glPopMatrix();
        }
    }

    @Override
    public void init(GL2 gl) {
        FloatBuffer positionBuffer = FloatBuffer.allocate((SUBDIVISIONS + 1) * 2);
        FloatBuffer colorBuffer = FloatBuffer.allocate((SUBDIVISIONS + 1) * 3);

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            positionBuffer.put((float) Math.cos(2 * Math.PI * i / SUBDIVISIONS));
            positionBuffer.put((float) Math.sin(2 * Math.PI * i / SUBDIVISIONS));
            if (i % 2 == 0) {
                colorBuffer.put(this.firstColor.getRed() / 255f);
                colorBuffer.put(this.firstColor.getGreen() / 255f);
                colorBuffer.put(this.firstColor.getBlue() / 255f);
            } else {
                colorBuffer.put(this.secondColor.getRed() / 255f);
                colorBuffer.put(this.secondColor.getGreen() / 255f);
                colorBuffer.put(this.secondColor.getBlue() / 255f);
            }
        }

        positionBuffer.flip();
        colorBuffer.flip();
        int positionBufferSize = positionBuffer.capacity();
        int colorBufferSize = colorBuffer.capacity();

        positionBufferID = generate(gl);
        colorBufferID = generate(gl);

        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, positionBufferID);
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, positionBufferSize * Buffers.SIZEOF_FLOAT, positionBuffer, GL2.GL_STATIC_DRAW);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, colorBufferID);
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, colorBufferSize * Buffers.SIZEOF_FLOAT, colorBuffer, GL2.GL_STATIC_DRAW);
    }

    private int generate(GL2 gl) {
        int[] tmpId = new int[1];
        gl.glGenBuffers(1, tmpId, 0);
        return tmpId[0];
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
    }

    @Override
    public RenderableType getType() {
        return this.renderableType;
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
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

    public double getLonstepDegrees() {
        return lonstepDegrees;
    }

    public void setLonstepDegrees(double lonstepDegrees) {
        this.lonstepDegrees = (float) lonstepDegrees;
    }

    public double getLatstepDegrees() {
        return latstepDegrees;
    }

    public void setLatstepDegrees(double latstepDegrees) {
        this.latstepDegrees = (float) latstepDegrees;
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
        gl.glDeleteBuffers(1, new int[] { positionBufferID }, 0);
        gl.glDeleteBuffers(1, new int[] { colorBufferID }, 0);
        oldPixelsPerSolarRadiusDoubled = -1;
    }

}
