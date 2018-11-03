package org.helioviewer.jhv.gui.components.timeselector;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarEvent;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarListener;
import org.helioviewer.jhv.time.TimeUtils;

@SuppressWarnings("serial")
class DateTimePanel extends JPanel implements ActionListener, JHVCalendarListener {

    private final HashSet<ActionListener> listeners = new HashSet<>();
    private final JHVCalendarDatePicker datePicker = new JHVCalendarDatePicker();
    private final TimeTextField timePicker = new TimeTextField();

    DateTimePanel(String text) {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.BOTH;

        c.gridx = 0;
        JLabel label = new JLabel(text, JLabel.RIGHT);
        label.setPreferredSize(new Dimension(40, 0));
        add(label, c);

        c.weightx = 0.5;
        c.gridx = 1;
        add(datePicker, c);
        c.gridx = 2;
        add(timePicker, c);

        datePicker.addJHVCalendarListener(this);
        timePicker.addActionListener(this);
    }

    long getTime() {
        return datePicker.getTime() + timePicker.getTime();
    }

    void setTime(long time) {
        datePicker.setTime(TimeUtils.floorDay(time));
        timePicker.setText(TimeUtils.formatTime(TimeUtils.floorSec(time)));
    }

    void addListener(ActionListener l) {
        listeners.add(l);
    }

    void removeListener(ActionListener l) {
        listeners.remove(l);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for (ActionListener l : listeners) {
            l.actionPerformed(e);
        }
    }

    @Override
    public void actionPerformed(JHVCalendarEvent e) {
        actionPerformed(new ActionEvent(this, 0, null));
    }

}
