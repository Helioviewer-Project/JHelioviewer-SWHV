package org.helioviewer.jhv.gui.time;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;

import com.jidesoft.swing.JideButton;

@SuppressWarnings("serial")
class CarringtonPicker extends JideButton {

    private final ArrayList<CalendarListener> listeners = new ArrayList<>();
    private final JPopupMenu popup = new JPopupMenu();
    private final JList<Integer> list = new JList<>(new AbstractListModel<>() {
        @Override
        public int getSize() {
            return Carrington.CR_start.length;
        }

        @Override
        public Integer getElementAt(int index) {
            return index + Carrington.CR_MINIMAL;
        }
    });

    private long time;

    CarringtonPicker() {
        setText("CR");
        setToolTipText("Select Carrington rotation");

        list.setVisibleRowCount(15);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                list.setToolTipText(index >= 0 ? TimeUtils.format(Carrington.CR_start[index]) : null);
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (index >= 0) {
                    setTimeFromCarrington(Carrington.CR_start[index]);
                    popup.setVisible(false);
                }
            }
        });
        list.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "selectCarrington");
        list.getActionMap().put("selectCarrington", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = list.getSelectedIndex();
                if (index >= 0) {
                    setTimeFromCarrington(Carrington.CR_start[index]);
                    popup.setVisible(false);
                }
            }
        });

        popup.add(new JScrollPane(list));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int cr = (int) Math.round(Carrington.time2CR(new JHVTime(time)) - Carrington.CR_MINIMAL);
                cr = Math.clamp(cr, 0, Carrington.CR_start.length - 1);
                list.setSelectedIndex(cr);
                list.ensureIndexIsVisible(cr);
                // setTimeFromCarrington(Carrington.CR_start[cr]); // snap TimeSelector to current CR
                popup.show(e.getComponent(), 0, e.getComponent().getHeight());
            }
        });
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
