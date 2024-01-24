package org.helioviewer.jhv.gui.components.base;

import com.jidesoft.swing.RangeSlider;

@SuppressWarnings("serial")
public final class JHVRangeSlider extends RangeSlider {

    public JHVRangeSlider(int min, int max, int low, int high) {
        super(min, max, low, high);
        setRangeDraggable(true);
        WheelSupport.installMouseWheelSupport(this);
    }

}
