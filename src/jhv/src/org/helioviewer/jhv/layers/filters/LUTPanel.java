package org.helioviewer.jhv.layers.filters;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.ImageLayerOptions;

import com.jidesoft.swing.JideToggleButton;

public class LUTPanel implements ActionListener, FilterDetails {

    private static final Icon invertIcon = IconBank.getIcon(JHVIcon.INVERT);
    private static final Icon enhanceIcon = IconBank.getIcon(JHVIcon.LAYER_IMAGE);

    private final Map<String, LUT> lutMap;

    private final JComboBox<String> combobox;
    private final JPanel buttonPanel;
    private final JideToggleButton invertButton;
    private final JideToggleButton enhanceButton;

    public LUTPanel() {
        lutMap = LUT.copyMap(); // duplicate

        Set<String> set = lutMap.keySet();
        combobox = new JComboBox<>(set.toArray(new String[set.size()]));
        combobox.setMaximumSize(combobox.getPreferredSize());
        combobox.setToolTipText("Choose a color table");
        combobox.addActionListener(this);

        invertButton = new JideToggleButton(invertIcon);
        invertButton.setToolTipText("Invert color table");
        invertButton.addActionListener(this);

        enhanceButton = new JideToggleButton(enhanceIcon);
        enhanceButton.setToolTipText("Enhance off-disk corona");
        enhanceButton.addActionListener(this);

        buttonPanel = new JPanel();
        buttonPanel.add(invertButton);
        buttonPanel.add(enhanceButton);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        LUT newMap = lutMap.get(combobox.getSelectedItem());
        ((ImageLayerOptions) getComponent().getParent()).getGLImage().setLUT(newMap, invertButton.isSelected());
        ((ImageLayerOptions) getComponent().getParent()).getGLImage().setEnhanced(enhanceButton.isSelected());
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
