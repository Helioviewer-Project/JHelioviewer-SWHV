package org.helioviewer.jhv.renderable.components;

import java.awt.Component;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Mat4;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
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

    private static final double RADIAL_UNIT = Sun.Radius /*Sun.MeanEarthDistance / 10*/;
    private static final double RADIAL_STEP = 15 /*45*/;

    // height of text in solar radii
    private static final float textScale = (float) (0.08 * Sun.Radius);
    private static final double thickness = 0.001;
    private static final double thicknessEarth = 0.0015;
    private static final double thicknessAxes = 0.003;

    private static final float[] R_LABEL_POS = { (float) (2 * RADIAL_UNIT), (float) (8 * RADIAL_UNIT), (float) (24 * RADIAL_UNIT) };

    private static final DecimalFormat formatter1 = MathUtils.numberFormatter("0", 1);
    private static final DecimalFormat formatter2 = MathUtils.numberFormatter("0", 2);

    private GridType gridType = GridType.Viewpoint;

    private double lonstepDegrees = 15;
    private double latstepDegrees = 20;
    private boolean gridNeedsInit = true;

    private boolean showAxis = true;
    private boolean showLabels = true;
    private boolean showRadial = false;

    private final GLLine axesLine = new GLLine();
    private final GLLine earthCircleLine = new GLLine();
    private final GLLine radialCircleLine = new GLLine();
    private final GLLine radialThickLine = new GLLine();
    private final GLLine flatLine = new GLLine();
    private final GLLine gridLine = new GLLine();

    private final ArrayList<GridLabel> latLabels = new ArrayList<>();
    private final ArrayList<GridLabel> lonLabels = new ArrayList<>();
    private final ArrayList<GridLabel> radialLabels;

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
        lonstepDegrees = jo.optDouble("lonstepDegrees", lonstepDegrees);
        latstepDegrees = jo.optDouble("latstepDegrees", latstepDegrees);
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
        makeLatLabels();
        makeLonLabels();
        radialLabels = makeRadialLabels(0);
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
        if (gridNeedsInit) {
            RenderableGridMath.initGrid(gl, gridLine, lonstepDegrees, latstepDegrees);
            gridNeedsInit = false;
        }

        if (showAxis)
            axesLine.render(gl, vp.aspect, thicknessAxes);

        Mat4 cameraMatrix = getGridQuat(camera, gridType).toMatrix();
        double pixelsPerSolarRadius = textScale * vp.height / (2 * camera.getWidth());

        gl.glPushMatrix();
        gl.glMultMatrixd(cameraMatrix.transpose().m, 0);
        {
            gridLine.render(gl, vp.aspect, thickness);
            if (showLabels) {
                drawGridText(gl, (int) pixelsPerSolarRadius);
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
                    drawRadialGridText(gl, pixelsPerSolarRadius * RADIAL_UNIT);
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
            RenderableGridMath.initFlatGrid(gl, flatLine, vp.aspect);
            previousAspect = vp.aspect;
        }
        flatLine.render(gl, vp.aspect, thickness);
    }

    private static void drawGridTextFlat(int size, GridScale scale, Viewport vp) {
        float w = (float) vp.aspect;
        float h = 1;
        TextRenderer renderer = GLText.getRenderer(size);
        float textScaleFactor = textScale / renderer.getFont().getSize2D() / 3 * vp.width / 2048;

        renderer.begin3DRendering();
        {
            for (int i = 0; i <= RenderableGridMath.FLAT_STEPS_THETA; i++) {
                if (i == RenderableGridMath.FLAT_STEPS_THETA / 2) {
                    continue;
                }
                float start = -w / 2 + i * w / RenderableGridMath.FLAT_STEPS_THETA;
                String label = formatter2.format(scale.getInterpolatedXValue(1. / RenderableGridMath.FLAT_STEPS_THETA * i));
                renderer.draw3D(label, start, 0, 0, textScaleFactor);
            }
            for (int i = 0; i <= RenderableGridMath.FLAT_STEPS_RADIAL; i++) {
                String label = formatter2.format(scale.getInterpolatedYValue(1. / RenderableGridMath.FLAT_STEPS_RADIAL * i));
                float start = -h / 2 + i * h / RenderableGridMath.FLAT_STEPS_RADIAL;
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

    private void drawRadialGridText(GL2 gl, double size) {
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

    private ArrayList<GridLabel> makeRadialLabels(double delta) {
        double size = Sun.Radius;
        double horizontalAdjustment = textScale / 2.;
        double verticalAdjustment = textScale / 3.;

        ArrayList<GridLabel> labels = new ArrayList<>();
        for (double phi = 0; phi < 360; phi += RADIAL_STEP) {
            double angle = -phi * Math.PI / 180. + delta;
            String txt = formatter1.format(phi);
            labels.add(new GridLabel(txt, (float) (Math.sin(angle) * size - horizontalAdjustment), (float) (Math.cos(angle) * size - verticalAdjustment), 0));
        }
        return labels;
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
        gridLine.init(gl);
        RenderableGridMath.initGrid(gl, gridLine, lonstepDegrees, latstepDegrees);
        gridNeedsInit = false;

        axesLine.init(gl);
        RenderableGridMath.initAxes(gl, axesLine);

        earthCircleLine.init(gl);
        RenderableGridMath.initEarthCircles(gl, earthCircleLine);

        radialCircleLine.init(gl);
        radialThickLine.init(gl);
        RenderableGridMath.initRadialCircles(gl, radialCircleLine, radialThickLine, RADIAL_UNIT, RADIAL_STEP);

        flatLine.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        gridLine.dispose(gl);
        axesLine.dispose(gl);
        earthCircleLine.dispose(gl);
        radialCircleLine.dispose(gl);
        radialThickLine.dispose(gl);
        flatLine.dispose(gl);
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
        lonstepDegrees = _lonstepDegrees;
        makeLonLabels();
        gridNeedsInit = true;
    }

    public double getLatstepDegrees() {
        return latstepDegrees;
    }

    public void setLatstepDegrees(double _latstepDegrees) {
        latstepDegrees = _latstepDegrees;
        makeLatLabels();
        gridNeedsInit = true;
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

    GridType getGridType() {
        return gridType;
    }

    void setGridType(GridType _gridType) {
        gridType = _gridType;
        makeLonLabels();
    }

}
