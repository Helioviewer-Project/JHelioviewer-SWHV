package org.helioviewer.jhv.layers.filters;

import java.awt.Component;

import javax.swing.JLabel;

import org.helioviewer.jhv.gui.components.base.JHVSlider;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.opengl.GLImage;

public class DeltaCROTAPanel implements FilterDetails {

    private final JHVSlider slider;
    private final JLabel label;
    private final JLabel title = new JLabel("\u03B4CROTA ", JLabel.RIGHT);

    public DeltaCROTAPanel(ImageLayer layer) {
        slider = new JHVSlider(GLImage.MIN_DCROTA * 10, GLImage.MAX_DCROTA * 10, (int) (layer.getGLImage().getDeltaCROTA() * 10));
        label = new JLabel(formatFloat(slider.getValue() / 10.), JLabel.RIGHT);
        slider.addChangeListener(e -> {
            double value = slider.getValue() / 10.;
            layer.getGLImage().setDeltaCROTA(value);
            label.setText(formatFloat(value));
            MovieDisplay.display();
        });
    }

    private static String formatFloat(double value) {
        return "<html><p align='right'>" + String.format("%.1f", value) + "\u00B0</p>";
    }

    @Override
    public Component getFirst() {
        return title;
    }

    @Override
    public Component getSecond() {
        return slider;
    }

    @Override
    public Component getThird() {
        return label;
    }

    public void setVisible(boolean visible) {
        title.setVisible(visible);
        slider.setVisible(visible);
        label.setVisible(visible);
    }

}
