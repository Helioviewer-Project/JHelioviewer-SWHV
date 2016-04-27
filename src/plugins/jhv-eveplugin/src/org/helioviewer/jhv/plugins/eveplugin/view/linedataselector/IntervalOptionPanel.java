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
import org.helioviewer.jhv.plugins.eveplugin.draw.TimeAxis;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimeLineListener;
import org.helioviewer.jhv.viewmodel.view.View;

@SuppressWarnings("serial")
public class IntervalOptionPanel extends JPanel implements ActionListener, LayersListener, TimeLineListener {

    private final JComboBox zoomComboBox;
    private final JToggleButton periodFromLayersButton;
    private boolean selectedIndexSetByProgram;
    private Interval selectedIntervalByZoombox = null;

    private enum ZOOM {
        CUSTOM, All, Year, Month, Day, Hour, Carrington, Movie
    };

    public IntervalOptionPanel() {

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
        EVEPlugin.dc.addTimeListener(this);
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
    public void fetchData(TimeAxis selectedAxis, TimeAxis availableAxis) {
        if (selectedIntervalByZoombox == null) {
            return;
        }
        if (selectedAxis.start == selectedIntervalByZoombox.start && selectedAxis.end == selectedIntervalByZoombox.end) {
            selectedIndexSetByProgram = true;
            zoomComboBox.setSelectedIndex(0);
        }
    }

    private void zoomTo(ZOOM zoom, long value) {
        TimeAxis selectedInterval = EVEPlugin.dc.selectedAxis;
        TimeAxis availableInterval = EVEPlugin.dc.availableAxis;

        switch (zoom) {
        case All:
            EVEPlugin.dc.setSelectedInterval(availableInterval.start, availableInterval.end);
            break;
        case Day:
            computeZoomInterval(selectedInterval.start, selectedInterval.end, Calendar.DAY_OF_MONTH, value);
            break;
        case Hour:
            computeZoomInterval(selectedInterval.start, selectedInterval.end, Calendar.HOUR, value);
            break;
        case Month:
            computeZoomInterval(selectedInterval.start, selectedInterval.end, Calendar.MONTH, value);
            break;
        case Year:
            computeZoomInterval(selectedInterval.start, selectedInterval.end, Calendar.YEAR, value);
            break;
        case Carrington:
            computeCarringtonInterval(selectedInterval.start, selectedInterval.end, value);
            break;
        case Movie:
            computeMovieInterval();
            break;
        case CUSTOM:
        default:
        }
    }

    private void computeMovieInterval() {
        View view = Layers.getActiveView();
        if (view != null && view.isMultiFrame()) {
            EVEPlugin.dc.setSelectedInterval(view.getFirstTime().milli, view.getLastTime().milli);
        }
        long now = System.currentTimeMillis();
        EVEPlugin.dc.setSelectedInterval(now, now);

    }

    private void computeCarringtonInterval(long start, long end, long value) {
        computeZoomForMilliSeconds(start, end, value * 2356585920l);
    }

    private void computeZoomForMilliSeconds(long start, long end, long differenceMilli) {
        long endDate = end;
        long now = System.currentTimeMillis();
        if (endDate > now) {
            endDate = now;
        }

        long startDate = endDate - differenceMilli;
        EVEPlugin.dc.setSelectedInterval(startDate, endDate);
    }

    private void computeZoomInterval(long start, long end, int calendarField, long difference) {
        computeZoomForMilliSeconds(start, end, differenceInMilliseconds(calendarField, difference));
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
