package org.helioviewer.swhv.gui.layerpanel.daterangelayer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.helioviewer.basegui.components.TimeTextField;
import org.helioviewer.globalstate.GlobalStateContainer;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarEvent;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarListener;
import org.helioviewer.swhv.gui.GUISettings;
import org.helioviewer.swhv.gui.layerpanel.SWHVAbstractOptionPanel;

public class SWHVDateRangeLayerOptionPanel extends SWHVAbstractOptionPanel implements JHVCalendarListener {
    private JPanel mainPanel;
    private JPanel mainPanelContainer;
    private SWHVDateRangeLayerModel model;
    private static final long serialVersionUID = 1L;

    private final JLabel labelStartDate = new JLabel("Start Date");
    private final JLabel labelStartTime = new JLabel("Start Time");
    private final JLabel labelEndDate = new JLabel("End Date");
    private final JLabel labelEndTime = new JLabel("End Time");

    private TimeTextField textStartTime;
    private TimeTextField textEndTime;
    private JHVCalendarDatePicker calendarStartDate;
    private JHVCalendarDatePicker calendarEndDate;
    private JButton submitDateButton;

    public SWHVDateRangeLayerOptionPanel(SWHVDateRangeLayerModel model) {
        this.model = model;
        createMainPanel();
        setPreferredSize(new Dimension(GUISettings.LEFTPANELWIDTH, GUISettings.OPTIONSHEIGHT));
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                addTab("Select date range", mainPanelContainer);
            }
        });
    }

    protected void createMainPanel() {
        getMinimumSize().width = GUISettings.LEFTPANELWIDTH;
        mainPanelContainer = new JPanel();
        mainPanel = new JPanel();
        initVisualComponents();
        mainPanelContainer.setLayout(new BoxLayout(mainPanelContainer, BoxLayout.PAGE_AXIS));
        mainPanel.setMaximumSize(mainPanel.getPreferredSize());
        mainPanelContainer.add(mainPanel);
        submitDateButton = new JButton("Submit");

        submitDateButton.addActionListener(model.getDateRangeLayerController().getDateRangeLayerSetDateActionListener());
        mainPanelContainer.add(submitDateButton);
        mainPanelContainer.add(GlobalStateContainer.getSingletonInstance().getChooseTypeContainerPanel());
        mainPanelContainer.validate();
        mainPanelContainer.repaint();
        JPanel ppanel = new JPanel();
        ppanel.setBackground(Color.GREEN);
        ppanel.getMinimumSize().height = 0;
        ppanel.getPreferredSize().height = 0;
        ppanel.getMaximumSize().height = 0;

        mainPanelContainer.add(ppanel);
        mainPanelContainer.validate();
        mainPanelContainer.repaint();

    }

    /**
     * Sets up the visual sub components and the component itself.
     * */
    private void initVisualComponents() {
        // set basic layout
        int GRIDLAYOUT_HGAP = 2;
        int GRIDLAYOUT_VGAP = 2;
        mainPanel.setLayout(new GridLayout(2, 2, GRIDLAYOUT_HGAP, GRIDLAYOUT_VGAP));
        // create end date picker
        calendarEndDate = new JHVCalendarDatePicker();
        calendarEndDate.setDateFormat(Settings.getSingletonInstance().getProperty("default.date.format"));
        calendarEndDate.addJHVCalendarListener(this);
        calendarEndDate.setToolTipText("Date in UTC ending the observation.\nIf its equal the start a single image closest to the time will be added.");

        // create end time field
        textEndTime = new TimeTextField();
        textEndTime.setToolTipText("Time in UTC ending the observation.\nIf its equal the start a single image closest to the time will be added.");

        // create start date picker
        calendarStartDate = new JHVCalendarDatePicker();
        calendarStartDate.setDateFormat(Settings.getSingletonInstance().getProperty("default.date.format"));
        calendarStartDate.addJHVCalendarListener(this);
        calendarStartDate.setToolTipText("Date in UTC starting the observation");

        // create start time field
        textStartTime = new TimeTextField();
        textStartTime.setToolTipText("Time in UTC starting the observation");

        // set date format to components
        updateDateFormat();

        // add components to panel
        final JPanel startDatePane = new JPanel(new BorderLayout());
        startDatePane.add(labelStartDate, BorderLayout.PAGE_START);
        startDatePane.add(calendarStartDate, BorderLayout.CENTER);

        final JPanel startTimePane = new JPanel(new BorderLayout());
        startTimePane.add(labelStartTime, BorderLayout.PAGE_START);
        startTimePane.add(textStartTime, BorderLayout.CENTER);

        final JPanel endDatePane = new JPanel(new BorderLayout());
        endDatePane.add(labelEndDate, BorderLayout.PAGE_START);
        endDatePane.add(calendarEndDate, BorderLayout.CENTER);

        final JPanel endTimePane = new JPanel(new BorderLayout());
        endTimePane.add(labelEndTime, BorderLayout.PAGE_START);
        endTimePane.add(textEndTime, BorderLayout.CENTER);

        mainPanel.add(startDatePane);
        mainPanel.add(startTimePane);
        mainPanel.add(endDatePane);
        mainPanel.add(endTimePane);
    }

    public void setupTime() {
        final Date endDate = new Date();//APIRequestManager.getLatestImageDate(instrumentsPanel.getObservatory(), instrumentsPanel.getInstrument(), instrumentsPanel.getDetector(), instrumentsPanel.getMeasurement());
        final GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(endDate);

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                calendarEndDate.setDate(gregorianCalendar.getTime());
                textEndTime.setText(TimeTextField.formatter.format(gregorianCalendar.getTime()));
                gregorianCalendar.add(GregorianCalendar.DAY_OF_MONTH, -1);
                calendarStartDate.setDate(gregorianCalendar.getTime());
                textStartTime.setText(TimeTextField.formatter.format(gregorianCalendar.getTime()));
            }
        });
    }

    /**
     * Set a new end date and time
     * 
     * @param newEnd
     *            new start date and time
     */
    public void setEndDate(Date newEnd) {
        calendarEndDate.setDate(newEnd);
        textEndTime.setText(TimeTextField.formatter.format(newEnd));
    }

    /**
     * Set a new start date and time
     * 
     * @param newStart
     *            new start date and time
     */
    public void setStartDate(Date newStart) {
        calendarStartDate.setDate(newStart);
        textStartTime.setText(TimeTextField.formatter.format(newStart));
    }

    /**
     * Updates the date format to the calendar components.
     */
    public void updateDateFormat() {
        String pattern = Settings.getSingletonInstance().getProperty("default.date.format");

        calendarStartDate.setDateFormat(pattern);
        calendarEndDate.setDateFormat(pattern);

        calendarStartDate.setDate(calendarStartDate.getDate());
        calendarEndDate.setDate(calendarEndDate.getDate());
    }

    /**
     * JHV calendar listener which notices when the user has chosen a date by
     * using the calendar component.
     */
    @Override
    public void actionPerformed(JHVCalendarEvent e) {
        Object source = e.getSource();
        if (source == calendarStartDate && !isStartDateBeforeEndDate()) {
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(calendarStartDate.getDate());
            calendar.add(Calendar.DATE, 1);
            calendarEndDate.setDate(calendar.getTime());
        }

        if (source == calendarEndDate && !isStartDateBeforeEndDate()) {
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(calendarEndDate.getDate());
            calendar.add(Calendar.DATE, -1);
            calendarStartDate.setDate(calendar.getTime());
        }
    }

    /**
     * Checks if the selected start date is before selected end date. The
     * methods checks the entered times when the dates are equal. If the start
     * time is greater or equal than the end time the method will return false.
     * 
     * @return boolean value if selected start date is before selected end date.
     */
    public boolean isStartDateBeforeEndDate() {
        return getStartTime().compareTo(getEndTime()) <= 0;
    }

    /**
     * Returns the selected start time.
     * 
     * @return selected start time.
     * */
    public String getStartTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'");
        return dateFormat.format(calendarStartDate.getDate()) + textStartTime.getFormattedInput() + "Z";
    }

    /**
     * Returns the selected end time.
     * 
     * @return selected end time.
     */
    public String getEndTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'");
        return dateFormat.format(calendarEndDate.getDate()) + textEndTime.getFormattedInput() + "Z";
    }

    @Override
    public void setActive() {
        mainPanelContainer.add(GlobalStateContainer.getSingletonInstance().getChooseTypeContainerPanel());
        mainPanelContainer.validate();
        mainPanelContainer.repaint();
    }

    public Date getBeginDate() {
        try {
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(calendarStartDate.getDate());
            calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE), 0, 0, 0);
            Date tdate = textStartTime.getValue();
            Calendar calendar2 = new GregorianCalendar();
            calendar2.setTime(tdate);
            calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE), calendar2.get(Calendar.HOUR_OF_DAY), calendar2.get(Calendar.MINUTE), calendar2.get(Calendar.SECOND));
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            return calendar.getTime();
        } catch (Exception e) {
            return new Date(0);
        }
    }

    public Date getEndDate() {
        try {
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(calendarEndDate.getDate());
            calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE), 0, 0, 0);
            Date tdate = textEndTime.getValue();
            Calendar calendar2 = new GregorianCalendar();
            calendar2.setTime(tdate);
            calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE), calendar2.get(Calendar.HOUR_OF_DAY), calendar2.get(Calendar.MINUTE), calendar2.get(Calendar.SECOND));
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            return calendar.getTime();
        } catch (Exception e) {
            return new Date(0);
        }
    }

    public void setDateRangeLayerModel(SWHVDateRangeLayerModel swhvDateRangeLayerModel) {
        this.model = swhvDateRangeLayerModel;
    }
}
