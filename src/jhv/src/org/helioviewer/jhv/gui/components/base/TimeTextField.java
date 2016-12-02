package org.helioviewer.jhv.gui.components.base;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.ParseException;

import javax.swing.JTextField;

import org.helioviewer.jhv.base.time.TimeUtils;

@SuppressWarnings("serial")
public class TimeTextField extends JTextField {

    private static final String defaultTime = "00:00:00";

    public TimeTextField() {
        super(defaultTime);
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                setText(TimeUtils.timeDateFormat.format(getTime()));
            }
        });
    }

    public long getTime() {
        String time = getText();
        try {
            return TimeUtils.timeDateFormat.parse(time).getTime();
        } catch (ParseException e) {
            try {
                return TimeUtils.timeDateFormat.parse(time + ":00").getTime();
            } catch (ParseException e2) {
                try {
                    return TimeUtils.timeDateFormat.parse(time + ":00:00").getTime();
                } catch (ParseException e3) {
                    try {
                        return TimeUtils.timeDateFormat.parse(defaultTime).getTime();
                    } catch (ParseException e4) {
                        return 0;
                    }
                }
            }
        }
    }

}
