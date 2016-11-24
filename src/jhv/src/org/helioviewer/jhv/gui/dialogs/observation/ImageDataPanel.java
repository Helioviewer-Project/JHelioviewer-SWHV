package org.helioviewer.jhv.gui.dialogs.observation;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.gui.components.base.TimeTextField;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarEvent;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarListener;
import org.helioviewer.jhv.gui.components.calendar.JHVCarringtonPicker;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModel;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModelListener;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataSourcesParser;
import org.helioviewer.jhv.io.DataSourcesTree;

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

    ImageDataPanel() {
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
        if (!sourcesTree.setParsedData(parser)) // not preferred
            return;

        DataSourcesTree.SourceItem item = sourcesTree.getSelectedItem();
        if (item == null) { // not valid
            Message.err("Could not retrieve data sources", "The list of available data could not be fetched, so you cannot use the GUI to add data." + System.getProperty("line.separator") + "This may happen if you do not have an internet connection or there are server problems. You can still open local files.", false);
        } else if (first) {
            first = false;

            timeSelectionPanel.setStartTime(item.end - 2 * TimeUtils.DAY_IN_MILLIS, false);
            timeSelectionPanel.setEndTime(item.end, false);

            if (Boolean.parseBoolean(Settings.getSingletonInstance().getProperty("startup.loadmovie"))) {
                loadRemote(ImageLayer.createImageLayer(), item);
            }
        }
    }

    int getSourceId() {
        DataSourcesTree.SourceItem item = sourcesTree.getSelectedItem();
        return item == null ? -1 : item.sourceId;
    }

    public long getStartTime() {
        return timeSelectionPanel.getStartTime();
    }

    public long getEndTime() {
        return timeSelectionPanel.getEndTime();
    }

    public int getCadence() {
        return cadencePanel.getCadence();
    }

    private void loadRemote(ImageLayer layer, DataSourcesTree.SourceItem item) { // valid item
        layer.load(new APIRequest(item.server, item.sourceId, getStartTime(), getEndTime(), getCadence()));
    }

    public void setupLayer(Object layer) {
        if (layer instanceof ImageLayer) {
            APIRequest req = ((ImageLayer) layer).getAPIRequest();
            if (req != null) {
                sourcesTree.setSelectedItem(req.server, req.sourceId);
                timeSelectionPanel.setStartTime(req.startTime, false);
                timeSelectionPanel.setEndTime(req.endTime, false);
                cadencePanel.setCadence(req.cadence);
            }
        }
    }

    @Override
    public boolean loadButtonPressed(Object layer) {
        DataSourcesTree.SourceItem item = sourcesTree.getSelectedItem();
        if (item == null) { // not valid
            Message.err("Data is not selected", "There is no information on what to add", false);
            return false;
        }

        ObservationDialogDateModel.setStartTime(getStartTime(), true);
        ObservationDialogDateModel.setEndTime(getEndTime(), true);

        // check if start date is before end date -> if not show message
        if (!timeSelectionPanel.isStartTimeBeforeEndTime()) {
            JOptionPane.showMessageDialog(null, "End date is before start date", "", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (!(layer instanceof ImageLayer)) { // can't happen
            Log.error("Not ImageLayer");
            return false;
        }

        loadRemote((ImageLayer) layer, item);
        return true;
    }

    // Time Selection Panel

    private static class TimeSelectionPanel extends JPanel implements JHVCalendarListener, ObservationDialogDateModelListener {

        private final TimeTextField textStartTime;
        private final TimeTextField textEndTime;
        private final JHVCalendarDatePicker calendarStartDate;
        private final JHVCalendarDatePicker calendarEndDate;
        private final JHVCarringtonPicker carringtonStart;
        private final JHVCarringtonPicker carringtonEnd;

        private boolean setFromOutside = false;

        public TimeSelectionPanel() {
            ObservationDialogDateModel.addListener(this);

            setLayout(new GridLayout(2, 2, GRIDLAYOUT_HGAP, GRIDLAYOUT_VGAP));

            // create end date picker
            calendarEndDate = new JHVCalendarDatePicker();
            calendarEndDate.addJHVCalendarListener(this);
            calendarEndDate.setToolTipText("UTC date for observation end");

            // create end time field
            textEndTime = new TimeTextField();
            textEndTime.setToolTipText("UTC time for observation end. If equal to start time, a single image closest to the time will be added.");

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

        public void setEndTime(long endTime, boolean byUser) {
            calendarEndDate.setTime(endTime);
            textEndTime.setText(TimeUtils.timeDateFormat.format(endTime));
            if (!setFromOutside) {
                ObservationDialogDateModel.setEndTime(endTime, byUser);
            } else {
                setFromOutside = false;
            }
        }

        public void setStartTime(long startTime, boolean byUser) {
            calendarStartDate.setTime(startTime);
            textStartTime.setText(TimeUtils.timeDateFormat.format(startTime));
            if (!setFromOutside) {
                ObservationDialogDateModel.setStartTime(startTime, byUser);
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
                setStartTime(time, true);
                carringtonStart.setTime(time);
            } else if (e.getSource() == calendarEndDate) {
                long time = getEndTime();
                setEndTime(time, true);
                carringtonEnd.setTime(time);
            } else if (e.getSource() == carringtonStart) {
                setStartTime(carringtonStart.getTime(), true);
            } else if (e.getSource() == carringtonEnd) {
                setEndTime(carringtonEnd.getTime(), true);
            }
        }

        boolean isStartTimeBeforeEndTime() {
            return calendarStartDate.getTime() <= calendarEndDate.getTime();
        }

        private long getStartTime() {
            return (calendarStartDate.getTime() / TimeUtils.DAY_IN_MILLIS) * TimeUtils.DAY_IN_MILLIS + textStartTime.getTime();
        }

        private long getEndTime() {
            return (calendarEndDate.getTime() / TimeUtils.DAY_IN_MILLIS) * TimeUtils.DAY_IN_MILLIS + textEndTime.getTime();
        }

        @Override
        public void startTimeChanged(long startTime) {
            setFromOutside = true;
            setStartTime(startTime, false);
        }

        @Override
        public void endTimeChanged(long endTime) {
            setFromOutside = true;
            setEndTime(endTime, false);
        }
    }

    // Cadence Panel

    private static class CadencePanel extends JPanel implements ActionListener {

        private static final String[] timeStepUnitStrings = { "sec", "min", "hours", "days", "get all" };

        private final JSpinner spinnerCadence = new JSpinner();
        private final JComboBox<String> comboUnit = new JComboBox<>(timeStepUnitStrings);

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

        // Returns the number of seconds of the selected cadence
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
                value = APIRequest.CADENCE_ANY;
                break;
            default:
                break;
            }

            return value;
        }

        public void setCadence(int value) {
            if (value == APIRequest.CADENCE_ANY)
                comboUnit.setSelectedItem(timeStepUnitStrings[4]);
            else if (value / 86400 != 0) {
                comboUnit.setSelectedItem(timeStepUnitStrings[3]);
                spinnerCadence.setValue(value / 86400);
            } else if (value / 3600 != 0) {
                comboUnit.setSelectedItem(timeStepUnitStrings[2]);
                spinnerCadence.setValue(value / 3600);
            } else if (value / 60 != 0) {
                comboUnit.setSelectedItem(timeStepUnitStrings[1]);
                spinnerCadence.setValue(value / 60);
            } else {
                comboUnit.setSelectedItem(timeStepUnitStrings[0]);
                spinnerCadence.setValue(value);
            }
        }

    }

}
