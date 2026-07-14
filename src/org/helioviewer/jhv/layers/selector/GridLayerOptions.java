package org.helioviewer.jhv.layers.selector;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

import javax.swing.JCheckBox;
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
import org.helioviewer.jhv.gui.component.JHVSpinner;
import org.helioviewer.jhv.gui.component.TerminatedFormatterFactory;
import org.helioviewer.jhv.layers.GridLayer;

@SuppressWarnings("serial")
final class GridLayerOptions extends JPanel {

    // The disk projection draws a radial ring/spoke grid; these lat/lon-graticule controls have
    // no meaning there (Longitude maps to the spoke spacing, so it stays enabled), so they are
    // greyed out while a disk projection is active.
    private final JComponent[] nonDiskControls;
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

        nonDiskControls = new JComponent[]{axisToggle, radialToggle, gridTypeBox, latSpinner};
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
        boolean enabled = !ViewState.getProjection().isDisk();
        for (JComponent control : nonDiskControls)
            control.setEnabled(enabled);
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
