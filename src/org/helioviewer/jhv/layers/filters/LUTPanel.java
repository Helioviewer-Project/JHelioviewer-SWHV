package org.helioviewer.jhv.layers.filters;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.base.lut.LUTComboBox;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.layers.ImageLayerOptions;

import com.jidesoft.swing.JideToggleButton;

public class LUTPanel implements ActionListener, FilterDetails {

    private final LUTComboBox lutCombo;
    private final JPanel buttonPanel;
    private final JideToggleButton invertButton;
    private final JideToggleButton enhanceButton;

    public LUTPanel() {
        lutCombo = new LUTComboBox();
        lutCombo.addActionListener(this);

        invertButton = new JideToggleButton(Buttons.invert);
        invertButton.setToolTipText("Invert color table");
        invertButton.addActionListener(this);

        enhanceButton = new JideToggleButton(Buttons.corona);
        enhanceButton.setToolTipText("Enhance off-disk corona");
        enhanceButton.addActionListener(this);

        buttonPanel = new JPanel();
        buttonPanel.add(invertButton);
        buttonPanel.add(enhanceButton);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ((ImageLayerOptions) getComponent().getParent()).getGLImage().setLUT(lutCombo.getLUT(), invertButton.isSelected());
        ((ImageLayerOptions) getComponent().getParent()).getGLImage().setEnhanced(enhanceButton.isSelected() ? 1 : 0);
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
