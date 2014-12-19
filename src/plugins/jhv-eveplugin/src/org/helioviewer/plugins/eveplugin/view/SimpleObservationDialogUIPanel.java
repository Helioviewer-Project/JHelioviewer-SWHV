package org.helioviewer.plugins.eveplugin.view;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarEvent;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarListener;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialogPanel;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.plugins.eveplugin.controller.DrawController;
import org.helioviewer.plugins.eveplugin.controller.ZoomController;
import org.helioviewer.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.plugins.eveplugin.radio.data.RadioDownloader;
import org.helioviewer.plugins.eveplugin.view.plot.PlotsContainerPanel;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.cache.DateTimeCache;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

public class SimpleObservationDialogUIPanel extends ObservationDialogPanel implements JHVCalendarListener, LayersListener {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    protected PlotsContainerPanel plotsContainerPanel;

    protected boolean enableLoadButton = true;

    private JLabel labelStartDate;
    private JLabel labelEndDate;
    private JHVCalendarDatePicker calendarStartDate;
    // private JHVCalendarDatePicker calendarEndDate;

    // protected JComboBox plotComboBox;

    protected JComboBox comboBoxGroup;
    protected JComboBox comboBoxData;

    private JPanel timePane;
    private JPanel plotPane;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    public SimpleObservationDialogUIPanel(final PlotsContainerPanel plotsContainerPanel) {
        this.plotsContainerPanel = plotsContainerPanel;
        LayersModel.getSingletonInstance().addLayersListener(this);
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                // long start = System.currentTimeMillis();
                labelStartDate = new JLabel("Start Date");
                labelEndDate = new JLabel("End Date");
                calendarStartDate = new JHVCalendarDatePicker();
                // calendarEndDate = new JHVCalendarDatePicker();

                // plotComboBox = new JComboBox(new String[] { "Plot 1",
                // "Plot 2" });

                comboBoxGroup = new JComboBox(new DefaultComboBoxModel());
                comboBoxData = new JComboBox(new DefaultComboBoxModel());
                timePane = new JPanel();
                plotPane = new JPanel();
                initVisualComponents();
                // Log.debug("SimpleObservationDialogUIPanel time : " +
                // (System.currentTimeMillis() - start));
            }
        });

        // initGroups();
    }

    private void initVisualComponents() {
        // set up time settings
        calendarStartDate.setDateFormat(Settings.getSingletonInstance().getProperty("default.date.format"));
        calendarStartDate.addJHVCalendarListener(this);
        calendarStartDate.setToolTipText("Date in UTC starting the observation");

        // calendarEndDate.setDateFormat(Settings.getSingletonInstance().getProperty("default.date.format"));
        // calendarEndDate.addJHVCalendarListener(this);
        // calendarEndDate.setToolTipText("Date in UTC ending the observation.");

        final JPanel startDatePane = new JPanel(new BorderLayout());
        startDatePane.add(labelStartDate, BorderLayout.PAGE_START);
        startDatePane.add(calendarStartDate, BorderLayout.CENTER);

        final JPanel endDatePane = new JPanel(new BorderLayout());
        endDatePane.add(labelEndDate, BorderLayout.PAGE_START);
        // endDatePane.add(calendarEndDate, BorderLayout.CENTER);

        timePane.setLayout(new GridLayout(1, 2, GRIDLAYOUT_HGAP, GRIDLAYOUT_VGAP));
        timePane.setBorder(BorderFactory.createTitledBorder(" Select time range of interest "));
        timePane.add(startDatePane);
        timePane.add(endDatePane);

        // set basic layout
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        this.add(timePane);
        this.add(plotPane);
    }

    public void setDate(final Date start) {
        calendarStartDate.setDate(start);
    }

    /*
     * public void setEndDate(final Date end) { calendarEndDate.setDate(end); }
     */

    public Date getDate() {
        return calendarStartDate.getDate();
    }

    /*
     * public Date getEndDate() { return calendarEndDate.getDate(); }
     */

    /**
     * Checks if the selected start date is before selected or equal to end
     * date. The methods checks the entered times when the dates are equal. If
     * the start time is greater than the end time the method will return false.
     * 
     * @return boolean value if selected start date is before selected end date.
     */
    /*
     * private boolean isStartDateBeforeOrEqualEndDate() { final
     * GregorianCalendar calendar = new GregorianCalendar();
     * calendar.setTime(getStartDate());
     * 
     * final GregorianCalendar calendar2 = new
     * GregorianCalendar(calendar.get(Calendar.YEAR),
     * calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)); final
     * long start = calendar2.getTimeInMillis();
     * 
     * calendar.clear(); calendar2.clear();
     * 
     * calendar.setTime(getEndDate());
     * calendar2.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
     * calendar.get(Calendar.DAY_OF_MONTH)); final long end =
     * calendar2.getTimeInMillis();
     * 
     * return start <= end; }
     */

    private void updateZoomController() {
        Interval<Date> interval = defineInterval(getDate());
        ZoomController.getSingletonInstance().setAvailableInterval(interval);
    }

    protected Interval<Date> defineInterval(Date date) {
        GregorianCalendar gce = new GregorianCalendar();
        gce.clear();
        gce.setTime(date);
        gce.set(Calendar.HOUR, 0);
        gce.set(Calendar.MINUTE, 0);
        gce.set(Calendar.SECOND, 0);
        gce.set(Calendar.MILLISECOND, 0);
        gce.add(Calendar.DAY_OF_MONTH, 1);
        Date endDate = gce.getTime();

        if (endDate.after(new Date())) {
            gce.clear();
            gce.setTime(new Date());
            gce.set(Calendar.HOUR, 0);
            gce.set(Calendar.MINUTE, 0);
            gce.set(Calendar.SECOND, 0);
            gce.set(Calendar.MILLISECOND, 0);
            endDate = gce.getTime();
        }

        GregorianCalendar gcs = new GregorianCalendar();
        gcs.clear();
        gcs.setTime(date);
        gcs.set(Calendar.HOUR, 0);
        gcs.set(Calendar.MINUTE, 0);
        gcs.set(Calendar.SECOND, 0);
        gcs.set(Calendar.MILLISECOND, 0);
        gcs.add(Calendar.DAY_OF_MONTH, -2);
        Date startDate = gcs.getTime();

        return new Interval<Date>(startDate, endDate);
    }

    // private void updateBandController() {

    /*
     * final String identifier = plotComboBox.getSelectedIndex() == 0 ?
     * PlotsContainerPanel.PLOT_IDENTIFIER_MASTER :
     * PlotsContainerPanel.PLOT_IDENTIFIER_SLAVE; if
     * (identifier.equals(PlotsContainerPanel.PLOT_IDENTIFIER_SLAVE)) {
     * plotsContainerPanel.setPlot2Visible(true); }
     */
    // }

    private void startRadioDownload() {
        final String identifier = PlotsContainerPanel.PLOT_IDENTIFIER_MASTER;
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Interval<Date> selectedInterval = ZoomController.getSingletonInstance().getSelectedInterval();
        String isoStart = df.format(selectedInterval.getStart());
        Calendar end = Calendar.getInstance();
        end.setTime(selectedInterval.getEnd());
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        String isoEnd = df.format(end.getTime());
        RadioDownloader.getSingletonInstance().requestAndOpenRemoteFile(isoStart, isoEnd, identifier);
    }

    @Override
    public void dialogOpened() {
        final Interval<Date> interval = ZoomController.getSingletonInstance().getAvailableInterval();

        final GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(interval.getEnd());
        calendar.add(Calendar.DAY_OF_MONTH, -1);

        // setStartDate(interval.getStart());
        // setEndDate(calendar.getTime());

        // plotComboBox.setSelectedIndex(0);
    }

    @Override
    public void selected() {
        ObservationDialog.getSingletonInstance().setLoadButtonEnabled(enableLoadButton);
    }

    @Override
    public void deselected() {
    }

    @Override
    public boolean loadButtonPressed() {
        // check if start date is before end date -> if not show message
        /*
         * if (!isStartDateBeforeOrEqualEndDate()) {
         * JOptionPane.showMessageDialog(null, "End date is before start date!",
         * "", JOptionPane.ERROR_MESSAGE); return false; }
         */

        Set<YAxisElement> yAxisElements = DrawController.getSingletonInstance().getYAxisElements(PlotsContainerPanel.PLOT_IDENTIFIER_MASTER);
        boolean downloadOK = false;
        if (yAxisElements.size() >= 2) {
            for (YAxisElement el : yAxisElements) {
                if (el.getOriginalLabel().equals("Mhz")) {
                    downloadOK = true;
                    break;
                }
            }
        } else {
            downloadOK = true;
        }
        if (downloadOK) {
            updateZoomController();
            startRadioDownload();
        } else {
            JOptionPane.showMessageDialog(ImageViewerGui.getMainFrame(), "No more than two y-axes can be used. Remove some of the lines before adding a new line.", "Too much y-axes", JOptionPane.WARNING_MESSAGE);
        }

        return true;
    }

    @Override
    public void cancelButtonPressed() {
    }

    // //////////////////////////////////////////////////////////////////////////////
    // JHV Calendar Listener
    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public void actionPerformed(final JHVCalendarEvent e) {
        /*
         * if (e.getSource() == calendarStartDate &&
         * !isStartDateBeforeOrEqualEndDate()) {
         * calendarEndDate.setDate(calendarStartDate.getDate()); }
         * 
         * if (e.getSource() == calendarEndDate &&
         * !isStartDateBeforeOrEqualEndDate()) {
         * calendarStartDate.setDate(calendarStartDate.getDate()); }
         */
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Action Listener
    // //////////////////////////////////////////////////////////////////////////////
    public void setLoadButtonEnabled(boolean shouldBeEnabled) {
        enableLoadButton = shouldBeEnabled;
    }

    public boolean getLoadButtonEnabled() {
        return enableLoadButton;
    }

    @Override
    public void layerAdded(int idx) {
        Date beginDate = null;
        Date endDate = null;
        View nextView = LayersModel.getSingletonInstance().getLayer(LayersModel.getSingletonInstance().getActiveLayer());
        if (nextView != null) {
            JHVJPXView jpxView = nextView.getAdapter(JHVJPXView.class);
            if (jpxView != null) {
                DateTimeCache dtc = jpxView.getDateTimeCache();
                for (int frame = 0; frame < jpxView.getMaximumFrameNumber(); frame++) {
                    ImmutableDateTime date = dtc.getDateTime(frame);
                    if (beginDate == null || date.getTime().getTime() < beginDate.getTime()) {
                        beginDate = date.getTime();
                    }
                    if (endDate == null || date.getTime().getTime() > endDate.getTime()) {
                        endDate = date.getTime();
                    }
                }
                if (beginDate != null && endDate != null) {
                    calendarStartDate.setDate(new Date(beginDate.getTime() + (endDate.getTime() - beginDate.getTime())));
                    // calendarEndDate.setDate(endDate);
                }
            }
        }
    }

    @Override
    public void layerRemoved(View oldView, int oldIdx) {
        // TODO Auto-generated method stub

    }

    @Override
    public void layerChanged(int idx) {
        // TODO Auto-generated method stub

    }

    @Override
    public void activeLayerChanged(int idx) {
        // TODO Auto-generated method stub

    }

    @Override
    public void viewportGeometryChanged() {
        // TODO Auto-generated method stub

    }

    @Override
    public void timestampChanged(int idx) {
        // TODO Auto-generated method stub

    }

    @Override
    public void subImageDataChanged() {
        // TODO Auto-generated method stub

    }

    @Override
    public void layerDownloaded(int idx) {
        // TODO Auto-generated method stub

    }

}
