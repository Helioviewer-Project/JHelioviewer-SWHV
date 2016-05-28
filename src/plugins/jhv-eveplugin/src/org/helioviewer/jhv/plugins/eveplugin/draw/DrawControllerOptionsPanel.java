package org.helioviewer.jhv.plugins.eveplugin.draw;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;

import javax.swing.JComboBox;
import javax.swing.JToggleButton;

import org.helioviewer.jhv.base.astronomy.Carrington;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.gui.ComponentUtils.SmallPanel;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.viewmodel.view.View;

@SuppressWarnings("serial")
class DrawControllerOptionsPanel extends SmallPanel implements ActionListener {

    private final JComboBox zoomCombo;
    final JToggleButton lockButton;

    private enum ZOOM {
        CUSTOM, All, Year, Month, Day, Hour, Carrington, Movie
    }

    public DrawControllerOptionsPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

        ZoomComboboxItem[] items = new ZoomComboboxItem[] {
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
        zoomCombo = new JComboBox(items);
        zoomCombo.addActionListener(this);

        lockButton = new JToggleButton(IconBank.getIcon(JHVIcon.MOVIE_UNLINK));
        lockButton.setBorderPainted(false);
        lockButton.setFocusPainted(false);
        lockButton.setContentAreaFilled(false);
        lockButton.setToolTipText("Synchronize movie and time series display");
        lockButton.setEnabled(Layers.getActiveView() != null);
        lockButton.addActionListener(this);

        add(zoomCombo);
        add(lockButton);

        setSmall();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source.equals(lockButton)) {
            EVEPlugin.dc.setLocked(lockButton.isSelected());
            if (lockButton.isSelected()) {
                lockButton.setIcon(IconBank.getIcon(JHVIcon.MOVIE_LINK));
            } else {
                lockButton.setIcon(IconBank.getIcon(JHVIcon.MOVIE_UNLINK));
            }
        } else if (source.equals(zoomCombo)) {
            ZoomComboboxItem item = (ZoomComboboxItem) zoomCombo.getSelectedItem();
            if (item != null) {
                zoomTo(item.zoom, item.number);
            }
        }
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

    void updateSelectedInterval(TimeAxis selectedAxis) {
        zoomCombo.setSelectedItem(zoomCombo.getItemAt(0));
    }

    private void zoomTo(ZOOM zoom, long value) {
        TimeAxis selectedInterval = EVEPlugin.dc.selectedAxis;
        TimeAxis availableInterval = EVEPlugin.dc.availableAxis;

        switch (zoom) {
        case All:
            EVEPlugin.dc.setSelectedInterval(availableInterval.start, availableInterval.end);
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

    private void computeMovieInterval() {
        View view = Layers.getActiveView();
        long now = System.currentTimeMillis();
        if (view != null) {
            if (view.isMultiFrame()) {
                EVEPlugin.dc.setSelectedInterval(view.getFirstTime().milli, view.getLastTime().milli);
            }
            else {
                long end = view.getFirstTime().milli + TimeUtils.DAY_IN_MILLIS / 2;
                if (end > now)
                    end = now;
                EVEPlugin.dc.setSelectedInterval(view.getFirstTime().milli - TimeUtils.DAY_IN_MILLIS / 2, end);
            }
        } else {
            EVEPlugin.dc.setSelectedInterval(now - TimeUtils.DAY_IN_MILLIS, now);
        }
    }

    private void computeCarringtonInterval(long end, long value) {
        computeZoomForMilliSeconds(end, (long) (Carrington.CR_SYNODIC_MEAN * TimeUtils.DAY_IN_MILLIS * value));
    }

    private void computeZoomInterval(long end, int calendarField, long difference) {
        computeZoomForMilliSeconds(end, differenceInMilliseconds(calendarField, difference));
    }

    private void computeZoomForMilliSeconds(long end, long differenceMilli) {
        long endDate = end;
        long now = System.currentTimeMillis();
        if (endDate > now) {
            endDate = now;
        }

        long startDate = endDate - differenceMilli;
        EVEPlugin.dc.setSelectedInterval(startDate, endDate);
    }

    private long differenceInMilliseconds(int calendarField, long value) {
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
            return value * 60 * 60 * 1000l;
        case Calendar.MINUTE:
            return value * 60 * 1000l;
        case Calendar.SECOND:
            return value * 1000l;
        case Calendar.MILLISECOND:
            return value * 1l;
        default:
            return 0;
        }
    }

}
