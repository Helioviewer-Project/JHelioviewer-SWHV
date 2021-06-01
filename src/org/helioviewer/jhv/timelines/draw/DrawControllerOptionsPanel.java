package org.helioviewer.jhv.timelines.draw;

import java.awt.BorderLayout;
import java.util.Objects;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.time.TimeUtils;

import com.jidesoft.swing.JideToggleButton;

@SuppressWarnings("serial")
class DrawControllerOptionsPanel extends JPanel {

    private final JComboBox<ZoomItem> zoomCombo;
    private final JideToggleButton lockButton;

    private enum ZOOM {
        CUSTOM, All, Year, Month, Day, Hour, Carrington, Movie
    }

    DrawControllerOptionsPanel() {
        setLayout(new BorderLayout());

        ZoomItem[] items = {
                new ZoomItem(ZOOM.CUSTOM, 0),
                new ZoomItem(ZOOM.All, 0),
                new ZoomItem(ZOOM.Movie, 0),
                new ZoomItem(ZOOM.Year, 1),
                new ZoomItem(ZOOM.Month, 6),
                new ZoomItem(ZOOM.Month, 3),
                new ZoomItem(ZOOM.Carrington, 1),
                new ZoomItem(ZOOM.Day, 7),
                new ZoomItem(ZOOM.Day, 3),
                new ZoomItem(ZOOM.Hour, 12),
                new ZoomItem(ZOOM.Hour, 6),
                new ZoomItem(ZOOM.Hour, 1)
        };
        zoomCombo = new JComboBox<>(items);
        zoomCombo.addActionListener(e -> {
            ZoomItem item = (ZoomItem) Objects.requireNonNull(zoomCombo.getSelectedItem());
            zoomTo(item.zoom, item.number);
        });

        lockButton = new JideToggleButton(Buttons.unlock);
        lockButton.setToolTipText("Synchronize movie with time series");
        lockButton.addActionListener(e -> {
            DrawController.setLocked(lockButton.isSelected());
            lockButton.setText(lockButton.isSelected() ? Buttons.lock : Buttons.unlock);
        });

        add(lockButton, BorderLayout.CENTER);
        add(zoomCombo, BorderLayout.LINE_END);
    }

    private static class ZoomItem {

        final ZOOM zoom;
        final long number;

        ZoomItem(ZOOM _zoom, long _number) {
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
                    return number + " hour" + plural;
                case Day:
                    return number + " day" + plural;
                case Month:
                    return number + " month" + plural;
                case Year:
                    return number + " year" + plural;
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
                DrawController.setSelectedInterval(availableInterval.start(), availableInterval.end());
                break;
            case Day:
                computeZoomInterval(selectedInterval.end(), TimeUtils.DAY_IN_MILLIS * value);
                break;
            case Hour:
                computeZoomInterval(selectedInterval.end(), 60 * 60 * 1000L * value);
                break;
            case Month:
                computeZoomInterval(selectedInterval.end(), (long) (30.6001 * TimeUtils.DAY_IN_MILLIS * value));
                break;
            case Year:
                computeZoomInterval(selectedInterval.end(), (long) (365.25 * TimeUtils.DAY_IN_MILLIS * value));
                break;
            case Carrington:
                computeZoomInterval(selectedInterval.end(), (long) (Carrington.CR_SYNODIC_MEAN * TimeUtils.DAY_IN_MILLIS * value));
                break;
            case Movie:
                DrawController.setSelectedInterval(Movie.getStartTime(), Movie.getEndTime());
                break;
            case CUSTOM:
                break;
        }
    }

    private static void computeZoomInterval(long end, long diff) {
        long now = System.currentTimeMillis();
        if (end > now)
            end = now;
        DrawController.setSelectedInterval(end - diff, end);
    }

    void setLocked(boolean locked) {
        if (lockButton.isSelected() != locked)
            lockButton.doClick();
    }

}
