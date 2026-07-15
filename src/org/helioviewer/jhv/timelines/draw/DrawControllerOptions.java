package org.helioviewer.jhv.timelines.draw;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Objects;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.gui.component.Buttons;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.time.TimeUtils;

import com.jidesoft.swing.JideToggleButton;

@SuppressWarnings("serial")
class DrawControllerOptions extends JPanel {

    private final JComboBox<ZoomItem> zoomCombo;
    private final JideToggleButton lockButton;
    private final JideToggleButton stackedButton;
    private final JLabel statusLabel;

    private enum ZOOM {
        CUSTOM, All, Year, Month, Day, Hour, Carrington, Movie
    }

    DrawControllerOptions() {
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
        zoomCombo.setMaximumSize(new Dimension(130, zoomCombo.getPreferredSize().height));
        zoomCombo.setPreferredSize(new Dimension(110, zoomCombo.getPreferredSize().height));
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

        stackedButton = new JideToggleButton(Buttons.overlaid);
        stackedButton.setToolTipText("Stack layers vertically");
        stackedButton.addActionListener(e -> {
            DrawController.setStacked(stackedButton.isSelected());
            stackedButton.setText(stackedButton.isSelected() ? Buttons.stacked : Buttons.overlaid);
        });

        statusLabel = new JLabel("", JLabel.RIGHT);

        JPanel togglePanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        togglePanel.add(stackedButton);
        togglePanel.add(zoomCombo);
        togglePanel.add(lockButton);

        add(statusLabel, BorderLayout.LINE_START);
        add(togglePanel, BorderLayout.CENTER);
    }

    private record ZoomItem(ZOOM zoom, long number) {
        @Override
        public String toString() {
            String plural = number > 1 ? "s" : "";
            return switch (zoom) {
                case CUSTOM -> "Custom";
                case All -> "Maximum interval";
                case Year -> number + " year" + plural;
                case Month -> number + " month" + plural;
                case Day -> number + " day" + plural;
                case Hour -> number + " hour" + plural;
                case Carrington -> "Carrington rotation" + plural;
                case Movie -> "Movie interval";
            };
        }
    }

    private static void zoomTo(ZOOM zoom, long value) {
        TimeAxis availableInterval = DrawController.availableAxis;
        long end = DrawController.selectedAxis.end();

        switch (zoom) {
            case CUSTOM -> {}
            case All -> DrawController.setSelectedInterval(availableInterval.start(), availableInterval.end());
            case Year -> setZoom(end, (long) (365.25 * TimeUtils.DAY_IN_MILLIS * value));
            case Month -> setZoom(end, (long) (30.6001 * TimeUtils.DAY_IN_MILLIS * value));
            case Day -> setZoom(end, TimeUtils.DAY_IN_MILLIS * value);
            case Hour -> setZoom(end, 60 * 60 * 1000L * value);
            case Carrington -> setZoom(end, (long) (Carrington.CR_SYNODIC_MEAN * TimeUtils.DAY_IN_MILLIS * value));
            case Movie -> DrawController.setSelectedInterval(Player.getStartTime(), Player.getEndTime());
        }
    }

    private static void setZoom(long end, long diff) {
        long now = System.currentTimeMillis();
        if (end > now)
            end = now;
        DrawController.setSelectedInterval(end - diff, end);
    }

    void setLocked(boolean locked) {
        if (lockButton.isSelected() != locked)
            lockButton.doClick();
    }

    void setStacked(boolean stacked) {
        if (stackedButton.isSelected() != stacked)
            stackedButton.doClick();
    }

    void setStatus(String status) {
        statusLabel.setText(status);
    }

}
