package org.helioviewer.jhv.layers.selector;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Objects;
import java.util.function.DoubleConsumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.GridType;
import org.helioviewer.jhv.gui.component.JHVSlider;
import org.helioviewer.jhv.gui.component.JHVSpinner;
import org.helioviewer.jhv.gui.component.TerminatedFormatterFactory;
import org.helioviewer.jhv.layers.GridLayer;

@SuppressWarnings("serial")
final class GridLayerOptions extends JPanel {

    GridLayerOptions(GridLayer layer) {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        GridBagConstraints c0 = new GridBagConstraints();
        c0.fill = GridBagConstraints.HORIZONTAL;
        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.anchor = GridBagConstraints.LINE_START;

        c0.gridy = 0;

        c0.gridx = 0;
        c0.gridwidth = 2;
        JCheckBox radial = new JCheckBox("Radial grid", layer.isShowRadial());
        radial.setHorizontalTextPosition(SwingConstants.LEFT);
        radial.addActionListener(e -> layer.setShowRadial(radial.isSelected()));
        add(radial, c0);

        c0.gridx = 2;
        c0.gridwidth = 1;
        c0.weightx = 0;
        add(new JLabel("Grid type ", JLabel.RIGHT), c0);
        c0.gridx = 3;
        c0.weightx = 1;
        add(createGridTypeBox(layer), c0);

        c0.gridy = 1;

        c0.gridx = 0;
        c0.weightx = 0;
        add(new JLabel("Longitude ", JLabel.RIGHT), c0);

        c0.gridx = 1;
        c0.weightx = 1;
        add(createGridResolutionSpinner(layer.getLonStep(), layer::setLonStep), c0);

        c0.gridx = 2;
        c0.weightx = 0;
        add(new JLabel("Latitude ", JLabel.RIGHT), c0);

        c0.gridx = 3;
        c0.weightx = 1;
        add(createGridResolutionSpinner(layer.getLatStep(), layer::setLatStep), c0);

        JPanel appearancePanel = new JPanel(new GridBagLayout());
        addAppearanceRow(appearancePanel, "Color ", createColorBox(layer), 0);
        addAppearanceRow(appearancePanel, "Line width ", createLineWidthSlider(layer), 1);
        addAppearanceRow(appearancePanel, "Line opacity ", createOpacitySlider(layer.getGridAlpha(), layer::setGridAlpha), 2);
        addAppearanceRow(appearancePanel, "Label opacity ", createOpacitySlider(layer.getLabelAlpha(), layer::setLabelAlpha), 3);
        addAppearanceRow(appearancePanel, "Label size ", createLabelSizeSlider(layer), 4);
        addAppearanceRow(appearancePanel, "Ring label angle ", createLabelAngleSlider(layer), 5);

        c0.gridy = 2;
        c0.gridx = 0;
        c0.gridwidth = 4;
        add(appearancePanel, c0);
    }

    private static void addAppearanceRow(JPanel panel, String text, Component component, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = y;
        c.anchor = GridBagConstraints.LINE_END;
        panel.add(new JLabel(text, JLabel.RIGHT), c);

        c.gridx = 1;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        panel.add(component, c);
    }

    private static JComboBox<Colors> createColorBox(GridLayer layer) {
        JComboBox<Colors> comboBox = new JComboBox<>(Colors.values());
        comboBox.setSelectedItem(layer.getGridColor());
        comboBox.setRenderer(new ColorRenderer());
        comboBox.addActionListener(e -> {
            Colors color = (Colors) Objects.requireNonNull(comboBox.getSelectedItem());
            layer.setGridColor(color);
        });
        return comboBox;
    }

    private static JHVSlider createOpacitySlider(double initialValue, DoubleConsumer valueSetter) {
        JHVSlider slider = new JHVSlider(0, 1, initialValue, 100);
        slider.addChangeListener(e -> valueSetter.accept(slider.getDoubleValue()));
        return slider;
    }

    private static JHVSlider createLineWidthSlider(GridLayer layer) {
        JHVSlider slider = new JHVSlider(GridLayer.GRID_LINE_SCALE_MIN, GridLayer.GRID_LINE_SCALE_MAX, layer.getGridLineScale(), 10);
        slider.addChangeListener(e -> layer.setGridLineScale(slider.getDoubleValue()));
        return slider;
    }

    private static JHVSlider createLabelSizeSlider(GridLayer layer) {
        JHVSlider slider = new JHVSlider(GridLayer.GRID_LABEL_SIZE_MIN, GridLayer.GRID_LABEL_SIZE_MAX, layer.getGridLabelSize(), 1);
        slider.addChangeListener(e -> layer.setGridLabelSize(slider.getDoubleValue()));
        return slider;
    }

    private static JHVSlider createLabelAngleSlider(GridLayer layer) {
        JHVSlider slider = new JHVSlider(GridLayer.GRID_LABEL_ANGLE_MIN, GridLayer.GRID_LABEL_ANGLE_MAX, layer.getGridLabelAngle(), 1);
        slider.addChangeListener(e -> layer.setGridLabelAngle(slider.getDoubleValue()));
        return slider;
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

    private static final class ColorIcon implements Icon {

        private static final int WIDTH = 24;
        private static final int HEIGHT = 12;

        private final Colors color;

        private ColorIcon(Colors _color) {
            color = _color;
        }

        @Override
        public int getIconWidth() {
            return WIDTH;
        }

        @Override
        public int getIconHeight() {
            return HEIGHT;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(color.awtColor());
            g.fillRect(x, y, WIDTH, HEIGHT);
            g.setColor(Color.DARK_GRAY);
            g.drawRect(x, y, WIDTH - 1, HEIGHT - 1);
        }
    }

    private static final class ColorRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Colors color) {
                label.setIcon(new ColorIcon(color));
                label.setText(color.toString());
            }
            return label;
        }
    }

}
