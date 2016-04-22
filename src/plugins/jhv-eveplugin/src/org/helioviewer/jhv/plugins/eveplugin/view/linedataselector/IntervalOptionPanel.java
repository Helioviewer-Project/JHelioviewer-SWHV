package org.helioviewer.jhv.plugins.eveplugin.view.linedataselector;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.BevelBorder;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimingListener;
import org.helioviewer.jhv.viewmodel.view.View;

@SuppressWarnings("serial")
public class IntervalOptionPanel extends JPanel implements ActionListener, LayersListener, TimingListener {

    private final JComboBox zoomComboBox;
    private final JToggleButton periodFromLayersButton;
    private boolean selectedIndexSetByProgram;
    private Interval selectedIntervalByZoombox = null;

    private enum ZOOM {
        CUSTOM, All, Year, Month, Day, Hour, Carrington, Movie
    };

    public IntervalOptionPanel() {
        EVEPlugin.dc.addTimingListener(this);

        zoomComboBox = new JComboBox(new DefaultComboBoxModel());
        fillZoomComboBox();
        zoomComboBox.addActionListener(this);

        periodFromLayersButton = new JToggleButton(IconBank.getIcon(JHVIcon.MOVIE_UNLINK));
        periodFromLayersButton.setToolTipText("Synchronize movie and time series display");
        periodFromLayersButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        periodFromLayersButton.setEnabled(Layers.getActiveView() != null);
        periodFromLayersButton.addActionListener(this);

        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        add(zoomComboBox);
        add(periodFromLayersButton);

        Layers.addLayersListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == periodFromLayersButton) {
            EVEPlugin.dc.setLocked(periodFromLayersButton.isSelected());
            if (periodFromLayersButton.isSelected()) {
                periodFromLayersButton.setIcon(IconBank.getIcon(JHVIcon.MOVIE_LINK));
                periodFromLayersButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            } else {
                periodFromLayersButton.setIcon(IconBank.getIcon(JHVIcon.MOVIE_UNLINK));
                periodFromLayersButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
            }
        } else if (e.getSource().equals(zoomComboBox)) {
            ZoomComboboxItem item = (ZoomComboboxItem) zoomComboBox.getSelectedItem();
            selectedIntervalByZoombox = null;

            if (item != null && !selectedIndexSetByProgram) {
                zoomTo(item.zoom, item.number);
                selectedIntervalByZoombox = EVEPlugin.dc.getSelectedInterval();
            } else {
                if (selectedIndexSetByProgram) {
                    selectedIndexSetByProgram = false;
                }
            }
        }
    }

    private void addCarringtonRotationToModel(DefaultComboBoxModel model, int numberOfRotations) {
        model.addElement(new ZoomComboboxItem(ZOOM.Carrington, numberOfRotations));
    }

    private void addMovieToModel(DefaultComboBoxModel model) {
        // TODO Auto-generated method stub
        model.addElement(new ZoomComboboxItem(ZOOM.Movie, 0));
    }

    private boolean addElementToModel(DefaultComboBoxModel model, int calendarValue, ZOOM zoom) {
        model.addElement(new ZoomComboboxItem(zoom, calendarValue));
        return true;
    }

    private void fillZoomComboBox() {
        DefaultComboBoxModel model = (DefaultComboBoxModel) zoomComboBox.getModel();
        model.removeAllElements();
        model.addElement(new ZoomComboboxItem(ZOOM.CUSTOM, 0));
        model.addElement(new ZoomComboboxItem(ZOOM.All, 0));
        addMovieToModel(model);
        addElementToModel(model, 1, ZOOM.Year);
        addElementToModel(model, 6, ZOOM.Month);
        addElementToModel(model, 3, ZOOM.Month);
        addCarringtonRotationToModel(model, 1);

        addElementToModel(model, 7, ZOOM.Day);
        addElementToModel(model, 3, ZOOM.Day);
        addElementToModel(model, 12, ZOOM.Hour);
        addElementToModel(model, 6, ZOOM.Hour);
        addElementToModel(model, 1, ZOOM.Hour);
    }

    private static class ZoomComboboxItem {

        private final ZOOM zoom;
        private final long number;

        public ZoomComboboxItem(ZOOM zoom, long number) {
            this.zoom = zoom;
            this.number = number;
        }

        @Override
        public String toString() {
            String plural = number > 1 ? "s" : "";

            switch (zoom) {
            case All:
                return "Maximum interval";
            case Hour:
                return Long.toString(number) + " hour" + plural;
            case Day:
                return Long.toString(number) + " day" + plural;
            case Month:
                return Long.toString(number) + " month" + plural;
            case Year:
                return Long.toString(number) + " year" + plural;
            case Carrington:
                return "Carrington rotation" + plural;
            case Movie:
                return "Movie interval";
            default:
                break;
            }

            return "Custom interval";
        }
    }

    @Override
    public void layerAdded(View view) {
    }

    @Override
    public void activeLayerChanged(View view) {
        periodFromLayersButton.setEnabled(view != null);
    }

    @Override
    public void availableIntervalChanged() {
    }

    @Override
    public void selectedIntervalChanged() {
        Interval newInterval = EVEPlugin.dc.getSelectedInterval();
        if (newInterval.equals(selectedIntervalByZoombox)) {
            selectedIndexSetByProgram = true;
            zoomComboBox.setSelectedIndex(0);
        }
    }

    private void zoomTo(ZOOM zoom, long value) {
        Interval selectedInterval = EVEPlugin.dc.getSelectedInterval();
        Interval availableInterval = EVEPlugin.dc.getAvailableInterval();

        Interval newInterval;
        switch (zoom) {
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
            break;
        case Movie:
            newInterval = computeMovieInterval();
            break;
        case CUSTOM:
        default:
            newInterval = selectedInterval;
        }
        EVEPlugin.dc.setSelectedInterval(newInterval.start, newInterval.end);
        EVEPlugin.dc.useFullValueRange(true);
        EVEPlugin.dc.resetAvailableTime();
    }

    private Interval computeMovieInterval() {
        View view = Layers.getActiveView();
        if (view != null && view.isMultiFrame()) {
            return new Interval(view.getFirstTime().milli, view.getLastTime().milli);
        }
        long now = System.currentTimeMillis();
        return new Interval(now, now);
    }

    private Interval computeCarringtonInterval(Interval interval, long value) {
        return computeZoomForMilliSeconds(interval, value * 2356585920l);
    }

    private Interval computeZoomForMilliSeconds(Interval interval, long differenceMilli) {
        long startDate = interval.start;
        long endDate = interval.end;
        long now = System.currentTimeMillis();
        long lastdataDate = EVEPlugin.dc.getLastDateWithData();
        if (lastdataDate != -1) {
            if (endDate > lastdataDate) {
                endDate = lastdataDate;
            }
        } else if (endDate > now) {
            endDate = now;
        }

        startDate = endDate - differenceMilli;

        /*
        Interval availableInterval = EVEPlugin.dc.getAvailableInterval();
        boolean sInAvailable = availableInterval.containsPointInclusive(startDate);
        boolean eInAvailable = availableInterval.containsPointInclusive(endDate);

        if (sInAvailable && eInAvailable) {
            return new Interval(startDate, endDate);
        }
        */

        return new Interval(startDate, endDate);
    }

    private Interval computeZoomInterval(Interval interval, int calendarField, long difference) {
        return computeZoomForMilliSeconds(interval, differenceInMilliseconds(calendarField, difference));
    }

    private Long differenceInMilliseconds(int calendarField, long value) {
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
