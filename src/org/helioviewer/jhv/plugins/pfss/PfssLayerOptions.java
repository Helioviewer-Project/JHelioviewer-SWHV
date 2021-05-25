package org.helioviewer.jhv.plugins.pfss;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.layers.MovieDisplay;

@SuppressWarnings("serial")
class PfssLayerOptions extends JPanel {

    private int detail;
    private boolean fixedColor;
    private double radius;

    PfssLayerOptions(int _detail, boolean _fixedColor, double _radius) {
        detail = _detail;
        fixedColor = _fixedColor;
        radius = _radius;
        setLayout(new GridBagLayout());

        JSpinner levelSpinner = new JSpinner(new SpinnerNumberModel(detail, 0, PfssSettings.MAX_DETAIL, 1));
        levelSpinner.addChangeListener(e -> {
            detail = (Integer) levelSpinner.getValue();
            MovieDisplay.display();
        });
        WheelSupport.installMouseWheelSupport(levelSpinner);

        JSpinner radiusSpinner = new JSpinner(new SpinnerNumberModel(radius, 1.099999999999999, PfssSettings.MAX_RADIUS, 0.1));
        radiusSpinner.addChangeListener(e -> {
            radius = (Double) radiusSpinner.getValue();
            MovieDisplay.display();
        });
        WheelSupport.installMouseWheelSupport(levelSpinner);

        GridBagConstraints c0 = new GridBagConstraints();
        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.gridy = 0;

        c0.gridx = 0;
        c0.anchor = GridBagConstraints.LINE_END;
        add(new JLabel("Detail", JLabel.RIGHT), c0);

        c0.gridx = 1;
        c0.anchor = GridBagConstraints.LINE_START;
        add(levelSpinner, c0);

        c0.gridx = 2;
        c0.anchor = GridBagConstraints.LINE_END;
        add(new JLabel("Radius", JLabel.RIGHT), c0);

        c0.gridx = 3;
        c0.anchor = GridBagConstraints.LINE_START;
        add(radiusSpinner, c0);

        JCheckBox fixedColors = new JCheckBox("Fixed colors", fixedColor);
        fixedColors.addActionListener(e -> {
            fixedColor = fixedColors.isSelected();
            MovieDisplay.display();
        });

        c0.gridx = 4;
        c0.anchor = GridBagConstraints.LINE_START;
        add(fixedColors, c0);

        JButton availabilityButton = new JButton("Available data");
        availabilityButton.setToolTipText("Click here to check the PFSS data availability");
        availabilityButton.addActionListener(e -> JHVGlobals.openURL(PfssSettings.availabilityURL));

        c0.gridy = 1;
        c0.anchor = GridBagConstraints.LINE_START;
        c0.gridx = 4;
        add(availabilityButton, c0);
    }

    int getDetail() {
        return detail;
    }

    boolean getFixedColor() {
        return fixedColor;
    }

    double getRadius() {
        return radius;
    }

}
