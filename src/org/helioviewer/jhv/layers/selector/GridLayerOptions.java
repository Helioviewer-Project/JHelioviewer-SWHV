package org.helioviewer.jhv.layers.selector;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Objects;
import java.util.function.Consumer;
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
import org.helioviewer.jhv.gui.component.Buttons;
import org.helioviewer.jhv.gui.component.JHVSlider;
import org.helioviewer.jhv.gui.component.JHVSpinner;
import org.helioviewer.jhv.gui.component.TerminatedFormatterFactory;
import org.helioviewer.jhv.layers.GridLayer;

import com.jidesoft.swing.JideToggleButton;

@SuppressWarnings("serial")
final class GridLayerOptions extends JPanel {

    GridLayerOptions(GridLayer layer) {
        setLayout(new GridBagLayout());

        GridBagConstraints c0 = new GridBagConstraints();
        c0.fill = GridBagConstraints.HORIZONTAL;
        c0.weightx = 1.;
        c0.weighty = 1.;

        c0.gridy = 0;

        c0.gridx = 1;
        c0.anchor = GridBagConstraints.LINE_END;
        add(createToggle("Solar axis", layer.isShowAxis(), layer::setShowAxis), c0);

        c0.gridx = 3;
        c0.anchor = GridBagConstraints.LINE_END;
        add(createToggle("Grid labels", layer.isShowLabels(), layer::setShowLabels), c0);

        c0.gridy = 1;

        c0.gridx = 1;
        c0.anchor = GridBagConstraints.LINE_END;
        add(createToggle("Radial grid", layer.isShowRadial(), layer::setShowRadial), c0);

        c0.gridx = 2;
        c0.anchor = GridBagConstraints.LINE_END;
        add(new JLabel("Grid type ", JLabel.RIGHT), c0);
        c0.gridx = 3;
        c0.anchor = GridBagConstraints.LINE_START;
        add(createGridTypeBox(layer), c0);

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
        add(createGridResolutionSpinner(layer.getLatStep(), layer::setLatStep), c0);

        JPanel adjustmentsPanel = new JPanel(new GridBagLayout());
        adjustmentsPanel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
        addAdjustmentRow(adjustmentsPanel, "Color ", createColorBox(layer), 0);
        addAdjustmentRow(adjustmentsPanel, "Line width ", createLineWidthSlider(layer), 1);
        addAdjustmentRow(adjustmentsPanel, "Line opacity ", createOpacitySlider(layer.getGridAlpha(), layer::setGridAlpha), 2);
        addAdjustmentRow(adjustmentsPanel, "Label opacity ", createOpacitySlider(layer.getLabelAlpha(), layer::setLabelAlpha), 3);

        JideToggleButton adjButton = new JideToggleButton(Buttons.adjustmentsRight);
        adjButton.addActionListener(e -> {
            boolean selected = adjButton.isSelected();
            adjustmentsPanel.setVisible(selected);
            adjButton.setText(selected ? Buttons.adjustmentsDown : Buttons.adjustmentsRight);
        });
        adjustmentsPanel.setVisible(false);

        c0.gridy = 3;
        c0.gridx = 1;
        c0.gridwidth = 3;
        c0.anchor = GridBagConstraints.LINE_START;
        c0.fill = GridBagConstraints.NONE;
        add(adjButton, c0);

        c0.gridy = 4;
        c0.gridx = 0;
        c0.gridwidth = 4;
        c0.anchor = GridBagConstraints.LINE_START;
        c0.fill = GridBagConstraints.HORIZONTAL;
        add(adjustmentsPanel, c0);
    }

    private static void addAdjustmentRow(JPanel panel, String text, Component component, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = y;
        c.anchor = GridBagConstraints.LINE_END;
        panel.add(new JLabel(text, JLabel.RIGHT), c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = y;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        panel.add(component, c);
    }

    private static JComboBox<Colors.NamedColor> createColorBox(GridLayer layer) {
        JComboBox<Colors.NamedColor> comboBox = new JComboBox<>(Colors.NamedColor.values());
        comboBox.setSelectedItem(layer.getGridColor());
        comboBox.setRenderer(new ColorRenderer());
        comboBox.addActionListener(e -> {
            Colors.NamedColor color = (Colors.NamedColor) Objects.requireNonNull(comboBox.getSelectedItem());
            layer.setGridColor(color);
        });
        return comboBox;
    }

    private static JHVSlider createOpacitySlider(double initialValue, DoubleConsumer valueSetter) {
        JHVSlider slider = new JHVSlider(0, 100, (int) Math.round(initialValue * 100));
        slider.addChangeListener(e -> valueSetter.accept(slider.getValue() / 100.));
        return slider;
    }

    private static JHVSlider createLineWidthSlider(GridLayer layer) {
        int min = (int) Math.round(GridLayer.GRID_LINE_SCALE_MIN * 10);
        int max = (int) Math.round(GridLayer.GRID_LINE_SCALE_MAX * 10);
        JHVSlider slider = new JHVSlider(min, max, (int) Math.round(layer.getGridLineScale() * 10));
        slider.addChangeListener(e -> layer.setGridLineScale(slider.getValue() / 10.));
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

    private static final class ColorIcon implements Icon {

        private static final int WIDTH = 24;
        private static final int HEIGHT = 12;

        private final Colors.NamedColor color;

        private ColorIcon(Colors.NamedColor _color) {
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
            if (value instanceof Colors.NamedColor color) {
                label.setIcon(new ColorIcon(color));
                label.setText(color.toString());
            }
            return label;
        }
    }

}
