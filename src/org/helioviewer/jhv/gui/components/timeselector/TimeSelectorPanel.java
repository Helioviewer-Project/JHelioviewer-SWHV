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
    private final TimePanel startTimePanel = new TimePanel();
    private final TimePanel endTimePanel = new TimePanel();
    private final CarringtonPicker carringtonPicker = new CarringtonPicker();

    public TimeSelectorPanel() {
        long milli = TimeUtils.START.milli;
        setTime(milli - 2 * TimeUtils.DAY_IN_MILLIS, milli);

        startTimePanel.addListener(this::timeChanged);
        endTimePanel.addListener(this::timeChanged);
        carringtonPicker.addListener(this::carringtonChanged);

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.weightx = 1;
        c.gridx = 0;
        add(startTimePanel, c);
        c.gridx = 1;
        add(endTimePanel, c);
        c.weightx = 0;
        c.gridx = 2;
        add(carringtonPicker, c);
    }

    private void timeChanged() {
        setTime(getStartTime(), getEndTime());
    }

    private void carringtonChanged() {
        long time = carringtonPicker.getTime();
        int cr = (int) Math.round(Carrington.time2CR(new JHVDate(time)) - Carrington.CR_MINIMAL) + 1;
        setTime(time, Carrington.CR_start[Math.min(cr, Carrington.CR_start.length - 1)]);
    }

    public void setTime(long start, long end) {
        carringtonPicker.setTime(start);
        startTimePanel.setTime(start);
        endTimePanel.setTime(end);

        listeners.forEach(listener -> listener.timeSelectionChanged(start, end));
    }

    public long getStartTime() {
        return startTimePanel.getTime();
    }

    public long getEndTime() {
        return endTimePanel.getTime();
    }

    public void addListener(TimeSelectorListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

}
