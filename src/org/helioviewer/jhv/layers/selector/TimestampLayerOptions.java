package org.helioviewer.jhv.layers.selector;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;

import org.helioviewer.jhv.gui.component.FormPanel;
import org.helioviewer.jhv.gui.component.JHVSlider;
import org.helioviewer.jhv.layers.TimestampLayer;

@SuppressWarnings("serial")
final class TimestampLayerOptions extends FormPanel {

    TimestampLayerOptions(TimestampLayer layer) {
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        JHVSlider slider = new JHVSlider(TimestampLayer.MIN_SCALE, TimestampLayer.MAX_SCALE, layer.getScale());
        slider.addChangeListener(e -> layer.setScale(slider.getValue()));

        JCheckBox showExtra = new JCheckBox("Extra info", layer.isExtra());
        showExtra.addActionListener(e -> layer.setExtra(showExtra.isSelected()));

        JCheckBox showTop = new JCheckBox("Top", layer.isTop());
        showTop.addActionListener(e -> layer.setTop(showTop.isSelected()));

        addRow("Size", slider);
        addRow("", showExtra, showTop);
    }

}
