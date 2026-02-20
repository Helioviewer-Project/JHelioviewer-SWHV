package org.helioviewer.jhv.gui.components.timeselector;

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
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;

import com.jidesoft.swing.JideButton;

@SuppressWarnings("serial")
class CarringtonPicker extends JideButton implements PopupMenuListener {

    private final ArrayList<CalendarListener> listeners = new ArrayList<>();
    private final JPopupMenu popup = new JPopupMenu();
    private final MenuScroller scroller = new MenuScroller(popup, 15, 100);

    private long time;

    CarringtonPicker() {
        setText("CR");
        setToolTipText("Select Carrington rotation");

        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < Carrington.CR_start.length; i++) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(Integer.toString(i + Carrington.CR_MINIMAL));
            item.addActionListener(e -> setTimeFromCarrington(Carrington.CR_start[Integer.parseInt(item.getText()) - Carrington.CR_MINIMAL]));
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
        int cr = (int) Math.round(Carrington.time2CR(new JHVTime(time)) - Carrington.CR_MINIMAL);
        cr = Math.clamp(cr, 0, Carrington.CR_start.length - 1);

        Component component = popup.getComponent(cr);
        if (component instanceof JMenuItem jmi) {
            jmi.setSelected(true);
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

    private void setTimeFromCarrington(long _time) {
        setTime(_time);
        listeners.forEach(CalendarListener::calendarAction);
    }

    void setTime(long _time) {
        if (_time >= TimeUtils.MINIMAL_TIME.milli && _time <= TimeUtils.MAXIMAL_TIME.milli)
            time = _time;
    }

    long getTime() {
        return time;
    }

    void addListener(CalendarListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

}
