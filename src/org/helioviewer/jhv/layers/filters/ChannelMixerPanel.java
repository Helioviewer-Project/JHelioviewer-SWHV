package org.helioviewer.jhv.layers.filters;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.layers.ImageLayer;

public final class ChannelMixerPanel implements FilterDetails {

    private final JPanel boxPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
    private final JPanel emptyPanel = new JPanel();
    private final JLabel title = new JLabel("Channels ", JLabel.RIGHT);

    public ChannelMixerPanel(ImageLayer layer) {
        JCheckBox redCheckBox = new JCheckBox("Red", layer.getGLImage().getRed());
        redCheckBox.setToolTipText("Toggle red channel");
        boxPanel.add(redCheckBox);

        JCheckBox greenCheckBox = new JCheckBox("Green", layer.getGLImage().getGreen());
        greenCheckBox.setToolTipText("Toggle green channel");
        boxPanel.add(greenCheckBox);

        JCheckBox blueCheckBox = new JCheckBox("Blue", layer.getGLImage().getBlue());
        blueCheckBox.setToolTipText("Toggle blue channel");
        boxPanel.add(blueCheckBox);

        ActionListener listener = e -> {
            layer.getGLImage().setColor(redCheckBox.isSelected() ? 1 : 0,
                    greenCheckBox.isSelected() ? 1 : 0,
                    blueCheckBox.isSelected() ? 1 : 0);
            DisplayController.display();
        };
        redCheckBox.addActionListener(listener);
        greenCheckBox.addActionListener(listener);
        blueCheckBox.addActionListener(listener);
    }

    @Override
    public Component getFirst() {
        return title;
    }

    @Override
    public Component getSecond() {
        return boxPanel;
    }

    @Override
    public Component getThird() {
        return emptyPanel;
    }

}
