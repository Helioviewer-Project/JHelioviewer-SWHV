package org.helioviewer.jhv.layers.fov;

import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.tree.DefaultMutableTreeNode;

import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.base.TerminatedFormatterFactory;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.gui.interfaces.JHVCell;
import org.helioviewer.jhv.layers.MovieDisplay;

@SuppressWarnings("serial")
class FOVPlatform extends DefaultMutableTreeNode implements JHVCell {

    private final JPanel panel;
    private final JSpinner spinnerX;
    private final JSpinner spinnerY;

    FOVPlatform(String name) {
        spinnerX = createSpinner();
        spinnerX.addChangeListener(e -> setCenterX((Double) spinnerX.getValue()));
        spinnerY = createSpinner();
        spinnerY.addChangeListener(e -> setCenterY((Double) spinnerY.getValue()));

        panel = new JPanel(new GridLayout(1, 5, 0, 0));
        panel.setOpaque(false);
        panel.add(new JLabel(name));
        panel.add(new JLabel("  Offpoint X: ", JLabel.RIGHT));
        panel.add(spinnerX);
        panel.add(new JLabel("  Offpoint Y: ", JLabel.RIGHT));
        panel.add(spinnerY);
        ComponentUtils.smallVariant(panel);
    }

    @Override
    public Component getComponent() {
        return panel;
    }

    private static double control2Center(double v) { // v in arcmin
        return Math.tan(v * (Math.PI / 180. / 60.));
    }

    private void setCenterX(double controlX) {
        children().asIterator().forEachRemaining(c -> ((FOVInstrument) c).setCenterX(control2Center(controlX)));
        MovieDisplay.display();
    }

    private void setCenterY(double controlY) {
        children().asIterator().forEachRemaining(c -> ((FOVInstrument) c).setCenterY(control2Center(controlY)));
        MovieDisplay.display();
    }

    private static final double min = -60;
    private static final double max = 60;

    private static JSpinner createSpinner() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, min, max, 0.1));
        JFormattedTextField f = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        f.setFormatterFactory(new TerminatedFormatterFactory("%.2f", "\u2032", min, max));
        WheelSupport.installMouseWheelSupport(spinner);
        return spinner;
    }

}
