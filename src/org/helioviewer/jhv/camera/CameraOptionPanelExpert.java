package org.helioviewer.jhv.camera;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.camera.object.SpaceObjectContainer;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.timeselector.TimeSelectorListener;
import org.helioviewer.jhv.gui.components.timeselector.TimeSelectorPanel;
import org.helioviewer.jhv.layers.TimespanListener;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

@SuppressWarnings("serial")
class CameraOptionPanelExpert extends JPanel implements TimespanListener, TimeSelectorListener {

    private final JCheckBox syncCheckBox;
    private final TimeSelectorPanel timeSelectorPanel = new TimeSelectorPanel();

    private final SpaceObjectContainer container;

    CameraOptionPanelExpert(JSONObject jo, UpdateViewpoint uv, String frame, boolean exclusive) {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.fill = GridBagConstraints.BOTH;

        boolean sync = true;
        JSONArray ja = null;
        long start = Movie.getTime().milli, end = start;
        if (jo != null) {
            ja = jo.optJSONArray("objects");
            sync = jo.optBoolean("syncInterval", sync);
            if (!sync) {
                long t = System.currentTimeMillis();
                start = TimeUtils.optParse(jo.optString("startTime"), t - 2 * TimeUtils.DAY_IN_MILLIS);
                end = TimeUtils.optParse(jo.optString("endTime"), t);
            }
        }
        if (ja == null)
            ja = new JSONArray(new String[] { "Earth" });

        c.gridy = 0;
        container = new SpaceObjectContainer(ja, uv, frame, exclusive, start, end);
        add(container, c);

        c.gridy = 1;
        syncCheckBox = new JCheckBox("Use movie time interval", sync);
        syncCheckBox.addActionListener(e -> timespanChanged(Movie.getStartTime(), Movie.getEndTime()));
        add(syncCheckBox, c);

        c.gridy = 2;
        timeSelectorPanel.setTime(start, end);
        timeSelectorPanel.addListener(this);
        add(timeSelectorPanel, c);

        ComponentUtils.smallVariant(this);
    }

    void activate() {
        Movie.addTimespanListener(this);
        timespanChanged(Movie.getStartTime(), Movie.getEndTime());
    }

    void deactivate() {
        Movie.removeTimespanListener(this);
    }

    @Override
    public void timespanChanged(long start, long end) {
        boolean notSync = !syncCheckBox.isSelected();
        timeSelectorPanel.setVisible(notSync);
        if (notSync)
            return;

        timeSelectorPanel.setTime(start, end);
    }

    @Override
    public void timeSelectionChanged(long start, long end) {
        container.setTime(start, end);
    }

    boolean isDownloading() {
        return container.isDownloading();
    }

    JSONObject toJson() {
        JSONObject jo = new JSONObject();
        boolean sync = syncCheckBox.isSelected();
        jo.put("syncInterval", sync);
        if (!sync) {
            jo.put("startTime", new JHVDate(timeSelectorPanel.getStartTime()));
            jo.put("endTime", new JHVDate(timeSelectorPanel.getEndTime()));
        }
        jo.put("objects", container.toJson());
        return jo;
    }

}
