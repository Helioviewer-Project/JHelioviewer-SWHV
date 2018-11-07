package org.helioviewer.jhv.gui.components.calendar;

import java.awt.BorderLayout;
import java.util.HashSet;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;

import javax.swing.JPanel;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.gui.components.base.MenuScroller;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.time.TimeUtils;

import com.jidesoft.swing.JideSplitButton;

@SuppressWarnings("serial")
public class JHVCarringtonPicker extends JPanel implements MenuListener {

    private final HashSet<JHVCalendarListener> listeners = new HashSet<>();

    private final JideSplitButton crButton = new JideSplitButton("CR");
    private final MenuScroller menuScroller = new MenuScroller(crButton);
    private long time;

    public JHVCarringtonPicker() {
        setLayout(new BorderLayout());

        crButton.setAlwaysDropdown(true);
        crButton.setToolTipText("Select Carrington rotation");

        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < Carrington.CR_start.length; i++) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(Integer.toString(i + Carrington.CR_MINIMAL));
            item.addActionListener(e -> setTimeFromCarrington(Carrington.CR_start[Integer.valueOf(item.getText()) - Carrington.CR_MINIMAL]));
            group.add(item);
            crButton.add(item);
        }
        crButton.addMenuListener(this);

        add(crButton);
    }

    @Override
    public void menuCanceled(MenuEvent e) {
    }

    @Override
    public void menuDeselected(MenuEvent e) {
    }

    @Override
    public void menuSelected(MenuEvent e) {
        int cr = (int) Math.round(Carrington.time2CR(new JHVDate(time)) - Carrington.CR_MINIMAL);
        menuScroller.keepVisible(cr + 1);
        crButton.getItem(cr).setSelected(true);
    }

    public void addJHVCalendarListener(JHVCalendarListener l) {
        listeners.add(l);
    }

    public void removeJHVCalendarListener(JHVCalendarListener l) {
        listeners.remove(l);
    }

    private void setTimeFromCarrington(long _time) {
        setTime(_time);
        informAllJHVCalendarListeners();
    }

    public void setTime(long _time) {
        if (_time > TimeUtils.MINIMAL_DATE.milli && _time < TimeUtils.MAXIMAL_DATE.milli) {
            time = _time;
        }
    }

    public long getTime() {
        return time;
    }

    private void informAllJHVCalendarListeners() {
        JHVCalendarEvent e = new JHVCalendarEvent(this);
        for (JHVCalendarListener l : listeners) {
            l.actionPerformed(e);
        }
    }

}
