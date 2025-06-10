package org.helioviewer.jhv.layers.filters;

import java.awt.BorderLayout;
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
    private final JPanel buttonPanel = new JPanel(new BorderLayout());
    private final JLabel title = new JLabel("Color ", JLabel.RIGHT);

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
        buttonPanel.add(invertButton, BorderLayout.LINE_END);
    }

    public void setLUT(LUT lut) {
        lutCombo.setLUT(lut);
    }

    @Override
    public Component getFirst() {
        return title;
    }

    @Override
    public Component getSecond() {
        return lutCombo;
    }

    @Override
    public Component getThird() {
        return buttonPanel;
    }

}
