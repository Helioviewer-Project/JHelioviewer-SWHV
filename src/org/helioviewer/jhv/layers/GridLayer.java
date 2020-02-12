package org.helioviewer.jhv.layers;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.base.scale.GridType;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.text.JhvTextRenderer;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class GridLayer extends AbstractLayer {

    private static boolean odeIsDone = false;

    private static final double RADIAL_UNIT = Sun.Radius;
    private static final double RADIAL_STEP = 15;
    private static final double RADIAL_UNIT_FAR = Sun.MeanEarthDistance / 10;
    private static final double RADIAL_STEP_FAR = 45;
    private static final float[] R_LABEL_POS = {(float) (2 * RADIAL_UNIT), (float) (8 * RADIAL_UNIT), (float) (24 * RADIAL_UNIT)};
    private static final float[] R_LABEL_POS_FAR = {(float) (2 * RADIAL_UNIT_FAR), (float) (8 * RADIAL_UNIT_FAR), (float) (24 * RADIAL_UNIT_FAR)};

    // height of text in solar radii
    private static final float textScale = GridLabel.textScale;
    static final double LINEWIDTH = GLSLLine.LINEWIDTH_BASIC;
    private static final double LINEWIDTH_THICK = 2 * LINEWIDTH;
    private static final double LINEWIDTH_EARTH = LINEWIDTH;
    private static final double LINEWIDTH_AXES = 2 * LINEWIDTH;

    private static final double PLANETEXT_Z = 0.01;

    private static final DecimalFormat formatter2 = MathUtils.numberFormatter("0", 2);

    private GridType gridType = GridType.Viewpoint;

    private double lonStep = 15;
    private double latStep = 20;
    private boolean gridNeedsInit = true;

    private boolean showAxis = true;
    private boolean showLabels = true;
    private boolean showRadial = false;

    private final GLSLShape earthPoint = new GLSLShape(false);
    private final GLSLLine axesLine = new GLSLLine(false);
    private final GLSLLine earthCircleLine = new GLSLLine(false);
    private final GLSLLine radialCircleLine = new GLSLLine(false);
    private final GLSLLine radialThickLine = new GLSLLine(false);
    private final GLSLLine radialCircleLineFar = new GLSLLine(false);
    private final GLSLLine radialThickLineFar = new GLSLLine(false);
    private final GLSLLine flatLine = new GLSLLine(false);
    private final GLSLLine gridLine = new GLSLLine(false);

    private List<GridLabel> latLabels;
    private List<GridLabel> lonLabels;
    private final List<GridLabel> radialLabels;
    private final List<GridLabel> radialLabelsFar;

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

    @Nonnull
    public Vec2 gridPoint(Camera camera, Viewport vp, int x, int y) {
        return Display.mode.scale.mouseToGrid(x, y, vp, camera, gridType);
    }

    public double gridLatitude(Position metaViewpoint) {
        return gridType.toLatitude(metaViewpoint);
    }

    public double gridLongitude(Position cameraViewpoint, Position metaViewpoint) {
        double gridLon = gridType.toLongitude(metaViewpoint);
        return gridType == GridType.Viewpoint ? gridLon - cameraViewpoint.lon : metaViewpoint.lon - gridLon;
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;
        if (gridNeedsInit) {
            GridMath.initGrid(gl, gridLine, lonStep, latStep);
            gridNeedsInit = false;
        }
        if (!odeIsDone)
            return;

        if (showAxis)
            axesLine.render(gl, vp.aspect, LINEWIDTH_AXES);

        Position viewpoint = camera.getViewpoint();
        float ztext = 0; //(float) (camera.getWidth() * PLANETEXT_Z);
        double pixFactor = CameraHelper.getPixelFactor(camera, vp);
        drawEarthCircles(gl, vp, pixFactor, Sun.getEarth(viewpoint.time));

        double pixelsPerSolarRadius = textScale * pixFactor;

        Transform.pushView();
        Transform.rotateViewInverse(gridType.toQuat(viewpoint));
        {
            gridLine.render(gl, vp.aspect, LINEWIDTH);
            if (showLabels) {
                drawGridText(gl, (int) pixelsPerSolarRadius, ztext);
            }
        }
        Transform.popView();

        if (showRadial) {
            Transform.pushView();
            Transform.rotateViewInverse(viewpoint.toQuat());
            {
                boolean far = Camera.useWideProjection(viewpoint.distance);
                if (far) {
                    Transform.pushProjection();
                    camera.projectionOrthoWide(vp.aspect);
                    radialCircleLineFar.render(gl, vp.aspect, LINEWIDTH);
                    radialThickLineFar.render(gl, vp.aspect, LINEWIDTH_THICK);
                    if (showLabels)
                        drawRadialGridText(gl, radialLabelsFar, pixelsPerSolarRadius * RADIAL_UNIT_FAR, ztext, R_LABEL_POS_FAR);
                    Transform.popProjection();
                } else {
                    radialCircleLine.render(gl, vp.aspect, LINEWIDTH);
                    radialThickLine.render(gl, vp.aspect, LINEWIDTH_THICK);
                    if (showLabels)
                        drawRadialGridText(gl, radialLabels, pixelsPerSolarRadius * RADIAL_UNIT, ztext, R_LABEL_POS);
                }
            }
            Transform.popView();
        }
    }

    @Override
    public void renderScale(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;
        int pixelsPerSolarRadius = (int) (textScale * CameraHelper.getPixelFactor(camera, vp));
        drawGridFlat(gl, vp);
        if (showLabels) {
            drawGridTextFlat(pixelsPerSolarRadius, Display.mode.scale, vp);
        }
    }

    private double previousAspect = -1;

    private void drawGridFlat(GL2 gl, Viewport vp) {
        if (previousAspect != vp.aspect) {
            GridMath.initFlatGrid(gl, flatLine, vp.aspect);
            previousAspect = vp.aspect;
        }
        flatLine.render(gl, vp.aspect, LINEWIDTH);
    }

    private static void drawGridTextFlat(int size, GridScale scale, Viewport vp) {
        float w = (float) vp.aspect;
        float h = 1;
        JhvTextRenderer renderer = GLText.getRenderer(size);
        float textScaleFactor = textScale / renderer.getFont().getSize2D() * w / GridMath.FLAT_STEPS_THETA * 5;

        renderer.begin3DRendering();
        {
            for (int i = 0; i <= GridMath.FLAT_STEPS_THETA; i++) {
                if (i == GridMath.FLAT_STEPS_THETA / 2) {
                    continue;
                }
                double lon = MathUtils.mapToMinus180To180(scale.getInterpolatedXValue(1. / GridMath.FLAT_STEPS_THETA * i) + 180);
                String txt = formatter2.format(lon);
                float x = i / (float) GridMath.FLAT_STEPS_THETA - 0.5f;
                renderer.draw3D(txt, w * x, 0, 0, textScaleFactor);
            }
            for (int i = 0; i <= GridMath.FLAT_STEPS_RADIAL; i++) {
                String txt = formatter2.format(scale.getInterpolatedYValue(1. / GridMath.FLAT_STEPS_RADIAL * i));
                float y = i / (float) GridMath.FLAT_STEPS_RADIAL - 0.5f;
                renderer.draw3D(txt, 0, h * y, 0, textScaleFactor);
            }
        }
        renderer.end3DRendering();
    }

    private void drawEarthCircles(GL2 gl, Viewport vp, double factor, Position p) {
        Transform.pushView();
        Transform.rotateViewInverse(p.toQuat());

        earthCircleLine.render(gl, vp.aspect, LINEWIDTH_EARTH);
        earthPoint.renderPoints(gl, factor);

        Transform.popView();
    }

    private static void drawRadialGridText(GL2 gl, List<GridLabel> labels, double size, float z, float[] labelPos) {
        float fuzz = 0.75f;
        gl.glDisable(GL2.GL_CULL_FACE);
        for (float rsize : labelPos) {
            JhvTextRenderer renderer = GLText.getRenderer((int) (fuzz * rsize * size));
            float textScaleFactor = textScale / renderer.getFont().getSize2D();

            renderer.begin3DRendering();
            labels.forEach(label -> renderer.draw3D(label.txt, rsize * label.x, rsize * label.y, z, fuzz * rsize * textScaleFactor));
            renderer.end3DRendering();
        }
        gl.glEnable(GL2.GL_CULL_FACE);
    }

    private void drawGridText(GL2 gl, int size, float z) {
        JhvTextRenderer renderer = GLText.getRenderer(size);
        // the scale factor has to be divided by the current font size
        float textScaleFactor = textScale / renderer.getFont().getSize2D();

        renderer.begin3DRendering();

        gl.glDisable(GL2.GL_CULL_FACE);
        latLabels.forEach(label -> renderer.draw3D(label.txt, label.x, label.y, z, textScaleFactor));
        renderer.flush();
        gl.glEnable(GL2.GL_CULL_FACE);

        lonLabels.forEach(lonLabel -> {
            Transform.pushView();

            Transform.mulView(lonLabel.m);
            renderer.draw3D(lonLabel.txt, 0, 0, 0, textScaleFactor);
            renderer.flush();

            Transform.popView();
        });
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

    @Nullable
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

    private static int shift = 0;
    private static final int shift_step = 2;

    @Override
    public void renderFloat(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;
        if (odeIsDone)
            return;

        int delta = (int) (vp.width / 4.);
        int size = (int) (vp.height * 0.015);
        shift += shift_step;

        if (GLInfo.pixelScale[1] == 1) //! nasty
            size *= 2;

        int y = 0;

        JhvTextRenderer renderer = GLText.getRenderer(size);
        renderer.beginRendering(vp.width, vp.height);
        for (int i = 0; i < ode.length; i++) {
            String text = ode[i];
            y = shift - 3 * i * size;

            renderer.setColor(GLText.shadowColor);
            renderer.draw(text, delta + GLText.shadowOffset[0], y + GLText.shadowOffset[1]);
            renderer.setColor(colors[(i + (shift / shift_step) / 6) % 6]);
            renderer.draw(text, delta, y);
        }
        renderer.setColor(Colors.WhiteFloat);
        renderer.endRendering();

        try {
            Thread.sleep(30);
        } catch (Exception ignore) {
        }

        if (y > vp.height + 100) {
            odeIsDone = true;
        }

        MovieDisplay.display();
    }

    private static final float[][] colors = new float[][]{
            Colors.floats(new Color(255, 235, 0), 1),
            Colors.floats(new Color(252, 0, 25), 1),
            Colors.floats(new Color(1, 255, 79), 1),
            Colors.floats(new Color(255, 1, 215), 1),
            Colors.floats(new Color(86, 0, 204), 1),
            Colors.floats(new Color(0, 237, 245), 1)
    };

    private static final String[] ode = {
            "Ode to Orbiter 2020",
            " ",
            "It's been a journey getting here -",
            "now you're about to disappear!",
            "Sent out to explore our dear star,",
            "while we watch proudly from afar.",
            " ",
            "Go carry all our greatest hopes,",
            "untangling twisted flux ropes,",
            "reach up to hidden stellar poles,",
            "and peer in those coronal holes.",
            " ",
            "Call up some divine providence",
            "for each erupting prominence,",
            "listening without apology,",
            "to helioseismology.",
            " ",
            "Watch every blazing plasma arc,",
            "found framed against the deepest dark,",
            "feeling each spinning Alfvén wave,",
            "amidst the storms where you stand brave.",
            " ",
            "Shed light on solar mysteries:",
            "coronal heating if you please!",
            "And what lies secret unawares -",
            "some global hidden nanoflares?",
            " ",
            "Gather data for articles,",
            "on energetic particles,",
            "and try to give us just a trace",
            "of how they then fill all of space.",
            " ",
            "Some of us have to say goodbye,",
            "waving you off into the sky,",
            "others know work has just begun,",
            "on fathoming our glorious Sun!",
            " ",
            "Congratulations Team Solar Orbiter!",
            " ",
            "Copyright © MMXX Max"
    };

}
