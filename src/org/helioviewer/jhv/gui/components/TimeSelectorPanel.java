package org.helioviewer.jhv.gui.components;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.gui.components.calendar.JHVCarringtonPicker;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.time.TimeUtils;

@SuppressWarnings("serial")
public class TimeSelectorPanel extends JPanel {

    private final DateTimePanel startDateTimePanel = new DateTimePanel("Start");
    private final DateTimePanel endDateTimePanel = new DateTimePanel("End");
    private final JHVCarringtonPicker carrington = new JHVCarringtonPicker();

    public TimeSelectorPanel() {
        long milli = TimeUtils.START.milli;
        setStartTime(milli - 2 * TimeUtils.DAY_IN_MILLIS);
        setEndTime(milli);

        startDateTimePanel.addListener(e -> setStartTime(startDateTimePanel.getTime()));
        endDateTimePanel.addListener(e -> setEndTime(endDateTimePanel.getTime()));
        carrington.addJHVCalendarListener(e -> {
            long time = carrington.getTime();
            setStartTime(time);
            int cr = (int) Math.round(Carrington.time2CR(new JHVDate(time)) - Carrington.CR_MINIMAL) + 1;
            setEndTime(Carrington.CR_start[Math.min(cr, Carrington.CR_start.length - 1)]);
        });

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
        add(carrington, c);

        c.gridy = 1;
        c.gridx = 0;
        c.weightx = 1;
        add(endDateTimePanel, c);
    }

    public void setStartTime(long time) {
        carrington.setTime(time);
        startDateTimePanel.setTime(time);
    }

    public void setEndTime(long time) {
        endDateTimePanel.setTime(time);
    }

    public long getStartTime() {
        return startDateTimePanel.getTime();
    }

    public long getEndTime() {
        return endDateTimePanel.getTime();
    }

}
