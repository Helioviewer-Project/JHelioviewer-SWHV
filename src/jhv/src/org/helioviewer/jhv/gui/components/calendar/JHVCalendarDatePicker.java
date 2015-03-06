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

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

/**
 * This component allows to select a date or enter a date by hand. It works
 * similar to a combobox apart from that the popup is not a list but represents
 * a {@link JHVCalendar} component.
 * <p>
 * To use this component create an instance of the class, set the default date
 * format by calling the method {@link #setDateFormat(String)} to display the
 * date like the user prefers and set the date which should be selected by
 * calling the method {@link #setDate(Date)}. To get the selected date call
 * {@link #getDate()}.
 * 
 * @see JHVCalendar
 * @author Stephan Pagel
 */
public class JHVCalendarDatePicker extends JPanel implements FocusListener, ActionListener, KeyListener, JHVCalendarListener {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    private final AbstractList<JHVCalendarListener> listeners = new LinkedList<JHVCalendarListener>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final Calendar calendar = new GregorianCalendar();

    private JHVCalendar jhvCalendar = null;
    private final Icon icon = IconBank.getIcon(JHVIcon.DATE);
    private final JTextField textField = new JTextField();

    public JTextField getTextField() {
        return textField;
    }

    private JButton popupButton;
    private Popup popup = null;

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    /**
     * Default constructor.
     */
    public JHVCalendarDatePicker() {

        initVisualComponents();
    }

    /**
     * Initialize the visual parts of the component.
     */
    private void initVisualComponents() {

        // set basic layout
        setLayout(new BorderLayout(0, 0));

        // set up text field
        textField.setText(dateFormat.format(calendar.getTime()));
        textField.addFocusListener(this);
        textField.addKeyListener(this);

        // set up popup button
        setPopupButton(new JButton(icon));
        getPopupButton().setPreferredSize(new Dimension(icon.getIconWidth() + 14, getPopupButton().getPreferredSize().height));
        getPopupButton().addFocusListener(this);
        getPopupButton().addActionListener(this);

        // place sub components
        add(getPopupButton(), BorderLayout.EAST);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void focusGained(FocusEvent arg0) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void focusLost(FocusEvent arg0) {

        // has popup button or a subcomponent of jhvCalendar lost the focus?
        if (arg0.getComponent() == getPopupButton() || (jhvCalendar != null && jhvCalendar.isAncestorOf(arg0.getComponent()))) {

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

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        // open or close the popup window when the event was fired by the
        // corresponding popup button
        if (e.getSource() == getPopupButton()) {

            setDate(parseDate(textField.getText()));

            if (popup == null) {
                showPopup();
            } else {
                hidePopup();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void keyPressed(KeyEvent e) {

        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            checkDateStringInTextField();
            informAllJHVCalendarListeners(new JHVCalendarEvent(this));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void keyReleased(KeyEvent e) {
    }

    /**
     * {@inheritDoc}
     */
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
        popup = factory.getPopup(getPopupButton(), jhvCalendar, x, y);
        popup.show();

        jhvCalendar.componentResized(null);

        // correct position of popup when it does not fit into screen area
        x = x + jhvCalendar.getSize().width > Toolkit.getDefaultToolkit().getScreenSize().width ? Toolkit.getDefaultToolkit().getScreenSize().width - jhvCalendar.getSize().width : x;
        x = x < 0 ? 0 : x;

        y = y + jhvCalendar.getSize().height > Toolkit.getDefaultToolkit().getScreenSize().height ? textField.getLocationOnScreen().y - jhvCalendar.getSize().height : y;
        y = y < 0 ? 0 : y;

        popup.hide();

        // show popup
        popup = factory.getPopup(getPopupButton(), jhvCalendar, x, y);
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * Set the date pattern. The date will be displayed in defined format.
     * 
     * @param pattern
     *            pattern how date should be displayed.
     * @return boolean value if pattern is valid.
     */
    public boolean setDateFormat(String pattern) {

        try {
            dateFormat.applyPattern(pattern);
            return true;

        } catch (NullPointerException e1) {
        } catch (IllegalArgumentException e2) {
        }

        return false;
    }

    /**
     * Sets the date format pattern. The date will be displayed in defined
     * format.
     * 
     * @param newFormat
     *            new pattern to use
     */
    public void setDateFormat(SimpleDateFormat newFormat) {
        if (newFormat != null) {
            dateFormat = newFormat;
        }
    }

    /**
     * 
     * @param date
     */
    public void setDate(Date date) {
        if (date != null) {
            calendar.setTime(date);
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
        getPopupButton().setEnabled(enabled);
    }

    public JButton getPopupButton() {
        return popupButton;
    }

    public void setPopupButton(JButton popupButton) {
        this.popupButton = popupButton;
    }
}
