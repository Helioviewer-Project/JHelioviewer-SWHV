package org.helioviewer.plugins.eveplugin.view;

import java.awt.BorderLayout;
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

import org.helioviewer.base.interval.Interval;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarEvent;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarListener;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModel;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModelListener;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialogPanel;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.plugins.eveplugin.controller.DrawController;
import org.helioviewer.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.plugins.eveplugin.radio.data.RadioDownloader;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;

public abstract class SimpleObservationDialogUIPanel extends ObservationDialogPanel implements JHVCalendarListener, LayersListener, ObservationDialogDateModelListener {

    private static final long serialVersionUID = 1L;
    protected boolean enableLoadButton = true;

    private final JLabel labelStartDate;
    private final JHVCalendarDatePicker calendarStartDate;
    protected JComboBox comboBoxGroup;
    protected JComboBox comboBoxData;

    private final JPanel timePane;
    private final JPanel plotPane;

    public SimpleObservationDialogUIPanel() {
        ObservationDialogDateModel.getInstance().addListener(this);

        labelStartDate = new JLabel("Start date");
        calendarStartDate = new JHVCalendarDatePicker();
        comboBoxGroup = new JComboBox(new DefaultComboBoxModel());
        comboBoxData = new JComboBox(new DefaultComboBoxModel());
        timePane = new JPanel();
        plotPane = new JPanel();
        initVisualComponents();
        Displayer.getLayersModel().addLayersListener(SimpleObservationDialogUIPanel.this);

        // initGroups();
    }

    private void initVisualComponents() {
        // set up time settings
        calendarStartDate.setDateFormat(Settings.getSingletonInstance().getProperty("default.date.format"));
        calendarStartDate.addJHVCalendarListener(this);
        calendarStartDate.setToolTipText("UTC date for observation start");

        final JPanel startDatePane = new JPanel(new BorderLayout());
        startDatePane.add(labelStartDate, BorderLayout.PAGE_START);
        startDatePane.add(calendarStartDate, BorderLayout.CENTER);

        timePane.setLayout(new GridLayout(1, 2, GRIDLAYOUT_HGAP, GRIDLAYOUT_VGAP));
        timePane.setBorder(BorderFactory.createEtchedBorder());
        timePane.add(startDatePane);

        // set basic layout
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        this.add(timePane);
        this.add(plotPane);
    }

    public void setDate(final Date start) {
        calendarStartDate.setDate(start);
    }

    public Date getDate() {
        return calendarStartDate.getDate();
    }

    private void updateDrawController() {
        Interval<Date> interval = defineInterval(getDate());
        DrawController.getSingletonInstance().setAvailableInterval(interval);
        DrawController.getSingletonInstance().setSelectedInterval(interval, true);
    }

    protected Interval<Date> defineInterval(Date date) {
        Interval<Date> movieInterval = new Interval<Date>(Displayer.getLayersModel().getFirstDate(), Displayer.getLayersModel().getLastDate());
        if (movieInterval.getStart() != null && movieInterval.getEnd() != null && movieInterval.containsPointInclusive(date)) {
            return movieInterval;
        } else {
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
            gcs.setTime(endDate);
            gcs.set(Calendar.HOUR, 0);
            gcs.set(Calendar.MINUTE, 0);
            gcs.set(Calendar.SECOND, 0);
            gcs.set(Calendar.MILLISECOND, 0);
            gcs.add(Calendar.DAY_OF_MONTH, -2);
            Date startDate = gcs.getTime();

            return new Interval<Date>(startDate, endDate);
        }
    }

    private void startRadioDownload() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Interval<Date> selectedInterval = DrawController.getSingletonInstance().getSelectedInterval();
        String isoStart = df.format(selectedInterval.getStart());
        Calendar end = Calendar.getInstance();
        end.setTime(selectedInterval.getEnd());
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        String isoEnd = df.format(end.getTime());
        RadioDownloader.getSingletonInstance().requestAndOpenRemoteFile(isoStart, isoEnd);
    }

    @Override
    public void dialogOpened() {
        final Interval<Date> interval = DrawController.getSingletonInstance().getAvailableInterval();

        final GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(interval.getEnd());
        calendar.add(Calendar.DAY_OF_MONTH, -1);

        // setStartDate(interval.getStart());
        // setEndDate(calendar.getTime());

        // plotComboBox.setSelectedIndex(0);
    }

    @Override
    public void selected() {
        ImageViewerGui.getSingletonInstance().getObservationDialog().setLoadButtonEnabled(enableLoadButton);
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
        ObservationDialogDateModel.getInstance().setStartDate(getDate(), true);
        Set<YAxisElement> yAxisElements = DrawController.getSingletonInstance().getYAxisElements();
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
            updateDrawController();
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
        if (e.getSource() == calendarStartDate && calendarStartDate.getDate() != null) {
            ObservationDialogDateModel.getInstance().setStartDate(calendarStartDate.getDate(), true);
        }
        /*
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
        AbstractView view = Displayer.getLayersModel().getLayer(idx);
        if (view instanceof JHVJPXView) {
            JHVJPXView jpxView = (JHVJPXView) view;
            Date start = Displayer.getLayersModel().getStartDate(jpxView).getTime();

            calendarStartDate.setDate(start);
            ObservationDialogDateModel.getInstance().setStartDate(start, false);
        }
    }

    @Override
    public void layerRemoved(int oldIdx) {
    }

    @Override
    public void activeLayerChanged(AbstractView view) {
    }

    @Override
    public void startDateChanged(Date startDate) {
        calendarStartDate.setDate(startDate);
    }

    @Override
    public void endDateChanged(Date endDate) {
    }

}
