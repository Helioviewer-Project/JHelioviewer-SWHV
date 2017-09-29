package org.helioviewer.jhv.gui.components.calendar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.PopupFactory;

import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.time.TimeUtils;

import com.jidesoft.swing.JideButton;

/**
 * This component allows to select a date or enter a date by hand. It works
 * similar to a combobox apart from that the popup is not a list but represents
 * a {@link JHVCalendar} component.
 *
 * @see JHVCalendar
 */
@SuppressWarnings("serial")
public class JHVCalendarDatePicker extends JPanel implements FocusListener {

    private final HashSet<JHVCalendarListener> listeners = new HashSet<>();
    private final Calendar calendar = new GregorianCalendar();

    private final JHVCalendar jhvCalendar = new JHVCalendar();
    private final JTextField textField = new JTextField();
    private final JideButton calPopupButton = new JideButton(Buttons.calendar);
    private Popup calPopup = null;

    public JHVCalendarDatePicker() {
        setLayout(new BorderLayout());

        // set up text field
        textField.setText(TimeUtils.formatDate(calendar.getTime().getTime()));
        textField.addFocusListener(this);
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    setDateFromTextField();
                }
            }
        });

        calPopupButton.addFocusListener(this);
        calPopupButton.addActionListener(e -> {
            setDateFromTextField();
            if (calPopup == null) {
                calPopupButton.requestFocus();
                showCalPopup();
            } else {
                hideCalPopup();
            }
        });

        // place sub components
        add(calPopupButton, BorderLayout.EAST);
        add(textField, BorderLayout.CENTER);

        jhvCalendar.setPreferredSize(jhvCalendar.getMinimumSize());
        jhvCalendar.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        jhvCalendar.addJHVCalendarListener(e -> hideCalPopup());

        addFocusListenerToAllChildren(jhvCalendar);
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

    @Override
    public void focusGained(FocusEvent e) {
    }

    @Override
    public void focusLost(FocusEvent e) {
        // if the receiver of the focus is not a subcomponent of the jhvCalendar and
        // the popup button or a subcomponent of jhvCalendar lost the focus than hide the popup
        if (!jhvCalendar.isAncestorOf(e.getOppositeComponent()) && (e.getComponent() == calPopupButton || jhvCalendar.isAncestorOf(e.getComponent()))) {
            hideCalPopup();
        }
        // has textfield lost the focus
        if (e.getComponent() == textField) {
            setDateFromTextField();
        }
    }

    private void setDateFromTextField() {
        try {
            setTime(TimeUtils.parseDate(textField.getText()));
        } catch (Exception e) {
            textField.setText(TimeUtils.formatDate(calendar.getTime().getTime()));
        }
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

    // Closes the popup window if it is still displayed.
    private void hideCalPopup() {
        if (calPopup != null) {
            setDateFromCalendar();
            calPopup.hide();
            calPopup = null;
        }
    }

    /**
     * Adds to all subcomponents of a component the focus listener of this
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

    public void setTime(long time) {
        if (time > TimeUtils.MINIMAL_DATE.milli && time < TimeUtils.MAXIMAL_DATE.milli)
            calendar.setTimeInMillis(time);
        textField.setText(TimeUtils.formatDate(calendar.getTime().getTime()));
    }

    /**
     * Returns the selected date.
     *
     * @return the selected date.
     */
    public long getTime() {
        return calendar.getTimeInMillis();
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
