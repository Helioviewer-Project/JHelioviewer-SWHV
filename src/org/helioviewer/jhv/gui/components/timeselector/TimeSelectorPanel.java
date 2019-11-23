package org.helioviewer.jhv.gui.components.timeselector;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;

import javax.swing.JPanel;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.time.TimeUtils;

@SuppressWarnings("serial")
public class TimeSelectorPanel extends JPanel {

    private final ArrayList<TimeSelectorListener> listeners = new ArrayList<>();
    private final DateTimePanel startDateTimePanel = new DateTimePanel("Start");
    private final DateTimePanel endDateTimePanel = new DateTimePanel("End");
    private final JHVCarringtonPicker carrington = new JHVCarringtonPicker();

    public TimeSelectorPanel() {
        long milli = TimeUtils.START.milli;
        setTime(milli - 2 * TimeUtils.DAY_IN_MILLIS, milli);

        startDateTimePanel.addListener(this::dateTimeChanged);
        endDateTimePanel.addListener(this::dateTimeChanged);
        carrington.addListener(this::carringtonChanged);

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

    private void dateTimeChanged() {
        setTime(getStartTime(), getEndTime());
    }

    private void carringtonChanged() {
        long time = carrington.getTime();
        int cr = (int) Math.round(Carrington.time2CR(new JHVDate(time)) - Carrington.CR_MINIMAL) + 1;
        setTime(time, Carrington.CR_start[Math.min(cr, Carrington.CR_start.length - 1)]);
    }

    public void setTime(long start, long end) {
        carrington.setTime(start);
        startDateTimePanel.setTime(start);
        endDateTimePanel.setTime(end);

        listeners.forEach(listener -> listener.timeSelectionChanged(start, end));
    }

    public long getStartTime() {
        return startDateTimePanel.getTime();
    }

    public long getEndTime() {
        return endDateTimePanel.getTime();
    }

    public void addListener(TimeSelectorListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

}
