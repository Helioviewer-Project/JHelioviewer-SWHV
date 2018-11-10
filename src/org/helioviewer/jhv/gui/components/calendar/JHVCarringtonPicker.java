package org.helioviewer.jhv.gui.components.calendar;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.gui.components.base.MenuScroller;
import org.helioviewer.jhv.gui.components.base.JHVButton;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.time.TimeUtils;

@SuppressWarnings("serial")
public class JHVCarringtonPicker extends JHVButton implements PopupMenuListener {

    private final ArrayList<CalendarListener> listeners = new ArrayList<>();
    private final JPopupMenu popup = new JPopupMenu();
    private final MenuScroller scroller = new MenuScroller(popup, 15, 100);

    private long time;

    public JHVCarringtonPicker() {
        setText("CR");
        setToolTipText("Select Carrington rotation");

        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < Carrington.CR_start.length; i++) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(Integer.toString(i + Carrington.CR_MINIMAL));
            item.addActionListener(e -> setTimeFromCarrington(Carrington.CR_start[Integer.valueOf(item.getText()) - Carrington.CR_MINIMAL]));
            group.add(item);
            popup.add(item);
        }
        popup.addPopupMenuListener(this);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                popup.show(e.getComponent(), 0, e.getComponent().getHeight());
            }
        });
    }

    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        int cr = (int) Math.round(Carrington.time2CR(new JHVDate(time)) - Carrington.CR_MINIMAL);
        Component component = popup.getComponent(cr);
        if (component instanceof JMenuItem) {
            ((JMenuItem) component).setSelected(true);
            setTimeFromCarrington(Carrington.CR_start[cr]);
        }
        scroller.keepVisible(cr + 1);
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
    }

    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {
    }

    public void addCalendarListener(CalendarListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    private void setTimeFromCarrington(long _time) {
        setTime(_time);
        informCalendarListeners();
    }

    public void setTime(long _time) {
        if (_time > TimeUtils.MINIMAL_DATE.milli && _time < TimeUtils.MAXIMAL_DATE.milli) {
            time = _time;
        }
    }

    public long getTime() {
        return time;
    }

    private void informCalendarListeners() {
        listeners.forEach(CalendarListener::calendarAction);
    }

}
