package org.helioviewer.jhv.gui.components.calendar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.PopupFactory;

import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.components.base.JHVButton;
import org.helioviewer.jhv.time.TimeUtils;

/*
 * This component allows to select a date or enter a date by hand. It works
 * similar to a combobox apart from that the popup is not a list but represents
 * a {@link JHVCalendar} component.
 */
@SuppressWarnings("serial")
public class JHVCalendarDatePicker extends JPanel {

    private final HashSet<JHVCalendarListener> listeners = new HashSet<>();
    private final Calendar calendar = new GregorianCalendar();

    private final JHVCalendar jhvCalendar = new JHVCalendar();
    private final JTextField textField = new JTextField();
    private final JHVButton calPopupButton = new JHVButton(Buttons.calendar);
    private Popup calPopup = null;

    public JHVCalendarDatePicker() {
        setLayout(new BorderLayout());

        setTextField();
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                setDateFromTextField(true);
            }
        });
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    setDateFromTextField(true);
                }
            }
        });

        calPopupButton.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                hideCalPopup();
            }
        });
        calPopupButton.addActionListener(e -> {
            setDateFromTextField(false);
            if (calPopup == null) {
                calPopupButton.requestFocus();
                showCalPopup();
            } else {
                hideCalPopup();
            }
        });

        add(calPopupButton, BorderLayout.EAST);
        add(textField, BorderLayout.CENTER);

        jhvCalendar.setPreferredSize(jhvCalendar.getMinimumSize());
        jhvCalendar.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        jhvCalendar.addJHVCalendarListener(e -> hideCalPopup());
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

    private void setDateFromTextField(boolean propagate) {
        try {
            setTime(TimeUtils.parseDate(textField.getText()));
        } catch (Exception e) {
            setTextField();
        }
        if (propagate)
            informAllJHVCalendarListeners();
    }

    private void setDateFromCalendar() {
        setTime(jhvCalendar.getTime());
        informAllJHVCalendarListeners();
    }

    /**
     * Opens an new popup window where the user can select a date.
     */
    private void showCalPopup() {
        // set up the popup content
        jhvCalendar.setDate(calendar.getTime());

        // get position for popup
        int x = textField.getLocationOnScreen().x;
        int y = textField.getLocationOnScreen().y + textField.getSize().height;

        // create popup
        PopupFactory factory = PopupFactory.getSharedInstance();
        calPopup = factory.getPopup(calPopupButton, jhvCalendar, x, y);
        calPopup.show();

        jhvCalendar.resizeSelectionPanel();

        // correct position of popup when it does not fit into screen area
        x = x + jhvCalendar.getSize().width > Toolkit.getDefaultToolkit().getScreenSize().width ? Toolkit.getDefaultToolkit().getScreenSize().width - jhvCalendar.getSize().width : x;
        x = x < 0 ? 0 : x;
        y = y + jhvCalendar.getSize().height > Toolkit.getDefaultToolkit().getScreenSize().height ? textField.getLocationOnScreen().y - jhvCalendar.getSize().height : y;
        y = y < 0 ? 0 : y;

        calPopup.hide();

        // show popup
        calPopup = factory.getPopup(calPopupButton, jhvCalendar, x, y);
        calPopup.show();
    }

    private void hideCalPopup() {
        if (calPopup != null) {
            calPopup.hide();
            calPopup = null;
            setDateFromCalendar();
        }
    }

    public void setTime(long time) {
        if (time > TimeUtils.MINIMAL_DATE.milli && time < TimeUtils.MAXIMAL_DATE.milli)
            calendar.setTimeInMillis(time);
        setTextField();
    }

    public long getTime() {
        return calendar.getTimeInMillis();
    }

    private void setTextField() {
        textField.setText(TimeUtils.formatDate(calendar.getTimeInMillis()));
    }

    /**
     * Override the setEnabled method in order to keep the containing
     * components' enabledState synced with the enabledState of this component.
     */
    @Override
    public void setEnabled(boolean enabled) {
        textField.setEnabled(enabled);
        calPopupButton.setEnabled(enabled);
    }

}
