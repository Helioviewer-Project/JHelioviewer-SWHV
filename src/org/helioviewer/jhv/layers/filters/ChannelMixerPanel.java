package org.helioviewer.jhv.layers.filters;

import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.image.ImageDisplaySettings;
import org.helioviewer.jhv.layers.ImageLayer;

public final class ChannelMixerPanel {

    private ChannelMixerPanel() {}

    public static FilterDetails create(ImageLayer layer) {
        JPanel boxPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        JLabel title = new JLabel("Channels ", JLabel.RIGHT);
        ImageDisplaySettings settings = layer.getDisplaySettings();
        JCheckBox redCheckBox = new JCheckBox("Red", settings.getRed());
        redCheckBox.setToolTipText("Toggle red channel");
        boxPanel.add(redCheckBox);

        JCheckBox greenCheckBox = new JCheckBox("Green", settings.getGreen());
        greenCheckBox.setToolTipText("Toggle green channel");
        boxPanel.add(greenCheckBox);

        JCheckBox blueCheckBox = new JCheckBox("Blue", settings.getBlue());
        blueCheckBox.setToolTipText("Toggle blue channel");
        boxPanel.add(blueCheckBox);

        ActionListener listener = e -> {
            settings.setColor(redCheckBox.isSelected() ? 1 : 0,
                    greenCheckBox.isSelected() ? 1 : 0,
                    blueCheckBox.isSelected() ? 1 : 0);
            DisplayController.display();
        };
        redCheckBox.addActionListener(listener);
        greenCheckBox.addActionListener(listener);
        blueCheckBox.addActionListener(listener);
        return new FilterRow(title, boxPanel, new JPanel());
    }

}
