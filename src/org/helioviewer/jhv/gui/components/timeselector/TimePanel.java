package org.helioviewer.jhv.gui.components.timeselector;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.PopupFactory;

import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.time.TimeUtils;

import com.jidesoft.swing.JideButton;

@SuppressWarnings("serial")
class TimePanel extends JPanel {

    private final ArrayList<CalendarListener> listeners = new ArrayList<>();
    private final Calendar calendar = new GregorianCalendar();

    private final CalendarPicker calendarPicker = new CalendarPicker();
    private final JTextField textField = new JTextField(TimeUtils.J2000.toString());
    private final JideButton calendarButton = new JideButton(Buttons.calendar);
    private Popup calPopup = null;

    TimePanel(String tip) {
        setLayout(new GridBagLayout());
        setBackground(textField.getBackground());
        setBorder(textField.getBorder());

        textField.setBorder(null);
        textField.setToolTipText(tip);
        calendarButton.setMargin(new Insets(0, 0, 0, 0));
        calendarButton.setToolTipText(tip);

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
        calendarButton.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                hidePopup();
            }
        });
        calendarButton.addActionListener(e -> {
            setTimeFromTextField(false);
            if (calPopup == null) {
                calendarButton.requestFocus();
                showPopup();
            } else {
                hidePopup();
            }
        });

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.weightx = 1;
        c.gridx = 0;
        add(textField, c);
        c.weightx = 0;
        c.gridx = 1;
        add(calendarButton, c);

        calendarPicker.setPreferredSize(calendarPicker.getMinimumSize());
        calendarPicker.addListener(this::hidePopup);
    }

    private void informListeners() {
        listeners.forEach(CalendarListener::calendarAction);
    }

    private void setTimeFromTextField(boolean propagate) {
        String text = textField.getText();
        if (text != null) { // satisfy coverity
            setTime(TimeUtils.optParse(text, getTime()));
            if (propagate)
                informListeners();
        }
    }

    private void setTimeFromCalendar() {
        setTime(calendarPicker.getTime());
        informListeners();
    }

    // Opens an new popup window where the user can select a date
    private void showPopup() {
        // set up the popup content
        calendarPicker.setTime(calendar.getTimeInMillis());

        // get position for popup
        int x = textField.getLocationOnScreen().x;
        int y = textField.getLocationOnScreen().y + textField.getSize().height;

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
        y = y + pickerSize.height > screenSize.height ? textField.getLocationOnScreen().y - pickerSize.height : y;
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

    void setTime(long time) {
        if (time > TimeUtils.MINIMAL_TIME.milli && time < TimeUtils.MAXIMAL_TIME.milli)
            calendar.setTimeInMillis(TimeUtils.floorSec(time));
        setTextField();
    }

    long getTime() {
        return calendar.getTimeInMillis();
    }

    private void setTextField() {
        textField.setText(TimeUtils.formatShort(getTime()));
    }

    // Override the setEnabled method in order to keep the containing
    // components' enabledState synced with the enabledState of this component.
    @Override
    public void setEnabled(boolean enabled) {
        textField.setEnabled(enabled);
        calendarButton.setEnabled(enabled);
    }

    void addListener(CalendarListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

}
