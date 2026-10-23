package org.helioviewer.jhv.timelines.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Objects;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.gui.component.Buttons;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.TimeAxis;

import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideToggleButton;

@SuppressWarnings("serial")
final class DrawControllerOptions extends JPanel implements DrawController.Listener {

    private final JComboBox<ZoomItem> zoomCombo;
    private final JideToggleButton lockButton;
    private final JideToggleButton stackedButton;
    private final JLabel statusLabel;

    private enum ZOOM {
        CUSTOM, All, Year, Month, Day, Hour, Carrington, Movie
    }

    DrawControllerOptions(JComboBox<String> predefinedCombo, JideButton addLayerButton) {
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
        lockButton.addActionListener(e -> DrawController.setLocked(lockButton.isSelected()));

        stackedButton = new JideToggleButton(Buttons.overlaid);
        stackedButton.setToolTipText("Stack layers vertically");
        stackedButton.addActionListener(e -> DrawController.setStacked(stackedButton.isSelected()));

        statusLabel = new JLabel("", JLabel.RIGHT);

        JPanel controls = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        controls.add(predefinedCombo, c);
        c.gridx = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        controls.add(stackedButton, c);
        c.gridx = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        controls.add(zoomCombo, c);

        JPanel actions = new JPanel(new BorderLayout());
        actions.add(addLayerButton, BorderLayout.LINE_START);
        actions.add(statusLabel, BorderLayout.CENTER);
        actions.add(lockButton, BorderLayout.LINE_END);

        add(controls, BorderLayout.NORTH);
        add(actions, BorderLayout.CENTER);

        optionsChanged();
        DrawController.addDrawListener(this);
    }

    @Override
    public void optionsChanged() {
        boolean locked = DrawController.isLocked();
        lockButton.setSelected(locked);
        lockButton.setText((locked ? Buttons.lock : Buttons.unlock) + "Lock movie");
        boolean stacked = DrawController.getGeometry().isStacked();
        stackedButton.setSelected(stacked);
        stackedButton.setText(stacked ? Buttons.stacked : Buttons.overlaid);
        statusLabel.setText(DrawController.getStatus());
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

}
