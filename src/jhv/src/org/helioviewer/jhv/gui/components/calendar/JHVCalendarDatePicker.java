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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;

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

    private final HashSet<JHVCalendarListener> listeners = new HashSet<>();
    private final Calendar calendar = new GregorianCalendar();

    private final JHVCalendar jhvCalendar = new JHVCalendar(true);
    private final JTextField textField = new JTextField();

    private static final Icon icon = IconBank.getIcon(JHVIcon.DATE);

    public JTextField getTextField() {
        return textField;
    }

    private final JButton calPopupButton;

    private Popup calPopup = null;

    public JHVCalendarDatePicker() {
        setLayout(new BorderLayout(0, 0));

        // set up text field
        textField.setText(TimeUtils.dateFormat.format(calendar.getTime()));
        textField.addFocusListener(this);
        textField.addKeyListener(this);

        // set up popup button
        calPopupButton = new JButton(icon);
        calPopupButton.setPreferredSize(new Dimension(icon.getIconWidth() + 14, calPopupButton.getPreferredSize().height));
        calPopupButton.addFocusListener(this);
        calPopupButton.addActionListener(this);

        // place sub components
        add(calPopupButton, BorderLayout.EAST);
        add(textField, BorderLayout.CENTER);

        jhvCalendar.setPreferredSize(jhvCalendar.getMinimumSize());
        jhvCalendar.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        jhvCalendar.addJHVCalendarListener(this);
        addFocusListenerToAllChildren(jhvCalendar);
    }

    public void addJHVCalendarListener(JHVCalendarListener l) {
        listeners.add(l);
    }

    public void removeJHVCalendarListener(JHVCalendarListener l) {
        listeners.remove(l);
    }

    /**
     * Informs all listener of this class by passing the corresponding event.
     *
     * @param e
     *            event
     */
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
        if (arg0.getComponent() == calPopupButton || (jhvCalendar != null && jhvCalendar.isAncestorOf(arg0.getComponent()))) {
            // if the receiver of the focus is not a subcomponent of the
            // jhvCalendar than hide the popup
            if (jhvCalendar != null && !jhvCalendar.isAncestorOf(arg0.getOppositeComponent())) {
                hideCalPopup();
            }
        }

        // has textfield lost the focus
        if (arg0.getComponent() == textField) {
            setDateFromTextField();
            informAllJHVCalendarListeners();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // open or close the popup window when the event was fired by the
        // corresponding popup button
        if (e.getSource() == calPopupButton) {
            setDateFromTextField();
            if (calPopup == null) {
                showCalPopup();
            } else {
                hideCalPopup();
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            setDateFromTextField();
            informAllJHVCalendarListeners();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    private void setDateFromTextField() {
        try {
            Date date = TimeUtils.dateFormat.parse(textField.getText());
            setTime(date.getTime());
        } catch (ParseException e) {
            textField.setText(TimeUtils.dateFormat.format(calendar.getTime()));
        }
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
    private void hideCalPopup() {
        if (calPopup != null) {
            calPopup.hide();
            calPopup = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(JHVCalendarEvent e) {
        if (e.getSource().equals(jhvCalendar)) {
            // close popup
            hideCalPopup();
            // set selected date
            setTime(jhvCalendar.getDate().getTime());
        }
        // inform all listeners of this class that a new date was choosen by the user
        informAllJHVCalendarListeners();
    }

    public void setTime(long time) {
        if (time > TimeUtils.MINIMAL_DATE.milli && time < TimeUtils.MAXIMAL_DATE.milli)
            calendar.setTimeInMillis(time);
        textField.setText(TimeUtils.dateFormat.format(calendar.getTime()));
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
