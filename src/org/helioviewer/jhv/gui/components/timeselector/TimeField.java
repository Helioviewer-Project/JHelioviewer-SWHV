package org.helioviewer.jhv.gui.components.timeselector;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.PopupFactory;

import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.time.TimeUtils;

import com.jidesoft.swing.JideButton;

@SuppressWarnings("serial")
class TimeField extends JTextField {

    private final ArrayList<CalendarListener> listeners = new ArrayList<>();
    private final Calendar calendar = new GregorianCalendar();

    private final CalendarPicker calendarPicker = new CalendarPicker();
    private final JideButton calendarButton = new JideButton(Buttons.calendar);
    private Popup calPopup = null;

    TimeField(String tip) {
        calendarButton.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                hidePopup();
            }
        });
        calendarButton.addActionListener(e -> {
            if (calPopup == null) {
                calendarButton.requestFocus();
                showPopup();
            } else {
                hidePopup();
            }
        });
        calendarPicker.addListener(this::hidePopup);
        calendarButton.setMargin(new Insets(0, 0, 0, 0));
        calendarPicker.setPreferredSize(calendarPicker.getMinimumSize());
        calendarButton.setToolTipText(tip);

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                setTimeFromText();
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    transferFocus();
                }
            }
        });
        putClientProperty("JTextField.trailingComponent", calendarButton); // FlatLaf 2 feature
        setToolTipText(tip);
    }

    // Opens a new popup window where the user can select a date
    private void showPopup() {
        // set up the popup content
        calendarPicker.setTime(getTime());

        // get position for popup
        int x = getLocationOnScreen().x;
        int y = getLocationOnScreen().y + getSize().height;

        // create popup
        PopupFactory factory = PopupFactory.getSharedInstance();
        calPopup = factory.getPopup(calendarButton, calendarPicker, x, y);
        calPopup.show();

        calendarPicker.resizeSelectionPanel();

        // correct position of popup when it does not fit into screen area
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension pickerSize = calendarPicker.getSize();
        x = x + pickerSize.width > screenSize.width ? screenSize.width - pickerSize.width : x;
        x = Math.max(x, 0);
        y = y + pickerSize.height > screenSize.height ? getLocationOnScreen().y - pickerSize.height : y;
        y = Math.max(y, 0);

        calPopup.hide();

        // show popup
        calPopup = factory.getPopup(calendarButton, calendarPicker, x, y);
        calPopup.show();
    }

    private void hidePopup() {
        if (calPopup != null) {
            calPopup.hide();
            calPopup = null;
            setTimeFromCalendar();
        }
    }

    private void informListeners() {
        listeners.forEach(CalendarListener::calendarAction);
    }

    private void setTimeFromText() {
        String text = getText();
        if (text != null) { // satisfy coverity
            setTime(TimeUtils.optParse(text, getTime()));
            informListeners();
        }
    }

    private void setTimeFromCalendar() {
        setTime(calendarPicker.getTime());
        informListeners();
    }

    void setTime(long time) {
        if (time > TimeUtils.MINIMAL_TIME.milli && time < TimeUtils.MAXIMAL_TIME.milli)
            calendar.setTimeInMillis(TimeUtils.floorSec(time));
        setText(TimeUtils.formatShort(getTime()));
    }

    long getTime() {
        return calendar.getTimeInMillis();
    }

    void addListener(CalendarListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

}
