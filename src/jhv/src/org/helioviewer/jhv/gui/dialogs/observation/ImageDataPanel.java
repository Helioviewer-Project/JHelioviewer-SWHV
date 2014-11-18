package org.helioviewer.jhv.gui.dialogs.observation;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Vector;

import javax.swing.BorderFactory;
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
import javax.swing.border.EmptyBorder;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.message.Message;
import org.helioviewer.basegui.components.TimeTextField;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarEvent;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarListener;
import org.helioviewer.jhv.io.APIRequestManager;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.DataSources.Item;

/**
 * In order to select and load image data from the Helioviewer server this class
 * provides the corresponding user interface. The UI will be displayed within
 * the {@link ObservationDialog}.
 *
 * @author Stephan Pagel
 * */
public class ImageDataPanel extends ObservationDialogPanel {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    private boolean enableLoadButton = false;
    private boolean isSelected = false;

    private final TimeSelectionPanel timeSelectionPanel = new TimeSelectionPanel();
    private final CadencePanel cadencePanel = new CadencePanel();
    private final InstrumentsPanel instrumentsPanel;

    /**
     * Used format for the API of the data and time
     */
    public static final SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    /**
     * Default constructor.
     * */
    public ImageDataPanel() {
        super();
        instrumentsPanel = new InstrumentsPanel(this);
        initVisualComponents();
        initDataSources(false);
    }

    /**
     * Sets up the visual sub components and the component itself.
     * */
    private void initVisualComponents() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        final JPanel timePane = new JPanel();
        timePane.setLayout(new BoxLayout(timePane, BoxLayout.PAGE_AXIS));
        timePane.setBorder(BorderFactory.createTitledBorder(" Select time range of interest "));
        timePane.add(timeSelectionPanel);
        timePane.add(cadencePanel);

        final JPanel instrumentsPane = new JPanel();
        instrumentsPane.setLayout(new BorderLayout());
        instrumentsPane.setBorder(BorderFactory.createTitledBorder(" Choose experiment specific data source "));
        instrumentsPane.add(instrumentsPanel, BorderLayout.CENTER);

        add(timePane);
        add(instrumentsPane);
    }

    /**
     * Adds available data to the displayed components
     * */
    private void initDataSources(final boolean donotloadStartup) {
        // Start the longer taking setups of the data sources and the time a new
        // thread
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    instrumentsPanel.setupSources();

                    enableLoadButton = true;
                    if (isSelected) {
                        ObservationDialog.getSingletonInstance().setLoadButtonEnabled(enableLoadButton);
                    }

                    // Check if we were able to set it up
                    if (instrumentsPanel.validSelection()) {
                        timeSelectionPanel.setupTime();

                        if (!donotloadStartup && Boolean.parseBoolean(Settings.getSingletonInstance().getProperty("startup.loadmovie"))) {
                            // wait until view chain is ready to go
                            while (ImageViewerGui.getSingletonInstance() == null || ImageViewerGui.getSingletonInstance().getMainView() == null) {
                                Thread.sleep(100);
                            }

                            loadMovie();
                        }
                    } else {
                        Message.err("Could not retrieve data sources", "The list of avaible data could not be fetched. So you cannot use the GUI to add data!" + System.getProperty("line.separator") + " This may happen if you do not have an internet connection or the there are server problems. You can still open local files.", false);
                    }
                } catch (InterruptedException e) {
                    Log.error("Could not setup observation dialog", e);
                    Message.err("Could not retrieve data sources", "The list of avaible data could not be fetched. So you cannot use the GUI to add data!" + System.getProperty("line.separator") + " This may happen if you do not have an internet connection or the there are server problems. You can still open local files.", false);
                } catch (InvocationTargetException e) {
                    Log.error("Could not setup observation dialog", e);
                    Message.err("Could not retrieve data sources", "The list of avaible data could not be fetched. So you cannot use the GUI to add data!" + System.getProperty("line.separator") + " This may happen if you do not have an internet connection or the there are server problems. You can still open local files.", false);
                }
            }
        }, "ObservationSetup");
        t.start();
    }

    /**
     * Returns the selected start time.
     *
     * @return selected start time.
     * */
    public String getStartTime() {
        return timeSelectionPanel.getStartTime();
    }

    /**
     * Returns the selected end time.
     *
     * @return seleted end time.
     */
    public String getEndTime() {
        return timeSelectionPanel.getEndTime();
    }

    /**
     * Set a new end date and time
     *
     * @param newEnd
     *            new start date and time
     */
    public void setEndDate(Date newEnd) {
        timeSelectionPanel.setEndDate(newEnd);
    }

    /**
     * Set a new start date and time
     *
     * @param newStart
     *            new start date and time
     */
    public void setStartDate(Date newStart) {
        timeSelectionPanel.setStartDate(newStart);
    }

    /**
     * Returns the selected cadence.
     *
     * @return selected cadence.
     */
    public String getCadence() {
        return Integer.toString(cadencePanel.getCadence());
    }

    /**
     * Returns the selected observatory.
     *
     * @return selected observatory.
     */
    public String getObservation() {
        return instrumentsPanel.getObservatory();
    }

    /**
     * Returns the selected instrument.
     *
     * @return selected instrument.
     * */
    public String getInstrument() {
        return instrumentsPanel.getInstrument();
    }

    /**
     * Returns the selected detector.
     *
     * @return selected detector.
     * */
    public String getDetector() {
        return instrumentsPanel.getDetector();
    }

    /**
     * Returns the selected measurement.
     *
     * @return selected measurement.
     * */
    public String getMeasurement() {
        return instrumentsPanel.getMeasurement();
    }

    /**
     * Updates the visual behavior of the component.
     */
    public void updateComponent() {
        timeSelectionPanel.updateDateFormat();
    }

    /**
     * Loads an image from the Helioviewer server and adds a new layer to the
     * GUI which represents the image.
     * */
    private void loadImage() {

        // show loading animation
        ImageViewerGui.getSingletonInstance().getMainImagePanel().setLoading(true);

        // download and open the requested image in a separated thread and hide
        // loading animation when finished
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {

                try {
                    APIRequestManager.requestAndOpenRemoteFile(true, null, getStartTime(), "", getObservation(), getInstrument(), getDetector(), getMeasurement(), true);
                } catch (IOException e) {
                    Log.error("An error occured while opening the remote file!", e);
                    Message.err("An error occured while opening the remote file!", e.getMessage(), false);
                } finally {
                    ImageViewerGui.getSingletonInstance().getMainImagePanel().setLoading(false);
                }
            }
        }, "LoadNewImage");

        thread.start();
    }

    /**
     * Loads an image series from the Helioviewer server and adds a new layer to
     * the GUI which represents the image series.
     * */
    private void loadMovie() {

        // show loading animation
        ImageViewerGui.getSingletonInstance().getTopToolBar().disableStateButton();

        ImageViewerGui.getSingletonInstance().getMainImagePanel().setLoading(true);

        // download and open the requested movie in a separated thread and hide
        // loading animation when finished
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    APIRequestManager.requestAndOpenRemoteFile(true, getCadence(), getStartTime(), getEndTime(), getObservation(), getInstrument(), getDetector(), getMeasurement(), true);
                } catch (IOException e) {
                    Log.error("An error occured while opening the remote file!", e);
                    Message.err("An error occured while opening the remote file!", e.getMessage(), false);
                } finally {
                    ImageViewerGui.getSingletonInstance().getMainImagePanel().setLoading(false);
                    ImageViewerGui.getSingletonInstance().getTopToolBar().enableStateButton();

                }
            }
        }, "LoadNewMovie");

        thread.start();
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Methods derived from Observation Dialog Panel
    // //////////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     * */
    @Override
    public void selected() {
        isSelected = true;
        ObservationDialog.getSingletonInstance().setLoadButtonEnabled(enableLoadButton);
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public void deselected() {
        isSelected = false;
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public boolean loadButtonPressed() {
        // Add some data if its nice
        if (!instrumentsPanel.validSelection()) {
            Message.err("Data is not selected", "There is no information what to add", false);
            return false;
        }
        if (timeSelectionPanel.getStartTime().equals(timeSelectionPanel.getEndTime())) {
            // load image
            loadImage();

        } else {
            // check if start date is before end date -> if not show message
            if (!timeSelectionPanel.isStartDateBeforeEndDate()) {
                JOptionPane.showMessageDialog(null, "End date is before start date!", "", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            loadMovie();
        }

        return true;
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public void cancelButtonPressed() {
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public void dialogOpened() {
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Time Selection Panel
    // //////////////////////////////////////////////////////////////////////////////

    /**
     * The panel bundles the components to select the start and end time.
     *
     * @author Stephan Pagel
     * */
    private class TimeSelectionPanel extends JPanel implements JHVCalendarListener {

        // //////////////////////////////////////////////////////////////////////////
        // Definitions
        // //////////////////////////////////////////////////////////////////////////

        private static final long serialVersionUID = 1L;

        private final JLabel labelStartDate = new JLabel("Start Date");
        private final JLabel labelStartTime = new JLabel("Start Time");
        private final JLabel labelEndDate = new JLabel("End Date");
        private final JLabel labelEndTime = new JLabel("End Time");

        private TimeTextField textStartTime;
        private TimeTextField textEndTime;
        private JHVCalendarDatePicker calendarStartDate;
        private JHVCalendarDatePicker calendarEndDate;

        // //////////////////////////////////////////////////////////////////////////
        // Methods
        // //////////////////////////////////////////////////////////////////////////

        public TimeSelectionPanel() {
            // set up the visual components (GUI)
            initVisualComponents();
        }

        /**
         * Sets up the visual sub components and the component itself.
         * */
        private void initVisualComponents() {
            // set basic layout
            setLayout(new GridLayout(2, 2, GRIDLAYOUT_HGAP, GRIDLAYOUT_VGAP));

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

            add(startDatePane);
            add(startTimePane);
            add(endDatePane);
            add(endTimePane);

            // add(labelStartDate);
            // add(labelStartTime);
            // add(calendarStartDate);
            // add(textStartTime);
            // add(labelEndDate);
            // add(labelEndTime);
            // add(calendarEndDate);
            // add(textEndTime);
        }

        /**
         * Sets the latest available image (or now if fails) to the end time and
         * the start 24h earlier.
         * <p>
         * Can be called from any thread and will take care that the GUI
         * operations run in EventQueue.
         * <p>
         * Must be called after the instrumentPanel has been setup
         *
         * @throws InvocationTargetException
         *             From inserting into the AWT Queue
         * @throws InterruptedException
         *             From inserting into the AWT Queue
         */
        public void setupTime() throws InterruptedException, InvocationTargetException {
            final Date endDate = APIRequestManager.getLatestImageDate(instrumentsPanel.getObservatory(), instrumentsPanel.getInstrument(), instrumentsPanel.getDetector(), instrumentsPanel.getMeasurement(), true);
            final GregorianCalendar gregorianCalendar = new GregorianCalendar();
            gregorianCalendar.setTime(endDate);
            gregorianCalendar.add(GregorianCalendar.SECOND, cadencePanel.getCadence());
            // The data is there, now just set
            EventQueue.invokeAndWait(new Runnable() {
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
         * JHV calendar listener which notices when the user has chosen a date
         * by using the calendar component.
         */
        @Override
        public void actionPerformed(JHVCalendarEvent e) {

            if (e.getSource() == calendarStartDate && !isStartDateBeforeEndDate()) {

                Calendar calendar = new GregorianCalendar();
                calendar.setTime(calendarStartDate.getDate());
                calendar.add(Calendar.DATE, 1);
                calendarEndDate.setDate(calendar.getTime());
            }

            if (e.getSource() == calendarEndDate && !isStartDateBeforeEndDate()) {

                Calendar calendar = new GregorianCalendar();
                calendar.setTime(calendarEndDate.getDate());
                calendar.add(Calendar.DATE, -1);
                calendarStartDate.setDate(calendar.getTime());
            }
        }

        /**
         * Checks if the selected start date is before selected end date. The
         * methods checks the entered times when the dates are equal. If the
         * start time is greater or equal than the end time the method will
         * return false.
         *
         * @return boolean value if selected start date is before selected end
         *         date.
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
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Cadence Panel
    // //////////////////////////////////////////////////////////////////////////////

    /**
     * The panel bundles the components to select the cadence.
     *
     * @author Stephan Pagel
     * */
    @SuppressWarnings("unused")
    private class CadencePanel extends JPanel implements ActionListener {

        // //////////////////////////////////////////////////////////////////////////
        // Definitions
        // //////////////////////////////////////////////////////////////////////////

        private static final long serialVersionUID = 1L;

        private final String[] timeStepUnitStrings = { "sec", "min", "hours", "days", "get all" };

        private final static int TIMESTEP_SECONDS = 0;
        private final static int TIMESTEP_MINUTES = 1;
        private final static int TIMESTEP_HOURS = 2;
        private final static int TIMESTEP_DAYS = 3;
        private final static int TIMESTEP_ALL = 4;

        private final JLabel labelTimeStep = new JLabel("Time Step");
        private final JSpinner spinnerCadence = new JSpinner();
        private final JComboBox comboUnit = new JComboBox(timeStepUnitStrings);

        // //////////////////////////////////////////////////////////////////////////
        // Methods
        // //////////////////////////////////////////////////////////////////////////

        /**
         * Default constructor.
         * */
        public CadencePanel() {
            // set up the visual components (GUI)
            initVisualComponents();
        }

        /**
         * Sets up the visual sub components and the component itself.
         * */
        private void initVisualComponents() {

            // set basic layout
            setLayout(new GridLayout(1, 2, GRIDLAYOUT_HGAP, GRIDLAYOUT_VGAP));
            setBorder(new EmptyBorder(3, 0, 0, 0));

            spinnerCadence.setPreferredSize(new Dimension(50, 25));
            spinnerCadence.setModel(new SpinnerNumberModel(30, 1, 1000000, 1));

            comboUnit.setSelectedIndex(TIMESTEP_MINUTES);
            comboUnit.addActionListener(this);

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
            panel.add(spinnerCadence);
            panel.add(comboUnit);

            // add components to panel
            add(labelTimeStep);
            add(panel);
        }

        /**
         * {@inheritDoc}
         */
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
            }

            return value;
        }
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Instruments Panel
    // //////////////////////////////////////////////////////////////////////////////

    /**
     * The panel bundles the components to select the instrument etc.
     * <p>
     * Reads the available data from org.helioviewer.jhv.io.DataSources
     *
     * @author rewritten Helge Dietert
     * @author original Stephan Pagel
     * */
    private static class InstrumentsPanel extends JPanel {

        private static final long serialVersionUID = 1L;
        /**
         * Label for observatory
         */
        private final JLabel labelObservatory = new JLabel("Observatory");
        /**
         * Label for instrument
         */
        private final JLabel labelInstrument = new JLabel("Instrument");
        /**
         * Label for detector and/or measurement
         */
        private final JLabel labelDetectorMeasurement = new JLabel("Detector/Measurement");
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
        private final JComboBox comboServer = new JComboBox(new String[] { "ROB", "Helioviewer.org", "IAS" });
        private final JLabel labelServer = new JLabel("Server");
        private ImageDataPanel imageDataPanel;

        /**
         * Default constructor which will setup the components and add listener
         * to update the available choices
         *
         * @param imageDataPanel
         */
        public InstrumentsPanel(final ImageDataPanel imageDataPanel) {
            // Setup grid
            this.imageDataPanel = imageDataPanel;
            setLayout(new GridLayout(4, 2, GRIDLAYOUT_HGAP, GRIDLAYOUT_VGAP));
            add(labelServer);
            add(comboServer);
            add(labelObservatory);
            add(comboObservatory);
            add(labelInstrument);
            add(comboInstrument);
            add(labelDetectorMeasurement);
            add(comboDetectorMeasurement);
            comboObservatory.setEnabled(false);
            comboInstrument.setEnabled(false);
            comboDetectorMeasurement.setEnabled(false);

            // Advanced rendering with tooltips for the items
            final ListCellRenderer itemRenderer = new DefaultListCellRenderer() {
                /**
                 *
                 */
                private static final long serialVersionUID = 1L;

                /**
                 * Override display component to show tooltip
                 *
                 * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList,
                 *      java.lang.Object, int, boolean, boolean)
                 */

                @Override
                public Component getListCellRendererComponent(JList list, Object value, int arg2, boolean arg3, boolean arg4) {
                    JLabel result = (JLabel) super.getListCellRendererComponent(list, value, arg2, arg3, arg4);
                    if (value != null) {
                        if (value instanceof DataSources.Item) {
                            DataSources.Item item = (DataSources.Item) value;
                            result.setToolTipText(item.getDescription());
                        } else if (value instanceof ItemPair) {
                            ItemPair item = (ItemPair) value;
                            result.setToolTipText(item.getDescription());
                        }
                    }
                    return result;
                }
            };
            comboObservatory.setRenderer(itemRenderer);
            comboInstrument.setRenderer(itemRenderer);
            comboDetectorMeasurement.setRenderer(itemRenderer);
            comboServer.setRenderer(itemRenderer);
            // Update the choices if necessary
            comboServer.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    String server = (String) comboServer.getSelectedItem();
                    if (server.contains("ROB")) {
                        Settings.getSingletonInstance().setProperty("API.dataSources.path", "http://swhv.oma.be/hv/api/?action=getDataSources&verbose=true&enable=[STEREO_A,STEREO_B,PROBA2]");
                        Settings.getSingletonInstance().setProperty("API.jp2images.path", "http://swhv.oma.be/hv/api/index.php");
                        Settings.getSingletonInstance().setProperty("API.jp2series.path", "http://swhv.oma.be/hv/api/index.php");
                        Settings.getSingletonInstance().setProperty("default.remote.path", "jpip://swhv.oma.be:8090");
                        Settings.getSingletonInstance().setProperty("API.event.path", "http://swhv.oma.be/hv/api/");
                        Settings.getSingletonInstance().setProperty("default.httpRemote.path", "http://swhv.oma.be/hv/jp2/");
                    } else if (server.contains("Helioviewer")) {
                        Settings.getSingletonInstance().setProperty("API.dataSources.path", "http://helioviewer.org/api/?action=getDataSources&verbose=true&enable=[STEREO_A,STEREO_B,PROBA2]");
                        Settings.getSingletonInstance().setProperty("API.jp2images.path", "http://helioviewer.org/api/index.php");
                        Settings.getSingletonInstance().setProperty("API.jp2series.path", "http://helioviewer.org/api/index.php");
                        Settings.getSingletonInstance().setProperty("default.remote.path", "jpip://helioviewer.org:8090");
                        Settings.getSingletonInstance().setProperty("API.event.path", "http://helioviewer.org/api/");
                        Settings.getSingletonInstance().setProperty("default.httpRemote.path", "http://helioviewer.org/jp2/");
                    } else if (server.contains("IAS")) {
                        Settings.getSingletonInstance().setProperty("API.dataSources.path", "http://helioviewer.ias.u-psud.fr/helioviewer/api/?action=getDataSources&verbose=true&enable=[STEREO_A,STEREO_B,PROBA2]");
                        Settings.getSingletonInstance().setProperty("API.jp2images.path", "http://helioviewer.ias.u-psud.fr/helioviewer/api/index.php");
                        Settings.getSingletonInstance().setProperty("API.jp2series.path", "http://helioviewer.ias.u-psud.fr/helioviewer/api/index.php");
                        Settings.getSingletonInstance().setProperty("default.remote.path", "jpip://helioviewer.ias.u-psud.fr:8080");
                        Settings.getSingletonInstance().setProperty("API.event.path", "http://helioviewer.ias.u-psud.fr/helioviewer/api/");
                        Settings.getSingletonInstance().setProperty("default.httpRemote.path", "http://helioviewer.ias.u-psud.fr/helioviewer/jp2/");
                    }
                    DataSources.getSingletonInstance().reload();
                    imageDataPanel.initDataSources(true);
                }
            });
            comboObservatory.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    setComboBox(comboInstrument, DataSources.getSingletonInstance().getInstruments(InstrumentsPanel.this.getObservatory()));
                }
            });
            comboInstrument.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    String obs = InstrumentsPanel.this.getObservatory();
                    String ins = InstrumentsPanel.this.getInstrument();

                    Vector<ItemPair> values = new Vector<ItemPair>();
                    Item[] detectors = DataSources.getSingletonInstance().getDetectors(obs, ins);

                    for (Item detector : detectors) {

                        Item[] measurements = DataSources.getSingletonInstance().getMeasurements(obs, ins, detector.getKey());

                        ItemPair.PrintMode printMode = ItemPair.PrintMode.BOTH;
                        if (detectors.length == 1) {
                            printMode = ItemPair.PrintMode.SECONDITEM_ONLY;
                        } else if (measurements.length == 1) {
                            printMode = ItemPair.PrintMode.FIRSTITEM_ONLY;
                        }

                        for (Item measurement : measurements) {
                            values.add(new ItemPair(detector, measurement, printMode));
                        }
                    }

                    setComboBox(comboDetectorMeasurement, values);
                    comboDetectorMeasurement.setEnabled(true);
                }
            });
        }

        /**
         * Function which will setup the data sources. Can be called from any
         * thread and will take care that EventQueue does the job and wait until
         * it is set to return
         *
         * @throws InvocationTargetException
         *             From inserting into the AWT Queue
         * @throws InterruptedException
         *             From inserting into the AWT Queue
         */
        public void setupSources() throws InterruptedException, InvocationTargetException {
            final DataSources source = DataSources.getSingletonInstance();
            EventQueue.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    InstrumentsPanel.this.setComboBox(comboObservatory, source.getObservatories());
                }
            });
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
            container.setEnabled(true);
            for (int i = 0; i < items.length; i++) {
                if (items[i].isDefaultItem()) {
                    container.setSelectedIndex(i);
                    return;
                }
            }
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
        private void setComboBox(JComboBox container, Vector<ItemPair> items) {
            container.setModel(new DefaultComboBoxModel(items));
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).isDefaultItem()) {
                    container.setSelectedIndex(i);
                    return;
                }
            }
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
            if (selectedItem != null) {
                DataSources.Item i = (DataSources.Item) selectedItem;
                return i.getKey();
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
            if (selectedItem != null) {
                DataSources.Item i = (DataSources.Item) selectedItem;
                return i.getKey();
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
            if (selectedItem != null) {
                DataSources.Item i = ((ItemPair) selectedItem).getFirstItem();
                return i.getKey();
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
            if (selectedItem != null) {
                DataSources.Item i = ((ItemPair) selectedItem).getSecondItem();
                return i.getKey();
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

            /**
             * Returns the first item.
             *
             * @return the fist item
             */
            public Item getFirstItem() {
                return firstItem;
            }

            /**
             * Returns the second item.
             *
             * @return the second item
             */
            public Item getSecondItem() {
                return secondItem;
            }

            /**
             * True if it was created as default item
             *
             * @return the defaultItem
             */
            public boolean isDefaultItem() {
                return firstItem.isDefaultItem() && secondItem.isDefaultItem();
            }

            /**
             * {@inheritDoc}
             */
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

            /**
             * @return the description
             */
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
