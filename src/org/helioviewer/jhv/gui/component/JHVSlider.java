package org.helioviewer.jhv.gui.component;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JSlider;

// Slider over [min, max] in steps of 1/scale; values are exact multiples of 1/scale.
@SuppressWarnings("serial")
public final class JHVSlider extends JSlider {

    private final int scale;

    public JHVSlider(int min, int max, int value) {
        this(min, max, value, 1);
    }

    public JHVSlider(double min, double max, double value, int _scale) {
        super(JSlider.HORIZONTAL, toStep(min, _scale), toStep(max, _scale), toStep(value, _scale));
        scale = _scale;
        WheelSupport.installMouseWheelSupport(this);

        int defaultStep = getValue();
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !e.isConsumed()) {
                    e.consume();
                    setValue(defaultStep);
                }
            }
        });
    }

    public double getDoubleValue() {
        return getValue() / (double) scale;
    }

    public void setDoubleValue(double value) {
        setValue(toStep(value, scale));
    }

    static int toStep(double value, int scale) {
        return (int) Math.round(value * scale);
    }

}
