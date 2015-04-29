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
import org.helioviewer.viewmodel.view.AbstractView;

/**
 * Panel containing a combobox for choosing the color table and button to add
 * further tables adapted
 *
 * @author Helge Dietert (extended)
 */
public class SOHOLUTPanel extends AbstractFilterPanel implements ActionListener, FilterDetails {

    private static final Icon invertIcon = IconBank.getIcon(JHVIcon.INVERT);

    private final Map<String, LUT> lutMap;

    /**
     * Shown combobox to choose
     */
    private final JComboBox combobox;
    private final JToggleButton invertButton;
    private final JLabel title;

    public SOHOLUTPanel() {
        lutMap = LUT.getStandardList();
        title = new JLabel("Color");
        title.setHorizontalAlignment(JLabel.RIGHT);

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
        jp2view.setLUT(newMap, invertButton.isSelected());
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
        jp2view.setLUT(lut, invertButton.isSelected());
    }

    public void setEnabled(boolean enabled) {
        title.setEnabled(enabled);
        combobox.setEnabled(enabled);
        invertButton.setEnabled(enabled);
    }

    /**
     * Sets the sharpen value.
     *
     * This may be useful, if the opacity is changed from another source than
     * the slider itself.
     *
     * @param lut
     *            New look up table
     * @param invertLUT
     *            true if the look up table shall be inverted, false otherwise.
     */
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
    public void setJP2View(AbstractView jp2view) {
        super.setJP2View(jp2view);
        setValue(jp2view.getLUT(), jp2view.getInvertLUT());
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
