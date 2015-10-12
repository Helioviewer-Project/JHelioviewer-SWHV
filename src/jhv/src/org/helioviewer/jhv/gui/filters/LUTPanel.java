package org.helioviewer.jhv.gui.filters;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.border.BevelBorder;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.filters.lut.LUT;
import org.helioviewer.jhv.opengl.GLImage;

public class LUTPanel extends AbstractFilterPanel implements ActionListener, FilterDetails {

    private static final Icon invertIcon = IconBank.getIcon(JHVIcon.INVERT);

    private final Map<String, LUT> lutMap;

    /**
     * Shown combobox to choose
     */
    private final JComboBox combobox;
    private final JToggleButton invertButton;
    private final JLabel title;

    public LUTPanel() {
        lutMap = LUT.getStandardList();

        title = new JLabel("Color", JLabel.RIGHT);

        combobox = new JComboBox(lutMap.keySet().toArray());
        combobox.setMaximumSize(combobox.getPreferredSize());
        combobox.setToolTipText("Choose a color table");
        combobox.addActionListener(this);

        invertButton = new JToggleButton(invertIcon);
        invertButton.setToolTipText("Invert color table");
        invertButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        invertButton.addActionListener(this);
    }

    /**
     * Sets the color table
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == invertButton) {
            if (invertButton.isSelected()) {
                invertButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            } else {
                invertButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
            }
        }

        LUT newMap = lutMap.get(combobox.getSelectedItem());
        image.setLUT(newMap, invertButton.isSelected());
        Displayer.display();
    }

    /**
     * Set the filter to the filter with the given name if the filter exists for
     * this panel
     *
     * @param name
     *            Name of the filter
     */
    public void setLutByName(String name) {
        combobox.setSelectedItem(name);
    }

    /**
     * Adds a color table to the available list and set it active
     *
     * @param lut
     *            Color table to add
     */
    public void addLut(LUT lut) {
        if (lutMap.put(lut.getName(), lut) == null)
            combobox.addItem(lut.getName());
        combobox.setSelectedItem(lut.getName());
        image.setLUT(lut, invertButton.isSelected());
    }

    void setValue(LUT lut, boolean invertLUT) {
        invertButton.setSelected(invertLUT);
        combobox.setSelectedItem(lut.getName());

        if (invertLUT) {
            invertButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        } else {
            invertButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        }
    }

    @Override
    public void setGLImage(GLImage image) {
        super.setGLImage(image);
        setValue(image.getLUT(), image.getInvertLUT());
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public Component getSlider() {
        return combobox;
    }

    @Override
    public Component getValue() {
        return invertButton;
    }

}
