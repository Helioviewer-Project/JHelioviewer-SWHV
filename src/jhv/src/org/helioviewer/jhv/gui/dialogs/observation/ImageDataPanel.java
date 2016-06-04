package org.helioviewer.jhv.gui.dialogs.observation;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerNumberModel;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.gui.components.base.TimeTextField;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarEvent;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarListener;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModel;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModelListener;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.DataSources.Item;
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
    private final InstrumentsPanel instrumentsPanel = new InstrumentsPanel();
    private static boolean first = true;

    protected ImageDataPanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        JPanel timePane = new JPanel();
        timePane.setLayout(new BoxLayout(timePane, BoxLayout.PAGE_AXIS));
        timePane.add(timeSelectionPanel);
        timePane.add(cadencePanel);

        JPanel instrumentsPane = new JPanel(new BorderLayout());
        instrumentsPane.add(instrumentsPanel, BorderLayout.CENTER);

        add(timePane);
        add(instrumentsPane);
    }

    public void setupSources() {
        instrumentsPanel.setupSources(DataSources.getSingletonInstance());
        if (instrumentsPanel.validSelection()) {
            if (first) {
                first = false;

                Date endDate = new Date();
                Object timeStamp = DataSources.getObject(instrumentsPanel.getObservatory(), instrumentsPanel.getInstrument(),
                                                         instrumentsPanel.getDetector(), instrumentsPanel.getMeasurement(), "end");
                if (timeStamp instanceof String) {
                    try {
                        endDate = TimeUtils.sqlDateFormat.parse((String) timeStamp);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                GregorianCalendar gregorianCalendar = new GregorianCalendar();
                gregorianCalendar.setTime(endDate);

                gregorianCalendar.add(GregorianCalendar.SECOND, getCadence());
                setEndDate(gregorianCalendar.getTime(), false);

                gregorianCalendar.add(GregorianCalendar.DAY_OF_MONTH, -1);
                setStartDate(gregorianCalendar.getTime(), false);

                if (Boolean.parseBoolean(Settings.getSingletonInstance().getProperty("startup.loadmovie")))
                    loadRemote();
            }
        } else {
            Message.err("Could not retrieve data sources", "The list of available data could not be fetched, so you cannot use the GUI to add data." +
                        System.getProperty("line.separator") +
                        "This may happen if you do not have an internet connection or there are server problems. You can still open local files.", false);
        }
    }

    public Object getSourceId() {
        return DataSources.getObject(instrumentsPanel.getObservatory(), instrumentsPanel.getInstrument(),
                                     instrumentsPanel.getDetector(), instrumentsPanel.getMeasurement(), "sourceId");
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
     * Set a new end date and time
     *
     * @param newEnd
     *            new start date and time
     */
    public void setEndDate(Date newEnd, boolean byUser) {
        timeSelectionPanel.setEndDate(newEnd, byUser);
    }

    /**
     * Set a new start date and time
     *
     * @param newStart
     *            new start date and time
     */
    public void setStartDate(Date newStart, boolean byUser) {
        timeSelectionPanel.setStartDate(newStart, byUser);
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
    public void loadRemote() {
        Object sourceId = getSourceId();
        if (sourceId != null) {
            LoadRemoteTask remoteTask = new LoadRemoteTask(sourceId.toString(), getStartTime(), getEndTime(), getCadence());
            JHVGlobals.getExecutorService().execute(remoteTask);
        } else
            throw new IllegalArgumentException();
    }

    // Methods derived from Observation Dialog Panel

    @Override
    public boolean loadButtonPressed() {
        if (!instrumentsPanel.validSelection()) {
            Message.err("Data is not selected", "There is no information on what to add", false);
            return false;
        }

        ObservationDialogDateModel.getInstance().setStartDate(new Date(timeSelectionPanel.getStartTime()), true);
        ObservationDialogDateModel.getInstance().setEndDate(new Date(timeSelectionPanel.getEndTime()), true);

        // check if start date is before end date -> if not show message
        if (!timeSelectionPanel.isStartDateBeforeEndDate()) {
            JOptionPane.showMessageDialog(null, "End date is before start date", "", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        loadRemote();
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

            // create start date picker
            calendarStartDate = new JHVCalendarDatePicker();
            calendarStartDate.addJHVCalendarListener(this);
            calendarStartDate.setToolTipText("UTC date for observation start");

            // create start time field
            textStartTime = new TimeTextField();
            textStartTime.setToolTipText("UTC time for observation start");

            // add components to panel
            JPanel startDatePane = new JPanel(new BorderLayout());
            startDatePane.add(new JLabel("Start date"), BorderLayout.PAGE_START);
            startDatePane.add(calendarStartDate, BorderLayout.CENTER);

            JPanel startTimePane = new JPanel(new BorderLayout());
            startTimePane.add(new JLabel("Start time"), BorderLayout.PAGE_START);
            startTimePane.add(textStartTime, BorderLayout.CENTER);

            JPanel endDatePane = new JPanel(new BorderLayout());
            endDatePane.add(new JLabel("End date"), BorderLayout.PAGE_START);
            endDatePane.add(calendarEndDate, BorderLayout.CENTER);

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
                setStartDate(new Date(getStartTime()), true);
            } else if (e.getSource() == calendarEndDate) {
                setEndDate(new Date(getEndTime()), true);
            }
        }

        /**
         * Checks if the selected start date is before or equal to selected end date.
         *
         * @return boolean value if selected start date is before or equal to selected end
         *         date.
         */
        public boolean isStartDateBeforeEndDate() {
            return calendarStartDate.getDate().getTime() <= calendarEndDate.getDate().getTime();
        }

        /**
         * Returns the selected start time.
         *
         * @return selected start time.
         * */
        public long getStartTime() {
            return (calendarStartDate.getDate().getTime() / TimeUtils.DAY_IN_MILLIS) * TimeUtils.DAY_IN_MILLIS + textStartTime.getValue().getTime();
        }

        /**
         * Returns the selected end time.
         *
         * @return selected end time.
         */
        public long getEndTime() {
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

    // Instruments Panel

    /**
     * The panel bundles the components to select the instrument etc.
     * Reads the available data from org.helioviewer.jhv.io.DataSources
     * */
    private static class InstrumentsPanel extends JPanel {
        /**
         * Combobox to select observatory
         */
        private final JComboBox comboObservatory = new JComboBox(new String[] { "Loading..." });
        /**
         * Combobox to select instruments
         */
        private final JComboBox comboInstrument = new JComboBox(new String[] { "Loading..." });
        /**
         * Combobox to select detector and/or measurement
         */
        private final JComboBox comboDetectorMeasurement = new JComboBox(new String[] { "Loading..." });

        public InstrumentsPanel() {
            setLayout(new GridLayout(4, 2, GRIDLAYOUT_HGAP, GRIDLAYOUT_VGAP));

            JLabel labelServer = new JLabel("Server", JLabel.RIGHT);
            add(labelServer);
            add(DataSources.getServerComboBox());

            JLabel labelObservatory = new JLabel("Observatory", JLabel.RIGHT);
            add(labelObservatory);
            add(comboObservatory);

            JLabel labelInstrument = new JLabel("Instrument", JLabel.RIGHT);
            add(labelInstrument);
            add(comboInstrument);

            JLabel labelDetectorMeasurement = new JLabel("Detector/Measurement", JLabel.RIGHT);
            add(labelDetectorMeasurement);
            add(comboDetectorMeasurement);

            comboObservatory.setEnabled(false);
            comboInstrument.setEnabled(false);
            comboDetectorMeasurement.setEnabled(false);

            ListCellRenderer itemRenderer = new DefaultListCellRenderer() {
                // Override to show tooltip
                @Override
                public Component getListCellRendererComponent(JList list, Object value, int arg2, boolean arg3, boolean arg4) {
                    JLabel result = (JLabel) super.getListCellRendererComponent(list, value, arg2, arg3, arg4);
                    if (value instanceof DataSources.Item) {
                        DataSources.Item item = (DataSources.Item) value;
                        result.setToolTipText(item.getDescription());
                    } else if (value instanceof ItemPair) {
                        ItemPair item = (ItemPair) value;
                        result.setToolTipText(item.getDescription());
                    }
                    return result;
                }
            };

            comboObservatory.setRenderer(itemRenderer);
            comboInstrument.setRenderer(itemRenderer);
            comboDetectorMeasurement.setRenderer(itemRenderer);

            comboObservatory.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    setComboBox(comboInstrument, DataSources.getSingletonInstance().getInstruments(getObservatory()));
                }
            });

            comboInstrument.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    String obs = getObservatory();
                    String ins = getInstrument();

                    ArrayList<ItemPair> values = new ArrayList<ItemPair>();
                    Item[] detectors = DataSources.getSingletonInstance().getDetectors(obs, ins);

                    for (Item detector : detectors) {
                        Item[] measurements = DataSources.getSingletonInstance().getMeasurements(obs, ins, detector.getKey());

                        ItemPair.PrintMode printMode = ItemPair.PrintMode.BOTH;
                        if (detectors.length == 1) {
                            printMode = ItemPair.PrintMode.SECONDITEM_ONLY;
                        } else if (measurements.length == 1) {
                            printMode = ItemPair.PrintMode.FIRSTITEM_ONLY;
                        }

                        if (measurements.length == 0) { // not found
                            for (Item d : detectors)
                                values.add(new ItemPair(d, d, ItemPair.PrintMode.FIRSTITEM_ONLY));
                            break;
                        } else {
                            for (Item measurement : measurements)
                                values.add(new ItemPair(detector, measurement, printMode));
                        }
                    }

                    setComboBox(comboDetectorMeasurement, values);
                    comboDetectorMeasurement.setEnabled(values.size() != 0);
                }
            });
        }

        public void setupSources(DataSources source) {
            setComboBox(comboObservatory, source.getObservatories());
        }

        /**
         * Set the items combobox to the to the given parameter and selects the
         * first default item or otherwise the first item
         *
         * @param items
         *            string array which contains the names for the items of the
         *            combobox.
         * @param container
         *            combobox where to add the items.
         */
        private void setComboBox(JComboBox container, Item[] items) {
            container.setModel(new DefaultComboBoxModel(items));
            container.setEnabled(items.length != 0);
            for (int i = 0; i < items.length; i++) {
                if (items[i].isDefaultItem()) {
                    container.setSelectedIndex(i);
                    return;
                }
            }
            if (items.length > 0)
                container.setSelectedIndex(0);
        }

        /**
         * Set the items combobox to the to the given parameter and selects the
         * first default item or otherwise the first item
         *
         * @param items
         *            string array which contains the names for the items of the
         *            combobox.
         * @param container
         *            combobox where to add the items.
         */
        private void setComboBox(JComboBox container, ArrayList<ItemPair> items) {
            container.setModel(new DefaultComboBoxModel(items.toArray()));
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).isDefaultItem()) {
                    container.setSelectedIndex(i);
                    return;
                }
            }
            if (!items.isEmpty())
                container.setSelectedIndex(0);
        }

        /**
         * Checks whether the user did some valid selection
         *
         * @return true if the user did some valid selecion
         */
        public boolean validSelection() {
            return getObservatory() != null && getInstrument() != null && getDetector() != null && getMeasurement() != null;
        }

        /**
         * Returns the selected observation.
         *
         * @return selected observation (key value), null if no is selected
         * */
        public String getObservatory() {
            Object selectedItem = comboObservatory.getSelectedItem();
            if (selectedItem instanceof DataSources.Item) {
                return ((DataSources.Item) selectedItem).getKey();
            } else {
                return null;
            }
        }

        /**
         * Returns the selected instrument.
         *
         * @return selected instrument (key value), null if no is selected
         * */
        public String getInstrument() {
            Object selectedItem = comboInstrument.getSelectedItem();
            if (selectedItem instanceof DataSources.Item) {
                return ((DataSources.Item) selectedItem).getKey();
            } else {
                return null;
            }
        }

        /**
         * Returns the selected detector.
         *
         * @return selected detector (key value), null if no is selected
         * */
        public String getDetector() {
            Object selectedItem = comboDetectorMeasurement.getSelectedItem();
            if (selectedItem instanceof ItemPair) {
                return ((ItemPair) selectedItem).getFirstItem().getKey();
            } else {
                return null;
            }
        }

        /**
         * Returns the selected measurement.
         *
         * @return selected measurement (key value), null if no is selected
         * */
        public String getMeasurement() {
            Object selectedItem = comboDetectorMeasurement.getSelectedItem();
            if (selectedItem instanceof ItemPair) {
                return ((ItemPair) selectedItem).getSecondItem().getKey();
            } else {
                return null;
            }
        }

        private static class ItemPair {

            enum PrintMode {
                FIRSTITEM_ONLY, SECONDITEM_ONLY, BOTH
            }

            private final Item firstItem;
            private final Item secondItem;
            private final PrintMode printMode;

            public ItemPair(Item first, Item second, PrintMode newPrintMode) {
                firstItem = first;
                secondItem = second;
                printMode = newPrintMode;
            }

            public Item getFirstItem() {
                return firstItem;
            }

            public Item getSecondItem() {
                return secondItem;
            }

            public boolean isDefaultItem() {
                return firstItem.isDefaultItem() && secondItem.isDefaultItem();
            }

            @Override
            public String toString() {
                switch (printMode) {
                case FIRSTITEM_ONLY:
                    return firstItem.toString();
                case SECONDITEM_ONLY:
                    return secondItem.toString();
                default:
                    return firstItem.toString() + " " + secondItem.toString();
                }
            }

            public String getDescription() {
                switch (printMode) {
                case FIRSTITEM_ONLY:
                    return firstItem.getDescription();
                case SECONDITEM_ONLY:
                    return secondItem.getDescription();
                default:
                    return firstItem.getDescription() + " " + secondItem.getDescription();
                }
            }
        }
    }

}
