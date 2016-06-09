package org.helioviewer.jhv.renderable.components.ImageLayer;

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
import org.helioviewer.jhv.opengl.GLImage;

public class LUTPanel extends AbstractFilterPanel implements ActionListener, FilterDetails {

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
            if (invertButton.isSelected()) {
                invertButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            } else {
                invertButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
            }
        } else if (e.getSource() == enhanceButton) {
            boolean isSelected = enhanceButton.isSelected();
            image.setEnhanced(isSelected);
            if (isSelected) {
                enhanceButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            } else {
                enhanceButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
            }
        }

        LUT newMap = lutMap.get((String) combobox.getSelectedItem());
        image.setLUT(newMap, invertButton.isSelected());
        Displayer.display();
    }

    private void setValue(LUT lut, boolean invertLUT, boolean enhanced) {
        invertButton.setSelected(invertLUT);
        enhanceButton.setSelected(enhanced);

        String name = lut.getName();
        if (lutMap.put(name, lut) == null)
            combobox.addItem(name);
        combobox.setSelectedItem(name);

        if (invertLUT) {
            invertButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        } else {
            invertButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        }

        if (enhanced) {
            enhanceButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        } else {
            enhanceButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        }
    }

    @Override
    public void setGLImage(GLImage image) {
        super.setGLImage(image);
        if (image != null) {
            setValue(image.getLUT(), image.getInvertLUT(), image.getEnhanced());
        }
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
