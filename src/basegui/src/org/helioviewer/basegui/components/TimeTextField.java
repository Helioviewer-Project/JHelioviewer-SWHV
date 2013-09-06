package org.helioviewer.basegui.components;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JTextField;

/**
 * This offers a text field to edit the time at a day. A normal
 * JFormattedTextField is for some reason badly from the user interface
 * (especially then backspace tends to delete more than one item).
 * <p>
 * It is based on the parts of the ObservationDialog from Stephan Pagel and I
 * therefore will assume a format from the user as HH:mm:ss.
 * <p>
 * It will validates when the focus leaves, and defaults to 00:00:00; like
 * before maybe worth changing later
 * 
 * @author Helge Dietert
 */
public class TimeTextField extends JTextField {
    /**
     * Default value used to set
     */
    private static final String defaultTime = "00:00:00";
    /**
     * Used time formatter
     */
    public static final SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    /**
     * 
     */
    private static final long serialVersionUID = 4082442421210153142L;

    /**
     * Creates a new time text field
     */
    public TimeTextField() {
        super(defaultTime);
        addFocusListener(new FocusListener() {
            /**
             * Nothing to do
             * 
             * @see java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
             */
            public void focusGained(FocusEvent arg0) {
            }

            /**
             * Validate the input
             * 
             * @see java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
             */
            public void focusLost(FocusEvent arg0) {
                validateInput();
            }
        });
    }

    /**
     * Gives the formatted input (normalized, e.g. 0:61 becomes 1:01)
     * 
     * @return formatted input or default time if its not valid
     */
    public String getFormattedInput() {
        try {
            return formatter.format(formatter.parse(getText()));
        } catch (ParseException e) {
            return defaultTime;
        }
    }

    /**
     * Gives a date object with the selected time
     * 
     * @return Date with selected time (or defaultTime if invalid)
     */
    public Date getValue() {
        try {
            return formatter.parse(getText());
        } catch (ParseException e) {
            try {
                return formatter.parse(defaultTime);
            } catch (ParseException e1) {
                // The default time should always parseable
                return null;
            }
        }
    }

    /**
     * Validates the fields and resets its if necessary
     */
    private void validateInput() {
        setText(getFormattedInput());
    }

    /**
     * Sets the time to the time given in the date
     * 
     * @param time
     *            new time to set
     */
    public void setValue(Date time) {
        setText(formatter.format(time));
    }
}