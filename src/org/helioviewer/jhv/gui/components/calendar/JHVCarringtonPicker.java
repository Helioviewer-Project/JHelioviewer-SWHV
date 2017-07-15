package org.helioviewer.jhv.gui.components.calendar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Popup;
import javax.swing.PopupFactory;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.time.TimeUtils;

import com.jidesoft.swing.JideButton;

@SuppressWarnings("serial")
public class JHVCarringtonPicker extends JPanel implements FocusListener {

    private final HashSet<JHVCalendarListener> listeners = new HashSet<>();

    private final JideButton crPopupButton = new JideButton("CR");
    private final JHVCarrington carringtonPanel = new JHVCarrington();
    private Popup crPopup = null;
    private long time;

    public JHVCarringtonPicker() {
        setLayout(new BorderLayout());

        crPopupButton.addFocusListener(this);
        crPopupButton.addActionListener(e -> {
            if (crPopup == null) {
                crPopupButton.requestFocus();
                showCRPopup();
            } else {
                hideCRPopup();
            }
        });
        add(crPopupButton);

        carringtonPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        addFocusListenerToAllChildren(carringtonPanel);
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

    @Override
    public void focusGained(FocusEvent e) {
    }

    @Override
    public void focusLost(FocusEvent e) {
        if (!carringtonPanel.isAncestorOf(e.getOppositeComponent()) && (e.getComponent() == crPopupButton || carringtonPanel.isAncestorOf(e.getComponent()))) {
            hideCRPopup();
        }
    }

    private void hideCRPopup() {
        if (crPopup != null) {
            crPopup.hide();
            informAllJHVCalendarListeners();
            crPopup = null;
        }
    }

    private void showCRPopup() {
        carringtonPanel.setTime(time);

        // get position for popup
        int x = crPopupButton.getLocationOnScreen().x;
        int y = crPopupButton.getLocationOnScreen().y + crPopupButton.getSize().height;

        // create popup
        PopupFactory factory = PopupFactory.getSharedInstance();

        // correct position of popup when it does not fit into screen area
        x = x + carringtonPanel.getSize().width > Toolkit.getDefaultToolkit().getScreenSize().width ? Toolkit.getDefaultToolkit().getScreenSize().width - carringtonPanel.getSize().width : x;
        x = x < 0 ? 0 : x;
        y = y + carringtonPanel.getSize().height > Toolkit.getDefaultToolkit().getScreenSize().height ? crPopupButton.getLocationOnScreen().y - carringtonPanel.getSize().height : y;
        y = y < 0 ? 0 : y;

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

        private final JComboBox<Integer> crCombo = new JComboBox<>(createCRList());

        JHVCarrington() {
            setLayout(new BorderLayout());

            crCombo.addActionListener(e -> {
                setTimeFromCarrington(Carrington.CR_start[crCombo.getSelectedIndex()]);
                hideCRPopup();
            });
            add(crCombo);
        }

        public void setTime(long t) {
            int cr = (int) Math.round(Carrington.time2CR(new JHVDate(t)) - Carrington.CR_MINIMAL);
            crCombo.setSelectedIndex(cr);
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
