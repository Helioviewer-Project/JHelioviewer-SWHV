package org.helioviewer.jhv.layers.selector;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.helioviewer.jhv.astronomy.Frame;
import org.helioviewer.jhv.gui.components.base.JHVSlider;
import org.helioviewer.jhv.gui.components.timeselector.TimeSelectorPanel;
import org.helioviewer.jhv.layers.ViewpointLayerOptionsExpert;

@SuppressWarnings("serial")
final class ViewpointLayerOptionsExpertPanel extends JPanel {

    private final ViewpointLayerOptionsExpert options;
    private final TimeSelectorPanel timeSelectorPanel = new TimeSelectorPanel();

    ViewpointLayerOptionsExpertPanel(ViewpointLayerOptionsExpert _options) {
        options = _options;

        JCheckBox syncCheckBox = new JCheckBox("Use movie time interval", options.isSyncInterval());
        syncCheckBox.addActionListener(e -> {
            options.setSyncInterval(syncCheckBox.isSelected());
            syncTimeSelector();
        });

        timeSelectorPanel.setTime(options.getStartTime(), options.getEndTime());
        timeSelectorPanel.setVisible(!options.isSyncInterval());
        timeSelectorPanel.addListener(options::setTimeSelection);

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1.;
        c.weighty = 1.;
        c.gridx = 0;
        c.fill = GridBagConstraints.BOTH;

        c.gridy = 0;
        add(syncCheckBox, c);
        c.gridy = 1;
        add(timeSelectorPanel, c);
        c.gridy = 2;
        add(options.getContainer(), c);
        if (options.hasFrameOptions()) {
            c.gridy = 3;
            add(createFramePanel(), c);
            c.gridy = 4;
            add(createSpiralPanel(), c);
        }
    }

    private void syncTimeSelector() {
        timeSelectorPanel.setVisible(!options.isSyncInterval());
        timeSelectorPanel.setTime(options.getStartTime(), options.getEndTime());
    }

    private JPanel createFramePanel() {
        JPanel framePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        framePanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        framePanel.add(new JLabel("Frame", JLabel.RIGHT));

        ButtonGroup modeGroup = new ButtonGroup();
        for (Frame frame : List.of(Frame.SOLO_HCI, Frame.SOLO_HEEQ, Frame.SOLO_HEE)) {
            JRadioButton radio = new JRadioButton(frame.uiString(), frame == options.getFrame());
            radio.addItemListener(e -> {
                if (radio.isSelected())
                    options.setFrame(frame);
            });
            framePanel.add(radio);
            modeGroup.add(radio);
        }

        JCheckBox relativeCheckBox = new JCheckBox("Relative longitude", options.isRelative());
        relativeCheckBox.addActionListener(e -> options.setRelative(relativeCheckBox.isSelected()));
        framePanel.add(relativeCheckBox);
        return framePanel;
    }

    private JPanel createSpiralPanel() {
        JCheckBox spiralCheckBox = new JCheckBox("Spiral", options.isSpiral());
        spiralCheckBox.addActionListener(e -> options.setSpiral(spiralCheckBox.isSelected()));

        JLabel spiralLabel = new JLabel(options.getSpiralSpeedValue() + " km/s");
        JHVSlider spiralSlider = new JHVSlider(ViewpointLayerOptionsExpert.MIN_SPEED_SPIRAL, ViewpointLayerOptionsExpert.MAX_SPEED_SPIRAL, options.getSpiralSpeedValue());
        spiralSlider.addChangeListener(e -> {
            options.setSpiralSpeed(spiralSlider.getValue());
            spiralLabel.setText(options.getSpiralSpeedValue() + " km/s");
        });

        JPanel spiralPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        spiralPanel.add(spiralCheckBox);
        spiralPanel.add(spiralSlider);
        spiralPanel.add(spiralLabel);
        return spiralPanel;
    }

}
