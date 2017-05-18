package org.helioviewer.jhv.layers.filters;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.layers.ImageLayerOptions;

public class ChannelMixerPanel implements FilterDetails {

    private final JPanel boxPanel;

    public ChannelMixerPanel(ImageLayerOptions parent) {
        boxPanel = new JPanel(new GridLayout(1, 3));

        JCheckBox redCheckBox = new JCheckBox("Red", parent.getGLImage().getRed());
        redCheckBox.setToolTipText("Toggle red channel");
        boxPanel.add(redCheckBox, BorderLayout.WEST);

        JCheckBox greenCheckBox = new JCheckBox("Green", parent.getGLImage().getGreen());
        greenCheckBox.setToolTipText("Toggle green channel");
        boxPanel.add(greenCheckBox, BorderLayout.CENTER);

        JCheckBox blueCheckBox = new JCheckBox("Blue", parent.getGLImage().getBlue());
        blueCheckBox.setToolTipText("Toggle blue channel");
        boxPanel.add(blueCheckBox, BorderLayout.EAST);

        ActionListener listener = e -> {
            parent.getGLImage().setColor(redCheckBox.isSelected() ? 1 : 0,
                                         greenCheckBox.isSelected() ? 1 : 0,
                                         blueCheckBox.isSelected() ? 1 : 0);
            Displayer.display();
        };
        redCheckBox.addActionListener(listener);
        greenCheckBox.addActionListener(listener);
        blueCheckBox.addActionListener(listener);
    }

    @Override
    public Component getTitle() {
        return new JLabel("Channels", JLabel.RIGHT);
    }

    @Override
    public Component getComponent() {
        return boxPanel;
    }

    @Override
    public Component getLabel() {
        return new JLabel("           ");
    }

}
