package org.helioviewer.jhv.gui.components.timeselector;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.PopupFactory;

import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.components.base.JHVButton;
import org.helioviewer.jhv.time.TimeUtils;

@SuppressWarnings("serial")
class TimePanel extends JPanel {

    private final ArrayList<CalendarListener> listeners = new ArrayList<>();
    private final Calendar calendar = new GregorianCalendar();

    private final CalendarPicker calendarPicker = new CalendarPicker();
    private final JTextField textField = new JTextField();
    private final JHVButton calPopupButton = new JHVButton(Buttons.calendar);
    private Popup calPopup = null;

    TimePanel(String text) {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.BOTH;

        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                setTimeFromTextField(true);
            }
        });
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    setTimeFromTextField(true);
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
            setTimeFromTextField(false);
            if (calPopup == null) {
                calPopupButton.requestFocus();
                showCalPopup();
            } else {
                hideCalPopup();
            }
        });
        calPopupButton.setToolTipText("Select date");

        JLabel label = new JLabel(text, JLabel.RIGHT);
        label.setPreferredSize(new Dimension(40, -1));

        c.weightx = 0;
        c.gridx = 0;
        add(label, c);
        c.weightx = 1;
        c.gridx = 1;
        add(textField, c);
        c.weightx = 0;
        c.gridx = 2;
        add(calPopupButton, c);

        calendarPicker.setPreferredSize(calendarPicker.getMinimumSize());
        calendarPicker.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        calendarPicker.addListener(this::hideCalPopup);
    }

    private void informListeners() {
        listeners.forEach(CalendarListener::calendarAction);
    }

    private void setTimeFromTextField(boolean propagate) {
        setTime(TimeUtils.optParse(textField.getText(), getTime()));
        if (propagate)
            informListeners();
    }

    private void setTimeFromCalendar() {
        setTime(calendarPicker.getTime());
        informListeners();
    }

    // Opens an new popup window where the user can select a date
    private void showCalPopup() {
        // set up the popup content
        calendarPicker.setTime(calendar.getTimeInMillis());

        // get position for popup
        int x = textField.getLocationOnScreen().x;
        int y = textField.getLocationOnScreen().y + textField.getSize().height;

        // create popup
        PopupFactory factory = PopupFactory.getSharedInstance();
        calPopup = factory.getPopup(calPopupButton, calendarPicker, x, y);
        calPopup.show();

        calendarPicker.resizeSelectionPanel();

        // correct position of popup when it does not fit into screen area
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension pickerSize = calendarPicker.getSize();
        x = x + pickerSize.width > screenSize.width ? screenSize.width - pickerSize.width : x;
        x = Math.max(x, 0);
        y = y + pickerSize.height > screenSize.height ? textField.getLocationOnScreen().y - pickerSize.height : y;
        y = Math.max(y, 0);

        calPopup.hide();

        // show popup
        calPopup = factory.getPopup(calPopupButton, calendarPicker, x, y);
        calPopup.show();
    }

    private void hideCalPopup() {
        if (calPopup != null) {
            calPopup.hide();
            calPopup = null;
            setTimeFromCalendar();
        }
    }

    void setTime(long time) {
        if (time > TimeUtils.MINIMAL_DATE.milli && time < TimeUtils.MAXIMAL_DATE.milli)
            calendar.setTimeInMillis(TimeUtils.floorSec(time));
        setTextField();
    }

    long getTime() {
        return calendar.getTimeInMillis();
    }

    private void setTextField() {
        textField.setText(TimeUtils.format(getTime()));
    }

    // Override the setEnabled method in order to keep the containing
    // components' enabledState synced with the enabledState of this component.
    @Override
    public void setEnabled(boolean enabled) {
        textField.setEnabled(enabled);
        calPopupButton.setEnabled(enabled);
    }

    void addListener(CalendarListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

}
