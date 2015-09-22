package org.helioviewer.jhv.renderable.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;

import org.helioviewer.base.FileUtils;
import org.helioviewer.base.astronomy.Position;
import org.helioviewer.base.astronomy.Sun;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.GL3DMat4d;
import org.helioviewer.base.math.MathUtils;
import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.jhv.renderable.components.RenderableGridOptionsPanel.GridChoiceType;
import org.helioviewer.jhv.renderable.gui.Renderable;
import org.helioviewer.jhv.renderable.helpers.RenderableHelper;
import org.helioviewer.jhv.renderable.viewport.GL3DViewport;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

public class RenderableGrid implements Renderable {

    private static final int SUBDIVISIONS = 120;
    private float lonstepDegrees = 15f;
    private float latstepDegrees = 20f;
    private final Color firstColor = Color.RED;
    private final Color secondColor = Color.GREEN;

    private boolean showAxes = true;
    private boolean showLabels = true;
    private Font font;
    private TextRenderer textRenderer;
    // the height of the text in solar radii
    private final float textScale = 0.08f;

    private final Component optionsPanel;
    private final String name = "Grid";
    private boolean isVisible = true;

    public RenderableGrid() {

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

    private float oldFontSize = -1;
    private int positionBufferID;
    private int colorBufferID;
    private GridChoiceType gridChoice = GridChoiceType.OBSERVER;

    @Override
    public void render(GL2 gl, GL3DViewport vp) {

        GL3DCamera activeCamera = vp.getCamera();

        renderBlackCircle(gl, activeCamera.getRotation().transpose().m);
        if (!isVisible)
            return;

        if (showAxes)
            drawAxes(gl);

        // cameraWidth ever changes so slightly with distance to Sun; 4x pix/Rsun
        int pixelsPerSolarRadius = (int) (2 * textScale * vp.getHeight() / activeCamera.getCameraWidth());
        float fontSize = Math.max(10, Math.min(288, pixelsPerSolarRadius));

        if (showLabels && (textRenderer == null || fontSize != oldFontSize)) {
            oldFontSize = fontSize;
            font = font.deriveFont(fontSize);

            if (textRenderer != null) {
                textRenderer.dispose();
            }

            boolean antiAlias = GLInfo.pixelScale[1] == 1 ? false : true;
            textRenderer = new TextRenderer(font, antiAlias, antiAlias, null, true);
            textRenderer.setUseVertexArrays(true);
            // textRenderer.setSmoothing(false);
            textRenderer.setColor(Color.WHITE);
        }

        GL3DMat4d cameraMatrix;
        switch (gridChoice) {
        case OBSERVER:
            cameraMatrix = activeCamera.getLocalRotation().toMatrix();
            break;
        case HCI:
            //TBD
            cameraMatrix = GL3DMat4d.identity();
            break;
        default:
            cameraMatrix = GL3DMat4d.identity();
            break;
        }

        double[] matrix = cameraMatrix.transpose().m;

        gl.glPushMatrix();
        gl.glMultMatrixd(matrix, 0);
        {
            if (showLabels)
                drawText(gl);

            gl.glDisable(GL2.GL_TEXTURE_2D);
            drawCircles(gl, cameraMatrix);
            gl.glEnable(GL2.GL_TEXTURE_2D);
        }
        gl.glPopMatrix();
    }

    private void renderBlackCircle(GL2 gl, double[] matrix) {
        gl.glPushMatrix();
        gl.glMultMatrixd(matrix, 0);
        {
            gl.glColor3f(0f, 0f, 0f);
            RenderableHelper.drawCircle(gl, 0, 0, 0.95, 25);
        }
        gl.glPopMatrix();
    }

    private void drawAxes(GL2 gl) {
        gl.glLineWidth(2f);

        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glBegin(GL2.GL_LINES);
        {
            gl.glColor4f(0, 0, 1, 1);
            gl.glVertex3f(0, -1.2f, 0);
            gl.glVertex3f(0, -1, 0);
            gl.glColor4f(1, 0, 0, 1);
            gl.glVertex3f(0, 1.2f, 0);
            gl.glVertex3f(0, 1, 0);
        }
        gl.glEnd();
        gl.glEnable(GL2.GL_TEXTURE_2D);
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
                while (rotation <= 180) {
                    gl.glDrawArrays(GL2.GL_LINE_STRIP, SUBDIVISIONS / 4, SUBDIVISIONS / 2 + 1);
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

                while (rotation >= -180) {
                    gl.glDrawArrays(GL2.GL_LINE_STRIP, SUBDIVISIONS / 4, SUBDIVISIONS / 2 + 1);
                    gl.glRotatef(-lonstepDegrees, 0, 1, 0);

                    rotation -= lonstepDegrees;
                }
            }
            gl.glPopMatrix();

            gl.glPushMatrix();
            {
                float rotation = 0;
                gl.glRotatef(90, 1, 0, 0);

                gl.glDrawArrays(GL2.GL_LINE_LOOP, 0, SUBDIVISIONS);
                while (rotation < 90) {
                    gl.glPushMatrix();
                    {
                        gl.glTranslatef(0, 0, (float) Math.sin(Math.PI / 180. * rotation));
                        float scale = (float) Math.cos(Math.PI / 180. * rotation);
                        gl.glScalef(scale, scale, scale);
                        gl.glDrawArrays(GL2.GL_LINE_LOOP, 0, SUBDIVISIONS);
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
                        gl.glDrawArrays(GL2.GL_LINE_LOOP, 0, SUBDIVISIONS);
                    }
                    gl.glPopMatrix();
                    rotation += latstepDegrees;
                }
                gl.glDrawArrays(GL2.GL_LINE_LOOP, 0, SUBDIVISIONS);
            }
            gl.glPopMatrix();

            gl.glDisableClientState(GL2.GL_COLOR_ARRAY);

            // last: Earth circles - static color, undo rotation
            gl.glColor4f(1, 1, 0, 1);

            gl.glPushMatrix();
            gl.glRotatef(-90, 0, 1, 0);
            gl.glMultMatrixd(cameraMatrix.transpose().m, 0);
            {
                Position.Latitudinal p = Sun.getEarth(Displayer.getLastUpdatedTimestamp());
                gl.glRotatef(90 - (float) (p.lon * MathUtils.radeg), 0, 1, 0);
                gl.glRotatef((float) -(p.lat * MathUtils.radeg), 0, 0, 1);
                gl.glDrawArrays(GL2.GL_LINE_LOOP, 0, SUBDIVISIONS);

                gl.glRotatef(90, 1, 0, 0);
                gl.glDrawArrays(GL2.GL_LINE_LOOP, 0, SUBDIVISIONS);
            }
            gl.glPopMatrix();
        }
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
    }

    private static String formatStrip(double v) {
        String txt = String.format("%.1f", v);
        if (txt.endsWith("0")) {
            txt = txt.substring(0, txt.length() - 2);
        }
        return txt;
    }

    private void drawText(GL2 gl) {
        float zdist = 0f;

        double size = Sun.Radius * 1.06;
        // the scale factor has to be divided by the current font size
        float textScaleFactor = textScale / font.getSize();
        // adjust for font size in horizontal and vertical direction (centering the text approximately)
        float horizontalAdjustment = textScale / 2f;
        float verticalAdjustment = textScale / 3f;

        textRenderer.begin3DRendering();
        for (double phi = 0; phi <= 90; phi += latstepDegrees) {
            double angle = (90 - phi) * Math.PI / 180.;
            String txt = formatStrip(phi);

            textRenderer.draw3D(txt, (float) (Math.sin(angle) * size), (float) (Math.cos(angle) * size - verticalAdjustment), zdist, textScaleFactor);
            if (phi != 90) {
                textRenderer.draw3D(txt, (float) (-Math.sin(angle) * size - horizontalAdjustment), (float) (Math.cos(angle) * size - verticalAdjustment), zdist, textScaleFactor);
            }
        }
        for (double phi = -latstepDegrees; phi >= -90; phi -= latstepDegrees) {
            double angle = (90 - phi) * Math.PI / 180.;
            String txt = formatStrip(phi);

            textRenderer.draw3D(txt, (float) (Math.sin(angle) * size), (float) (Math.cos(angle) * size - verticalAdjustment), zdist, textScaleFactor);
            if (phi != -90) {
                textRenderer.draw3D(txt, (float) (-Math.sin(angle) * size - horizontalAdjustment), (float) (Math.cos(angle) * size - verticalAdjustment), zdist, textScaleFactor);
            }
        }
        textRenderer.end3DRendering();

        size = Sun.Radius * 1.02;

        for (double theta = 0; theta <= 180.; theta += lonstepDegrees) {
            double angle = (90 - theta) * Math.PI / 180.;
            String txt = formatStrip(theta);

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
            double angle = (90 - theta) * Math.PI / 180.;
            String txt = formatStrip(theta);

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
                colorBuffer.put(firstColor.getRed() / 255f);
                colorBuffer.put(firstColor.getGreen() / 255f);
                colorBuffer.put(firstColor.getBlue() / 255f);
            } else {
                colorBuffer.put(secondColor.getRed() / 255f);
                colorBuffer.put(secondColor.getGreen() / 255f);
                colorBuffer.put(secondColor.getBlue() / 255f);
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

    public void showLabels(boolean show) {
        showLabels = show;
    }

    public void showAxes(boolean show) {
        showAxes = show;
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
        oldFontSize = -1;
    }

    @Override
    public void renderMiniview(GL2 gl, GL3DViewport vp) {
    }

    public void setCoordinates(GridChoiceType gridChoice) {
        this.gridChoice = gridChoice;
    }

}
