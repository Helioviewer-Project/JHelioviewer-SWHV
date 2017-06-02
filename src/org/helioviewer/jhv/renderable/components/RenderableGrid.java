package org.helioviewer.jhv.renderable.components;

import java.awt.Component;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Mat4;
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec2;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.opengl.GLLine;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

public class RenderableGrid extends AbstractRenderable {

    public enum GridType {
        Viewpoint, Stonyhurst, Carrington, HCI
    }

    // height of text in solar radii
    private static final float textScale = (float) (0.08 * Sun.Radius);
    private static final int SUBDIVISIONS = 360;
    private static final double thickness = 0.001;
    private static final double thicknessEarth = 0.0015;
    private static final double thicknessAxes = 0.003;

    private static final float[] color1 = BufferUtils.colorRed;
    private static final float[] color2 = BufferUtils.colorGreen;
    private static final float[] earthLineColor = BufferUtils.colorYellow;
    private static final float[] radialLineColor = BufferUtils.colorWhite;

    private static final int TENS_RADIUS = 3;
    private static final int END_RADIUS = TENS_RADIUS * 10;
    private static final int START_RADIUS = 2;
    private static final float[] R_LABEL_POS = { 2, 8, 24 };
    private static final float STEP_DEGREES = 15;

    private static final int FLAT_STEPS_THETA = 24;
    private static final int FLAT_STEPS_RADIAL = 10;

    private static final DecimalFormat formatter1 = MathUtils.numberFormatter("0", 1);
    private static final DecimalFormat formatter2 = MathUtils.numberFormatter("0", 2);

    private GridType gridType = GridType.Viewpoint;

    private float lonstepDegrees = 15f;
    private float latstepDegrees = 20f;
    private boolean needsInit = true;

    private boolean showAxis = true;
    private boolean showLabels = true;
    private boolean showRadial = false;

    private final GLLine gridline = new GLLine();
    private final GLLine axesLine = new GLLine();
    private final GLLine flatline = new GLLine();
    private final GLLine earthCircleLine = new GLLine();
    private final GLLine radialCircleLine = new GLLine();
    private final GLLine radialThickLine = new GLLine();

    private final Component optionsPanel;

    @Override
    public void serialize(JSONObject jo) {
        jo.put("lonstepDegrees", lonstepDegrees);
        jo.put("latstepDegrees", latstepDegrees);
        jo.put("showAxis", showAxis);
        jo.put("showLabels", showLabels);
        jo.put("showRadial", showRadial);
        jo.put("type", gridType);
    }

    private void deserialize(JSONObject jo) {
        lonstepDegrees = (float) jo.optDouble("lonstepDegrees", lonstepDegrees);
        latstepDegrees = (float) jo.optDouble("latstepDegrees", latstepDegrees);
        showAxis = jo.optBoolean("showAxis", showAxis);
        showLabels = jo.optBoolean("showLabels", showLabels);
        showRadial = jo.optBoolean("showRadial", showRadial);

        String strGridType = jo.optString("type", gridType.toString());
        try {
            gridType = GridType.valueOf(strGridType);
        } catch (Exception ignore) {
        }
    }

    public RenderableGrid(JSONObject jo) {
        if (jo != null)
            deserialize(jo);

        optionsPanel = new RenderableGridOptionsPanel(this);
        setVisible(true);

        makeLatLabels();
        makeLonLabels();
        makeRadialLabels();
    }

    public Vec2 gridPoint(Camera camera, Viewport vp, int x, int y) {
        return Displayer.mode.scale.mouseToGrid(x, y, vp, camera, gridType);
    }

    public static Quat getGridQuat(Camera camera, GridType _gridType) { // should be in GridScale
        switch (_gridType) {
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

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;
        if (needsInit) {
            initGrid(gl);
        }
        if (showAxis)
            axesLine.render(gl, vp.aspect, thicknessAxes);

        Mat4 cameraMatrix = getGridQuat(camera, gridType).toMatrix();
        int pixelsPerSolarRadius = (int) (textScale * vp.height / (2 * camera.getWidth()));

        gl.glPushMatrix();
        gl.glMultMatrixd(cameraMatrix.transpose().m, 0);
        {
            gridline.render(gl, vp.aspect, thickness);
            if (showLabels) {
                drawGridText(gl, pixelsPerSolarRadius);
            }
        }
        gl.glPopMatrix();
        drawEarthCircles(gl, vp.aspect, Sun.getEarthQuat(camera.getViewpoint().time).orientation);

        if (showRadial) {
            cameraMatrix = camera.getViewpoint().orientation.toMatrix();
            gl.glPushMatrix();
            gl.glMultMatrixd(cameraMatrix.transpose().m, 0);
            {
                radialCircleLine.render(gl, vp.aspect, thickness);
                radialThickLine.render(gl, vp.aspect, 3 * thickness);
                if (showLabels) {
                    drawRadialGridText(gl, pixelsPerSolarRadius);
                }
            }
            gl.glPopMatrix();
        }
    }

    @Override
    public void renderScale(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;
        int pixelsPerSolarRadius = (int) (textScale * vp.height / (2 * camera.getWidth()));
        {
            drawGridFlat(gl, vp);
            if (showLabels) {
                drawGridTextFlat(pixelsPerSolarRadius, Displayer.mode.scale, vp);
            }
        }
    }

    private double previousAspect = -1;

    private void drawGridFlat(GL2 gl, Viewport vp) {
        if (previousAspect != vp.aspect) {
            initFlatline(gl, vp.aspect);
            previousAspect = vp.aspect;
        }
        flatline.render(gl, vp.aspect, thickness);
    }

    private static void drawGridTextFlat(int size, GridScale scale, Viewport vp) {
        float w = (float) vp.aspect;
        float h = 1;
        TextRenderer renderer = GLText.getRenderer(size);
        float textScaleFactor = textScale / renderer.getFont().getSize2D() / 3 * vp.width / 2048;

        renderer.begin3DRendering();
        {
            for (int i = 0; i <= FLAT_STEPS_THETA; i++) {
                if (i == FLAT_STEPS_THETA / 2) {
                    continue;
                }
                float start = -w / 2 + i * w / FLAT_STEPS_THETA;
                String label = formatter2.format(scale.getInterpolatedXValue(1. / FLAT_STEPS_THETA * i));
                renderer.draw3D(label, start, 0, 0, textScaleFactor);
            }
            for (int i = 0; i <= FLAT_STEPS_RADIAL; i++) {
                String label = formatter2.format(scale.getInterpolatedYValue(1. / FLAT_STEPS_RADIAL * i));
                float start = -h / 2 + i * h / FLAT_STEPS_RADIAL;
                renderer.draw3D(label, 0, start, 0, textScaleFactor);
            }
        }
        renderer.end3DRendering();
    }

    private void drawEarthCircles(GL2 gl, double aspect, Quat q) {
        gl.glPushMatrix();
        gl.glMultMatrixd(q.toMatrix().transpose().m, 0);
        earthCircleLine.render(gl, aspect, thicknessEarth);
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
            radialLabels.add(new GridLabel(txt, (float) (Math.sin(angle) * size - horizontalAdjustment),
                    (float) (Math.cos(angle) * size - verticalAdjustment), 0));
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

            latLabels.add(new GridLabel(txt, (float) (Math.sin(angle) * size),
                    (float) (Math.cos(angle) * size - verticalAdjustment), 0));
            if (phi != 90) {
                latLabels.add(new GridLabel(txt, (float) (-Math.sin(angle) * size - horizontalAdjustment),
                        (float) (Math.cos(angle) * size - verticalAdjustment), 0));
            }
        }
        for (double phi = -latstepDegrees; phi >= -90; phi -= latstepDegrees) {
            double angle = (90 - phi) * Math.PI / 180.;
            String txt = formatter1.format(phi);

            latLabels.add(new GridLabel(txt, (float) (Math.sin(angle) * size),
                    (float) (Math.cos(angle) * size - verticalAdjustment), 0));
            if (phi != -90) {
                latLabels.add(new GridLabel(txt, (float) (-Math.sin(angle) * size - horizontalAdjustment),
                        (float) (Math.cos(angle) * size - verticalAdjustment), 0));
            }
        }
    }

    private void makeLonLabels() {
        lonLabels.clear();

        double size = Sun.Radius * 1.05;
        for (double theta = 0; theta <= 180.; theta += lonstepDegrees) {
            double angle = (90 - theta) * Math.PI / 180.;
            String txt = formatter1.format(theta);
            lonLabels.add(new GridLabel(txt, (float) (Math.cos(angle) * size), (float) (Math.sin(angle) * size),
                    (float) theta));
        }
        for (double theta = -lonstepDegrees; theta > -180.; theta -= lonstepDegrees) {
            double angle = (90 - theta) * Math.PI / 180.;
            String txt = gridType == GridType.Carrington ? formatter1.format(theta + 360) : formatter1.format(theta);
            lonLabels.add(new GridLabel(txt, (float) (Math.cos(angle) * size), (float) (Math.sin(angle) * size),
                    (float) theta));
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
        gridline.init(gl);
        initGrid(gl);

        axesLine.init(gl);
        RenderableGridMath.initAxes(gl, axesLine);

        earthCircleLine.init(gl);
        initEarthCircles(gl);
        radialCircleLine.init(gl);
        radialThickLine.init(gl);
        initRadialCircles(gl);
        flatline.init(gl);
    }

    private void initFlatline(GL2 gl, double aspect) {
        float w = (float) aspect;
        float h = 1;

        int plen = 4 * ((FLAT_STEPS_THETA + 1) + (FLAT_STEPS_RADIAL + 1));
        FloatBuffer positionBuffer = BufferUtils.newFloatBuffer(plen * 3);
        FloatBuffer colorBuffer = BufferUtils.newFloatBuffer(plen * 4);

        for (int i = 0; i <= FLAT_STEPS_THETA; i++) {
            float start = -w / 2 + i * w / FLAT_STEPS_THETA;
            BufferUtils.put3f(positionBuffer, start, -h / 2, 0);
            colorBuffer.put(BufferUtils.colorNull);

            BufferUtils.put3f(positionBuffer, start, -h / 2, 0);
            if (i == FLAT_STEPS_THETA / 2) {
                colorBuffer.put(color2);
                colorBuffer.put(color2);
            } else {
                colorBuffer.put(color1);
                colorBuffer.put(color1);
            }
            BufferUtils.put3f(positionBuffer, start, h / 2, 0);

            BufferUtils.put3f(positionBuffer, start, h / 2, 0);
            colorBuffer.put(BufferUtils.colorNull);
        }
        for (int i = 0; i <= FLAT_STEPS_RADIAL; i++) {
            float start = -h / 2 + i * h / FLAT_STEPS_RADIAL;
            BufferUtils.put3f(positionBuffer, -w / 2, start, 0);
            colorBuffer.put(BufferUtils.colorNull);

            BufferUtils.put3f(positionBuffer, -w / 2, start, 0);
            if (i == FLAT_STEPS_RADIAL / 2) {
                colorBuffer.put(color2);
                colorBuffer.put(color2);
            } else {
                colorBuffer.put(color1);
                colorBuffer.put(color1);
            }
            BufferUtils.put3f(positionBuffer, w / 2, start, 0);

            BufferUtils.put3f(positionBuffer, w / 2, start, 0);
            colorBuffer.put(BufferUtils.colorNull);
        }
        positionBuffer.flip();
        colorBuffer.flip();
        flatline.setData(gl, positionBuffer, colorBuffer);
    }

    private void initRadialCircles(GL2 gl) {
        int no_lines = (int) Math.ceil(360 / STEP_DEGREES);

        int no_points = (END_RADIUS - START_RADIUS + 1 - TENS_RADIUS) * (SUBDIVISIONS + 1) + 4 * no_lines + 1;
        FloatBuffer positionBuffer = BufferUtils.newFloatBuffer(no_points * 3);
        FloatBuffer colorBuffer = BufferUtils.newFloatBuffer(no_points * 4);
        FloatBuffer positionThick = BufferUtils.newFloatBuffer(TENS_RADIUS * (SUBDIVISIONS + 3) * 3);
        FloatBuffer colorThick = BufferUtils.newFloatBuffer(TENS_RADIUS * (SUBDIVISIONS + 3) * 4);

        Vec3 v = new Vec3();
        for (int i = START_RADIUS; i <= END_RADIUS; i++) {
            for (int j = 0; j <= SUBDIVISIONS; j++) {
                v.x = i * Sun.Radius * Math.cos(2 * Math.PI * j / SUBDIVISIONS);
                v.y = i * Sun.Radius * Math.sin(2 * Math.PI * j / SUBDIVISIONS);
                v.z = 0.;
                if (i % 10 == 0) {
                    if (j == 0) {
                        BufferUtils.put3f(positionThick, v);
                        colorThick.put(BufferUtils.colorNull);
                    }
                    BufferUtils.put3f(positionThick, v);
                    colorThick.put(radialLineColor);
                    if (j == SUBDIVISIONS) {
                        BufferUtils.put3f(positionThick, v);
                        colorThick.put(BufferUtils.colorNull);
                    }
                } else {
                    BufferUtils.put3f(positionBuffer, v);
                    colorBuffer.put(radialLineColor);
                }
            }
        }

        // repeat last point with 0 alpha
        BufferUtils.put3f(positionBuffer, v);
        colorBuffer.put(BufferUtils.colorNull);

        float i = 0;
        for (int j = 0; j < no_lines; j++) {
            i += STEP_DEGREES;
            Quat q = Quat.createRotation(2 * Math.PI * i / 360., new Vec3(0, 0, 1));

            v.set(START_RADIUS, 0, 0);
            Vec3 rotv1 = q.rotateVector(v);
            BufferUtils.put3f(positionBuffer, rotv1);
            colorBuffer.put(BufferUtils.colorNull);
            BufferUtils.put3f(positionBuffer, rotv1);
            colorBuffer.put(radialLineColor);

            v.set(END_RADIUS, 0, 0);
            Vec3 rotv2 = q.rotateVector(v);
            BufferUtils.put3f(positionBuffer, rotv2);
            colorBuffer.put(radialLineColor);
            BufferUtils.put3f(positionBuffer, rotv2);
            colorBuffer.put(BufferUtils.colorNull);
        }
        positionBuffer.flip();
        colorBuffer.flip();
        positionThick.flip();
        colorThick.flip();

        radialCircleLine.setData(gl, positionBuffer, colorBuffer);
        radialThickLine.setData(gl, positionThick, colorThick);
    }

    private static final double gridRadius = Sun.Radius;

    private void initEarthCircles(GL2 gl) {
        int no_points = 2 * (SUBDIVISIONS + 3);
        FloatBuffer positionBuffer = BufferUtils.newFloatBuffer(no_points * 3);
        FloatBuffer colorBuffer = BufferUtils.newFloatBuffer(no_points * 4);

        Vec3 rotv = new Vec3(), v = new Vec3();
        Quat q = Quat.createRotation(Math.PI / 2, new Vec3(1, 0, 0));
        for (int i = 0; i <= SUBDIVISIONS; i++) {
            v.x = gridRadius * Math.cos(2 * Math.PI * i / SUBDIVISIONS);
            v.y = gridRadius * Math.sin(2 * Math.PI * i / SUBDIVISIONS);
            v.z = 0.;
            rotv = q.rotateVector(v);
            if (i == 0) {
                BufferUtils.put3f(positionBuffer, rotv);
                colorBuffer.put(BufferUtils.colorNull);
            }
            BufferUtils.put3f(positionBuffer, rotv);
            colorBuffer.put(earthLineColor);
        }

        BufferUtils.put3f(positionBuffer, rotv);
        colorBuffer.put(BufferUtils.colorNull);

        v = new Vec3();
        q = Quat.createRotation(Math.PI / 2, new Vec3(0, 1, 0));
        for (int i = 0; i <= SUBDIVISIONS; i++) {
            v.x = gridRadius * Math.cos(2 * Math.PI * i / SUBDIVISIONS);
            v.y = gridRadius * Math.sin(2 * Math.PI * i / SUBDIVISIONS);
            v.z = 0.;
            rotv = q.rotateVector(v);
            if (i == 0) {
                BufferUtils.put3f(positionBuffer, rotv);
                colorBuffer.put(BufferUtils.colorNull);
            }
            BufferUtils.put3f(positionBuffer, rotv);
            colorBuffer.put(earthLineColor);
        }

        BufferUtils.put3f(positionBuffer, rotv);
        colorBuffer.put(BufferUtils.colorNull);

        positionBuffer.flip();
        colorBuffer.flip();
        earthCircleLine.setData(gl, positionBuffer, colorBuffer);
    }

    private void initGrid(GL2 gl) {
        int no_lon_steps = ((int) Math.ceil(360 / lonstepDegrees)) / 2 + 1;
        int no_lat_steps = ((int) Math.ceil(180 / latstepDegrees)) / 2;
        int HALFDIVISIONS = SUBDIVISIONS / 2;

        int no_points = 2 * (no_lat_steps + no_lon_steps) * (HALFDIVISIONS + 3);

        FloatBuffer positionBuffer = BufferUtils.newFloatBuffer(no_points * 3);
        FloatBuffer colorBuffer = BufferUtils.newFloatBuffer(no_points * 4);

        Vec3 v = new Vec3();
        double rotation;
        for (int j = 0; j < no_lon_steps; j++) {
            for (int k = -1; k <= 1; k = k + 2) {
                rotation = lonstepDegrees * j * k;
                Quat q = Quat.createRotation(Math.PI / 2 + Math.PI + 2 * Math.PI * rotation / 360., new Vec3(0, 1, 0));
                for (int i = 0; i <= HALFDIVISIONS; i++) {
                    v.x = gridRadius * Math.cos(-Math.PI / 2 + Math.PI * i / HALFDIVISIONS);
                    v.y = gridRadius * Math.sin(-Math.PI / 2 + Math.PI * i / HALFDIVISIONS);
                    v.z = 0.;
                    Vec3 rotv = q.rotateVector(v);
                    if (i == 0) {
                        BufferUtils.put3f(positionBuffer, rotv);
                        colorBuffer.put(BufferUtils.colorNull);
                    }
                    BufferUtils.put3f(positionBuffer, rotv);
                    if (i % 2 == 0) {
                        colorBuffer.put(color1);
                    } else {
                        colorBuffer.put(color2);
                    }

                    if (i == HALFDIVISIONS) {
                        BufferUtils.put3f(positionBuffer, rotv);
                        colorBuffer.put(BufferUtils.colorNull);
                    }
                }
            }
        }

        for (int j = 0; j < no_lat_steps; j++) {
            for (int k = -1; k <= 1; k = k + 2) {
                rotation = latstepDegrees * j * k;
                for (int i = 0; i <= HALFDIVISIONS; i++) {
                    double scale = Math.cos(Math.PI / 180. * (90 - rotation));
                    v.y = gridRadius * scale;
                    v.x = gridRadius * Math.sqrt(1. - scale * scale) * Math.sin(2 * Math.PI * i / HALFDIVISIONS);
                    v.z = gridRadius * Math.sqrt(1. - scale * scale) * Math.cos(2 * Math.PI * i / HALFDIVISIONS);
                    if (i == 0) {
                        BufferUtils.put3f(positionBuffer, v);
                        colorBuffer.put(BufferUtils.colorNull);
                    }
                    BufferUtils.put3f(positionBuffer, v);
                    if (i % 2 == 0) {
                        colorBuffer.put(color1);
                    } else {
                        colorBuffer.put(color2);
                    }
                    if (i == HALFDIVISIONS) {
                        BufferUtils.put3f(positionBuffer, v);
                        colorBuffer.put(BufferUtils.colorNull);
                    }
                }
            }
        }

        positionBuffer.flip();
        colorBuffer.flip();
        gridline.setData(gl, positionBuffer, colorBuffer);
        needsInit = false;
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
        return "Grid";
    }

    public double getLonstepDegrees() {
        return lonstepDegrees;
    }

    public void setLonstepDegrees(double _lonstepDegrees) {
        lonstepDegrees = (float) _lonstepDegrees;
        makeLonLabels();
        needsInit = true;
    }

    public double getLatstepDegrees() {
        return latstepDegrees;
    }

    public void setLatstepDegrees(double _latstepDegrees) {
        latstepDegrees = (float) _latstepDegrees;
        makeLatLabels();
        needsInit = true;
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
        gridline.dispose(gl);
        axesLine.dispose(gl);
        flatline.dispose(gl);
        earthCircleLine.dispose(gl);
        radialCircleLine.dispose(gl);
        radialThickLine.dispose(gl);
    }

    GridType getGridType() {
        return gridType;
    }

    void setGridType(GridType _gridType) {
        gridType = _gridType;
        makeLonLabels();
    }

}
