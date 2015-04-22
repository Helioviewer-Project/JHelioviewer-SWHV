package org.helioviewer.plugins.eveplugin.view.linedataselector;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.helioviewer.base.interval.Interval;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.plugins.eveplugin.draw.DrawController;
import org.helioviewer.plugins.eveplugin.draw.TimeIntervalLockModel;
import org.helioviewer.plugins.eveplugin.draw.TimingListener;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;

public class IntervalOptionPanel extends JPanel implements ActionListener, LayersListener, TimingListener, LineDataSelectorModelListener {

    private final JComboBox zoomComboBox = new JComboBox(new DefaultComboBoxModel());
    private final ImageIcon movietimeIcon = IconBank.getIcon(JHVIcon.LAYER_MOVIE_TIME);
    private final JToggleButton periodFromLayersButton = new JToggleButton(movietimeIcon);
    private boolean setDefaultPeriod = true;
    private boolean selectedIndexSetByProgram;
    private Interval<Date> selectedIntervalByZoombox = null;
    private final DrawController drawController;

    private enum ZOOM {
        CUSTOM, All, Year, Month, Day, Hour, Carrington
    };

    public IntervalOptionPanel() {
        Displayer.getLayersModel().addLayersListener(this);
        drawController = DrawController.getSingletonInstance();
        drawController.addTimingListener(this);
        LineDataSelectorModel.getSingletonInstance().addLineDataSelectorModelListener(this);
        initVisualComponents();
    }

    private void initVisualComponents() {
        zoomComboBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        fillZoomComboBox();
        zoomComboBox.addActionListener(this);
        zoomComboBox.setEnabled(false);

        periodFromLayersButton.setToolTipText("Synchronize movie and time series display");
        periodFromLayersButton.setPreferredSize(new Dimension(movietimeIcon.getIconWidth() + 14, periodFromLayersButton.getPreferredSize().height));
        periodFromLayersButton.addActionListener(this);
        periodFromLayersButton.setMargin(new Insets(0, 0, 0, 0));
        setEnabledStateOfPeriodMovieButton();

        GridBagConstraints gbc = new GridBagConstraints();
        setLayout(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(zoomComboBox, gbc);

        gbc.gridx = 1;
        add(periodFromLayersButton, gbc);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == periodFromLayersButton) {
            TimeIntervalLockModel.getInstance().setLocked(periodFromLayersButton.isSelected());
            if (periodFromLayersButton.isSelected()) {
                setDateRange();
            }
        } else if (e.getSource().equals(zoomComboBox)) {
            final ZoomComboboxItem item = (ZoomComboboxItem) zoomComboBox.getSelectedItem();
            selectedIntervalByZoombox = null;

            if (item != null && !selectedIndexSetByProgram) {
                selectedIntervalByZoombox = zoomTo(item.getZoom(), item.getNumber());
            } else {
                if (selectedIndexSetByProgram) {
                    selectedIndexSetByProgram = false;
                }
            }
        }
    }

    @Override
    public void downloadStartded(LineDataSelectorElement element) {
    }

    @Override
    public void downloadFinished(LineDataSelectorElement element) {
    }

    @Override
    public void lineDataAdded(LineDataSelectorElement element) {
        zoomComboBox.setEnabled(true);
    }

    @Override
    public void lineDataRemoved(LineDataSelectorElement element) {
        if (LineDataSelectorModel.getSingletonInstance().getNumberOfAvailableLineData() == 0) {
            zoomComboBox.setEnabled(false);
        }
    }

    @Override
    public void lineDataUpdated(LineDataSelectorElement element) {
    }

    private void addCarringtonRotationToModel(final DefaultComboBoxModel model, final int numberOfRotations) {
        model.addElement(new ZoomComboboxItem(ZOOM.Carrington, numberOfRotations));
    }

    private boolean addElementToModel(final DefaultComboBoxModel model, final int calendarField, final int calendarValue, final ZOOM zoom) {
        model.addElement(new ZoomComboboxItem(zoom, calendarValue));
        return true;
    }

    private void setDateRange() {
        AbstractView activeView = Displayer.getLayersModel().getActiveView();
        if (activeView instanceof JHVJPXView) {
            JHVJPXView jpxView = (JHVJPXView) activeView;
            Date start = Displayer.getLayersModel().getStartDate(jpxView).getTime();
            Date end = Displayer.getLayersModel().getEndDate(jpxView).getTime();

            Interval<Date> interval = new Interval<Date>(start, end);
            DrawController.getSingletonInstance().setSelectedInterval(interval, true);
        }
    }

    private void setEnabledStateOfPeriodMovieButton() {
        Date start = Displayer.getLayersModel().getFirstDate();
        Date end = Displayer.getLayersModel().getLastDate();

        periodFromLayersButton.setEnabled(start != null && end != null);
    }

    private void fillZoomComboBox() {
        final DefaultComboBoxModel model = (DefaultComboBoxModel) zoomComboBox.getModel();
        model.removeAllElements();
        model.addElement(new ZoomComboboxItem(ZOOM.CUSTOM, 0));
        model.addElement(new ZoomComboboxItem(ZOOM.All, 0));

        addElementToModel(model, Calendar.YEAR, 1, ZOOM.Year);
        addElementToModel(model, Calendar.MONTH, 6, ZOOM.Month);
        addElementToModel(model, Calendar.MONTH, 3, ZOOM.Month);
        addCarringtonRotationToModel(model, 1);

        addElementToModel(model, Calendar.DATE, 7, ZOOM.Day);

        addElementToModel(model, Calendar.HOUR, 12, ZOOM.Hour);
        addElementToModel(model, Calendar.HOUR, 6, ZOOM.Hour);
        addElementToModel(model, Calendar.HOUR, 1, ZOOM.Hour);
    }

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
                return "Maximum Interval";
            case Hour:
                return Integer.toString(number) + " Hour" + plural;
            case Day:
                return Integer.toString(number) + " Day" + plural;
            case Month:
                return Integer.toString(number) + " Month" + plural;
            case Year:
                return Integer.toString(number) + " Year" + plural;
            case Carrington:
                return "Carrington Rotation" + plural;
            default:
                break;
            }

            return "Custom Interval";
        }
    }

    @Override
    public void layerAdded(int idx) {
    }

    @Override
    public void layerRemoved(int oldIdx) {
        setEnabledStateOfPeriodMovieButton();
    }

    @Override
    public void activeLayerChanged(AbstractView view) {
        setEnabledStateOfPeriodMovieButton();
        if (setDefaultPeriod || TimeIntervalLockModel.getInstance().isLocked()) {
            setDefaultPeriod = false;
            if (view instanceof JHVJPXView) {
                JHVJPXView jpxView = (JHVJPXView) view;
                Date start = Displayer.getLayersModel().getStartDate(jpxView).getTime();
                Date end = Displayer.getLayersModel().getEndDate(jpxView).getTime();

                Interval<Date> interval = new Interval<Date>(start, end);
                // ZoomController.getSingletonInstance().setAvailableInterval(interval);
                if (TimeIntervalLockModel.getInstance().isLocked()) {
                    DrawController.getSingletonInstance().setSelectedInterval(interval, false);
                }
            }
        }
    }

    @Override
    public void availableIntervalChanged() {
    }

    @Override
    public void selectedIntervalChanged() {
        Interval<Date> newInterval = DrawController.getSingletonInstance().getSelectedInterval();
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

    public Interval<Date> zoomTo(final ZOOM zoom, final int value) {
        Interval<Date> newInterval = new Interval<Date>(null, null);
        Interval<Date> selectedInterval = drawController.getSelectedInterval();
        Interval<Date> availableInterval = drawController.getAvailableInterval();
        switch (zoom) {
        case CUSTOM:
            newInterval = selectedInterval;
            break;
        case All:
            newInterval = availableInterval;
            break;
        case Day:
            newInterval = computeZoomInterval(selectedInterval, Calendar.DAY_OF_MONTH, value);
            break;
        case Hour:
            newInterval = computeZoomInterval(selectedInterval, Calendar.HOUR, value);
            break;
        case Month:
            newInterval = computeZoomInterval(selectedInterval, Calendar.MONTH, value);
            break;
        case Year:
            newInterval = computeZoomInterval(selectedInterval, Calendar.YEAR, value);
            break;
        case Carrington:
            newInterval = computeCarringtonInterval(selectedInterval, value);
        }
        return drawController.setSelectedInterval(newInterval, true);
    }

    private Interval<Date> computeCarringtonInterval(Interval<Date> interval, int value) {
        return computeZoomForMilliSeconds(interval, value * 2356585920l);
    }

    private Interval<Date> computeZoomForMilliSeconds(final Interval<Date> interval, long differenceMilli) {
        Date middle = new Date(interval.getStart().getTime() + (interval.getEnd().getTime() - interval.getStart().getTime()) / 2);
        Date startDate = interval.getStart();
        Interval<Date> availableInterval = drawController.getAvailableInterval();
        // Date endDate = interval.getEnd();
        GregorianCalendar gce = new GregorianCalendar();
        gce.clear();
        gce.setTime(new Date(middle.getTime() + differenceMilli / 2));
        Date endDate = gce.getTime();

        final Date lastdataDate = DrawController.getSingletonInstance().getLastDateWithData();
        if (lastdataDate != null) {
            if (endDate.after(lastdataDate)) {
                endDate = lastdataDate;
            }
        } else if (endDate.after(new Date())) {
            endDate = new Date();
        }
        final Date availableStartDate = availableInterval.getStart();

        if (startDate == null || endDate == null || availableStartDate == null) {
            return new Interval<Date>(null, null);
        }

        final GregorianCalendar calendar = new GregorianCalendar();

        // add difference to start date -> when calculated end date is within
        // available interval it is the result
        calendar.clear();
        calendar.setTime(new Date(endDate.getTime() - differenceMilli));

        startDate = calendar.getTime();

        boolean sInAvailable = availableInterval.containsPointInclusive(startDate);
        boolean eInAvailable = availableInterval.containsPointInclusive(endDate);

        if (sInAvailable && eInAvailable) {
            return new Interval<Date>(startDate, endDate);
        }

        Date availableS = sInAvailable ? availableInterval.getStart() : startDate;
        Date availableE = eInAvailable ? availableInterval.getEnd() : endDate;

        drawController.setAvailableInterval(new Interval<Date>(availableS, availableE));

        return new Interval<Date>(startDate, endDate);

    }

    private Interval<Date> computeZoomInterval(final Interval<Date> interval, final int calendarField, final int difference) {
        return computeZoomForMilliSeconds(interval, differenceInMilliseconds(calendarField, difference));
    }

    private Long differenceInMilliseconds(final int calendarField, final int value) {
        switch (calendarField) {
        case Calendar.YEAR:
            return value * 365 * 24 * 60 * 60 * 1000l;
        case Calendar.MONTH:
            return value * 30 * 24 * 60 * 60 * 1000l;
        case Calendar.DAY_OF_MONTH:
        case Calendar.DAY_OF_WEEK:
        case Calendar.DAY_OF_WEEK_IN_MONTH:
        case Calendar.DAY_OF_YEAR:
            return value * 24 * 60 * 60 * 1000l;
        case Calendar.HOUR:
        case Calendar.HOUR_OF_DAY:
            return value * 60 * 60 * 1000l;
        case Calendar.MINUTE:
            return value * 60 * 1000l;
        case Calendar.SECOND:
            return value * 1000l;
        case Calendar.MILLISECOND:
            return value * 1l;
        default:
            return null;
        }
    }
}
