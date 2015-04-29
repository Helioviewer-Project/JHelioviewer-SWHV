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
import org.helioviewer.viewmodel.imagedata.ColorMask;
import org.helioviewer.viewmodel.view.AbstractView;

/**
 * Panel containing three check boxes to modify the color mask of an image.
 *
 * @author Markus Langenberg
 */
public class ChannelMixerPanel extends AbstractFilterPanel implements ItemListener, FilterAlignmentDetails {

    private final JCheckBox redCheckBox;
    private final JCheckBox greenCheckBox;
    private final JCheckBox blueCheckBox;
    private final JLabel title;
    JPanel boxPanel;

    public ChannelMixerPanel() {

        title = new JLabel("Channels");
        title.setHorizontalAlignment(JLabel.RIGHT);

        boxPanel = new JPanel(new GridLayout(1, 3));
        // boxPanel.setLayout(new BoxLayout(boxPanel, BoxLayout.LINE_AXIS));

        redCheckBox = new JCheckBox("Red", true);
        redCheckBox.setToolTipText("Unchecked to omit the red color channel when drawing this layer");
        redCheckBox.addItemListener(this);
        boxPanel.add(redCheckBox, BorderLayout.WEST);

        greenCheckBox = new JCheckBox("Green", true);
        greenCheckBox.setToolTipText("Unchecked to omit the green color channel when drawing this layer");
        greenCheckBox.addItemListener(this);
        boxPanel.add(greenCheckBox, BorderLayout.CENTER);

        blueCheckBox = new JCheckBox("Blue", true);
        blueCheckBox.setToolTipText("Unchecked to omit the blue color channel when drawing this layer");
        blueCheckBox.addItemListener(this);
        boxPanel.add(blueCheckBox, BorderLayout.EAST);
    }

    /**
     * Changes the channel selection of the image.
     */
    @Override
    public void itemStateChanged(ItemEvent e) {
        jp2view.setColorMask(redCheckBox.isSelected(), greenCheckBox.isSelected(), blueCheckBox.isSelected());
        Displayer.display();
    }

    /**
     * Override the setEnabled method in order to keep the containing
     * components' enabledState synced with the enabledState of this component.
     */
    public void setEnabled(boolean enabled) {
        redCheckBox.setEnabled(enabled);
        greenCheckBox.setEnabled(enabled);
        blueCheckBox.setEnabled(enabled);
        title.setEnabled(enabled);
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
    public void setJP2View(AbstractView jp2view) {
        super.setJP2View(jp2view);
        setValue(jp2view.getColorMask());
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
        return null;
    }

}
