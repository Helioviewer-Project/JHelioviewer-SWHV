package org.helioviewer.jhv.layers;

import java.util.List;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.GridType;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.display.ViewportMath;
import org.helioviewer.jhv.layers.grid.FlatGrid;
import org.helioviewer.jhv.layers.grid.GridLabel;
import org.helioviewer.jhv.layers.grid.GridMath;
import org.helioviewer.jhv.layers.grid.RadialWarpGrid;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.opengl.GL;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.Transform;
import org.helioviewer.jhv.opengl.text.SdfTextRenderer;

import org.json.JSONObject;

public final class GridLayer extends AbstractLayer {

    private static final double RADIAL_UNIT = Sun.Radius;
    private static final double RADIAL_STEP = 15;
    private static final double RADIAL_UNIT_FAR = Sun.MeanEarthDistance / 10;
    private static final double RADIAL_STEP_FAR = 45;
    private static final float[] R_LABEL_POS = {(float) (2 * RADIAL_UNIT), (float) (8 * RADIAL_UNIT), (float) (24 * RADIAL_UNIT)};
    private static final float[] R_LABEL_POS_FAR = {(float) (2 * RADIAL_UNIT_FAR), (float) (8 * RADIAL_UNIT_FAR), (float) (24 * RADIAL_UNIT_FAR)};
    public static final double GRID_STEP_MIN = 5;
    public static final double GRID_STEP_MAX = 90;
    public static final double GRID_STEP = 0.1;
    public static final double GRID_LINE_SCALE_MIN = 0.5;
    public static final double GRID_LINE_SCALE_MAX = 5;
    public static final double GRID_LABEL_SIZE_MIN = 8;
    public static final double GRID_LABEL_SIZE_MAX = 48;
    public static final double GRID_LABEL_SIZE_REF = 22;

    // height of text in solar radii
    private static final float textScale = GridLabel.textScale;
    private static final double LINEWIDTH = GridMath.LINEWIDTH;
    private static final double LINEWIDTH_THICK = 2 * LINEWIDTH;
    private static final double LINEWIDTH_EARTH = LINEWIDTH;
    private static final double LINEWIDTH_AXES = 2 * LINEWIDTH;
    // private static final double PLANETEXT_Z = 0.01;

    private double lonStep = 15;
    private double latStep = 20;
    private boolean gridNeedsInit = true;

    private boolean showAxis = true;
    private boolean showLabels = true;
    private boolean showRadial = false;

    private Colors.NamedColor gridColor = Colors.NamedColor.DarkGray;
    private double gridAlpha = 1;
    private byte[] gridColorBytes = gridColor.bytes();
    private double labelAlpha = 1;
    private double gridLineScale = 1;
    private double gridLabelSize = GRID_LABEL_SIZE_REF;
    private double gridLabelAngle = 0;

    private final GLSLShape earthPoint = new GLSLShape(false);
    private final GLSLLine axesLine = new GLSLLine(false);
    private final GLSLLine earthCircleLine = new GLSLLine(false);
    private final GLSLLine radialCircleLine = new GLSLLine(false);
    private final GLSLLine radialThickLine = new GLSLLine(false);
    private final GLSLLine radialCircleLineFar = new GLSLLine(false);
    private final GLSLLine radialThickLineFar = new GLSLLine(false);
    private final FlatGrid flatGrid = new FlatGrid();
    private final RadialWarpGrid radialWarpGrid = new RadialWarpGrid();
    private final GLSLLine gridLine = new GLSLLine(false);

    private List<GridLabel> latLabels;
    private List<GridLabel.TransformedGridLabel> lonLabels;
    private final List<GridLabel> radialLabels;
    private final List<GridLabel> radialLabelsFar;

    @Override
    public void serialize(JSONObject jo) {
        jo.put("lonStep", lonStep);
        jo.put("latStep", latStep);
        jo.put("showAxis", showAxis);
        jo.put("showLabels", showLabels);
        jo.put("showRadial", showRadial);
        jo.put("type", Display.gridType);
        jo.put("color", gridColor.name());
        jo.put("alpha", gridAlpha);
        jo.put("labelAlpha", labelAlpha);
        jo.put("lineScale", gridLineScale);
        jo.put("labelSize", gridLabelSize);
        jo.put("labelAngle", gridLabelAngle);
    }

    private void deserialize(JSONObject jo) {
        lonStep = Math.clamp(jo.optDouble("lonStep", lonStep), GRID_STEP_MIN, GRID_STEP_MAX);
        latStep = Math.clamp(jo.optDouble("latStep", latStep), GRID_STEP_MIN, GRID_STEP_MAX);

        showAxis = jo.optBoolean("showAxis", showAxis);
        showLabels = jo.optBoolean("showLabels", showLabels);
        showRadial = jo.optBoolean("showRadial", showRadial);
        gridColor = Colors.NamedColor.parse(jo.optString("color", gridColor.name()), gridColor);
        gridAlpha = Math.clamp(jo.optDouble("alpha", gridAlpha), 0, 1);
        updateGridColorBytes();
        labelAlpha = Math.clamp(jo.optDouble("labelAlpha", labelAlpha), 0, 1);
        gridLineScale = Math.clamp(jo.optDouble("lineScale", gridLineScale), GRID_LINE_SCALE_MIN, GRID_LINE_SCALE_MAX);
        gridLabelSize = Math.clamp(jo.optDouble("labelSize", gridLabelSize), GRID_LABEL_SIZE_MIN, GRID_LABEL_SIZE_MAX);
        gridLabelAngle = jo.optDouble("labelAngle", gridLabelAngle);

        String strGridType = jo.optString("type", Display.gridType.toString());
        try {
            Display.setGridType(GridType.valueOf(strGridType));
        } catch (Exception ignore) {}
    }

    public GridLayer(JSONObject jo) {
        if (jo != null)
            deserialize(jo);
        else
            setEnabled(true);

        latLabels = GridLabel.makeLatLabels(latStep);
        lonLabels = GridLabel.makeLonLabels(Display.gridType, lonStep);
        radialLabels = GridLabel.makeRadialLabels(0, RADIAL_STEP);
        radialLabelsFar = GridLabel.makeRadialLabels(Math.PI / 2, RADIAL_STEP_FAR);
    }

    @Override
    public void render(MapView mv, Viewport vp) {
        if (!isVisible[vp.idx])
            return;
        if (gridNeedsInit) {
            GridMath.initGrid(gridLine, lonStep, latStep, gridColorBytes);
            gridNeedsInit = false;
        }

        Position viewpoint = mv.viewpoint();
        float ztext = 0;
        double pixFactor = ViewportMath.getPixelFactor(vp, mv.cameraWidth(vp));

        // correct order: grid lines -> Earth indicators -> axis -> grid labels -> radial grid
        Quat gridQuat = mv.gridType().toCarrington(viewpoint);

        Transform.pushView();
        Transform.rotateViewInverse(gridQuat);
        gridLine.renderLine(vp, LINEWIDTH * gridLineScale);
        Transform.popView();

        drawEarthCircles(vp, pixFactor, Sun.getEarth(viewpoint.time));

        if (showAxis)
            axesLine.renderLine(vp, LINEWIDTH_AXES);

        if (showLabels) {
            Transform.pushView();
            Transform.rotateViewInverse(gridQuat);
            drawGridText(ztext);
            Transform.popView();
        }

        if (showRadial) {
            Transform.pushView();
            Transform.rotateViewInverse(viewpoint.toQuat());
            {
                if (viewpoint.distance > 100 * Sun.MeanEarthDistance) {
                    radialCircleLineFar.renderLine(vp, LINEWIDTH);
                    radialThickLineFar.renderLine(vp, LINEWIDTH_THICK);
                    if (showLabels)
                        drawRadialGridText(radialLabelsFar, ztext, R_LABEL_POS_FAR, Colors.fade(Colors.MiddleGrayFloat, labelAlpha));
                } else {
                    radialCircleLine.renderLine(vp, LINEWIDTH);
                    radialThickLine.renderLine(vp, LINEWIDTH_THICK);
                    if (showLabels)
                        drawRadialGridText(radialLabels, ztext, R_LABEL_POS, Colors.fade(Colors.MiddleGrayFloat, labelAlpha));
                }
            }
            Transform.popView();
        }
    }

    @Override
    public void renderScale(MapView mv, Viewport vp) {
        if (!isVisible[vp.idx])
            return;
        if (mv.isRadialWarp())
            radialWarpGrid.render(mv, vp, showLabels, lonStep, gridColorBytes, gridLineScale, Colors.fade(Colors.WhiteFloat, labelAlpha), gridLabelSize, gridLabelAngle);
        else
            flatGrid.render(mv, vp, showLabels, gridColorBytes, gridLineScale, Colors.fade(Colors.WhiteFloat, labelAlpha), gridLabelSize);
    }

    private void drawEarthCircles(Viewport vp, double factor, Position p) {
        Transform.pushView();
        Transform.rotateViewInverse(p.toQuat());

        earthCircleLine.renderLine(vp, LINEWIDTH_EARTH);
        earthPoint.renderPoints(factor);

        Transform.popView();
    }

    private void drawRadialGridText(List<GridLabel> labels, float z, float[] labelPos, float[] color) {
        SdfTextRenderer renderer = GLText.renderer();
        renderer.setColor(color);
        float textScaleFactor = (float) (textScale * gridLabelSize / GRID_LABEL_SIZE_REF / renderer.getFontSize());
        float fuzz = 0.75f;

        GL.glDisable(GL.CULL_FACE);
        renderer.begin3DRendering();
        for (float rsize : labelPos) {
            labels.forEach(label -> renderer.draw(label.txt, rsize * label.x, rsize * label.y, z, fuzz * rsize * textScaleFactor));
        }
        renderer.end3DRendering();
        GL.glEnable(GL.CULL_FACE);
    }

    private void drawGridText(float z) {
        SdfTextRenderer renderer = GLText.renderer();
        renderer.setColor(Colors.fade(Colors.WhiteFloat, labelAlpha));
        // the scale factor has to be divided by the current font size
        float textScaleFactor = (float) (textScale * gridLabelSize / GRID_LABEL_SIZE_REF / renderer.getFontSize());

        renderer.begin3DRendering();

        // need flushes for state toggle
        lonLabels.forEach(lonLabel -> renderer.draw(lonLabel.txt, lonLabel.origin, lonLabel.basisX, lonLabel.basisY, textScaleFactor));
        renderer.flush();

        GL.glDisable(GL.CULL_FACE);
        latLabels.forEach(label -> renderer.draw(label.txt, label.x, label.y, z, textScaleFactor));
        renderer.flush();
        GL.glEnable(GL.CULL_FACE);

        renderer.end3DRendering();
    }

    @Override
    public void init() {
        gridLine.init();
        GridMath.initGrid(gridLine, lonStep, latStep, gridColorBytes);
        gridNeedsInit = false;

        axesLine.init();
        GridMath.initAxes(axesLine);

        earthCircleLine.init();
        GridMath.initEarthCircles(earthCircleLine);
        earthPoint.init();
        GridMath.initEarthPoint(earthPoint);

        radialCircleLine.init();
        radialThickLine.init();
        GridMath.initRadialCircles(radialCircleLine, radialThickLine, RADIAL_UNIT, RADIAL_STEP);
        radialCircleLineFar.init();
        radialThickLineFar.init();
        GridMath.initRadialCircles(radialCircleLineFar, radialThickLineFar, RADIAL_UNIT_FAR, RADIAL_STEP_FAR);

        flatGrid.init();
        radialWarpGrid.init();
    }

    @Override
    public void dispose() {
        gridLine.dispose();
        axesLine.dispose();
        earthCircleLine.dispose();
        earthPoint.dispose();
        radialCircleLine.dispose();
        radialThickLine.dispose();
        radialCircleLineFar.dispose();
        radialThickLineFar.dispose();
        flatGrid.dispose();
        radialWarpGrid.dispose();
    }

    @Override
    public void remove() {
        dispose();
    }

    @Override
    public String getName() {
        return "Grid";
    }

    public boolean isShowAxis() {
        return showAxis;
    }

    public void setShowAxis(boolean _showAxis) {
        showAxis = _showAxis;
        DisplayController.display();
    }

    public boolean isShowLabels() {
        return showLabels;
    }

    public void setShowLabels(boolean _showLabels) {
        showLabels = _showLabels;
        DisplayController.display();
    }

    public boolean isShowRadial() {
        return showRadial;
    }

    public void setShowRadial(boolean _showRadial) {
        showRadial = _showRadial;
        DisplayController.display();
    }

    public double getLonStep() {
        return lonStep;
    }

    public void setLonStep(double _lonStep) {
        lonStep = _lonStep;
        lonLabels = GridLabel.makeLonLabels(Display.gridType, lonStep);
        gridNeedsInit = true;
        DisplayController.display();
    }

    public double getLatStep() {
        return latStep;
    }

    public void setLatStep(double _latStep) {
        latStep = _latStep;
        latLabels = GridLabel.makeLatLabels(latStep);
        gridNeedsInit = true;
        DisplayController.display();
    }

    public void setGridType(GridType gridType) {
        Display.setGridType(gridType);
        lonLabels = GridLabel.makeLonLabels(gridType, lonStep);
        DisplayController.display();
    }

    public Colors.NamedColor getGridColor() {
        return gridColor;
    }

    public void setGridColor(Colors.NamedColor _gridColor) {
        gridColor = _gridColor;
        updateGridColorBytes();
        gridNeedsInit = true;
        DisplayController.display();
    }

    public double getGridAlpha() {
        return gridAlpha;
    }

    public void setGridAlpha(double _gridAlpha) {
        gridAlpha = Math.clamp(_gridAlpha, 0, 1);
        updateGridColorBytes();
        gridNeedsInit = true;
        DisplayController.display();
    }

    public double getLabelAlpha() {
        return labelAlpha;
    }

    public void setLabelAlpha(double _labelAlpha) {
        labelAlpha = Math.clamp(_labelAlpha, 0, 1);
        DisplayController.display();
    }

    public double getGridLineScale() {
        return gridLineScale;
    }

    public void setGridLineScale(double _gridLineScale) {
        gridLineScale = Math.clamp(_gridLineScale, GRID_LINE_SCALE_MIN, GRID_LINE_SCALE_MAX);
        DisplayController.display();
    }

    public double getGridLabelSize() {
        return gridLabelSize;
    }

    public void setGridLabelSize(double _gridLabelSize) {
        gridLabelSize = Math.clamp(_gridLabelSize, GRID_LABEL_SIZE_MIN, GRID_LABEL_SIZE_MAX);
        DisplayController.display();
    }

    public double getGridLabelAngle() {
        return gridLabelAngle;
    }

    public void setGridLabelAngle(double _gridLabelAngle) {
        gridLabelAngle = _gridLabelAngle;
        DisplayController.display();
    }

    private void updateGridColorBytes() {
        gridColorBytes = gridAlpha == 1 ? gridColor.bytes() : Colors.bytes(gridColor.awtColor(), gridAlpha);
    }

}
