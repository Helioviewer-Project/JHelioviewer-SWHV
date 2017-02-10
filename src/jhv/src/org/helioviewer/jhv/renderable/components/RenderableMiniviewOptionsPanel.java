package org.helioviewer.jhv.renderable.components;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.base.TerminatedFormatterFactory;
import org.helioviewer.jhv.gui.components.base.WheelSupport;

@SuppressWarnings("serial")
class RenderableMiniviewOptionsPanel extends JPanel {

    private static final int DEFAULT = 10;
    private static final int MIN = 5;
    private static final int MAX = 15;
    int scale = DEFAULT;

    private final RenderableMiniview miniview;

    public RenderableMiniviewOptionsPanel(RenderableMiniview _miniview) {
        miniview = _miniview;
        setLayout(new GridBagLayout());

        JSpinner spinner = new JSpinner(new SpinnerNumberModel(Double.valueOf(DEFAULT), Double.valueOf(MIN), Double.valueOf(MAX), Double.valueOf(1)));
        JFormattedTextField f = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        f.setFormatterFactory(new TerminatedFormatterFactory("%.0f", "%", MIN, MAX));

        spinner.addChangeListener(e -> {
            scale = ((Double) spinner.getValue()).intValue();
            miniview.reshapeViewport();
            Displayer.display();
        });
        WheelSupport.installMouseWheelSupport(spinner);

        GridBagConstraints c0 = new GridBagConstraints();
        c0.anchor = GridBagConstraints.EAST;
        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.gridy = 0;
        c0.gridx = 0;
        add(new JLabel("Size", JLabel.RIGHT), c0);

        c0.anchor = GridBagConstraints.WEST;
        c0.gridx = 1;
        add(spinner, c0);

        ComponentUtils.smallVariant(this);
    }

}
