package org.helioviewer.jhv.plugins.pfss;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.gui.DesktopIntegration;
import org.helioviewer.jhv.gui.components.base.JHVSpinner;

@SuppressWarnings("serial")
class PfssLayerOptions extends JPanel {

    PfssLayerOptions(PfssLayer layer) {
        setLayout(new GridBagLayout());

        JHVSpinner levelSpinner = new JHVSpinner(layer.getDetail(), 0, PfssSettings.MAX_DETAIL, 1);
        levelSpinner.addChangeListener(e -> layer.setDetail((Integer) levelSpinner.getValue()));

        JHVSpinner radiusSpinner = new JHVSpinner(layer.getRadius(), 1.099999999999999, PfssSettings.MAX_RADIUS, 0.1);
        radiusSpinner.addChangeListener(e -> layer.setRadius((Double) radiusSpinner.getValue()));

        GridBagConstraints c0 = new GridBagConstraints();
        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.gridy = 0;

        c0.gridx = 0;
        c0.anchor = GridBagConstraints.LINE_START;
        add(createSpinnerPanel("Detail", levelSpinner), c0);

        c0.gridx = 1;
        c0.anchor = GridBagConstraints.LINE_START;
        add(createSpinnerPanel("Radius", radiusSpinner), c0);

        JCheckBox fixedColors = new JCheckBox("Fixed colors", layer.getFixedColor());
        fixedColors.setHorizontalTextPosition(SwingConstants.LEFT);
        fixedColors.addActionListener(e -> layer.setFixedColor(fixedColors.isSelected()));

        c0.gridx = 2;
        c0.anchor = GridBagConstraints.LINE_END;
        add(fixedColors, c0);

        JButton availabilityButton = new JButton("Available data");
        availabilityButton.setToolTipText("Click here to check the PFSS data availability");
        availabilityButton.addActionListener(e -> DesktopIntegration.openURL(PfssSettings.AVAILABILITY_URL));

        c0.gridy = 1;
        c0.anchor = GridBagConstraints.LINE_START;
        c0.gridx = 0;
        add(availabilityButton, c0);
    }

    private static JPanel createSpinnerPanel(String label, JHVSpinner spinner) {
        JPanel panel = new JPanel();
        panel.add(new JLabel(label, JLabel.RIGHT));
        panel.add(spinner);
        return panel;
    }

}
