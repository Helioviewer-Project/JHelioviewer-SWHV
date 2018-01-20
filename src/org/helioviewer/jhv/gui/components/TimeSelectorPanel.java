package org.helioviewer.jhv.gui.components;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

import org.helioviewer.jhv.gui.components.calendar.JHVCarringtonPicker;
import org.helioviewer.jhv.time.TimeUtils;

@SuppressWarnings("serial")
public class TimeSelectorPanel extends JPanel {

    private final DateTimePanel startDateTimePanel = new DateTimePanel("Start");
    private final DateTimePanel endDateTimePanel = new DateTimePanel("End");
    private final JHVCarringtonPicker startCarrington = new JHVCarringtonPicker();
    private final JHVCarringtonPicker endCarrington = new JHVCarringtonPicker();

    public TimeSelectorPanel() {
        long milli = TimeUtils.START.milli;
        setStartTime(milli - 2 * TimeUtils.DAY_IN_MILLIS);
        setEndTime(milli);

        startDateTimePanel.addListener(e -> setStartTime(startDateTimePanel.getTime()));
        endDateTimePanel.addListener(e -> setEndTime(endDateTimePanel.getTime()));
        startCarrington.addJHVCalendarListener(e -> setStartTime(startCarrington.getTime()));
        endCarrington.addJHVCalendarListener(e -> setEndTime(endCarrington.getTime()));

        startDateTimePanel.add(startCarrington);
        endDateTimePanel.add(endCarrington);

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;

        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 1;
        add(startDateTimePanel, c);
        c.gridx = 1;
        c.weightx = 0;
        add(startCarrington, c);

        c.gridy = 1;
        c.gridx = 0;
        c.weightx = 1;
        add(endDateTimePanel, c);
        c.gridx = 1;
        c.weightx = 0;
        add(endCarrington, c);
    }

    public void setStartTime(long time) {
        startDateTimePanel.setTime(time);
        startCarrington.setTime(time);
    }

    public void setEndTime(long time) {
        endDateTimePanel.setTime(time);
        endCarrington.setTime(time);
    }

    public long getStartTime() {
        return startDateTimePanel.getTime();
    }

    public long getEndTime() {
        return endDateTimePanel.getTime();
    }

}
