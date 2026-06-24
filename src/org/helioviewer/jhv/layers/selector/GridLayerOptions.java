package org.helioviewer.jhv.layers.selector;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.GridType;
import org.helioviewer.jhv.gui.component.JHVSlider;
import org.helioviewer.jhv.gui.component.JHVSpinner;
import org.helioviewer.jhv.gui.component.TerminatedFormatterFactory;
import org.helioviewer.jhv.layers.GridLayer;

@SuppressWarnings("serial")
final class GridLayerOptions extends JPanel {

    // The disk projection draws a radial ring/spoke grid; these lat/lon-graticule controls have
    // no meaning there (Longitude maps to the spoke spacing, so it stays enabled), so they are
    // greyed out while a disk projection is active.
    private final JComponent[] nonDiskControls;
    private final JComponent[] diskOnlyControls;
    private final ViewState.ModeListener modeListener = this::applyProjectionEnablement;

    GridLayerOptions(GridLayer layer) {
        setLayout(new GridBagLayout());

        GridBagConstraints c0 = new GridBagConstraints();
        c0.fill = GridBagConstraints.HORIZONTAL;
        c0.weightx = 1.;
        c0.weighty = 1.;

        c0.gridy = 0;

        c0.gridx = 1;
        c0.anchor = GridBagConstraints.LINE_END;
        JCheckBox axisToggle = createToggle("Solar axis", layer.isShowAxis(), layer::setShowAxis);
        add(axisToggle, c0);

        c0.gridx = 3;
        c0.anchor = GridBagConstraints.LINE_END;
        add(createToggle("Grid labels", layer.isShowLabels(), layer::setShowLabels), c0);

        c0.gridy = 1;

        c0.gridx = 1;
        c0.anchor = GridBagConstraints.LINE_END;
        JCheckBox radialToggle = createToggle("Radial grid", layer.isShowRadial(), layer::setShowRadial);
        add(radialToggle, c0);

        c0.gridx = 2;
        c0.anchor = GridBagConstraints.LINE_END;
        add(new JLabel("Grid type ", JLabel.RIGHT), c0);
        c0.gridx = 3;
        c0.anchor = GridBagConstraints.LINE_START;
        JComboBox<GridType> gridTypeBox = createGridTypeBox(layer);
        add(gridTypeBox, c0);

        c0.gridy = 2;

        c0.gridx = 0;
        c0.anchor = GridBagConstraints.LINE_END;
        add(new JLabel("Longitude ", JLabel.RIGHT), c0);

        c0.gridx = 1;
        c0.anchor = GridBagConstraints.LINE_START;
        add(createGridResolutionSpinner(layer.getLonStep(), layer::setLonStep), c0);

        c0.gridx = 2;
        c0.anchor = GridBagConstraints.LINE_END;
        add(new JLabel("Latitude ", JLabel.RIGHT), c0);

        c0.gridx = 3;
        c0.anchor = GridBagConstraints.LINE_START;
        JHVSpinner latSpinner = createGridResolutionSpinner(layer.getLatStep(), layer::setLatStep);
        add(latSpinner, c0);

        c0.gridy = 3;

        c0.gridx = 0;
        c0.anchor = GridBagConstraints.LINE_END;
        add(new JLabel("Color ", JLabel.RIGHT), c0);
        c0.gridx = 1;
        c0.anchor = GridBagConstraints.LINE_START;
        add(createColorButton(layer), c0);

        c0.gridx = 2;
        c0.anchor = GridBagConstraints.LINE_END;
        add(new JLabel("Line opacity ", JLabel.RIGHT), c0);
        c0.gridx = 3;
        c0.anchor = GridBagConstraints.LINE_START;
        add(createOpacitySlider("Grid line opacity", layer.getGridAlpha(), layer::setGridAlpha), c0);

        c0.gridy = 4;

        c0.gridx = 0;
        c0.anchor = GridBagConstraints.LINE_END;
        add(new JLabel("Line width ", JLabel.RIGHT), c0);
        c0.gridx = 1;
        c0.anchor = GridBagConstraints.LINE_START;
        add(createLineWidthSlider(layer), c0);

        c0.gridx = 2;
        c0.anchor = GridBagConstraints.LINE_END;
        add(new JLabel("Label opacity ", JLabel.RIGHT), c0);
        c0.gridx = 3;
        c0.anchor = GridBagConstraints.LINE_START;
        add(createOpacitySlider("Grid label opacity", layer.getLabelAlpha(), layer::setLabelAlpha), c0);

        c0.gridy = 5;

        c0.gridx = 0;
        c0.anchor = GridBagConstraints.LINE_END;
        add(new JLabel("Label size ", JLabel.RIGHT), c0);
        c0.gridx = 1;
        c0.anchor = GridBagConstraints.LINE_START;
        JHVSlider labelSizeSlider = createLabelSizeSlider(layer);
        add(labelSizeSlider, c0);

        c0.gridx = 2;
        c0.anchor = GridBagConstraints.LINE_END;
        add(new JLabel("Label angle ", JLabel.RIGHT), c0);
        c0.gridx = 3;
        c0.anchor = GridBagConstraints.LINE_START;
        JHVSlider labelAngleSlider = createLabelAngleSlider(layer);
        add(labelAngleSlider, c0);

        nonDiskControls = new JComponent[]{axisToggle, radialToggle, gridTypeBox, latSpinner};
        // The radius-label spoke angle only makes sense for the disk grid, so enable it only in a
        // disk projection. Label size stays available alongside label opacity (always enabled).
        diskOnlyControls = new JComponent[]{labelAngleSlider};
        applyProjectionEnablement();
        // Track the projection only while the panel is on screen, so the listener is not leaked
        addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                ViewState.addModeListener(modeListener);
                applyProjectionEnablement();
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
                ViewState.removeModeListener(modeListener);
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
            }
        });
    }

    private void applyProjectionEnablement() {
        boolean disk = ViewState.getProjection().isDisk();
        for (JComponent control : nonDiskControls)
            control.setEnabled(!disk);
        for (JComponent control : diskOnlyControls)
            control.setEnabled(disk);
    }

    private JButton createColorButton(GridLayer layer) {
        JButton button = new JButton(swatchIcon(layer.getGridColor()));
        button.setToolTipText("Grid line color");
        button.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(this, "Grid color", layer.getGridColor());
            if (chosen != null) {
                layer.setGridColor(chosen);
                button.setIcon(swatchIcon(chosen));
            }
        });
        return button;
    }

    private static Icon swatchIcon(Color color) {
        return new Icon() {
            @Override
            public int getIconWidth() {
                return 24;
            }

            @Override
            public int getIconHeight() {
                return 12;
            }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setColor(color);
                g.fillRect(x, y, getIconWidth(), getIconHeight());
                g.setColor(Color.DARK_GRAY);
                g.drawRect(x, y, getIconWidth() - 1, getIconHeight() - 1);
            }
        };
    }

    private static JHVSlider createOpacitySlider(String tooltip, double initial, DoubleConsumer setter) {
        JHVSlider slider = new JHVSlider(0, 100, (int) Math.round(initial * 100));
        slider.setToolTipText(tooltip);
        slider.addChangeListener(e -> setter.accept(slider.getValue() / 100.));
        return slider;
    }

    private static JHVSlider createLineWidthSlider(GridLayer layer) {
        int min = (int) Math.round(GridLayer.GRID_LINE_SCALE_MIN * 10);
        int max = (int) Math.round(GridLayer.GRID_LINE_SCALE_MAX * 10);
        JHVSlider slider = new JHVSlider(min, max, (int) Math.round(layer.getGridLineScale() * 10));
        slider.setToolTipText("Grid line width");
        slider.addChangeListener(e -> layer.setGridLineScale(slider.getValue() / 10.));
        return slider;
    }

    private static JHVSlider createLabelSizeSlider(GridLayer layer) {
        int min = (int) GridLayer.GRID_LABEL_SIZE_MIN;
        int max = (int) GridLayer.GRID_LABEL_SIZE_MAX;
        JHVSlider slider = new JHVSlider(min, max, (int) Math.round(layer.getGridLabelSize()));
        slider.setToolTipText("Disk grid label font size");
        slider.addChangeListener(e -> layer.setGridLabelSize(slider.getValue()));
        return slider;
    }

    private static JHVSlider createLabelAngleSlider(GridLayer layer) {
        JHVSlider slider = new JHVSlider(0, 360, (int) Math.round(layer.getGridLabelAngle()));
        slider.setToolTipText("Disk grid radius-label spoke angle (° clockwise from vertical)");
        slider.addChangeListener(e -> layer.setGridLabelAngle(slider.getValue()));
        return slider;
    }

    private JCheckBox createToggle(String text, boolean initialValue, Consumer<Boolean> onChange) {
        JCheckBox checkBox = new JCheckBox(text, initialValue);
        checkBox.setHorizontalTextPosition(SwingConstants.LEFT);
        checkBox.addActionListener(e -> onChange.accept(checkBox.isSelected()));
        return checkBox;
    }

    private JComboBox<GridType> createGridTypeBox(GridLayer layer) {
        JComboBox<GridType> comboBox = new JComboBox<>(GridType.values());
        comboBox.setSelectedItem(Display.gridType);
        comboBox.addActionListener(e -> {
            GridType gridType = (GridType) Objects.requireNonNull(comboBox.getSelectedItem());
            layer.setGridType(gridType);
        });
        return comboBox;
    }

    private JHVSpinner createGridResolutionSpinner(double initialValue, DoubleConsumer valueSetter) {
        JHVSpinner spinner = new JHVSpinner(initialValue, GridLayer.GRID_STEP_MIN, GridLayer.GRID_STEP_MAX, GridLayer.GRID_STEP);
        spinner.addChangeListener(e -> valueSetter.accept((Double) spinner.getValue()));
        JFormattedTextField textField = ((JHVSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        textField.setFormatterFactory(new TerminatedFormatterFactory("%.1f", "°", GridLayer.GRID_STEP_MIN, GridLayer.GRID_STEP_MAX));
        return spinner;
    }

}
