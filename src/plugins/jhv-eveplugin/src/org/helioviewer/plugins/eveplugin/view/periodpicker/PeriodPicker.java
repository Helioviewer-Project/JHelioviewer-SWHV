package org.helioviewer.plugins.eveplugin.view.periodpicker;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.plugins.eveplugin.view.periodpicker.PeriodPickerDialog.PeriodPickerDialogResult;

/**
 * @author Stephan Pagel
 * */
public class PeriodPicker extends JPanel implements ActionListener, MouseListener {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

    private final LinkedList<PeriodPickerListener> listeners = new LinkedList<PeriodPickerListener>();
    private Interval<Date> interval = new Interval<Date>(null, null);

    private final ImageIcon icon = IconBank.getIcon(JHVIcon.DATE);
    private final JTextField textField = new JTextField();
    private final JButton popupButton = new JButton(icon);

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    public PeriodPicker() {
        initVisualComponents();
    }

    private void initVisualComponents() {
        // set basic layout
        setLayout(new BorderLayout(0, 0));

        // set up text field
        textField.setEditable(false);
        textField.addMouseListener(this);
        updateTextField();

        // set up popup button
        popupButton.setPreferredSize(new Dimension(icon.getIconWidth() + 14, popupButton.getPreferredSize().height));
        popupButton.setToolTipText("Select a period manually");
        popupButton.addActionListener(this);

        // place sub components
        add(popupButton, BorderLayout.EAST);
        add(textField, BorderLayout.CENTER);
    }

    private void updateTextField() {
        final Date startDate = interval.getStart() == null ? new Date() : interval.getStart();
        final Date endDate = interval.getEnd() == null ? new Date() : interval.getEnd();

        textField.setText(dateFormat.format(startDate) + " - " + dateFormat.format(endDate));
    }

    public void addPeriodPickerListener(final PeriodPickerListener listener) {
        listeners.add(listener);
    }

    public void removePeriodPickerListener(final PeriodPickerListener listener) {
        listeners.remove(listener);
    }

    private void fireIntervalChanged(final Interval<Date> interval) {
        for (final PeriodPickerListener listener : listeners) {
            listener.intervalChanged(interval);
        }
    }

    /**
     * Sets the date format pattern. The date will be displayed in defined
     * format.
     * 
     * @param newFormat
     *            new pattern to use
     */
    public void setDateFormat(final SimpleDateFormat newFormat) {
        if (newFormat != null)
            dateFormat = newFormat;
    }

    public void setInterval(final Interval<Date> interval) {
        this.interval = interval;
        updateTextField();
    }

    public Interval<Date> getInterval() {
        return interval;
    }

    private void showPeriodSelectionDialog() {
        final PeriodPickerDialog dialog = new PeriodPickerDialog();
        dialog.setInterval(interval);

        if (dialog.showDialog() == PeriodPickerDialogResult.NEWPERIOD) {
            setInterval(dialog.getInterval());
            fireIntervalChanged(interval);
        }
    }

    // ////////////////////////////////////////////////////////////////
    // Action Listener
    // ////////////////////////////////////////////////////////////////

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == popupButton) {
            showPeriodSelectionDialog();
        }
    }

    // ////////////////////////////////////////////////////////////////
    // Mouse Listener
    // ////////////////////////////////////////////////////////////////

    public void mouseClicked(MouseEvent e) {
        if (e.getSource() == textField) {
            showPeriodSelectionDialog();
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }
}
