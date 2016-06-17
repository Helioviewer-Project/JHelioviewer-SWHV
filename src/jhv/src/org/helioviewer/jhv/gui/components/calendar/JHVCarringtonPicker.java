package org.helioviewer.jhv.gui.components.calendar;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.AbstractList;
import java.util.Date;
import java.util.LinkedList;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.astronomy.Carrington;
import org.helioviewer.jhv.base.time.JHVDate;

@SuppressWarnings("serial")
public class JHVCarringtonPicker extends JPanel implements ActionListener {

    private final AbstractList<JHVCalendarListener> listeners = new LinkedList<JHVCalendarListener>();
    private Date currentDate = new Date();
    private final JComboBox carringtonRotations;

    public JHVCarringtonPicker(Date date) {
        setLayout(new BorderLayout());

        setMinimumSize(new Dimension(250, 200));

        // add sub components
        JPanel p = new JPanel();
        carringtonRotations = new JComboBox(createCarringtonRotations());
        setDate(date);
        carringtonRotations.addActionListener(this);
        p.add(carringtonRotations);
        add(p, BorderLayout.CENTER);
    }

    /**
     * Sets the current date to the calendar component.
     *
     * @param date
     *            Selected date of the calendar component.
     */
    public void setDate(Date date) {
        currentDate = date;
        carringtonRotations.setSelectedIndex((int) Math.floor(Carrington.time2CR(new JHVDate(currentDate.getTime()))) - Carrington.CR_MINIMAL);
    }

    /**
     * Returns the selected date of the calendar component.
     *
     * @return selected date.
     */
    public Date getDate() {
        return currentDate;
    }

    /**
     * Adds a listener which will be informed when a date has been selected.
     *
     * @param l
     *            listener which has to be informed.
     */
    public void addJHVCalendarListener(JHVCalendarListener l) {
        if (l != null) {
            listeners.add(l);
        }
    }

    /**
     * Removes a listener which should not be informed anymore when a date has
     * been selected.
     *
     * @param l
     *            listener which should not be informed anymore.
     */
    public void removeJHVCalendarListener(JHVCalendarListener l) {
        if (l != null) {
            listeners.remove(l);
        }
    }

    /**
     * Informs all listener of this class by passing the corresponding event.
     *
     * @param e
     *            event
     */
    private void informAllJHVCalendarListeners(JHVCalendarEvent e) {
        for (JHVCalendarListener l : listeners) {
            l.actionPerformed(e);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        currentDate = new Date(Carrington.CR_start[((Integer) carringtonRotations.getSelectedItem()) - Carrington.CR_MINIMAL]);
        informAllJHVCalendarListeners(new JHVCalendarEvent(this));
    }

    private Integer[] createCarringtonRotations() {
        Integer[] cr = new Integer[Carrington.CR_start.length];
        for (int i = 0; i < cr.length; i++) {
            cr[i] = i + Carrington.CR_MINIMAL;
        }
        return cr;
    }
}
