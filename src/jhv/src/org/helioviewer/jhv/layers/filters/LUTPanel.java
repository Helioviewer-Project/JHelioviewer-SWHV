package org.helioviewer.jhv.layers.filters;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.base.lut.LUTComboBox;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.ImageLayerOptions;

import com.jidesoft.swing.JideToggleButton;

public class LUTPanel implements ActionListener, FilterDetails {

    private static final Icon invertIcon = IconBank.getIcon(JHVIcon.INVERT);
    private static final Icon enhanceIcon = IconBank.getIcon(JHVIcon.LAYER_IMAGE);

    private final LUTComboBox lutCombo;
    private final JPanel buttonPanel;
    private final JideToggleButton invertButton;
    private final JideToggleButton enhanceButton;

    public LUTPanel() {
        lutCombo = new LUTComboBox();
        lutCombo.addActionListener(this);

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
        ((ImageLayerOptions) getComponent().getParent()).getGLImage().setLUT(lutCombo.getLUT(), invertButton.isSelected());
        ((ImageLayerOptions) getComponent().getParent()).getGLImage().setEnhanced(enhanceButton.isSelected());
        Displayer.display();
    }

    public void setLUT(LUT lut) {
        lutCombo.setLUT(lut);
    }

    @Override
    public Component getTitle() {
        return new JLabel("Color", JLabel.RIGHT);
    }

    @Override
    public Component getComponent() {
        return lutCombo;
    }

    @Override
    public Component getLabel() {
        return buttonPanel;
    }

}
