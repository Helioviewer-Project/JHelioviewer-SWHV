package org.helioviewer.jhv.layers.filters;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.BevelBorder;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.ImageLayerOptions;

public class LUTPanel implements ActionListener, FilterDetails {

    private static final Icon invertIcon = IconBank.getIcon(JHVIcon.INVERT);
    private static final Icon enhanceIcon = IconBank.getIcon(JHVIcon.LAYER_IMAGE);

    private final Map<String, LUT> lutMap;

    private final JComboBox combobox;
    private final JPanel buttonPanel;
    private final JToggleButton invertButton;
    private final JToggleButton enhanceButton;

    public LUTPanel() {
        lutMap = new TreeMap<String, LUT>(LUT.getStandardList());

        combobox = new JComboBox(lutMap.keySet().toArray());
        combobox.setMaximumSize(combobox.getPreferredSize());
        combobox.setToolTipText("Choose a color table");
        combobox.addActionListener(this);

        invertButton = new JToggleButton(invertIcon);
        invertButton.setToolTipText("Invert color table");
        invertButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        invertButton.addActionListener(this);

        enhanceButton = new JToggleButton(enhanceIcon);
        enhanceButton.setToolTipText("Enhance off-disk corona");
        enhanceButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        enhanceButton.addActionListener(this);

        buttonPanel = new JPanel();
        buttonPanel.add(invertButton);
        buttonPanel.add(enhanceButton);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == invertButton) {
            invertButton.setBorder(BorderFactory.createBevelBorder(invertButton.isSelected() ? BevelBorder.LOWERED : BevelBorder.RAISED));
        } else if (e.getSource() == enhanceButton) {
            boolean isSelected = enhanceButton.isSelected();
            ((ImageLayerOptions) getComponent().getParent()).getGLImage().setEnhanced(isSelected);
            enhanceButton.setBorder(BorderFactory.createBevelBorder(isSelected ? BevelBorder.LOWERED : BevelBorder.RAISED));
        }

        LUT newMap = lutMap.get(combobox.getSelectedItem());
        ((ImageLayerOptions) getComponent().getParent()).getGLImage().setLUT(newMap, invertButton.isSelected());
        Displayer.display();
    }

    public void setLUT(LUT lut) {
        String name;
        if (lut != null) {
            name = lut.getName();
            if (lutMap.get(name) == null) {
                lutMap.put(name, lut);
                combobox.addItem(name);
            }
        } else // e.g. RGB
            name = "Gray";

        combobox.setSelectedItem(name);
    }

    @Override
    public Component getTitle() {
        return new JLabel("Color", JLabel.RIGHT);
    }

    @Override
    public Component getComponent() {
        return combobox;
    }

    @Override
    public Component getLabel() {
        return buttonPanel;
    }

}
