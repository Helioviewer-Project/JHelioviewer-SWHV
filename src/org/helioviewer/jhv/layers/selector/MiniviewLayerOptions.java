package org.helioviewer.jhv.layers.selector;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.component.JHVSlider;
import org.helioviewer.jhv.layers.MiniviewLayer;

@SuppressWarnings("serial")
final class MiniviewLayerOptions extends JPanel {

    MiniviewLayerOptions(MiniviewLayer layer) {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        JHVSlider slider = new JHVSlider(MiniviewLayer.MIN_SCALE, MiniviewLayer.MAX_SCALE, layer.getScale());
        slider.addChangeListener(e -> layer.setScale(slider.getValue()));

        add(new JLabel("Size ", JLabel.RIGHT), BorderLayout.LINE_START);
        add(slider, BorderLayout.CENTER);
    }

}
