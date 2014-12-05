package org.helioviewer.plugins.eveplugin.view.plot;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.plugins.eveplugin.controller.ZoomController;
import org.helioviewer.plugins.eveplugin.controller.ZoomController.ZOOM;
import org.helioviewer.plugins.eveplugin.controller.ZoomControllerListener;
import org.helioviewer.plugins.eveplugin.model.TimeIntervalLockModel;
//import org.helioviewer.plugins.eveplugin.model.PlotTimeSpace;
import org.helioviewer.plugins.eveplugin.settings.EVEAPI.API_RESOLUTION_AVERAGES;
import org.helioviewer.plugins.eveplugin.view.periodpicker.PeriodPickerListener;
import org.helioviewer.viewmodel.view.View;

/**
 * @author Stephan Pagel
 * */
public class PlotsControlPanel extends JPanel implements ZoomControllerListener, ActionListener, PeriodPickerListener, LayersListener {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    private Interval<Date> selectedIntervalByZoombox = null;

    private boolean setDefaultPeriod = true;

    private final JLabel zoomLabel = new JLabel("Time interval:");
    private final JComboBox zoomComboBox = new JComboBox(new DefaultComboBoxModel());

    private final JLabel lockIntervalLabel = new JLabel("Lock Time Interval:");
    private final JCheckBox lockIntervalCheckBox = new JCheckBox();

    private boolean selectedIndexSetByProgram = false;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    public PlotsControlPanel() {
        initVisualComponents();
        ZoomController.getSingletonInstance().addZoomControllerListener(this);
        LayersModel.getSingletonInstance().addLayersListener(this);
    }

    private void initVisualComponents() {
        setLayout(new BorderLayout());

        initLockIntervalCheckBox();

        final JPanel zoomPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        zoomPane.add(lockIntervalLabel);
        zoomPane.add(lockIntervalCheckBox);
        zoomPane.add(zoomLabel);
        zoomPane.add(zoomComboBox);

        add(zoomPane, BorderLayout.CENTER);

        zoomComboBox.addActionListener(this);

    }

    /**
     *
     */
    private void initLockIntervalCheckBox() {
        lockIntervalCheckBox.setSelected(TimeIntervalLockModel.getInstance().isLocked());

        lockIntervalCheckBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                TimeIntervalLockModel.getInstance().setLocked(lockIntervalCheckBox.isSelected());
            }

        });
    }

    private void fillZoomComboBox() {
        final Interval<Date> interval = ZoomController.getSingletonInstance().getAvailableInterval();
        final Date startDate = interval.getStart();

        final Calendar calendar = new GregorianCalendar();
        calendar.clear();
        calendar.setTime(startDate);
        calendar.add(Calendar.YEAR, 1);
        calendar.add(Calendar.HOUR, 1);
        final boolean years = interval.containsPointInclusive(calendar.getTime());

        calendar.clear();
        calendar.setTime(startDate);
        calendar.add(Calendar.MONTH, 3);
        ;
        final boolean months = interval.containsPointInclusive(calendar.getTime());

        final DefaultComboBoxModel model = (DefaultComboBoxModel) zoomComboBox.getModel();
        model.removeAllElements();

        model.addElement(new ZoomComboboxItem(ZOOM.CUSTOM, 0));
        model.addElement(new ZoomComboboxItem(ZOOM.All, 0));

        addElementToModel(model, startDate, interval, Calendar.YEAR, 10, ZOOM.Year);
        addElementToModel(model, startDate, interval, Calendar.YEAR, 5, ZOOM.Year);

        addElementToModel(model, startDate, interval, Calendar.YEAR, 1, ZOOM.Year);
        addElementToModel(model, startDate, interval, Calendar.MONTH, 6, ZOOM.Month);
        addElementToModel(model, startDate, interval, Calendar.MONTH, 3, ZOOM.Month);

        addElementToModel(model, startDate, interval, Calendar.MONTH, 1, ZOOM.Month);

        if (!years) {
            addElementToModel(model, startDate, interval, Calendar.DATE, 14, ZOOM.Day);
            addElementToModel(model, startDate, interval, Calendar.DATE, 7, ZOOM.Day);
            addElementToModel(model, startDate, interval, Calendar.DATE, 1, ZOOM.Day);

            if (!months) {
                addElementToModel(model, startDate, interval, Calendar.HOUR, 12, ZOOM.Hour);
                addElementToModel(model, startDate, interval, Calendar.HOUR, 6, ZOOM.Hour);
                addElementToModel(model, startDate, interval, Calendar.HOUR, 1, ZOOM.Hour);
            }
        }
    }

    private boolean addElementToModel(final DefaultComboBoxModel model, final Date startDate, final Interval<Date> interval,
            final int calendarField, final int calendarValue, final ZOOM zoom) {
        final Calendar calendar = new GregorianCalendar();

        calendar.clear();
        calendar.setTime(startDate);
        calendar.add(calendarField, calendarValue);

        if (interval.containsPointInclusive(calendar.getTime())) {
            model.addElement(new ZoomComboboxItem(zoom, calendarValue));
            return true;
        }

        return false;
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Layers Listener
    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public void layerAdded(int idx) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                // long start = System.currentTimeMillis();
                if (setDefaultPeriod) {
                    setDefaultPeriod = false;
                    final Interval<Date> interval = new Interval<Date>(LayersModel.getSingletonInstance().getFirstDate(), LayersModel
                            .getSingletonInstance().getLastDate());
                    ZoomController.getSingletonInstance().setAvailableInterval(interval);
                    // PlotTimeSpace.getInstance().setSelectedMinAndMaxTime(interval.getStart(),
                    // interval.getEnd());
                }

                // Log.debug("layer added time : " + (System.currentTimeMillis()
                // - start));
            }
        });

    }

    @Override
    public void layerRemoved(View oldView, int oldIdx) {
    }

    @Override
    public void layerChanged(int idx) {
    }

    @Override
    public void activeLayerChanged(int idx) {
    }

    @Override
    public void viewportGeometryChanged() {
    }

    @Override
    public void timestampChanged(int idx) {
    }

    @Override
    public void subImageDataChanged() {
    }

    @Override
    public void layerDownloaded(int idx) {
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Period Picker Listener
    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public void intervalChanged(Interval<Date> interval) {
        setDefaultPeriod = false;

        final ZoomController zoomController = ZoomController.getSingletonInstance();

        zoomController.setAvailableInterval(interval);
        zoomController.setSelectedInterval(zoomController.getAvailableInterval(), false);
        // PlotTimeSpace plotTimeSpace = PlotTimeSpace.getInstance();
        // plotTimeSpace.setMinAndMaxTime(interval.getStart(),
        // interval.getEnd());
        // plotTimeSpace.setSelectedMinAndMaxTime(plotTimeSpace.getMinTime(),
        // plotTimeSpace.getMaxTime());
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Action Listener
    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource().equals(zoomComboBox)) {
            final ZoomComboboxItem item = (ZoomComboboxItem) zoomComboBox.getSelectedItem();
            selectedIntervalByZoombox = null;

            if (item != null && !selectedIndexSetByProgram) {
                selectedIntervalByZoombox = ZoomController.getSingletonInstance().zoomTo(item.getZoom(), item.getNumber());
            } else {
                if (selectedIndexSetByProgram) {
                    selectedIndexSetByProgram = false;
                }
            }
        }
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Zoom Controller Listener
    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public void availableIntervalChanged(final Interval<Date> newInterval) {
        if (newInterval.getStart() != null || newInterval.getEnd() != null) {
            final Calendar calendar = new GregorianCalendar();
            calendar.clear();
            calendar.setTime(newInterval.getEnd());
            calendar.add(Calendar.DATE, -1);

            fillZoomComboBox();
        }
    }

    @Override
    public void selectedIntervalChanged(final Interval<Date> newInterval, boolean keepFullValueSpace) {
        if (selectedIntervalByZoombox != null && newInterval != null) {
            if (!selectedIntervalByZoombox.equals(newInterval)) {
                try {
                    selectedIndexSetByProgram = true;
                    zoomComboBox.setSelectedIndex(0);
                } catch (final IllegalArgumentException ex) {
                }
            }
        }
    }

    @Override
    public void selectedResolutionChanged(final API_RESOLUTION_AVERAGES newResolution) {
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Zoom Combobox Item
    // //////////////////////////////////////////////////////////////////////////////

    private class ZoomComboboxItem {

        // //////////////////////////////////////////////////////////////////////////
        // Definitions
        // //////////////////////////////////////////////////////////////////////////

        private final ZOOM zoom;
        private final int number;

        // //////////////////////////////////////////////////////////////////////////
        // Methods
        // //////////////////////////////////////////////////////////////////////////

        public ZoomComboboxItem(final ZOOM zoom, final int number) {
            this.zoom = zoom;
            this.number = number;
        }

        public ZOOM getZoom() {
            return zoom;
        }

        public int getNumber() {
            return number;
        }

        @Override
        public String toString() {
            final String plural = number > 1 ? "s" : "";

            switch (zoom) {
            case All:
                return "Maximum";
            case Hour:
                return Integer.toString(number) + " Hour" + plural;
            case Day:
                return Integer.toString(number) + " Day" + plural;
            case Month:
                return Integer.toString(number) + " Month" + plural;
            case Year:
                return Integer.toString(number) + " Year" + plural;
            default:
                break;
            }

            return "Custom";
        }
    }
}
