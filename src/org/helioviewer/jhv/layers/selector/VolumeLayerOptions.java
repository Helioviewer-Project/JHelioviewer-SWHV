package org.helioviewer.jhv.layers.selector;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.gui.component.JHVRangeSlider;
import org.helioviewer.jhv.image.lut.LUTComboBox;
import org.helioviewer.jhv.layers.VolumeLayer;
import org.helioviewer.jhv.layers.filters.FilterDetails;
import org.helioviewer.jhv.layers.filters.SliderFilterPanel;

@SuppressWarnings("serial")
final class VolumeLayerOptions extends JPanel {

    VolumeLayerOptions(VolumeLayer layer) {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        addRow(SliderFilterPanel.opacity(layer.getOpacity(), layer::setOpacity), 0);
        LUTComboBox lutCombo = new LUTComboBox();
        lutCombo.setLUT(layer.getLUT());
        lutCombo.addActionListener(e -> {
            layer.setLUT(lutCombo.getLUT());
            DisplayController.display();
        });
        addRow(new JLabel("Color ", JLabel.RIGHT), lutCombo, new JPanel(), 1);

        for (int axis = 0; axis < 3; axis++)
            addCropRow(layer, axis, axis + 2);
    }

    private void addCropRow(VolumeLayer layer, int axis, int y) {
        int low = (int) Math.round(100 * layer.getCropMin(axis));
        int high = (int) Math.round(100 * layer.getCropMax(axis));
        JHVRangeSlider slider = new JHVRangeSlider(0, 100, low, high);
        JLabel values = new JLabel(formatRange(low, high), JLabel.RIGHT);
        slider.addChangeListener(e -> {
            int newLow = slider.getLowValue();
            int newHigh = slider.getHighValue();
            layer.setCrop(axis, newLow / 100., newHigh / 100.);
            values.setText(formatRange(newLow, newHigh));
            DisplayController.display();
        });
        JLabel title = new JLabel("Crop " + (axis + 1) + ' ', JLabel.RIGHT);
        title.setToolTipText("Crop along FITS pixel axis " + (axis + 1));
        addRow(title, slider, values, y);
    }

    private static String formatRange(int low, int high) {
        return "<html><p align='right'>" + low + "%</p><p align='right'>" + high + "%</p>";
    }

    private void addRow(FilterDetails details, int y) {
        addRow(details.getFirst(), details.getSecond(), details.getThird(), y);
    }

    private void addRow(Component first, Component second, Component third, int y) {
        add(first, constraints(0, y, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE));
        add(second, constraints(1, y, 1, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL));
        add(third, constraints(2, y, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE));
    }

    private static GridBagConstraints constraints(int x, int y, double weightX, int anchor, int fill) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.weightx = weightX;
        constraints.weighty = 1;
        constraints.anchor = anchor;
        constraints.fill = fill;
        return constraints;
    }
}
