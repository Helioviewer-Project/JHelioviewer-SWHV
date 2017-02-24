package org.helioviewer.jhv.renderable.components;

import java.awt.Color;
import java.awt.Component;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Mat4;
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec2;
import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.opengl.GLSLSolarShader;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;
import org.json.JSONObject;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

public class RenderableGrid extends AbstractRenderable {

    public enum GridChoiceType {
        Viewpoint, Stonyhurst, Carrington, HCI;
    }

    // height of text in solar radii
    private static final float textScale = (float) (0.08 * Sun.Radius);
    private static final int SUBDIVISIONS = 360;

    private static final float[] color1 = { Color.RED.getRed() / 255f, Color.RED.getGreen() / 255f, Color.RED.getBlue() / 255f };
    private static final float[] color2 = { Color.GREEN.getRed() / 255f, Color.GREEN.getGreen() / 255f, Color.GREEN.getBlue() / 255f };

    private static final DecimalFormat formatter1 = MathUtils.numberFormatter("0", 1);
    private static final DecimalFormat formatter2 = MathUtils.numberFormatter("0", 2);

    private float lonstepDegrees = 15f;
    private float latstepDegrees = 20f;

    private boolean showAxis = true;
    private boolean showLabels = true;
    private boolean showRadial = false;

    private final Component optionsPanel;
    private static final String name = "Grid";

    public JSONObject serialize() {
        JSONObject jo = new JSONObject();
        jo.put("lonstepDegrees", lonstepDegrees);
        jo.put("latstepDegrees", latstepDegrees);
        jo.put("showAxis", showAxis);
        jo.put("showLabels", showLabels);
        jo.put("showRadial", showRadial);
        jo.put("name", name);
        return jo;
    }

    private void deserialize(JSONObject jo) {
        if (jo.has("lonstepDegrees"))
            lonstepDegrees = (float) jo.getDouble("lonstepDegrees");
        if (jo.has("latstepDegrees"))
            latstepDegrees = (float) jo.getDouble("latstepDegrees");
        if (jo.has("showAxis"))
            showAxis = jo.getBoolean("showAxis");
        if (jo.has("showLabels"))
            showLabels = jo.getBoolean("showLabels");
        if (jo.has("showRadial"))
            showRadial = jo.getBoolean("showRadial");
    }

    public RenderableGrid(JSONObject jo) {
        if (jo != null) {
            deserialize(jo);
        }
        optionsPanel = new RenderableGridOptionsPanel(this);
        setVisible(true);

        makeLatLabels();
        makeLonLabels();
        makeRadialLabels();
    }

    private int positionBufferID;
    private int colorBufferID;
    private GridChoiceType gridChoice = GridChoiceType.Viewpoint;

    public Vec2 gridPoint(Camera camera, Viewport vp, int x, int y) {
        return GridScale.current.mouseToGrid(x, y, vp, camera, gridChoice);
    }

    public static Quat getGridQuat(Camera camera, GridChoiceType gridChoice) {
        switch (gridChoice) {
        case Viewpoint:
            return camera.getViewpoint().orientation;
        case Stonyhurst:
            Position.L p = Sun.getEarth(camera.getViewpoint().time);
            return new Quat(0, p.lon);
        case HCI:
            return Sun.getHCI(camera.getViewpoint().time);
        default: // Carrington
            return Quat.ZERO;
        }
    }

    private static final int FLAT_STEPS_THETA = 24;
    private static final int FLAT_STEPS_RADIAL = 10;

    @Override
    public void renderScale(Camera camera, Viewport vp, GL2 gl, GLSLSolarShader shader, GridScale scale) {
        if (!isVisible[vp.idx])
            return;
        int pixelsPerSolarRadius = (int) (textScale * vp.height / (2 * camera.getWidth()));
        {
            drawGridFlat(gl, vp);
            if (showLabels) {
                drawGridTextFlat(pixelsPerSolarRadius, scale, vp);
            }
        }
    }

    private static void drawGridFlat(GL2 gl, Viewport vp) {
        float w = (float) vp.aspect;
        float h = 1;

        gl.glColor3f(color1[0], color1[1], color1[2]);
        gl.glLineWidth(1);
        {
            gl.glBegin(GL2.GL_LINES);
            for (int i = 0; i < (FLAT_STEPS_THETA + 1); i++) {
                float start = -w / 2 + i * w / FLAT_STEPS_THETA;
                if (i == FLAT_STEPS_THETA / 2) {
                    gl.glColor3f(color2[0], color2[1], color2[2]);
                }
                gl.glVertex2f(start, -h / 2);
                gl.glVertex2f(start, h / 2);
                if (i == FLAT_STEPS_THETA / 2) {
                    gl.glColor3f(color1[0], color1[1], color1[2]);
                }
            }
            for (int i = 0; i < (FLAT_STEPS_RADIAL + 1); i++) {
                float start = -h / 2 + i * h / FLAT_STEPS_RADIAL;
                if (i == FLAT_STEPS_RADIAL / 2) {
                    gl.glColor3f(color2[0], color2[1], color2[2]);
                }
                gl.glVertex2f(-w / 2, start);
                gl.glVertex2f(w / 2, start);
                if (i == FLAT_STEPS_RADIAL / 2) {
                    gl.glColor3f(color1[0], color1[1], color1[2]);
                }
            }
            gl.glEnd();
        }
    }

    private static void drawGridTextFlat(int size, GridScale scale, Viewport vp) {
        float w = (float) vp.aspect;
        float h = 1;
        TextRenderer renderer = GLText.getRenderer(size);
        float textScaleFactor = textScale / renderer.getFont().getSize2D() / 3 * vp.width / 2048;

        renderer.begin3DRendering();
        {
            for (int i = 0; i < (FLAT_STEPS_THETA + 1); ++i) {
                if (i == FLAT_STEPS_THETA / 2) {
                    continue;
                }
                float start = -w / 2 + i * w / FLAT_STEPS_THETA;
                String label = formatter2.format(scale.getInterpolatedXValue(1. / FLAT_STEPS_THETA * i));
                renderer.draw3D(label, start, 0, 0, textScaleFactor);
            }
            for (int i = 0; i < (FLAT_STEPS_RADIAL + 1); ++i) {
                String label = formatter2.format(scale.getInterpolatedYValue(1. / FLAT_STEPS_RADIAL * i));
                float start = -h / 2 + i * h / FLAT_STEPS_RADIAL;
                renderer.draw3D(label, 0, start, 0, textScaleFactor);
            }
        }
        renderer.end3DRendering();
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;

        if (showAxis)
            drawAxis(gl);

        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, positionBufferID);
        gl.glVertexPointer(2, GL2.GL_FLOAT, 0, 0);

        int pixelsPerSolarRadius = (int) (textScale * vp.height / (2 * camera.getWidth()));
        Mat4 cameraMatrix = getGridQuat(camera, gridChoice).toMatrix();

        gl.glPushMatrix();
        gl.glMultMatrixd(cameraMatrix.transpose().m, 0);
        {
            drawGrid(gl);
            if (showLabels) {
                drawGridText(gl, pixelsPerSolarRadius);
            }
        }
        gl.glPopMatrix();

        if (showRadial) {
            cameraMatrix = camera.getViewpoint().orientation.toMatrix();
            gl.glPushMatrix();
            gl.glMultMatrixd(cameraMatrix.transpose().m, 0);
            {
                drawRadialGrid(gl);
                if (showLabels) {
                    drawRadialGridText(gl, pixelsPerSolarRadius);
                }
            }
            gl.glPopMatrix();
        }

        drawEarthCircles(gl, Sun.getEarth(camera.getViewpoint().time));

        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
    }

    private static final float AXIS_START = (float) (1. * Sun.Radius);
    private static final float AXIS_STOP = (float) (1.2 * Sun.Radius);

    private static void drawAxis(GL2 gl) {
        gl.glLineWidth(2);

        gl.glBegin(GL2.GL_LINES);
        {
            gl.glColor3f(0, 0, 1);
            gl.glVertex3f(0, -AXIS_STOP, 0);
            gl.glVertex3f(0, -AXIS_START, 0);
            gl.glColor3f(1, 0, 0);
            gl.glVertex3f(0, AXIS_STOP, 0);
            gl.glVertex3f(0, AXIS_START, 0);
        }
        gl.glEnd();
    }

    private static void drawEarthCircles(GL2 gl, Position.L p) {
        gl.glLineWidth(1);
        gl.glColor3f(1, 1, 0);

        gl.glPushMatrix();
        gl.glRotatef((float) (90 - 180 / Math.PI * p.lon), 0, 1, 0);
        gl.glDrawArrays(GL2.GL_LINE_LOOP, 0, SUBDIVISIONS);

        gl.glRotatef(-90, 0, 1, 0);
        gl.glRotatef((float) (90 - 180 / Math.PI * p.lat), 1, 0, 0);
        gl.glDrawArrays(GL2.GL_LINE_LOOP, 0, SUBDIVISIONS);
        gl.glPopMatrix();
    }

    private static final float END_RADIUS = 30;
    private static final float START_RADIUS = 2;
    private static final float[] R_LABEL_POS = { 2, 8, 24 };
    private static final float STEP_DEGREES = 15;

    private static void drawRadialGrid(GL2 gl) {
        gl.glPushMatrix();
        {
            gl.glColor3f(1, 1, 1);
            gl.glRotatef(90, 0, 0, 1);
            {
                gl.glPushMatrix();
                gl.glScalef(1, 1, 1);
                for (float i = START_RADIUS; i <= END_RADIUS; i++) {
                    if (i % 10 == 0) {
                        gl.glLineWidth(2);
                    } else {
                        gl.glLineWidth(1);
                    }
                    gl.glScalef(i / (i - 1), i / (i - 1), i / (i - 1));
                    gl.glDrawArrays(GL2.GL_LINE_LOOP, 0, SUBDIVISIONS);
                }
                gl.glPopMatrix();
            }
            gl.glLineWidth(1);
            {
                gl.glPushMatrix();
                for (float i = 0; i < 360; i += STEP_DEGREES) {
                    gl.glBegin(GL2.GL_LINES);
                    gl.glVertex3f(START_RADIUS, 0, 0);
                    gl.glVertex3f(END_RADIUS, 0, 0);
                    gl.glEnd();
                    gl.glRotatef(STEP_DEGREES, 0, 0, 1);
                }
                gl.glPopMatrix();
            }
        }
        gl.glPopMatrix();
    }

    private void drawRadialGridText(GL2 gl, int size) {
        gl.glDisable(GL2.GL_CULL_FACE);

        float fuzz = 0.75f;
        for (float rsize : R_LABEL_POS) {
            TextRenderer renderer = GLText.getRenderer((int) (fuzz * rsize * size));
            float textScaleFactor = textScale / renderer.getFont().getSize2D();
            renderer.begin3DRendering();
            for (GridLabel label : radialLabels) {
                renderer.draw3D(label.txt, rsize * label.x, rsize * label.y, 0, fuzz * rsize * textScaleFactor);
            }
            renderer.end3DRendering();
        }

        gl.glEnable(GL2.GL_CULL_FACE);
    }

    private void drawGrid(GL2 gl) {
        gl.glPushMatrix();
        {
            gl.glLineWidth(1);
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
                float scale, rotation = 0;
                gl.glRotatef(90, 1, 0, 0);

                gl.glDrawArrays(GL2.GL_LINE_LOOP, 0, SUBDIVISIONS);
                while (rotation < 90) {
                    gl.glPushMatrix();
                    {
                        gl.glTranslatef(0, 0, (float) (Sun.Radius * Math.sin(Math.PI / 180. * rotation)));
                        scale = (float) Math.cos(Math.PI / 180. * rotation);
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
                        gl.glTranslatef(0, 0, -(float) (Sun.Radius * Math.sin(Math.PI / 180. * rotation)));
                        scale = (float) Math.cos(Math.PI / 180. * rotation);
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
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
        }
        gl.glPopMatrix();
    }

    private static class GridLabel {
        final String txt;
        final float x;
        final float y;
        final float theta;

        GridLabel(String _txt, float _x, float _y, float _theta) {
            txt = _txt;
            x = _x;
            y = _y;
            theta = _theta;
        }
    }

    private final ArrayList<GridLabel> latLabels = new ArrayList<>();
    private final ArrayList<GridLabel> lonLabels = new ArrayList<>();
    private final ArrayList<GridLabel> radialLabels = new ArrayList<>();

    private void makeRadialLabels() {
        double size = Sun.Radius;
        double horizontalAdjustment = textScale / 2.;
        double verticalAdjustment = textScale / 3.;

        radialLabels.clear();

        for (double phi = 0; phi < 360; phi += STEP_DEGREES) {
            double angle = -phi * Math.PI / 180.;
            String txt = formatter1.format(phi);
            radialLabels.add(new GridLabel(txt, (float) (Math.sin(angle) * size - horizontalAdjustment), (float) (Math.cos(angle) * size - verticalAdjustment), 0));
        }
    }

    private void makeLatLabels() {
        double size = Sun.Radius * 1.1;
        // adjust for font size in horizontal and vertical direction (centering the text approximately)
        double horizontalAdjustment = textScale / 2.;
        double verticalAdjustment = textScale / 3.;

        latLabels.clear();

        for (double phi = 0; phi <= 90; phi += latstepDegrees) {
            double angle = (90 - phi) * Math.PI / 180.;
            String txt = formatter1.format(phi);

            latLabels.add(new GridLabel(txt, (float) (Math.sin(angle) * size), (float) (Math.cos(angle) * size - verticalAdjustment), 0));
            if (phi != 90) {
                latLabels.add(new GridLabel(txt, (float) (-Math.sin(angle) * size - horizontalAdjustment), (float) (Math.cos(angle) * size - verticalAdjustment), 0));
            }
        }
        for (double phi = -latstepDegrees; phi >= -90; phi -= latstepDegrees) {
            double angle = (90 - phi) * Math.PI / 180.;
            String txt = formatter1.format(phi);

            latLabels.add(new GridLabel(txt, (float) (Math.sin(angle) * size), (float) (Math.cos(angle) * size - verticalAdjustment), 0));
            if (phi != -90) {
                latLabels.add(new GridLabel(txt, (float) (-Math.sin(angle) * size - horizontalAdjustment), (float) (Math.cos(angle) * size - verticalAdjustment), 0));
            }
        }
    }

    private void makeLonLabels() {
        lonLabels.clear();

        double size = Sun.Radius * 1.05;
        for (double theta = 0; theta <= 180.; theta += lonstepDegrees) {
            double angle = (90 - theta) * Math.PI / 180.;
            String txt = formatter1.format(theta);
            lonLabels.add(new GridLabel(txt, (float) (Math.cos(angle) * size), (float) (Math.sin(angle) * size), (float) theta));
        }
        for (double theta = -lonstepDegrees; theta > -180.; theta -= lonstepDegrees) {
            double angle = (90 - theta) * Math.PI / 180.;
            String txt = gridChoice == GridChoiceType.Carrington ? formatter1.format(theta + 360) : formatter1.format(theta);
            lonLabels.add(new GridLabel(txt, (float) (Math.cos(angle) * size), (float) (Math.sin(angle) * size), (float) theta));
        }
    }

    private void drawGridText(GL2 gl, int size) {
        TextRenderer renderer = GLText.getRenderer(size);
        // the scale factor has to be divided by the current font size
        float textScaleFactor = textScale / renderer.getFont().getSize2D();

        renderer.begin3DRendering();

        gl.glDisable(GL2.GL_CULL_FACE);
        for (GridLabel label : latLabels) {
            renderer.draw3D(label.txt, label.x, label.y, 0, textScaleFactor);
        }
        renderer.flush();
        gl.glEnable(GL2.GL_CULL_FACE);

        for (GridLabel lonLabel : lonLabels) {
            gl.glPushMatrix();
            {
                gl.glTranslatef(lonLabel.x, 0, lonLabel.y);
                gl.glRotatef(lonLabel.theta, 0, 1, 0);

                renderer.draw3D(lonLabel.txt, 0, 0, 0, textScaleFactor);
                renderer.flush();
            }
            gl.glPopMatrix();
        }
        renderer.end3DRendering();
    }

    @Override
    public void init(GL2 gl) {
        FloatBuffer positionBuffer = FloatBuffer.allocate((SUBDIVISIONS + 1) * 2);
        FloatBuffer colorBuffer = FloatBuffer.allocate((SUBDIVISIONS + 1) * 3);

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            positionBuffer.put((float) (Sun.Radius * Math.cos(2 * Math.PI * i / SUBDIVISIONS)));
            positionBuffer.put((float) (Sun.Radius * Math.sin(2 * Math.PI * i / SUBDIVISIONS)));
            if (i % 2 == 0) {
                colorBuffer.put(color1);
            } else {
                colorBuffer.put(color2);
            }
        }
        positionBuffer.flip();
        colorBuffer.flip();

        positionBufferID = generate(gl);
        colorBufferID = generate(gl);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, positionBufferID);
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, positionBuffer.capacity() * Buffers.SIZEOF_FLOAT, positionBuffer, GL2.GL_STATIC_DRAW);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, colorBufferID);
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, colorBuffer.capacity() * Buffers.SIZEOF_FLOAT, colorBuffer, GL2.GL_STATIC_DRAW);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
    }

    private static int generate(GL2 gl) {
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

    public double getLonstepDegrees() {
        return lonstepDegrees;
    }

    public void setLonstepDegrees(double _lonstepDegrees) {
        lonstepDegrees = (float) _lonstepDegrees;
        makeLonLabels();
    }

    public double getLatstepDegrees() {
        return latstepDegrees;
    }

    public void setLatstepDegrees(double _latstepDegrees) {
        latstepDegrees = (float) _latstepDegrees;
        makeLatLabels();
    }

    public boolean getShowLabels() {
        return showLabels;
    }

    public boolean getShowAxis() {
        return showAxis;
    }

    public boolean getShowRadial() {
        return showRadial;
    }

    public void showLabels(boolean show) {
        showLabels = show;
    }

    public void showAxis(boolean show) {
        showAxis = show;
    }

    public void showRadial(boolean show) {
        showRadial = show;
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
        gl.glDeleteBuffers(1, new int[] { positionBufferID }, 0);
        gl.glDeleteBuffers(1, new int[] { colorBufferID }, 0);
    }

    public void setCoordinates(GridChoiceType _gridChoice) {
        gridChoice = _gridChoice;
        makeLonLabels();
    }

}
