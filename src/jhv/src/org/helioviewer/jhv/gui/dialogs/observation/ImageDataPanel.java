package org.helioviewer.jhv.gui.dialogs.observation;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.gui.components.base.TimeTextField;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarEvent;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarListener;
import org.helioviewer.jhv.gui.components.calendar.JHVCarringtonPicker;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModel;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModelListener;
import org.helioviewer.jhv.io.DataSourcesParser;
import org.helioviewer.jhv.io.DataSourcesTree;
import org.helioviewer.jhv.io.LoadRemoteTask;

/**
 * In order to select and load image data from the Helioviewer server this class
 * provides the corresponding user interface. The UI will be displayed within
 * the {@link ObservationDialog}.
 * */
@SuppressWarnings("serial")
public class ImageDataPanel extends ObservationDialogPanel {

    private final TimeSelectionPanel timeSelectionPanel = new TimeSelectionPanel();
    private final CadencePanel cadencePanel = new CadencePanel();
    private final DataSourcesTree sourcesTree = new DataSourcesTree();
    private static boolean first = true;

    protected ImageDataPanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        JPanel timePane = new JPanel();
        timePane.setLayout(new BoxLayout(timePane, BoxLayout.PAGE_AXIS));
        timePane.add(timeSelectionPanel);
        timePane.add(cadencePanel);

        JPanel instrumentsPane = new JPanel(new BorderLayout());
        instrumentsPane.add(new JScrollPane(sourcesTree));

        add(timePane);
        add(instrumentsPane);
    }

    public void setupSources(DataSourcesParser parser) {
        sourcesTree.setParsedData(parser);

        DataSourcesTree.SourceItem item = sourcesTree.getSelectedItem();
        if (item != null) { // valid
            if (first) {
                first = false;

                Date endDate = new Date(item.end);

                GregorianCalendar gregorianCalendar = new GregorianCalendar();
                gregorianCalendar.setTime(endDate);

                gregorianCalendar.add(GregorianCalendar.SECOND, getCadence());
                timeSelectionPanel.setEndDate(gregorianCalendar.getTime(), false);

                gregorianCalendar.add(GregorianCalendar.DAY_OF_MONTH, -1);
                timeSelectionPanel.setStartDate(gregorianCalendar.getTime(), false);

                if (Boolean.parseBoolean(Settings.getSingletonInstance().getProperty("startup.loadmovie"))) {
                    loadRemote(item);
                }
            }
        } else {
            Message.err("Could not retrieve data sources", "The list of available data could not be fetched, so you cannot use the GUI to add data." + System.getProperty("line.separator") + "This may happen if you do not have an internet connection or there are server problems. You can still open local files.", false);
        }
    }

    int getSourceId() {
        DataSourcesTree.SourceItem item = sourcesTree.getSelectedItem();
        if (item != null) // valid
            return item.sourceId;
        else
            return -1;
    }

    /**
     * Returns the selected start time.
     *
     * @return selected start time.
     * */
    public long getStartTime() {
        return timeSelectionPanel.getStartTime();
    }

    /**
     * Returns the selected end time.
     *
     * @return seleted end time.
     */
    public long getEndTime() {
        return timeSelectionPanel.getEndTime();
    }

    /**
     * Returns the selected cadence.
     *
     * @return selected cadence.
     */
    public int getCadence() {
        return cadencePanel.getCadence();
    }

    /**
     * Loads an image series from the Helioviewer server and adds a new layer to
     * the GUI which represents the image series.
     * */
    private void loadRemote(DataSourcesTree.SourceItem item) { // valid item
        LoadRemoteTask remoteTask = new LoadRemoteTask(item.server, item.sourceId, getStartTime(), getEndTime(), getCadence());
        JHVGlobals.getExecutorService().execute(remoteTask);
    }

    // Methods derived from Observation Dialog Panel

    @Override
    public boolean loadButtonPressed() {
        DataSourcesTree.SourceItem item = sourcesTree.getSelectedItem();
        if (item == null) { // not valid
            Message.err("Data is not selected", "There is no information on what to add", false);
            return false;
        }

        ObservationDialogDateModel.getInstance().setStartDate(new Date(getStartTime()), true);
        ObservationDialogDateModel.getInstance().setEndDate(new Date(getEndTime()), true);

        // check if start date is before end date -> if not show message
        if (!timeSelectionPanel.isStartDateBeforeEndDate()) {
            JOptionPane.showMessageDialog(null, "End date is before start date", "", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        loadRemote(item);
        return true;
    }

    // Time Selection Panel

    /**
     * The panel bundles the components to select the start and end time.
     * */
    private static class TimeSelectionPanel extends JPanel implements JHVCalendarListener, ObservationDialogDateModelListener {

        private final TimeTextField textStartTime;
        private final TimeTextField textEndTime;
        private final JHVCalendarDatePicker calendarStartDate;
        private final JHVCalendarDatePicker calendarEndDate;
        private final JHVCarringtonPicker carringtonStart;
        private final JHVCarringtonPicker carringtonEnd;

        private boolean setFromOutside = false;

        public TimeSelectionPanel() {
            ObservationDialogDateModel.getInstance().addListener(this);

            setLayout(new GridLayout(2, 2, GRIDLAYOUT_HGAP, GRIDLAYOUT_VGAP));

            // create end date picker
            calendarEndDate = new JHVCalendarDatePicker();
            calendarEndDate.addJHVCalendarListener(this);
            calendarEndDate.setToolTipText("UTC date for observation end");

            // create end time field
            textEndTime = new TimeTextField();
            textEndTime.setToolTipText("UTC time for observation end.\nIf equal to start time, a single image closest to the time will be added.");

            // create end date Carrington picker
            carringtonEnd = new JHVCarringtonPicker();
            carringtonEnd.addJHVCalendarListener(this);
            carringtonEnd.setToolTipText("Carrington rotation for observation end");
            carringtonEnd.setTime(getEndTime());

            // create start date picker
            calendarStartDate = new JHVCalendarDatePicker();
            calendarStartDate.addJHVCalendarListener(this);
            calendarStartDate.setToolTipText("UTC date for observation start");

            // create start time field
            textStartTime = new TimeTextField();
            textStartTime.setToolTipText("UTC time for observation start");

            // create start date Carrington picker
            carringtonStart = new JHVCarringtonPicker();
            carringtonStart.addJHVCalendarListener(this);
            carringtonStart.setToolTipText("Carrington rotation for observation start");
            carringtonStart.setTime(getStartTime());

            // add components to panel
            JPanel startDatePane = new JPanel(new BorderLayout());
            startDatePane.add(new JLabel("Start date"), BorderLayout.PAGE_START);
            startDatePane.add(calendarStartDate, BorderLayout.CENTER);
            startDatePane.add(carringtonStart, BorderLayout.LINE_END);

            JPanel startTimePane = new JPanel(new BorderLayout());
            startTimePane.add(new JLabel("Start time"), BorderLayout.PAGE_START);
            startTimePane.add(textStartTime, BorderLayout.CENTER);

            JPanel endDatePane = new JPanel(new BorderLayout());
            endDatePane.add(new JLabel("End date"), BorderLayout.PAGE_START);
            endDatePane.add(calendarEndDate, BorderLayout.CENTER);
            endDatePane.add(carringtonEnd, BorderLayout.LINE_END);

            JPanel endTimePane = new JPanel(new BorderLayout());
            endTimePane.add(new JLabel("End time"), BorderLayout.PAGE_START);
            endTimePane.add(textEndTime, BorderLayout.CENTER);

            add(startDatePane);
            add(startTimePane);
            add(endDatePane);
            add(endTimePane);
        }

        /**
         * Set a new end date and time
         *
         * @param newEnd
         *            new start date and time
         */
        public void setEndDate(Date newEnd, boolean byUser) {
            calendarEndDate.setDate(newEnd);
            textEndTime.setText(TimeUtils.timeDateFormat.format(newEnd));
            if (!setFromOutside) {
                ObservationDialogDateModel.getInstance().setEndDate(newEnd, byUser);
            } else {
                setFromOutside = false;
            }
        }

        /**
         * Set a new start date and time
         *
         * @param newStart
         *            new start date and time
         */
        public void setStartDate(Date newStart, boolean byUser) {
            calendarStartDate.setDate(newStart);
            textStartTime.setText(TimeUtils.timeDateFormat.format(newStart));
            if (!setFromOutside) {
                ObservationDialogDateModel.getInstance().setStartDate(newStart, byUser);
            } else {
                setFromOutside = false;
            }
        }

        /**
         * JHV calendar listener which notices when the user has chosen a date
         * by using the calendar component.
         */
        @Override
        public void actionPerformed(JHVCalendarEvent e) {
            if (e.getSource() == calendarStartDate) {
                long time = getStartTime();
                setStartDate(new Date(time), true);
                carringtonStart.setTime(time);
            } else if (e.getSource() == calendarEndDate) {
                long time = getEndTime();
                setEndDate(new Date(time), true);
                carringtonEnd.setTime(time);
            } else if (e.getSource() == carringtonStart) {
                setStartDate(new Date(carringtonStart.getTime()), true);
            } else if (e.getSource() == carringtonEnd) {
                setEndDate(new Date(carringtonEnd.getTime()), true);
            }
        }

        /**
         * Checks if the selected start date is before or equal to selected end
         * date.
         *
         * @return boolean value if selected start date is before or equal to
         *         selected end date.
         */
        boolean isStartDateBeforeEndDate() {
            return calendarStartDate.getDate().getTime() <= calendarEndDate.getDate().getTime();
        }

        /**
         * Returns the selected start time.
         *
         * @return selected start time.
         * */
        private long getStartTime() {
            return (calendarStartDate.getDate().getTime() / TimeUtils.DAY_IN_MILLIS) * TimeUtils.DAY_IN_MILLIS + textStartTime.getValue().getTime();
        }

        /**
         * Returns the selected end time.
         *
         * @return selected end time.
         */
        private long getEndTime() {
            return (calendarEndDate.getDate().getTime() / TimeUtils.DAY_IN_MILLIS) * TimeUtils.DAY_IN_MILLIS + textEndTime.getValue().getTime();
        }

        @Override
        public void startDateChanged(Date startDate) {
            setFromOutside = true;
            setStartDate(startDate, false);
        }

        @Override
        public void endDateChanged(Date endDate) {
            setFromOutside = true;
            setEndDate(endDate, false);
        }
    }

    // Cadence Panel

    /**
     * The panel bundles the components to select the cadence.
     * */
    private static class CadencePanel extends JPanel implements ActionListener {

        private static final String[] timeStepUnitStrings = { "sec", "min", "hours", "days", "get all" };

        private final JSpinner spinnerCadence = new JSpinner();
        private final JComboBox comboUnit = new JComboBox(timeStepUnitStrings);

        public CadencePanel() {
            setLayout(new GridLayout(1, 2, GRIDLAYOUT_HGAP, GRIDLAYOUT_VGAP));

            spinnerCadence.setPreferredSize(new Dimension(50, 25));
            spinnerCadence.setModel(new SpinnerNumberModel(30, 1, 1000000, 1));

            comboUnit.setSelectedItem("min");
            comboUnit.addActionListener(this);

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
            panel.add(spinnerCadence);
            panel.add(comboUnit);

            JLabel labelTimeStep = new JLabel("Time step", JLabel.RIGHT);
            add(labelTimeStep);
            add(panel);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == comboUnit) {
                spinnerCadence.setEnabled(comboUnit.getSelectedIndex() != 4);
            }
        }

        /**
         * Returns the number of seconds of the selected cadence.
         *
         * If no cadence is specified, returns -1.
         *
         * @return number of seconds of the selected cadence.
         * */
        public int getCadence() {
            int value = ((SpinnerNumberModel) spinnerCadence.getModel()).getNumber().intValue();

            switch (comboUnit.getSelectedIndex()) {
            case 1: // min
                value *= 60;
                break;
            case 2: // hour
                value *= 3600;
                break;
            case 3: // day
                value *= 86400;
                break;
            case 4:
                value = -1;
                break;
            default:
                break;
            }

            return value;
        }

    }

}
