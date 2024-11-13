package org.helioviewer.jhv.layers.filters;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;

import org.helioviewer.jhv.gui.components.base.JHVRangeSlider;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.MovieDisplay;

public class LevelsPanel implements FilterDetails {

    private final JHVRangeSlider slider;
    private final JLabel label;
    private final JLabel title = new JLabel("Levels ", JLabel.RIGHT);

    static String formatPercent(int value) {
        return "<html><p align='right'>" + value + "%</p>";
    }

    static String formatPercent(int low, int high) {
        return "<html><p align='right'>" + low + "%</p><p align='right'>" + high + "%</p>";
    }

    static String formatDegree(int low, int high) {
        return "<html><p align='right'>" + low + "\u00B0</p><p align='right'>" + high + "\u00B0</p>";
    }

    public LevelsPanel(ImageLayer layer) {
        double offset = layer.getGLImage().getBrightOffset();
        double scale = layer.getGLImage().getBrightScale();
        int high = (int) (100 * (offset + scale));

        slider = new JHVRangeSlider(-101, 201, (int) (offset * 100), high);
        slider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    slider.setLowValue(0);
                    slider.setHighValue(100);
                }
            }
        });

        label = new JLabel(formatPercent(slider.getLowValue(), slider.getHighValue()), JLabel.RIGHT);
        slider.addChangeListener(e -> {
            int lo = slider.getLowValue();
            int hi = slider.getHighValue();
            layer.getGLImage().setBrightness(lo / 100., (hi - lo) / 100.);
            label.setText(formatPercent(lo, hi));
            MovieDisplay.display();
        });
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

}
