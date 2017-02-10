package org.helioviewer.jhv.layers.filters;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.layers.ImageLayerOptions;

public class ChannelMixerPanel implements ActionListener, FilterDetails {

    private final JCheckBox redCheckBox;
    private final JCheckBox greenCheckBox;
    private final JCheckBox blueCheckBox;
    private final JPanel boxPanel;

    public ChannelMixerPanel() {
        boxPanel = new JPanel(new GridLayout(1, 3));

        redCheckBox = new JCheckBox("Red", true);
        redCheckBox.setToolTipText("Toggle red channel");
        redCheckBox.addActionListener(this);
        boxPanel.add(redCheckBox, BorderLayout.WEST);

        greenCheckBox = new JCheckBox("Green", true);
        greenCheckBox.setToolTipText("Toggle green channel");
        greenCheckBox.addActionListener(this);
        boxPanel.add(greenCheckBox, BorderLayout.CENTER);

        blueCheckBox = new JCheckBox("Blue", true);
        blueCheckBox.setToolTipText("Toggle blue channel");
        blueCheckBox.addActionListener(this);
        boxPanel.add(blueCheckBox, BorderLayout.EAST);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ((ImageLayerOptions) getComponent().getParent()).getGLImage().setColorMask(redCheckBox.isSelected(), greenCheckBox.isSelected(), blueCheckBox.isSelected());
        Displayer.display();
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
