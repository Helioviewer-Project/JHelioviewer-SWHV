package org.helioviewer.jhv.gui.components.base;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import com.jidesoft.swing.RangeSlider;

@SuppressWarnings("serial")
public final class JHVRangeSlider extends RangeSlider {

    public JHVRangeSlider(int min, int max, int low, int high) {
        super(min, max, low, high);
        setRangeDraggable(true);
        WheelSupport.installMouseWheelSupport(this);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    setLowValue(low);
                    setHighValue(high);
                }
            }
        });
    }

}
