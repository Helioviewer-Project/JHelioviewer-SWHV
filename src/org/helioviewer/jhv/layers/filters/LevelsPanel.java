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

    private static String align3(int value) {
        if (value < -99)
            return "\u2212" + -value;
        if (value < -9)
            return "\u2007\u2212" + -value;
        if (value < 0)
            return "\u2007\u2007\u2212" + -value;
        if (value < 10)
            return "\u2007\u2007\u2007" + value;
        if (value < 100)
            return "\u2007\u2007" + value;
        return "\u2007" + value;
    }

    static String align(int value) {
        return "<html>" + align3(value) + '%';
    }

    static String format(int low, int high) {
        return "<html>" + align3(low) + "%<br/>" + align3(high) + '%';
    }

    static String formatDegree(int low, int high) {
        return "<html>" + align3(low) + "\u00B0<br/>" + align3(high) + '\u00B0';
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

        label = new JLabel(format(slider.getLowValue(), slider.getHighValue()), JLabel.RIGHT);
        slider.addChangeListener(e -> {
            int lo = slider.getLowValue();
            int hi = slider.getHighValue();
            layer.getGLImage().setBrightness(lo / 100., (hi - lo) / 100.);
            label.setText(format(lo, hi));
            MovieDisplay.display();
        });
    }

    @Override
    public Component getTitle() {
        return new JLabel("Levels", JLabel.RIGHT);
    }

    @Override
    public Component getComponent() {
        return slider;
    }

    @Override
    public Component getLabel() {
        return label;
    }

}
