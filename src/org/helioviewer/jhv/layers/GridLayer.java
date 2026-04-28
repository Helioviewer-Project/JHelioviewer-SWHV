package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.camera.Transform;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.GridType;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.components.base.JHVSpinner;
import org.helioviewer.jhv.gui.components.base.TerminatedFormatterFactory;
import org.helioviewer.jhv.layers.grid.FlatGrid;
import org.helioviewer.jhv.layers.grid.GridLabel;
import org.helioviewer.jhv.layers.grid.GridMath;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.opengl.GL;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.text.TextRenderer;

import org.json.JSONObject;

public final class GridLayer extends AbstractLayer {

    private static final double RADIAL_UNIT = Sun.Radius;
    private static final double RADIAL_STEP = 15;
    private static final double RADIAL_UNIT_FAR = Sun.MeanEarthDistance / 10;
    private static final double RADIAL_STEP_FAR = 45;
    private static final float[] R_LABEL_POS = {(float) (2 * RADIAL_UNIT), (float) (8 * RADIAL_UNIT), (float) (24 * RADIAL_UNIT)};
    private static final float[] R_LABEL_POS_FAR = {(float) (2 * RADIAL_UNIT_FAR), (float) (8 * RADIAL_UNIT_FAR), (float) (24 * RADIAL_UNIT_FAR)};
    private static final double GRID_STEP_MIN = 5;
    private static final double GRID_STEP_MAX = 90;
    private static final double GRID_STEP = 0.1;

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

    private final GLSLShape earthPoint = new GLSLShape(false);
    private final GLSLLine axesLine = new GLSLLine(false);
    private final GLSLLine earthCircleLine = new GLSLLine(false);
    private final GLSLLine radialCircleLine = new GLSLLine(false);
    private final GLSLLine radialThickLine = new GLSLLine(false);
    private final GLSLLine radialCircleLineFar = new GLSLLine(false);
    private final GLSLLine radialThickLineFar = new GLSLLine(false);
    private final FlatGrid flatGrid = new FlatGrid();
    private final GLSLLine gridLine = new GLSLLine(false);

    private List<GridLabel> latLabels;
    private List<GridLabel.TransformedGridLabel> lonLabels;
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
        jo.put("type", Display.gridType);
    }

    private void deserialize(JSONObject jo) {
        lonStep = Math.clamp(jo.optDouble("lonStep", lonStep), GRID_STEP_MIN, GRID_STEP_MAX);
        latStep = Math.clamp(jo.optDouble("latStep", latStep), GRID_STEP_MIN, GRID_STEP_MAX);

        showAxis = jo.optBoolean("showAxis", showAxis);
        showLabels = jo.optBoolean("showLabels", showLabels);
        showRadial = jo.optBoolean("showRadial", showRadial);

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
        optionsPanel = new GridLayerOptions();

        latLabels = GridLabel.makeLatLabels(latStep);
        lonLabels = GridLabel.makeLonLabels(Display.gridType, lonStep);
        radialLabels = GridLabel.makeRadialLabels(0, RADIAL_STEP);
        radialLabelsFar = GridLabel.makeRadialLabels(Math.PI / 2, RADIAL_STEP_FAR);
    }

    @Override
    public void render(Camera camera, Viewport vp) {
        if (!isVisible[vp.idx])
            return;
        if (gridNeedsInit) {
            GridMath.initGrid(gridLine, lonStep, latStep);
            gridNeedsInit = false;
        }

        Position viewpoint = camera.getViewpoint();
        float ztext = 0; //(float) (camera.getWidth() * PLANETEXT_Z);
        double pixFactor = CameraHelper.getPixelFactor(camera, vp);
        double logicalPixelsPerSolarRadius = textScale * pixFactor / Display.pixelScale[1];

        // correct order: grid lines -> Earth indicators -> axis -> grid labels -> radial grid
        Quat gridQuat = Display.gridType.toCarrington(viewpoint);

        Transform.pushView();
        Transform.rotateViewInverse(gridQuat);
        gridLine.renderLine(vp, LINEWIDTH);
        Transform.popView();

        drawEarthCircles(vp, pixFactor, Sun.getEarth(viewpoint.time));

        if (showAxis)
            axesLine.renderLine(vp, LINEWIDTH_AXES);

        if (showLabels) {
            Transform.pushView();
            Transform.rotateViewInverse(gridQuat);
            drawGridText((int) logicalPixelsPerSolarRadius, ztext);
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
                        drawRadialGridText(radialLabelsFar, logicalPixelsPerSolarRadius * RADIAL_UNIT_FAR, ztext, R_LABEL_POS_FAR);
                } else {
                    radialCircleLine.renderLine(vp, LINEWIDTH);
                    radialThickLine.renderLine(vp, LINEWIDTH_THICK);
                    if (showLabels)
                        drawRadialGridText(radialLabels, logicalPixelsPerSolarRadius * RADIAL_UNIT, ztext, R_LABEL_POS);
                }
            }
            Transform.popView();
        }
    }

    @Override
    public void renderScale(Camera camera, Viewport vp) {
        if (!isVisible[vp.idx])
            return;
        flatGrid.render(camera, vp, showLabels);
    }

    private void drawEarthCircles(Viewport vp, double factor, Position p) {
        Transform.pushView();
        Transform.rotateViewInverse(p.toQuat());

        earthCircleLine.renderLine(vp, LINEWIDTH_EARTH);
        earthPoint.renderPoints(factor);

        Transform.popView();
    }

    private static void drawRadialGridText(List<GridLabel> labels, double size, float z, float[] labelPos) {
        float fuzz = 0.75f;
        GL.glDisable(GL.CULL_FACE);
        for (float rsize : labelPos) {
            TextRenderer renderer = GLText.getRenderer((int) (fuzz * rsize * size));
            renderer.setColor(Colors.MiddleGrayFloat);
            float textScaleFactor = textScale / renderer.getFontSize();

            renderer.begin3DRendering();
            labels.forEach(label -> renderer.draw(label.txt, rsize * label.x, rsize * label.y, z, fuzz * rsize * textScaleFactor));
            renderer.end3DRendering();
        }
        GL.glEnable(GL.CULL_FACE);
    }

    private void drawGridText(int size, float z) {
        TextRenderer renderer = GLText.getRenderer(size);
        renderer.setColor(Colors.WhiteFloat);
        // the scale factor has to be divided by the current font size
        float textScaleFactor = textScale / renderer.getFontSize();

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
        GridMath.initGrid(gridLine, lonStep, latStep);
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
    }

    @Override
    public void remove() {
        dispose();
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public String getName() {
        return "Grid";
    }

    @SuppressWarnings("serial")
    private class GridLayerOptions extends JPanel {

        GridLayerOptions() {
            setLayout(new GridBagLayout());

            GridBagConstraints c0 = new GridBagConstraints();
            c0.fill = GridBagConstraints.HORIZONTAL;
            c0.weightx = 1.;
            c0.weighty = 1.;

            c0.gridy = 0;

            c0.gridx = 1;
            c0.anchor = GridBagConstraints.LINE_END;
            add(createToggle("Solar axis", showAxis, selected -> showAxis = selected), c0);

            c0.gridx = 3;
            c0.anchor = GridBagConstraints.LINE_END;
            add(createToggle("Grid labels", showLabels, selected -> showLabels = selected), c0);

            c0.gridy = 1;

            c0.gridx = 1;
            c0.anchor = GridBagConstraints.LINE_END;
            add(createToggle("Radial grid", showRadial, selected -> showRadial = selected), c0);

            c0.gridx = 2;
            c0.anchor = GridBagConstraints.LINE_END;
            add(new JLabel("Grid type ", JLabel.RIGHT), c0);
            c0.gridx = 3;
            c0.anchor = GridBagConstraints.LINE_START;
            add(createGridTypeBox(), c0);

            c0.gridy = 2;

            c0.gridx = 0;
            c0.anchor = GridBagConstraints.LINE_END;
            add(new JLabel("Longitude ", JLabel.RIGHT), c0);

            c0.gridx = 1;
            c0.anchor = GridBagConstraints.LINE_START;
            add(createGridResolutionSpinner(lonStep, value -> lonStep = value, () -> lonLabels = GridLabel.makeLonLabels(Display.gridType, lonStep)), c0);

            c0.gridx = 2;
            c0.anchor = GridBagConstraints.LINE_END;
            add(new JLabel("Latitude ", JLabel.RIGHT), c0);

            c0.gridx = 3;
            c0.anchor = GridBagConstraints.LINE_START;
            add(createGridResolutionSpinner(latStep, value -> latStep = value, () -> latLabels = GridLabel.makeLatLabels(latStep)), c0);
        }

        private JCheckBox createToggle(String text, boolean initialValue, Consumer<Boolean> onChange) {
            JCheckBox checkBox = new JCheckBox(text, initialValue);
            checkBox.setHorizontalTextPosition(SwingConstants.LEFT);
            checkBox.addActionListener(e -> {
                onChange.accept(checkBox.isSelected());
                MovieDisplay.display();
            });
            return checkBox;
        }

        private JComboBox<GridType> createGridTypeBox() {
            JComboBox<GridType> comboBox = new JComboBox<>(GridType.values());
            comboBox.setSelectedItem(Display.gridType);
            comboBox.addActionListener(e -> {
                GridType gridType = (GridType) Objects.requireNonNull(comboBox.getSelectedItem());
                Display.setGridType(gridType);
                lonLabels = GridLabel.makeLonLabels(gridType, lonStep);
                MovieDisplay.display();
            });
            return comboBox;
        }

        private JHVSpinner createGridResolutionSpinner(double initialValue, DoubleConsumer valueSetter, Runnable onChange) {
            JHVSpinner spinner = new JHVSpinner(initialValue, GRID_STEP_MIN, GRID_STEP_MAX, GRID_STEP);
            spinner.addChangeListener(e -> {
                valueSetter.accept((Double) spinner.getValue());
                onChange.run();
                gridNeedsInit = true;
                MovieDisplay.display();
            });
            JFormattedTextField textField = ((JHVSpinner.DefaultEditor) spinner.getEditor()).getTextField();
            textField.setFormatterFactory(new TerminatedFormatterFactory("%.1f", "\u00B0", GRID_STEP_MIN, GRID_STEP_MAX));
            return spinner;
        }

    }

}
