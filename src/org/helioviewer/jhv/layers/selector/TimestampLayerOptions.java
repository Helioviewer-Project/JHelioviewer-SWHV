package org.helioviewer.jhv.layers.selector;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.gui.components.base.JHVSlider;
import org.helioviewer.jhv.layers.TimestampLayer;

@SuppressWarnings("serial")
final class TimestampLayerOptions extends JPanel implements LayerOptions.OptionsPanel {

    TimestampLayerOptions(TimestampLayer layer) {
        JHVSlider slider = new JHVSlider(TimestampLayer.MIN_SCALE, TimestampLayer.MAX_SCALE, layer.getScale());
        slider.addChangeListener(e -> layer.setScale(slider.getValue()));

        JPanel panelSlider = new JPanel(new GridBagLayout());
        GridBagConstraints c0 = new GridBagConstraints();
        c0.weightx = 1;
        c0.weighty = 1;
        c0.gridy = 0;
        c0.anchor = GridBagConstraints.LINE_END;
        c0.gridx = 0;
        panelSlider.add(new JLabel("Size", JLabel.RIGHT), c0);
        c0.anchor = GridBagConstraints.LINE_START;
        c0.gridx = 1;
        panelSlider.add(slider, c0);

        JPanel panelCheck = new JPanel(new GridBagLayout());
        GridBagConstraints c1 = new GridBagConstraints();
        c1.weightx = 1;
        c1.weighty = 1;
        c1.gridy = 0;
        c1.anchor = GridBagConstraints.LINE_END;
        c1.gridx = 0;
        JCheckBox showExtra = new JCheckBox("Extra info", layer.isExtra());
        showExtra.setHorizontalTextPosition(SwingConstants.LEFT);
        showExtra.addActionListener(e -> layer.setExtra(showExtra.isSelected()));
        panelCheck.add(showExtra, c1);

        c1.anchor = GridBagConstraints.LINE_START;
        c1.gridx = 1;
        JCheckBox showTop = new JCheckBox("Top", layer.isTop());
        showTop.setHorizontalTextPosition(SwingConstants.LEFT);
        showTop.addActionListener(e -> layer.setTop(showTop.isSelected()));
        panelCheck.add(showTop, c1);

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(panelSlider);
        add(panelCheck);
    }

}
