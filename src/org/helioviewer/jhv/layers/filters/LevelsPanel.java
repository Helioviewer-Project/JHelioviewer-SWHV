package org.helioviewer.jhv.layers.filters;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;

import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.MovieDisplay;

import com.jidesoft.swing.MultilineLabel;
import com.jidesoft.swing.RangeSlider;

public class LevelsPanel implements FilterDetails {

    private final RangeSlider slider;
    private final MultilineLabel label;

    static String align3(int value) {
        if (value < -99)
            return value + "%";
        if (value < -9)
            return "\u2007" + value + '%';
        if (value < 0)
            return "\u2007\u2007" + value + '%';
        if (value < 10)
            return "\u2007\u2007\u2007" + value + '%';
        if (value < 100)
            return "\u2007\u2007" + value + '%';
        return "\u2007" + value + '%';
    }

    static String format(int low, int high) {
        return align3(low) + '\n' + align3(high);
    }

    public LevelsPanel(ImageLayer layer) {
        double offset = layer.getGLImage().getBrightOffset();
        double scale = layer.getGLImage().getBrightScale();
        int high = (int) (100 * (offset + scale));

        slider = new RangeSlider(-101, 201, (int) (offset * 100), high);
        slider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    slider.setLowValue(0);
                    slider.setHighValue(100);
                }
            }
        });
        slider.setRangeDraggable(true);

        label = new MultilineLabel(format(slider.getLowValue(), slider.getHighValue()));

        slider.addChangeListener(e -> {
            int lo = slider.getLowValue();
            int hi = slider.getHighValue();
            layer.getGLImage().setBrightness(lo / 100., (hi - lo) / 100.);
            label.setText(format(lo, hi));
            MovieDisplay.display();
        });
        WheelSupport.installMouseWheelSupport(slider);
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
