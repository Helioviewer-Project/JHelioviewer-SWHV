package org.helioviewer.jhv.layers.selector;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.gui.component.JHVSlider;
import org.helioviewer.jhv.layers.TimestampLayer;

@SuppressWarnings("serial")
final class TimestampLayerOptions extends JPanel {

    TimestampLayerOptions(TimestampLayer layer) {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        JHVSlider slider = new JHVSlider(TimestampLayer.MIN_SCALE, TimestampLayer.MAX_SCALE, layer.getScale());
        slider.addChangeListener(e -> layer.setScale(slider.getValue()));

        add(new JLabel("Size ", JLabel.RIGHT), BorderLayout.LINE_START);
        add(slider, BorderLayout.CENTER);
        add(createCheckboxes(layer), BorderLayout.SOUTH);
    }

    private static JPanel createCheckboxes(TimestampLayer layer) {
        JCheckBox showExtra = new JCheckBox("Extra info", layer.isExtra());
        showExtra.setHorizontalTextPosition(SwingConstants.LEFT);
        showExtra.addActionListener(e -> layer.setExtra(showExtra.isSelected()));

        JCheckBox showTop = new JCheckBox("Top", layer.isTop());
        showTop.setHorizontalTextPosition(SwingConstants.LEFT);
        showTop.addActionListener(e -> layer.setTop(showTop.isSelected()));

        JPanel checkboxes = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        checkboxes.add(showExtra);
        checkboxes.add(showTop);
        return checkboxes;
    }

}
