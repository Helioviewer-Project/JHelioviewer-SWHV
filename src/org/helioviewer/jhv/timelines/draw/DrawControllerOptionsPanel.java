package org.helioviewer.jhv.timelines.draw;

import java.awt.BorderLayout;
import java.util.Calendar;
import java.util.Objects;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.time.TimeUtils;

import com.jidesoft.swing.JideToggleButton;

@SuppressWarnings("serial")
class DrawControllerOptionsPanel extends JPanel {

    private final JComboBox<ZoomComboboxItem> zoomCombo;

    private enum ZOOM {
        CUSTOM, All, Year, Month, Day, Hour, Carrington, Movie
    }

    DrawControllerOptionsPanel() {
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
            ZoomComboboxItem item = (ZoomComboboxItem) Objects.requireNonNull(zoomCombo.getSelectedItem());
            zoomTo(item.zoom, item.number);
        });

        JideToggleButton lockButton = new JideToggleButton(Buttons.unlock);
        lockButton.setToolTipText("Synchronize time series with movie");
        lockButton.addActionListener(e -> {
            DrawController.setLocked(lockButton.isSelected());
            lockButton.setText(lockButton.isSelected() ? Buttons.lock : Buttons.unlock);
        });

        add(lockButton, BorderLayout.CENTER);
        add(zoomCombo, BorderLayout.EAST);

        ComponentUtils.smallVariant(this);
    }

    private static class ZoomComboboxItem {

        final ZOOM zoom;
        final long number;

        ZoomComboboxItem(ZOOM _zoom, long _number) {
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
            DrawController.setSelectedInterval(Layers.getStartTime(), Layers.getEndTime());
            break;
        case CUSTOM:
        default:
            break;
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
