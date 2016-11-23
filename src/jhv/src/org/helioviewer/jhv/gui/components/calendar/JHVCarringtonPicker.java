package org.helioviewer.jhv.gui.components.calendar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.HashSet;

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

@SuppressWarnings("serial")
public class JHVCarringtonPicker extends JPanel implements FocusListener, ActionListener, JHVCalendarListener {

    private final HashSet<JHVCalendarListener> listeners = new HashSet<>();

    private final JButton crPopupButton;
    private Popup crPopup = null;
    private JHVCarrington carringtonPanel = null;
    private long time;

    public JHVCarringtonPicker() {
        setLayout(new BorderLayout());

        crPopupButton = new JButton("CR");
        crPopupButton.setMargin(new Insets(0, 0, 0, 0));
        FontMetrics fm = crPopupButton.getFontMetrics(crPopupButton.getFont());
        crPopupButton.setPreferredSize(new Dimension(fm.stringWidth("CR") + 14, fm.getHeight()));
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
            carringtonPanel = null;
        }
        // inform all listeners of this class that a new date was choosen by the user
        informAllJHVCalendarListeners();
    }

    public void addJHVCalendarListener(JHVCalendarListener l) {
        listeners.add(l);
    }

    public void removeJHVCalendarListener(JHVCalendarListener l) {
        listeners.remove(l);
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
        carringtonPanel = new JHVCarrington();
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

    private class JHVCarrington extends JPanel {

        private final HashSet<JHVCalendarListener> listeners = new HashSet<>();
        private final JComboBox<Integer> crCombo = new JComboBox<>(createCRList());

        public JHVCarrington() {
            setLayout(new BorderLayout());

            double cr = Carrington.time2CR(new JHVDate(time)) - Carrington.CR_MINIMAL;
            crCombo.setSelectedIndex((int) Math.round(cr));
            crCombo.addActionListener(e -> {
                time = Carrington.CR_start[crCombo.getSelectedIndex()];
                informAllJHVCalendarListeners();
            });
            add(crCombo);
        }

        public void addJHVCalendarListener(JHVCalendarListener l) {
            listeners.add(l);
        }

        public void removeJHVCalendarListener(JHVCalendarListener l) {
            listeners.remove(l);
        }

        private void informAllJHVCalendarListeners() {
            JHVCalendarEvent e = new JHVCalendarEvent(this);
            for (JHVCalendarListener l : listeners) {
                l.actionPerformed(e);
            }
        }

        private Integer[] createCRList() {
            Integer[] cr = new Integer[Carrington.CR_start.length];
            for (int i = 0; i < cr.length; i++) {
                cr[i] = i + Carrington.CR_MINIMAL;
            }
            return cr;
        }

    }

}
