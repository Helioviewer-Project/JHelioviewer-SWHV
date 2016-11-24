package org.helioviewer.jhv.plugins.pfssplugin;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ComponentUtils.SmallPanel;
import org.helioviewer.jhv.gui.components.base.WheelSupport;

@SuppressWarnings("serial")
class PfssPluginPanel extends SmallPanel {

    public PfssPluginPanel() {
        setLayout(new GridBagLayout());

        JSpinner levelSpinner = new JSpinner();
        levelSpinner.setModel(new SpinnerNumberModel(0, 0, 8, 1));
        levelSpinner.addChangeListener(e -> {
            PfssSettings.qualityReduction = 8 - (Integer) levelSpinner.getValue();
            Displayer.display();
        });
        WheelSupport.installMouseWheelSupport(levelSpinner);

        GridBagConstraints c0 = new GridBagConstraints();
        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.gridy = 0;

        c0.gridx = 0;
        c0.anchor = GridBagConstraints.EAST;
        add(new JLabel("Level", JLabel.RIGHT), c0);

        c0.gridx = 1;
        c0.anchor = GridBagConstraints.WEST;
        add(levelSpinner, c0);

        JCheckBox fixedColors = new JCheckBox("Fixed colors", false);
        fixedColors.addItemListener(e -> {
            PfssSettings.fixedColor = (e.getStateChange() == ItemEvent.SELECTED);
            Displayer.display();
        });

        c0.gridx = 2;
        c0.anchor = GridBagConstraints.WEST;
        add(fixedColors, c0);

        JButton availabilityButton = new JButton("Available data");
        availabilityButton.setToolTipText("Click here to check the PFSS data availability");
        availabilityButton.addActionListener(e -> JHVGlobals.openURL(PfssSettings.availabilityURL));

        c0.anchor = GridBagConstraints.EAST;
        c0.gridx = 3;
        add(availabilityButton, c0);

        setSmall();
    }

    @Override
    public void setEnabled(boolean b) {
    }

}
