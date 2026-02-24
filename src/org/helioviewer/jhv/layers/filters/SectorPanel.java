package org.helioviewer.jhv.layers.filters;

import java.awt.Component;

import javax.swing.JLabel;

import org.helioviewer.jhv.gui.components.base.JHVRangeSlider;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.MovieDisplay;

public class SectorPanel implements FilterDetails {

    private final JHVRangeSlider slider;
    private final JLabel label;
    private final JLabel title = new JLabel("Sector", JLabel.RIGHT);

    public SectorPanel(ImageLayer layer) {
        int left = 0; // (int) (layer.getGLImage().getSector0() + .5);
        int right = 0; // (int) (layer.getGLImage().getSector1() + .5);
        slider = new JHVRangeSlider(-180, 180, left, right);

        label = new JLabel(formatDegree(slider.getLowValue(), slider.getHighValue()), JLabel.RIGHT);
        slider.addChangeListener(e -> {
            int lo = slider.getLowValue();
            int hi = slider.getHighValue();
            // layer.getGLImage().setSector(lo, hi);
            label.setText(formatDegree(lo, hi));
            MovieDisplay.display();
        });
    }

    private static String formatDegree(int low, int high) {
        return "<html><p align='right'>" + low + "\u00B0</p><p align='right'>" + high + "\u00B0</p>";
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
