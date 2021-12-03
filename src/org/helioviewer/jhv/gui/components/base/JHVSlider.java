package org.helioviewer.jhv.gui.components.base;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JSlider;

@SuppressWarnings("serial")
public class JHVSlider extends JSlider {

    private final int defaultValue;

    public JHVSlider(int min, int max, int value) {
        super(JSlider.HORIZONTAL, min, max, value);

        defaultValue = value;
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !e.isConsumed()) {
                    e.consume();
                    setValue(defaultValue);
                }
            }
        });
        WheelSupport.installMouseWheelSupport(this);
    }

}
