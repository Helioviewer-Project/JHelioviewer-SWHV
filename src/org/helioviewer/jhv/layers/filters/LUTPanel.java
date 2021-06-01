package org.helioviewer.jhv.layers.filters;

import java.awt.Component;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.base.lut.LUTComboBox;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.MovieDisplay;

import com.jidesoft.swing.JideToggleButton;

public class LUTPanel implements FilterDetails {

    private final LUTComboBox lutCombo;
    private final JPanel buttonPanel = new JPanel();

    public LUTPanel(ImageLayer layer) {
        lutCombo = new LUTComboBox();
        JideToggleButton invertButton = new JideToggleButton(Buttons.invert, layer.getGLImage().getInvertLUT());
        invertButton.setToolTipText("Invert color table");

        ActionListener listener = e -> {
            layer.getGLImage().setLUT(lutCombo.getLUT(), invertButton.isSelected());
            MovieDisplay.display();
        };
        lutCombo.addActionListener(listener);
        invertButton.addActionListener(listener);

        JideToggleButton enhanceButton = new JideToggleButton(Buttons.corona, layer.getGLImage().getEnhanced());
        enhanceButton.setToolTipText("Enhance off-disk corona");
        enhanceButton.addActionListener(e -> {
            layer.getGLImage().setEnhanced(enhanceButton.isSelected());
            MovieDisplay.display();
        });

        buttonPanel.add(invertButton);
        buttonPanel.add(enhanceButton);
    }

    public void setLUT(LUT lut) {
        lutCombo.setLUT(lut);
    }

    @Override
    public Component getTitle() {
        return new JLabel("Color ", JLabel.RIGHT);
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
