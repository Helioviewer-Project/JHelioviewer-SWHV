package org.helioviewer.jhv.gui.dialogs.observation;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
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
import org.helioviewer.jhv.gui.components.DateTimePanel;
import org.helioviewer.jhv.gui.components.calendar.JHVCarringtonPicker;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.CommandLine;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.DataSourcesParser;
import org.helioviewer.jhv.io.DataSourcesTree;

/**
 * In order to select and load image data from the Helioviewer server this class
 * provides the corresponding user interface. The UI will be displayed within
 * the {@link ObservationDialog}.
 * */
@SuppressWarnings("serial")
public class ImageDataPanel extends JPanel {

    private final TimeSelectionPanel timeSelectionPanel = new TimeSelectionPanel();
    private final CadencePanel cadencePanel = new CadencePanel();
    private final DataSourcesTree sourcesTree = new DataSourcesTree();
    private static boolean first = true;

    ImageDataPanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        add(timeSelectionPanel);
        add(cadencePanel);
        add(new JScrollPane(sourcesTree));
    }

    JComponent getFocused() {
        return sourcesTree;
    }

    public void setupSources(DataSourcesParser parser) {
        if (!sourcesTree.setParsedData(parser)) // not preferred
            return;

        CommandLine.load();

        DataSourcesTree.SourceItem item = sourcesTree.getSelectedItem();
        if (item == null) { // not valid
            Message.err("Could not retrieve data sources", "The list of available data could not be fetched, so you cannot use the GUI to add data." + System.getProperty("line.separator") + "This may happen if you do not have an internet connection or there are server problems. You can still open local files.", false);
        } else if (first) {
            first = false;

            timeSelectionPanel.setStartTime(item.end - 2 * TimeUtils.DAY_IN_MILLIS);
            timeSelectionPanel.setEndTime(item.end);

            if (Boolean.parseBoolean(Settings.getSingletonInstance().getProperty("startup.loadmovie"))) {
                loadRemote(ImageLayer.createImageLayer(), item);
            }
        }
    }

    String getAvailabilityURL() {
        DataSourcesTree.SourceItem item = sourcesTree.getSelectedItem();
        return item == null ? null : DataSources.getServerSetting(item.server, "availability.images") + "#IID" + item.sourceId;
    }

    private long getStartTime() {
        return timeSelectionPanel.getStartTime();
    }

    private long getEndTime() {
        return timeSelectionPanel.getEndTime();
    }

    public int getCadence() {
        return cadencePanel.getCadence();
    }

    private void loadRemote(ImageLayer layer, DataSourcesTree.SourceItem item) { // valid item
        layer.load(new APIRequest(item.server, item.sourceId, getStartTime(), getEndTime(), getCadence()));
    }

    public void setupLayer(ImageLayer layer) {
        APIRequest req = layer.getAPIRequest();
        if (req != null) {
            sourcesTree.setSelectedItem(req.server, req.sourceId);
            timeSelectionPanel.setStartTime(req.startTime);
            timeSelectionPanel.setEndTime(req.endTime);
            cadencePanel.setCadence(req.cadence);
        }
    }

    boolean loadButtonPressed(Object layer) {
        DataSourcesTree.SourceItem item = sourcesTree.getSelectedItem();
        if (item == null) { // not valid
            Message.err("Data is not selected", "There is no information on what to add", false);
            return false;
        }

        // show message if end date before start date
        if (getStartTime() > getEndTime()) {
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
    private static class TimeSelectionPanel extends JPanel {

        private final DateTimePanel startDateTimePanel = new DateTimePanel("Start");
        private final DateTimePanel endDateTimePanel = new DateTimePanel("End");
        private final JHVCarringtonPicker startCarrington = new JHVCarringtonPicker();
        private final JHVCarringtonPicker endCarrington = new JHVCarringtonPicker();

        public TimeSelectionPanel() {
            startDateTimePanel.addListener(e -> setStartTime(startDateTimePanel.getTime()));
            endDateTimePanel.addListener(e -> setEndTime(endDateTimePanel.getTime()));
            startCarrington.addJHVCalendarListener(e -> setStartTime(startCarrington.getTime()));
            endCarrington.addJHVCalendarListener(e -> setEndTime(endCarrington.getTime()));

            startDateTimePanel.add(startCarrington);
            endDateTimePanel.add(endCarrington);

            setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.LINE_START;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weighty = 0;

            c.gridy = 0;
            c.gridx = 0;
            c.weightx = 1;
            add(startDateTimePanel, c);
            c.gridx = 1;
            c.weightx = 0;
            add(startCarrington, c);

            c.gridy = 1;
            c.gridx = 0;
            c.weightx = 1;
            add(endDateTimePanel, c);
            c.gridx = 1;
            c.weightx = 0;
            add(endCarrington, c);
        }

        void setStartTime(long time) {
            startDateTimePanel.setTime(time);
            startCarrington.setTime(time);
        }

        void setEndTime(long time) {
            endDateTimePanel.setTime(time);
            endCarrington.setTime(time);
        }

        long getStartTime() {
            return startDateTimePanel.getTime();
        }

        long getEndTime() {
            return endDateTimePanel.getTime();
        }

    }

    // Cadence Panel
    private static class CadencePanel extends JPanel {

        private static final String[] timeStepUnitStrings = { "sec", "min", "hours", "days", "get all" };

        private final JSpinner spinnerCadence = new JSpinner(new SpinnerNumberModel(1, 1, 1000000, 1));
        private final JComboBox<String> comboUnit = new JComboBox<>(timeStepUnitStrings);

        public CadencePanel() {
            setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 0));

            setCadence(APIRequest.CADENCE_DEFAULT);
            spinnerCadence.setPreferredSize(new Dimension(50, spinnerCadence.getPreferredSize().height));
            comboUnit.setSelectedItem("min");
            comboUnit.addActionListener(e -> spinnerCadence.setEnabled(comboUnit.getSelectedIndex() != 4));

            add(new JLabel("Time step", JLabel.RIGHT));
            add(spinnerCadence);
            add(comboUnit);
        }

        // Returns the number of seconds of the selected cadence
        public int getCadence() {
            int value = (Integer) spinnerCadence.getValue();

            switch (comboUnit.getSelectedIndex()) {
            case 1: // min
                return value * 60;
            case 2: // hour
                return value * 3600;
            case 3: // day
                return value * 86400;
            case 4:
                return APIRequest.CADENCE_ANY;
            default:
                return value;
            }
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
