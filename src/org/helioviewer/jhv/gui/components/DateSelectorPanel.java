package org.helioviewer.jhv.gui.components;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.components.calendar.JHVCarringtonPicker;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.time.TimeUtils;

@SuppressWarnings("serial")
public class DateSelectorPanel extends JPanel {

    private final JHVCalendarDatePicker startDatePicker = new JHVCalendarDatePicker();
    private final JHVCalendarDatePicker endDatePicker = new JHVCalendarDatePicker();
    private final JHVCarringtonPicker carrington = new JHVCarringtonPicker();

    public DateSelectorPanel() {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        c.gridy = 0;

        c.gridx = 0;
        c.weightx = 0.5;
        add(startDatePicker, c);
        c.gridx = 1;
        c.weightx = 0.5;
        add(endDatePicker, c);
        c.gridx = 2;
        c.weightx = 0;
        add(carrington, c);

        startDatePicker.addJHVCalendarListener(e -> setStartTime(startDatePicker.getTime()));
        endDatePicker.addJHVCalendarListener(e -> setEndTime(endDatePicker.getTime()));
        carrington.addJHVCalendarListener(e -> {
            long time = carrington.getTime();
            setStartTime(time);
            int cr = (int) Math.round(Carrington.time2CR(new JHVDate(time)) - Carrington.CR_MINIMAL) + 1;
            setEndTime(Carrington.CR_start[Math.min(cr, Carrington.CR_start.length - 1)]);
        });
    }

    public void setStartTime(long time) {
        carrington.setTime(time);
        time -= time % TimeUtils.DAY_IN_MILLIS;
        startDatePicker.setTime(time);
    }

    public void setEndTime(long time) {
        time -= time % TimeUtils.DAY_IN_MILLIS;
        endDatePicker.setTime(time);
    }

    public long getStartTime() {
        return startDatePicker.getTime();
    }

    public long getEndTime() {
        long time = endDatePicker.getTime() + TimeUtils.DAY_IN_MILLIS - 1; // till end of day
        return time;
    }

}
