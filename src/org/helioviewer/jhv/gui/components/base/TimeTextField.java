package org.helioviewer.jhv.gui.components.base;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JTextField;

import org.helioviewer.jhv.time.TimeUtils;

@SuppressWarnings("serial")
public class TimeTextField extends JTextField {

    private static final String defaultTime = "00:00:00";

    public TimeTextField() {
        super(defaultTime);
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                setText(TimeUtils.formatTime(getTime()));
            }
        });
    }

    public long getTime() {
        String time = getText();
        try {
            return TimeUtils.parseTime(time);
        } catch (Exception e1) {
            try {
                return TimeUtils.parseTime(time + ":00");
            } catch (Exception e2) {
                try {
                    return TimeUtils.parseTime(time + ":00:00");
                } catch (Exception e3) {
                    try {
                        return TimeUtils.parseTime(defaultTime);
                    } catch (Exception e4) {
                        return 0;
                    }
                }
            }
        }
    }

}
