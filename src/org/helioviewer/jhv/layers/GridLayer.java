package org.helioviewer.jhv.layers;

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
import org.helioviewer.jhv.opengl.GLPoint;
import org.helioviewer.jhv.opengl.GLText;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

public class GridLayer extends AbstractLayer {

    public enum GridType {
        Viewpoint, Stonyhurst, Carrington, HCI
    }

    private static final double RADIAL_UNIT = Sun.Radius;
    private static final double RADIAL_STEP = 15;
    private static final double RADIAL_UNIT_FAR = Sun.MeanEarthDistance / 10;
    private static final double RADIAL_STEP_FAR = 45;
    private static final float[] R_LABEL_POS = { (float) (2 * RADIAL_UNIT), (float) (8 * RADIAL_UNIT), (float) (24 * RADIAL_UNIT) };
    private static final float[] R_LABEL_POS_FAR = { (float) (2 * RADIAL_UNIT_FAR), (float) (8 * RADIAL_UNIT_FAR), (float) (24 * RADIAL_UNIT_FAR) };

    // height of text in solar radii
    private static final float textScale = GridLabel.textScale;
    private static final double thickness = 0.002;
    private static final double thicknessEarth = 0.003;
    private static final double thicknessAxes = 0.005;

    private static final DecimalFormat formatter2 = MathUtils.numberFormatter("0", 2);

    private GridType gridType = GridType.Viewpoint;

    private double lonStep = 15;
    private double latStep = 20;
    private boolean gridNeedsInit = true;

    private boolean showAxis = true;
    private boolean showLabels = true;
    private boolean showRadial = false;

    private final GLPoint earthPoint = new GLPoint();
    private final GLLine axesLine = new GLLine();
    private final GLLine earthCircleLine = new GLLine();
    private final GLLine radialCircleLine = new GLLine();
    private final GLLine radialThickLine = new GLLine();
    private final GLLine radialCircleLineFar = new GLLine();
    private final GLLine radialThickLineFar = new GLLine();
    private final GLLine flatLine = new GLLine();
    private final GLLine gridLine = new GLLine();

    private ArrayList<GridLabel> latLabels;
    private ArrayList<GridLabel> lonLabels;
    private final ArrayList<GridLabel> radialLabels;
    private final ArrayList<GridLabel> radialLabelsFar;

    private final Component optionsPanel;

    @Override
    public void serialize(JSONObject jo) {
        jo.put("lonStep", lonStep);
        jo.put("latStep", latStep);
        jo.put("showAxis", showAxis);
        jo.put("showLabels", showLabels);
        jo.put("showRadial", showRadial);
        jo.put("type", gridType);
    }

    private void deserialize(JSONObject jo) {
        lonStep = jo.optDouble("lonStep", lonStep);
        latStep = jo.optDouble("latStep", latStep);
        showAxis = jo.optBoolean("showAxis", showAxis);
        showLabels = jo.optBoolean("showLabels", showLabels);
        showRadial = jo.optBoolean("showRadial", showRadial);

        String strGridType = jo.optString("type", gridType.toString());
        try {
            gridType = GridType.valueOf(strGridType);
        } catch (Exception ignore) {
        }
    }

    public GridLayer(JSONObject jo) {
        if (jo != null)
            deserialize(jo);
        else
            setEnabled(true);
        optionsPanel = new GridLayerOptions(this);

        latLabels = GridLabel.makeLatLabels(latStep);
        lonLabels = GridLabel.makeLonLabels(gridType, lonStep);
        radialLabels = GridLabel.makeRadialLabels(0, RADIAL_STEP);
        radialLabelsFar = GridLabel.makeRadialLabels(Math.PI / 2, RADIAL_STEP_FAR);
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
            GridMath.initGrid(gl, gridLine, lonStep, latStep);
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
        drawEarthCircles(gl, vp.aspect, 1 / camera.getFOV(), Sun.getEarthQuat(camera.getViewpoint().time).orientation);

        if (showRadial) {
            boolean far = camera.getViewpoint().distance > 100 * Sun.MeanEarthDistance;
            cameraMatrix = camera.getViewpoint().orientation.toMatrix();
            gl.glPushMatrix();
            gl.glMultMatrixd(cameraMatrix.transpose().m, 0);
            {
                if (far) {
                    radialCircleLineFar.render(gl, vp.aspect, thickness);
                    radialThickLineFar.render(gl, vp.aspect, 3 * thickness);
                } else {
                    radialCircleLine.render(gl, vp.aspect, thickness);
                    radialThickLine.render(gl, vp.aspect, 3 * thickness);
                }
                if (showLabels) {
                    if (far)
                        drawRadialGridText(gl, radialLabelsFar, pixelsPerSolarRadius * RADIAL_UNIT_FAR, R_LABEL_POS_FAR);
                    else
                        drawRadialGridText(gl, radialLabels, pixelsPerSolarRadius * RADIAL_UNIT, R_LABEL_POS);
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
            GridMath.initFlatGrid(gl, flatLine, vp.aspect);
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
            for (int i = 0; i <= GridMath.FLAT_STEPS_THETA; i++) {
                if (i == GridMath.FLAT_STEPS_THETA / 2) {
                    continue;
                }
                float start = -w / 2 + i * w / GridMath.FLAT_STEPS_THETA;
                String label = formatter2.format(scale.getInterpolatedXValue(1. / GridMath.FLAT_STEPS_THETA * i));
                renderer.draw3D(label, start, 0, 0, textScaleFactor);
            }
            for (int i = 0; i <= GridMath.FLAT_STEPS_RADIAL; i++) {
                String label = formatter2.format(scale.getInterpolatedYValue(1. / GridMath.FLAT_STEPS_RADIAL * i));
                float start = -h / 2 + i * h / GridMath.FLAT_STEPS_RADIAL;
                renderer.draw3D(label, 0, start, 0, textScaleFactor);
            }
        }
        renderer.end3DRendering();
    }

    private void drawEarthCircles(GL2 gl, double aspect, double factor, Quat q) {
        gl.glPushMatrix();
        gl.glMultMatrixd(q.toMatrix().transpose().m, 0);
        earthCircleLine.render(gl, aspect, thicknessEarth);
        earthPoint.render(gl, factor);
        gl.glPopMatrix();
    }

    private static void drawRadialGridText(GL2 gl, ArrayList<GridLabel> labels, double size, float[] labelPos) {
        gl.glDisable(GL2.GL_CULL_FACE);

        float fuzz = 0.75f;
        for (float rsize : labelPos) {
            TextRenderer renderer = GLText.getRenderer((int) (fuzz * rsize * size));
            float textScaleFactor = textScale / renderer.getFont().getSize2D();
            renderer.begin3DRendering();
            for (GridLabel label : labels) {
                renderer.draw3D(label.txt, rsize * label.x, rsize * label.y, 0, fuzz * rsize * textScaleFactor);
            }
            renderer.end3DRendering();
        }

        gl.glEnable(GL2.GL_CULL_FACE);
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
        GridMath.initGrid(gl, gridLine, lonStep, latStep);
        gridNeedsInit = false;

        axesLine.init(gl);
        GridMath.initAxes(gl, axesLine);

        earthCircleLine.init(gl);
        GridMath.initEarthCircles(gl, earthCircleLine);
        earthPoint.init(gl);
        GridMath.initEarthPoint(gl, earthPoint);

        radialCircleLine.init(gl);
        radialThickLine.init(gl);
        GridMath.initRadialCircles(gl, radialCircleLine, radialThickLine, RADIAL_UNIT, RADIAL_STEP);
        radialCircleLineFar.init(gl);
        radialThickLineFar.init(gl);
        GridMath.initRadialCircles(gl, radialCircleLineFar, radialThickLineFar, RADIAL_UNIT_FAR, RADIAL_STEP_FAR);

        flatLine.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        gridLine.dispose(gl);
        axesLine.dispose(gl);
        earthCircleLine.dispose(gl);
        earthPoint.dispose(gl);
        radialCircleLine.dispose(gl);
        radialThickLine.dispose(gl);
        radialCircleLineFar.dispose(gl);
        radialThickLineFar.dispose(gl);
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

    public double getLonStep() {
        return lonStep;
    }

    public void setLonStep(double _lonStep) {
        lonStep = _lonStep;
        lonLabels = GridLabel.makeLonLabels(gridType, lonStep);
        gridNeedsInit = true;
    }

    public double getLatStep() {
        return latStep;
    }

    public void setLatStep(double _latStep) {
        latStep = _latStep;
        latLabels = GridLabel.makeLatLabels(latStep);
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
        lonLabels = GridLabel.makeLonLabels(gridType, lonStep);
    }

}
