package org.helioviewer.jhv.layers.selector;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.component.JHVSlider;
import org.helioviewer.jhv.layers.MiniviewLayer;

@SuppressWarnings("serial")
final class MiniviewLayerOptions extends JPanel {

    MiniviewLayerOptions(MiniviewLayer layer) {
        super(new GridBagLayout());

        JHVSlider slider = new JHVSlider(MiniviewLayer.MIN_SCALE, MiniviewLayer.MAX_SCALE, layer.getScale());
        slider.addChangeListener(e -> layer.setScale(slider.getValue()));

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.weighty = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_END;
        c.gridx = 0;
        add(new JLabel("Size", JLabel.RIGHT), c);
        c.anchor = GridBagConstraints.LINE_START;
        c.gridx = 1;
        add(slider, c);
    }

}
