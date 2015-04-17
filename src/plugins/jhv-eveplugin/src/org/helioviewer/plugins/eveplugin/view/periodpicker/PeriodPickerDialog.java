package org.helioviewer.plugins.eveplugin.view.periodpicker;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.helioviewer.base.interval.Interval;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;

/**
 * @author Stephan Pagel
 * */
public class PeriodPickerDialog extends JDialog implements ActionListener {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    public enum PeriodPickerDialogResult {
        NEWPERIOD, CANCEL
    };

    private final JPanel contentPane = new JPanel();
    private final JLabel headerLabel = new JLabel("Select a period for displaying data.");
    private final JLabel fromLabel = new JLabel("From:");
    private final JLabel toLabel = new JLabel("To:");
    private final JHVCalendarDatePicker fromPicker = new JHVCalendarDatePicker();
    private final JHVCalendarDatePicker toPicker = new JHVCalendarDatePicker();
    private final JButton setPeriodButton = new JButton("Set Period");
    private final JButton cancelButton = new JButton("Cancel");

    private PeriodPickerDialogResult dialogResult = PeriodPickerDialogResult.CANCEL;

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    public PeriodPickerDialog() {
        initVisualComponents();
    }

    private void initVisualComponents() {

        final JPanel headerPane = new JPanel();
        headerPane.setLayout(new FlowLayout(FlowLayout.LEFT));
        headerPane.add(headerLabel);

        final JPanel selectionPane = new JPanel();
        selectionPane.setLayout(new GridLayout(2, 2));
        selectionPane.setBorder(BorderFactory.createEmptyBorder(3, 9, 1, 9));
        selectionPane.add(fromLabel);
        selectionPane.add(fromPicker);
        selectionPane.add(toLabel);
        selectionPane.add(toPicker);

        final JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttonPane.add(setPeriodButton);
        buttonPane.add(cancelButton);

        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.add(headerPane);
        contentPane.add(selectionPane);
        contentPane.add(buttonPane);
        setContentPane(contentPane);

        fromPicker.setDateFormat(Settings.getSingletonInstance().getProperty("default.date.format"));
        toPicker.setDateFormat(Settings.getSingletonInstance().getProperty("default.date.format"));

        setPeriodButton.addActionListener(this);
        cancelButton.addActionListener(this);
    }

    public PeriodPickerDialogResult showDialog() {
        pack();
        setSize(getPreferredSize());
        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        setModal(true);

        dialogResult = PeriodPickerDialogResult.CANCEL;

        setVisible(true);

        return dialogResult;
    }

    public void setInterval(final Interval<Date> interval) {
        fromPicker.setDate(interval.getStart());
        toPicker.setDate(interval.getEnd());
    }

    public Interval<Date> getInterval() {
        return new Interval<Date>(fromPicker.getDate(), toPicker.getDate());
    }

    /**
     * Checks if the selected start date is before selected end date. The
     * methods checks the entered times when the dates are equal. If the start
     * time is greater than the end time the method will return false.
     * 
     * @return boolean value if selected start date is before selected end date.
     */
    private boolean isStartDateBeforeEndDate(final Date startDate, final Date endDate) {
        return startDate.compareTo(endDate) <= 0;
    }

    // ////////////////////////////////////////////////////////////////
    // Action Listener
    // ////////////////////////////////////////////////////////////////

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == setPeriodButton) {
            if (!isStartDateBeforeEndDate(fromPicker.getDate(), toPicker.getDate())) {
                JOptionPane.showMessageDialog(this, "Start date has to be before end date.", "Invalid period", JOptionPane.INFORMATION_MESSAGE);

                return;
            }

            dialogResult = PeriodPickerDialogResult.NEWPERIOD;
            setVisible(false);
        } else if (e.getSource() == cancelButton) {
            dialogResult = PeriodPickerDialogResult.CANCEL;
            setVisible(false);
        }
    }
}
