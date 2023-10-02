package org.helioviewer.jhv.layers.filters;

import java.awt.Component;

import javax.swing.JLabel;

import org.helioviewer.jhv.gui.components.base.JHVSlider;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.opengl.GLImage;

public class InnerMaskPanel implements FilterDetails {

    private final JHVSlider slider;
    private final JLabel label;
    private final JLabel title = new JLabel("Mask", JLabel.RIGHT);

    public InnerMaskPanel(ImageLayer layer) {
        slider = new JHVSlider(0, GLImage.MAX_INNER * 10, (int) (layer.getGLImage().getInnerMask() * 10));
        label = new JLabel(alignFloat(slider.getValue() / 10.), JLabel.RIGHT);
        slider.addChangeListener(e -> {
            double value = slider.getValue() / 10.;
            layer.getGLImage().setInnerMask(value);
            label.setText(alignFloat(value));
            MovieDisplay.display();
        });
    }

    private static String alignFloat(double value) {
        return "<html>\u2007" + String.format("%.1f", value) + "R\u2609";
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public Component getComponent() {
        return slider;
    }

    @Override
    public Component getLabel() {
        return label;
    }

    public void setVisible(boolean visible) {
        title.setVisible(visible);
        slider.setVisible(visible);
        label.setVisible(visible);
    }

}
