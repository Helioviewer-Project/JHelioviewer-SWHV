package org.helioviewer.jhv.plugins.eveplugin.draw;

import java.awt.BorderLayout;
import java.util.Calendar;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.helioviewer.jhv.base.astronomy.Carrington;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.view.View;

@SuppressWarnings("serial")
class DrawControllerOptionsPanel extends JPanel {

    private final JComboBox<ZoomComboboxItem> zoomCombo;
    final JToggleButton lockButton;

    private enum ZOOM {
        CUSTOM, All, Year, Month, Day, Hour, Carrington, Movie
    }

    public DrawControllerOptionsPanel() {
        setLayout(new BorderLayout());

        ZoomComboboxItem[] items = {
            new ZoomComboboxItem(ZOOM.CUSTOM, 0),
            new ZoomComboboxItem(ZOOM.All, 0),
            new ZoomComboboxItem(ZOOM.Movie, 0),
            new ZoomComboboxItem(ZOOM.Year, 1),
            new ZoomComboboxItem(ZOOM.Month, 6),
            new ZoomComboboxItem(ZOOM.Month, 3),
            new ZoomComboboxItem(ZOOM.Carrington, 1),
            new ZoomComboboxItem(ZOOM.Day, 7),
            new ZoomComboboxItem(ZOOM.Day, 3),
            new ZoomComboboxItem(ZOOM.Hour, 12),
            new ZoomComboboxItem(ZOOM.Hour, 6),
            new ZoomComboboxItem(ZOOM.Hour, 1)
        };
        zoomCombo = new JComboBox<>(items);
        zoomCombo.addActionListener(e -> {
            ZoomComboboxItem item = (ZoomComboboxItem) zoomCombo.getSelectedItem();
            zoomTo(item.zoom, item.number);
        });

        lockButton = new JToggleButton(Buttons.unlock);
        lockButton.setBorderPainted(false);
        lockButton.setFocusPainted(false);
        lockButton.setContentAreaFilled(false);
        lockButton.setToolTipText("Synchronize time series with movie");
        lockButton.setEnabled(Layers.getActiveView() != null);
        lockButton.addActionListener(e -> {
            DrawController.setLocked(lockButton.isSelected());
            lockButton.setText(lockButton.isSelected() ? Buttons.lock : Buttons.unlock);
        });

        add(lockButton, BorderLayout.CENTER);
        add(zoomCombo, BorderLayout.EAST);

        ComponentUtils.smallVariant(this);
    }

    private static class ZoomComboboxItem {

        private final ZOOM zoom;
        private final long number;

        public ZoomComboboxItem(ZOOM _zoom, long _number) {
            zoom = _zoom;
            number = _number;
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

    void updateSelectedInterval() {
        zoomCombo.setSelectedItem(zoomCombo.getItemAt(0));
    }

    private static void zoomTo(ZOOM zoom, long value) {
        TimeAxis selectedInterval = DrawController.selectedAxis;
        TimeAxis availableInterval = DrawController.availableAxis;

        switch (zoom) {
        case All:
            DrawController.setSelectedInterval(availableInterval.start, availableInterval.end);
            break;
        case Day:
            computeZoomInterval(selectedInterval.end, Calendar.DAY_OF_MONTH, value);
            break;
        case Hour:
            computeZoomInterval(selectedInterval.end, Calendar.HOUR, value);
            break;
        case Month:
            computeZoomInterval(selectedInterval.end, Calendar.MONTH, value);
            break;
        case Year:
            computeZoomInterval(selectedInterval.end, Calendar.YEAR, value);
            break;
        case Carrington:
            computeCarringtonInterval(selectedInterval.end, value);
            break;
        case Movie:
            computeMovieInterval();
            break;
        case CUSTOM:
        default:
            break;
        }
    }

    private static void computeMovieInterval() {
        View view = Layers.getActiveView();
        long now = System.currentTimeMillis();
        if (view != null) {
            if (view.isMultiFrame()) {
                DrawController.setSelectedInterval(view.getFirstTime().milli, view.getLastTime().milli);
            }
            else {
                long end = view.getFirstTime().milli + TimeUtils.DAY_IN_MILLIS / 2;
                if (end > now)
                    end = now;
                DrawController.setSelectedInterval(view.getFirstTime().milli - TimeUtils.DAY_IN_MILLIS / 2, end);
            }
        } else {
            DrawController.setSelectedInterval(now - TimeUtils.DAY_IN_MILLIS, now);
        }
    }

    private static void computeCarringtonInterval(long end, long value) {
        computeZoomForMilliSeconds(end, (long) (Carrington.CR_SYNODIC_MEAN * TimeUtils.DAY_IN_MILLIS * value));
    }

    private static void computeZoomInterval(long end, int calendarField, long difference) {
        computeZoomForMilliSeconds(end, differenceInMilliseconds(calendarField, difference));
    }

    private static void computeZoomForMilliSeconds(long end, long differenceMilli) {
        long endDate = end;
        long now = System.currentTimeMillis();
        if (endDate > now) {
            endDate = now;
        }

        long startDate = endDate - differenceMilli;
        DrawController.setSelectedInterval(startDate, endDate);
    }

    private static long differenceInMilliseconds(int calendarField, long value) {
        switch (calendarField) {
        case Calendar.YEAR:
            return value * 365 * TimeUtils.DAY_IN_MILLIS;
        case Calendar.MONTH:
            return value * 30 * TimeUtils.DAY_IN_MILLIS;
        case Calendar.DAY_OF_MONTH:
        case Calendar.DAY_OF_WEEK:
        case Calendar.DAY_OF_WEEK_IN_MONTH:
        case Calendar.DAY_OF_YEAR:
            return value * TimeUtils.DAY_IN_MILLIS;
        case Calendar.HOUR:
        case Calendar.HOUR_OF_DAY:
            return value * 60 * 60 * 1000L;
        case Calendar.MINUTE:
            return value * 60 * 1000L;
        case Calendar.SECOND:
            return value * 1000L;
        case Calendar.MILLISECOND:
            return value;
        default:
            return 0;
        }
    }

}
