package org.helioviewer.jhv.renderable.components.ImageLayer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.opengl.GLImage;
import org.helioviewer.jhv.viewmodel.imagedata.ColorMask;

public class ChannelMixerPanel extends AbstractFilterPanel implements ItemListener, FilterDetails {

    private final JCheckBox redCheckBox;
    private final JCheckBox greenCheckBox;
    private final JCheckBox blueCheckBox;
    private final JPanel boxPanel;

    public ChannelMixerPanel() {
        boxPanel = new JPanel(new GridLayout(1, 3));

        redCheckBox = new JCheckBox("Red", true);
        redCheckBox.setToolTipText("Toggle red channel");
        redCheckBox.addItemListener(this);
        boxPanel.add(redCheckBox, BorderLayout.WEST);

        greenCheckBox = new JCheckBox("Green", true);
        greenCheckBox.setToolTipText("Toggle green channel");
        greenCheckBox.addItemListener(this);
        boxPanel.add(greenCheckBox, BorderLayout.CENTER);

        blueCheckBox = new JCheckBox("Blue", true);
        blueCheckBox.setToolTipText("Toggle blue channel");
        blueCheckBox.addItemListener(this);
        boxPanel.add(blueCheckBox, BorderLayout.EAST);
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        image.setColorMask(redCheckBox.isSelected(), greenCheckBox.isSelected(), blueCheckBox.isSelected());
        Displayer.display();
    }

    /**
     * @param colorMask
     *            Mask representing the new values
     */
    private void setValue(ColorMask colorMask) {
        redCheckBox.setSelected(colorMask.showRed());
        greenCheckBox.setSelected(colorMask.showGreen());
        blueCheckBox.setSelected(colorMask.showBlue());
    }

    @Override
    public void setGLImage(GLImage image) {
        super.setGLImage(image);
        if (image != null) {
            setValue(image.getColorMask());
        }
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
