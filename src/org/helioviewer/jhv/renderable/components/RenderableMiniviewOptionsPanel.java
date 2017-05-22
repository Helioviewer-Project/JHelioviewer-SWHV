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

    public RenderableMiniviewOptionsPanel(RenderableMiniview miniview, int scale, int min_scale, int max_scale) {
        setLayout(new GridBagLayout());

        JSpinner spinner = new JSpinner(new SpinnerNumberModel(Double.valueOf(scale), Double.valueOf(min_scale), Double.valueOf(max_scale), Double.valueOf(1)));
        JFormattedTextField f = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        f.setFormatterFactory(new TerminatedFormatterFactory("%.0f", "%", min_scale, max_scale));

        spinner.addChangeListener(e -> {
            miniview.setScale(((Double) spinner.getValue()).intValue());
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
