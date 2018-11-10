package org.helioviewer.jhv.gui.components.timeselector;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.components.calendar.CalendarListener;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.time.TimeUtils;

@SuppressWarnings("serial")
class DateTimePanel extends JPanel implements ActionListener, CalendarListener {

    private final ArrayList<ActionListener> listeners = new ArrayList<>();
    private final JHVCalendarDatePicker datePicker = new JHVCalendarDatePicker();
    private final TimeTextField timePicker = new TimeTextField();

    DateTimePanel(String text) {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.BOTH;

        c.gridx = 0;
        JLabel label = new JLabel(text, JLabel.RIGHT);
        label.setPreferredSize(new Dimension(40, -1));
        add(label, c);

        c.weightx = 0.5;
        c.gridx = 1;
        add(datePicker, c);
        c.gridx = 2;
        add(timePicker, c);

        datePicker.addCalendarListener(this);
        timePicker.addActionListener(this);
    }

    long getTime() {
        return datePicker.getTime() + timePicker.getTime();
    }

    void setTime(long time) {
        datePicker.setTime(TimeUtils.floorDay(time));
        timePicker.setText(TimeUtils.formatTime(TimeUtils.floorSec(time)));
    }

    void addListener(ActionListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        listeners.forEach(listener -> listener.actionPerformed(e));
    }

    @Override
    public void calendarAction() {
        actionPerformed(new ActionEvent(this, 0, null));
    }

}
