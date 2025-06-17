package org.helioviewer.jhv.layers.filters;

import java.awt.Component;

import javax.swing.JLabel;

import org.helioviewer.jhv.gui.components.base.JHVSlider;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.opengl.GLImage;

public class DeltaCRVAL2Panel implements FilterDetails {

    private final JHVSlider slider;
    private final JLabel label;
    private final JLabel title = new JLabel("\u03B4CRVAL2", JLabel.RIGHT);

    public DeltaCRVAL2Panel(ImageLayer layer) {
        slider = new JHVSlider(GLImage.MIN_DCRVAL2, GLImage.MAX_DCRVAL2, layer.getGLImage().getDeltaCRVAL2());
        label = new JLabel(formatInt(slider.getValue()), JLabel.RIGHT);
        slider.addChangeListener(e -> {
            int value = slider.getValue();
            layer.getGLImage().setDeltaCRVAL2(value);
            label.setText(formatInt(value));
            MovieDisplay.display();
        });
    }

    private static String formatInt(int value) {
        return "<html><p align='right'>" + value + "\u2033</p>";
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
