package org.helioviewer.jhv.gui.components.calendar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.AbstractList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Popup;
import javax.swing.PopupFactory;

import org.helioviewer.jhv.base.astronomy.Carrington;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.base.time.TimeUtils;

public class JHVCarringtonPicker extends JPanel implements FocusListener, ActionListener, JHVCalendarListener {
    private final AbstractList<JHVCalendarListener> listeners = new LinkedList<JHVCalendarListener>();

    private final JButton crPopupButton;
    private Popup crPopup = null;
    private JHVCarrington carringtonPanel = null;
    private final Calendar calendar = new GregorianCalendar();
    private final boolean isEndDate;

    public JHVCarringtonPicker(boolean isEnd) {
        isEndDate = isEnd;

        crPopupButton = new JButton("CR");
        // crPopupButton.setPreferredSize();
        crPopupButton.addFocusListener(this);
        crPopupButton.addActionListener(this);
        add(crPopupButton);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // open or close the popup window when the event was fired by the
        // corresponding popup button
        if (e.getSource() == crPopupButton) {
            if (crPopup == null) {
                showCRPopup();
            } else {
                hideCRPopup();
            }
        }
    }

    @Override
    public void actionPerformed(JHVCalendarEvent e) {
        if (e.getSource().equals(carringtonPanel)) {
            // close popup
            hideCRPopup();
            // set selected date
            setDate(carringtonPanel.getDate());
            carringtonPanel = null;
        }
        // inform all listeners of this class that a new date was choosen by the
        // user
        informAllJHVCalendarListeners(new JHVCalendarEvent(this));
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

    public void setDate(Date date) {
        if (date != null) {
            long milli = date.getTime();
            if (milli > TimeUtils.MINIMAL_DATE.milli && milli < TimeUtils.MAXIMAL_DATE.milli) {
                calendar.setTime(date);
            }
        }
    }

    public Date getDate() {
        return calendar.getTime();
    }

    private void informAllJHVCalendarListeners(JHVCalendarEvent e) {
        for (JHVCalendarListener l : listeners) {
            l.actionPerformed(e);
        }
    }

    @Override
    public void focusGained(FocusEvent arg0) {
    }

    @Override
    public void focusLost(FocusEvent arg0) {
        // has popup button or a subcomponent of jhvCalendar lost the focus?
        if (arg0.getComponent() == crPopupButton || (carringtonPanel != null && carringtonPanel.isAncestorOf(arg0.getComponent()))) {
            // if the receiver of the focus is not a subcomponent of the
            // jhvCalendar than hide the popup
            if (carringtonPanel != null && !carringtonPanel.isAncestorOf(arg0.getOppositeComponent())) {
                hideCRPopup();
            }
        }
    }

    private void hideCRPopup() {
        if (crPopup != null) {
            crPopup.hide();
            crPopup = null;
        }

    }

    private void showCRPopup() {
        // set up the popup content
        carringtonPanel = new JHVCarrington(calendar.getTime(), isEndDate);
        carringtonPanel.setPreferredSize(carringtonPanel.getMinimumSize());
        carringtonPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        carringtonPanel.addJHVCalendarListener(this);
        addFocusListenerToAllChildren(carringtonPanel);

        // get position for popup
        int x = crPopupButton.getLocationOnScreen().x;
        int y = crPopupButton.getLocationOnScreen().y + crPopupButton.getSize().height;

        // create popup
        PopupFactory factory = PopupFactory.getSharedInstance();
        crPopup = factory.getPopup(crPopupButton, carringtonPanel, x, y);
        crPopup.show();

        // carringtonPicker.componentResized(null);

        // correct position of popup when it does not fit into screen area
        x = x + carringtonPanel.getSize().width > Toolkit.getDefaultToolkit().getScreenSize().width ? Toolkit.getDefaultToolkit().getScreenSize().width - carringtonPanel.getSize().width : x;
        x = x < 0 ? 0 : x;

        y = y + carringtonPanel.getSize().height > Toolkit.getDefaultToolkit().getScreenSize().height ? crPopupButton.getLocationOnScreen().y - carringtonPanel.getSize().height : y;
        y = y < 0 ? 0 : y;

        crPopup.hide();

        // show popup
        crPopup = factory.getPopup(crPopupButton, carringtonPanel, x, y);
        crPopup.show();
    }

    /**
     * Adds to all subcomponents of a component the focus listener off this
     * class.
     *
     * @param parent
     *            add focus listener to subcomponents of this parent
     */
    private void addFocusListenerToAllChildren(JComponent parent) {
        for (Component component : parent.getComponents()) {
            if (component.getFocusListeners().length > 0) {
                component.addFocusListener(this);
            }
            if (component instanceof JComponent) {
                addFocusListenerToAllChildren((JComponent) component);
            }
        }
    }

    @SuppressWarnings("serial")
    private static class JHVCarrington extends JPanel implements ActionListener {

        private final AbstractList<JHVCalendarListener> listeners = new LinkedList<JHVCalendarListener>();
        private Date currentDate = new Date();
        private final JComboBox carringtonRotations;
        private final boolean isEndDate;

        public JHVCarrington(Date date, boolean isEnd) {
            isEndDate = isEnd;
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
         * Removes a listener which should not be informed anymore when a date
         * has been selected.
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
         * Informs all listener of this class by passing the corresponding
         * event.
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
            int location = ((Integer) carringtonRotations.getSelectedItem()) - Carrington.CR_MINIMAL;
            if (!isEndDate) {
                currentDate = new Date(Carrington.CR_start[location]);
            } else {

                if ((location + 1) < Carrington.CR_start.length) {
                    currentDate = new Date(Carrington.CR_start[location + 1] - 1000);
                } else {
                    currentDate = new Date(Carrington.CR_start[location]);
                }
            }
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

}
