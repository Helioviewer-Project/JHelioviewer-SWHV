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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.PopupFactory;

import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

/**
 * This component allows to select a date or enter a date by hand. It works
 * similar to a combobox apart from that the popup is not a list but represents
 * a {@link JHVCalendar} component.
 *
 * @see JHVCalendar
 * @author Stephan Pagel
 */
@SuppressWarnings("serial")
public class JHVCalendarDatePicker extends JPanel implements FocusListener, ActionListener, KeyListener, JHVCalendarListener {

    private final AbstractList<JHVCalendarListener> listeners = new LinkedList<JHVCalendarListener>();
    private final Calendar calendar = new GregorianCalendar();

    private JHVCalendar jhvCalendar = null;
    private final JTextField textField = new JTextField();

    private static final Icon icon = IconBank.getIcon(JHVIcon.DATE);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public JTextField getTextField() {
        return textField;
    }

    private final JButton calPopupButton;
    private final JButton crPopupButton;

    private Popup popup = null;

    public JHVCalendarDatePicker() {
        setLayout(new BorderLayout(0, 0));

        // set up text field
        textField.setText(dateFormat.format(calendar.getTime()));
        textField.addFocusListener(this);
        textField.addKeyListener(this);

        // set up popup button
        calPopupButton = new JButton(icon);
        calPopupButton.setPreferredSize(new Dimension(icon.getIconWidth() + 14, calPopupButton.getPreferredSize().height));
        calPopupButton.addFocusListener(this);
        calPopupButton.addActionListener(this);

        // place sub components

        crPopupButton = new JButton("CR");
        crPopupButton.setPreferredSize(new Dimension(icon.getIconWidth() + 14, calPopupButton.getPreferredSize().height));
        crPopupButton.addFocusListener(this);
        crPopupButton.addActionListener(this);

        JPanel buttonGroup = new JPanel();
        buttonGroup.add(calPopupButton);
        buttonGroup.add(crPopupButton);
        add(buttonGroup, BorderLayout.EAST);
        add(textField, BorderLayout.CENTER);
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
    public void focusGained(FocusEvent arg0) {
    }

    @Override
    public void focusLost(FocusEvent arg0) {
        // has popup button or a subcomponent of jhvCalendar lost the focus?
        if (arg0.getComponent() == calPopupButton || (jhvCalendar != null && jhvCalendar.isAncestorOf(arg0.getComponent()))) {
            // if the receiver of the focus is not a subcomponent of the
            // jhvCalendar than hide the popup
            if (jhvCalendar != null && !jhvCalendar.isAncestorOf(arg0.getOppositeComponent())) {
                hidePopup();
            }
        }

        // has textfield lost the focus
        if (arg0.getComponent() == textField) {
            // check the entered date when text field lost the focus
            checkDateStringInTextField();
            informAllJHVCalendarListeners(new JHVCalendarEvent(this));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // open or close the popup window when the event was fired by the
        // corresponding popup button
        if (e.getSource() == calPopupButton) {
            setDate(parseDate(textField.getText()));
            if (popup == null) {
                showPopup();
            } else {
                hidePopup();
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            checkDateStringInTextField();
            informAllJHVCalendarListeners(new JHVCalendarEvent(this));
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public void checkDateStringInTextField() {
        Date date = parseDate(textField.getText());
        if (date == null) {
            textField.setText(dateFormat.format(calendar.getTime()));
        } else {
            setDate(date);
        }
    }

    /**
     * Opens an new popup window where the user can select a date.
     */
    private void showPopup() {
        // set up the popup content
        jhvCalendar = new JHVCalendar();
        jhvCalendar.setPreferredSize(jhvCalendar.getMinimumSize());
        jhvCalendar.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        jhvCalendar.addJHVCalendarListener(this);
        jhvCalendar.setDate(calendar.getTime());
        addFocusListenerToAllChildren(jhvCalendar);

        // get position for popup
        int x = textField.getLocationOnScreen().x;
        int y = textField.getLocationOnScreen().y + textField.getSize().height;

        // create popup
        PopupFactory factory = PopupFactory.getSharedInstance();
        popup = factory.getPopup(calPopupButton, jhvCalendar, x, y);
        popup.show();

        jhvCalendar.componentResized(null);

        // correct position of popup when it does not fit into screen area
        x = x + jhvCalendar.getSize().width > Toolkit.getDefaultToolkit().getScreenSize().width ? Toolkit.getDefaultToolkit().getScreenSize().width - jhvCalendar.getSize().width : x;
        x = x < 0 ? 0 : x;

        y = y + jhvCalendar.getSize().height > Toolkit.getDefaultToolkit().getScreenSize().height ? textField.getLocationOnScreen().y - jhvCalendar.getSize().height : y;
        y = y < 0 ? 0 : y;

        popup.hide();

        // show popup
        popup = factory.getPopup(calPopupButton, jhvCalendar, x, y);
        popup.show();
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

    /**
     * Closes the popup window if it is still displayed.
     */
    private void hidePopup() {
        if (popup != null) {
            popup.hide();
            popup = null;
        }
    }

    @Override
    public void actionPerformed(JHVCalendarEvent e) {
        // close popup
        hidePopup();
        // set selected date
        setDate(jhvCalendar.getDate());
        jhvCalendar = null;
        // inform all listeners of this class that a new date was choosen by the
        // user
        informAllJHVCalendarListeners(new JHVCalendarEvent(this));
    }

    public void setDate(Date date) {
        if (date != null) {
            long milli = date.getTime();
            if (milli > TimeUtils.MINIMAL_DATE.milli && milli < TimeUtils.MAXIMAL_DATE.milli) {
                calendar.setTime(date);
            }
        }
        textField.setText(dateFormat.format(calendar.getTime()));
    }

    /**
     * Returns the selected date.
     *
     * @return the selected date.
     */
    public Date getDate() {
        return calendar.getTime();
    }

    /**
     * Tries to parse a given date string to a date object. If the string could
     * not be parsed the method returns a null value.
     *
     * @param source
     *            the given date string.
     * @return the corresponding date object.
     */
    private Date parseDate(String source) {
        Date date = null;
        try {
            date = dateFormat.parse(source);
        } catch (ParseException e) {
        }
        return date;
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
