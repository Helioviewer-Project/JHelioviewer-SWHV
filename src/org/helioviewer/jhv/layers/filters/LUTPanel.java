package org.helioviewer.jhv.layers.filters;

import java.awt.Component;
import java.awt.event.ActionListener;

import javax.swing.JLabel;

import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.gui.component.Buttons;
import org.helioviewer.jhv.image.ImageDisplaySettings;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.image.lut.LUTComboBox;
import org.helioviewer.jhv.layers.ImageLayer;

import com.jidesoft.swing.JideToggleButton;

public final class LUTPanel implements FilterDetails {

    private final ImageDisplaySettings settings;
    private final LUTComboBox lutCombo;
    private final JideToggleButton invertButton;
    private final JLabel title = new JLabel("Color ", JLabel.RIGHT);

    public LUTPanel(ImageLayer layer) {
        settings = layer.getDisplaySettings();
        lutCombo = new LUTComboBox();
        invertButton = new JideToggleButton(Buttons.invert, settings.getInvertLUT());
        invertButton.setToolTipText("Invert color table");

        ActionListener listener = e -> {
            LUT lut = lutCombo.getLUT();
            boolean inverted = invertButton.isSelected();
            if (lut.equals(settings.getLUT()) && inverted == settings.getInvertLUT())
                return;
            settings.setLUT(lut, inverted);
            DisplayController.display();
        };
        lutCombo.addActionListener(listener);
        invertButton.addActionListener(listener);
    }

    public void refresh() {
        invertButton.setSelected(settings.getInvertLUT());
        lutCombo.selectLUT(settings.getLUT());
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
        return invertButton;
    }

}
