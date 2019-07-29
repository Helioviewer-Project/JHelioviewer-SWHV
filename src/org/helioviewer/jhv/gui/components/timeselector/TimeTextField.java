package org.helioviewer.jhv.gui.components.timeselector;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JTextField;

import org.helioviewer.jhv.time.TimeUtils;
import org.apache.commons.validator.routines.IntegerValidator;

@SuppressWarnings("serial")
class TimeTextField extends JTextField {

    private static final String defaultTime = "00:00:00";
    private static final IntegerValidator validator = IntegerValidator.getInstance();

    TimeTextField() {
        super(defaultTime);
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                setText(TimeUtils.formatTime(getTime()));
            }
        });
    }

    private static String fix(String in, int clip) {
        Integer v = validator.validate(in);
        if (v == null || v < 0)
            return "00";
        if (v < 10)
            return String.format("%02d", v);
        if (v > clip)
            return String.format("%d", clip);
        return in;
    }

    long getTime() {
        String text = getText();
        if (text == null)
            return 0;

        String[] parts = text.split(":", 3);
        String h = fix(parts[0], 23);
        String m = "00";
        String s = "00";

        if (parts.length > 1)
            m = fix(parts[1], 59);
        if (parts.length > 2)
            s = fix(parts[2], 59);

        try {
            return TimeUtils.parseTime(String.join(":", h, m, s));
        } catch (Exception e) {
            return 0;
        }
    }

}
