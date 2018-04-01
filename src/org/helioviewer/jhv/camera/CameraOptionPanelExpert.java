package org.helioviewer.jhv.camera;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.Box;
import javax.swing.JCheckBox;

import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.camera.object.SpaceObjectContainer;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.DateTimePanel;
import org.helioviewer.jhv.layers.TimespanListener;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

@SuppressWarnings("serial")
public class CameraOptionPanelExpert extends CameraOptionPanel implements TimespanListener {

    private final JCheckBox syncCheckBox;
    private final DateTimePanel startPanel = new DateTimePanel("Start");
    private final DateTimePanel endPanel = new DateTimePanel("End");

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
        startPanel.setTime(start);
        startPanel.addListener(e -> request());
        startPanel.add(Box.createRigidArea(new Dimension(40, 0)));
        add(startPanel, c);
        c.gridy = 3;
        endPanel.setTime(end);
        endPanel.addListener(e -> request());
        endPanel.add(Box.createRigidArea(new Dimension(40, 0)));
        add(endPanel, c);

        ComponentUtils.smallVariant(this);
    }

    @Override
    void activate() {
        Movie.addTimespanListener(this);
        timespanChanged(Movie.getStartTime(), Movie.getEndTime());
    }

    @Override
    void deactivate() {
        Movie.removeTimespanListener(this);
    }

    @Override
    public void timespanChanged(long start, long end) {
        boolean notSync = !syncCheckBox.isSelected();
        startPanel.setVisible(notSync);
        endPanel.setVisible(notSync);
        if (notSync)
            return;

        startPanel.setTime(start);
        endPanel.setTime(end);
        request();
    }

    JSONObject toJson() {
        JSONObject jo = new JSONObject();
        boolean sync = syncCheckBox.isSelected();
        jo.put("syncInterval", sync);
        if (!sync) {
            jo.put("startTime", new JHVDate(startPanel.getTime()));
            jo.put("endTime", new JHVDate(endPanel.getTime()));
        }
        jo.put("objects", container.toJson());
        return jo;
    }

    private void request() {
        container.setTime(startPanel.getTime(), endPanel.getTime());
    }

}
