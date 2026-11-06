package org.helioviewer.jhv.layers.selector;

import javax.swing.BorderFactory;

import org.helioviewer.jhv.gui.component.FormPanel;
import org.helioviewer.jhv.gui.component.JHVSlider;
import org.helioviewer.jhv.layers.MiniviewLayer;

@SuppressWarnings("serial")
final class MiniviewLayerOptions extends FormPanel {

    MiniviewLayerOptions(MiniviewLayer layer) {
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        JHVSlider slider = new JHVSlider(MiniviewLayer.MIN_SCALE, MiniviewLayer.MAX_SCALE, layer.getScale());
        slider.addChangeListener(e -> layer.setScale(slider.getValue()));
        addRow("Size", slider);
    }

}
