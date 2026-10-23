package org.helioviewer.jhv.layers.filters;

import java.awt.FlowLayout;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.gui.component.Buttons;
import org.helioviewer.jhv.image.ImageDisplaySettings;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.ImageLayers;

import com.jidesoft.swing.JideButton;

public final class DifferencePanel {

    private DifferencePanel() {}

    public static FilterDetails create(ImageLayer layer) {
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        JideButton syncButton = new JideButton(Buttons.sync);
        JLabel title = new JLabel(" Difference ", JLabel.RIGHT);
        ImageDisplaySettings settings = layer.getDisplaySettings();
        ButtonGroup modeGroup = new ButtonGroup();
        for (ImageDisplaySettings.DifferenceMode mode : ImageDisplaySettings.DifferenceMode.values()) {
            JRadioButton item = new JRadioButton(mode.toString());
            if (mode == settings.getDifferenceMode())
                item.setSelected(true);
            item.addActionListener(e -> {
                settings.setDifferenceMode(mode);
                DisplayController.display();
            });
            modeGroup.add(item);
            modePanel.add(item);
        }

        syncButton.setToolTipText("Sync all layers to this layer's time range using selected sampling");
        syncButton.addActionListener(e -> ImageLayers.syncLayersSpan(layer.getStartTime(), layer.getEndTime()));
        return new FilterRow(title, modePanel, syncButton);
    }

}
