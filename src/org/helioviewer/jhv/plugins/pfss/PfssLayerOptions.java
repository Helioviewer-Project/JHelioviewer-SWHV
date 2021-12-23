package org.helioviewer.jhv.plugins.pfss;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.components.base.JHVSpinner;
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

        JHVSpinner levelSpinner = new JHVSpinner(detail, 0, PfssSettings.MAX_DETAIL, 1);
        levelSpinner.addChangeListener(e -> {
            detail = (Integer) levelSpinner.getValue();
            MovieDisplay.display();
        });

        JHVSpinner radiusSpinner = new JHVSpinner(radius, 1.099999999999999, PfssSettings.MAX_RADIUS, 0.1);
        radiusSpinner.addChangeListener(e -> {
            radius = (Double) radiusSpinner.getValue();
            MovieDisplay.display();
        });

        JPanel detailPanel = new JPanel();
        detailPanel.add(new JLabel("Detail", JLabel.RIGHT));
        detailPanel.add(levelSpinner);

        JPanel radiusPanel = new JPanel();
        radiusPanel.add(new JLabel("Radius", JLabel.RIGHT));
        radiusPanel.add(radiusSpinner);

        GridBagConstraints c0 = new GridBagConstraints();
        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.gridy = 0;

        c0.gridx = 0;
        c0.anchor = GridBagConstraints.LINE_START;
        add(detailPanel, c0);

        c0.gridx = 1;
        c0.anchor = GridBagConstraints.LINE_START;
        add(radiusPanel, c0);

        JCheckBox fixedColors = new JCheckBox("Fixed colors", fixedColor);
        fixedColors.setHorizontalTextPosition(SwingConstants.LEFT);
        fixedColors.addActionListener(e -> {
            fixedColor = fixedColors.isSelected();
            MovieDisplay.display();
        });

        c0.gridx = 2;
        c0.anchor = GridBagConstraints.LINE_END;
        add(fixedColors, c0);

        JButton availabilityButton = new JButton("Available data");
        availabilityButton.setToolTipText("Click here to check the PFSS data availability");
        availabilityButton.addActionListener(e -> JHVGlobals.openURL(PfssSettings.AVAILABILITY_URL));

        c0.gridy = 1;
        c0.anchor = GridBagConstraints.LINE_START;
        c0.gridx = 0;
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
