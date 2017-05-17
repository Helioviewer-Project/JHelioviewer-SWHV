package org.helioviewer.jhv.layers.filters;

import java.awt.Component;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.base.lut.LUTComboBox;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.layers.ImageLayerOptions;

import com.jidesoft.swing.JideToggleButton;

public class LUTPanel implements FilterDetails {

    private final LUTComboBox lutCombo;
    private final JPanel buttonPanel;

    public LUTPanel(ImageLayerOptions parent) {
        lutCombo = new LUTComboBox();
        JideToggleButton invertButton = new JideToggleButton(Buttons.invert);
        invertButton.setToolTipText("Invert color table");

        ActionListener listener = e -> {
            parent.getGLImage().setLUT(lutCombo.getLUT(), invertButton.isSelected());
            Displayer.display();
        };
        lutCombo.addActionListener(listener);
        invertButton.addActionListener(listener);

        JideToggleButton enhanceButton = new JideToggleButton(Buttons.corona, parent.getGLImage().getEnhanced() == 1);

        enhanceButton.setToolTipText("Enhance off-disk corona");
        enhanceButton.addActionListener(e -> {
            parent.getGLImage().setEnhanced(enhanceButton.isSelected() ? 1 : 0);
            Displayer.display();
        });

        buttonPanel = new JPanel();
        buttonPanel.add(invertButton);
        buttonPanel.add(enhanceButton);
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
