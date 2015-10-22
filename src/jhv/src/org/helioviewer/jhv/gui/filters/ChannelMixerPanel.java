package org.helioviewer.jhv.gui.filters;

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

/**
 * Panel containing three check boxes to modify the color mask of an image.
 *
 * @author Markus Langenberg
 */
public class ChannelMixerPanel extends AbstractFilterPanel implements ItemListener, FilterDetails {

    private final JCheckBox redCheckBox;
    private final JCheckBox greenCheckBox;
    private final JCheckBox blueCheckBox;
    private final JLabel title;
    private final JPanel boxPanel;

    public ChannelMixerPanel() {
        title = new JLabel("Channels", JLabel.RIGHT);

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

    /**
     * Changes the channel selection of the image.
     */
    @Override
    public void itemStateChanged(ItemEvent e) {
        image.setColorMask(redCheckBox.isSelected(), greenCheckBox.isSelected(), blueCheckBox.isSelected());
        Displayer.display();
    }

    /**
     * Sets the panel values.
     *
     * This may be useful if the values are changed from another source than the
     * panel itself.
     *
     * @param colorMask
     *            Mask representing the new values
     */
    void setValue(ColorMask colorMask) {
        redCheckBox.setSelected(colorMask.showRed());
        greenCheckBox.setSelected(colorMask.showGreen());
        blueCheckBox.setSelected(colorMask.showBlue());
    }

    @Override
    public void setGLImage(GLImage image) {
        super.setGLImage(image);
        setValue(image.getColorMask());
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public Component getSlider() {
        return boxPanel;
    }

    @Override
    public Component getValue() {
        return new JLabel("           ");
    }

}
