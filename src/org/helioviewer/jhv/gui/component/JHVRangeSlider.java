package org.helioviewer.jhv.gui.component;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import com.jidesoft.swing.RangeSlider;

// Range slider over [min, max] in steps of 1/scale; values are exact multiples of 1/scale.
@SuppressWarnings("serial")
public final class JHVRangeSlider extends RangeSlider {

    private final int scale;

    public JHVRangeSlider(double min, double max, double low, double high, int _scale) {
        super(JHVSlider.toStep(min, _scale), JHVSlider.toStep(max, _scale), JHVSlider.toStep(low, _scale), JHVSlider.toStep(high, _scale));
        scale = _scale;
        setRangeDraggable(true);
        WheelSupport.installMouseWheelSupport(this);

        int defaultLow = getLowValue();
        int defaultHigh = getHighValue();
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !e.isConsumed()) {
                    e.consume();
                    // one model update: a single change event, both ends at once
                    getModel().setRangeProperties(defaultLow, defaultHigh - defaultLow, getMinimum(), getMaximum(), false);
                }
            }
        });
    }

    public double getLowDouble() {
        return getLowValue() / (double) scale;
    }

    public double getHighDouble() {
        return getHighValue() / (double) scale;
    }

}
