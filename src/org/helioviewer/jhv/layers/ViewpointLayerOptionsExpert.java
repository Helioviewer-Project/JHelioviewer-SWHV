package org.helioviewer.jhv.layers;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.helioviewer.jhv.astronomy.Frame;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.timeselector.TimeSelectorListener;
import org.helioviewer.jhv.gui.components.timeselector.TimeSelectorPanel;
import org.helioviewer.jhv.layers.spaceobject.SpaceObjectContainer;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

@SuppressWarnings("serial")
class ViewpointLayerOptionsExpert extends JPanel implements TimeSelectorListener {

    private final SpaceObjectContainer container;
    private final JCheckBox syncCheckBox;
    private final ButtonGroup modeGroup = new ButtonGroup();
    private final TimeSelectorPanel timeSelectorPanel = new TimeSelectorPanel();

    private Frame frame;

    ViewpointLayerOptionsExpert(JSONObject jo, UpdateViewpoint uv, Frame _frame, boolean exclusive) {
        frame = _frame;

        boolean sync = true;
        JSONArray ja = null;
        long start = Movie.getStartTime();
        long end = Movie.getEndTime();
        if (jo != null) {
            try {
                frame = Frame.valueOf(jo.optString("frame"));
            } catch (Exception ignore) {
            }
            ja = jo.optJSONArray("objects");
            sync = jo.optBoolean("syncInterval", sync);
            if (!sync) {
                start = TimeUtils.optParse(jo.optString("startTime"), start);
                end = TimeUtils.optParse(jo.optString("endTime"), end);
            }
        }
        if (ja == null)
            ja = new JSONArray(new String[] { "Earth" });

        container = new SpaceObjectContainer(ja, uv, frame, exclusive, start, end);

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        radioPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        radioPanel.add(new JLabel("Frame", JLabel.RIGHT));
        for (Frame f : Frame.values()) {
            JRadioButton radio = new JRadioButton(f.toString(), frame == f);
            radio.addItemListener(e -> {
                if (radio.isSelected()) {
                    frame = f;
                    container.setFrame(frame);
                }
            });
            radioPanel.add(radio);
            modeGroup.add(radio);
        }

        syncCheckBox = new JCheckBox("Use movie time interval", sync);
        syncCheckBox.addActionListener(e -> setTimespan(Movie.getStartTime(), Movie.getEndTime()));

        timeSelectorPanel.setTime(start, end);
        timeSelectorPanel.addListener(this);
        ComponentUtils.setEnabled(timeSelectorPanel, !sync);

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1.;
        c.weighty = 1.;
        c.gridx = 0;
        c.fill = GridBagConstraints.BOTH;

        c.gridy = 0;
        add(container, c);
        c.gridy = 1;
        add(radioPanel, c);
        c.gridy = 2;
        add(syncCheckBox, c);
        c.gridy = 3;
        add(timeSelectorPanel, c);

        ComponentUtils.smallVariant(this);
    }

    void setTimespan(long start, long end) {
        boolean notSync = !syncCheckBox.isSelected();
        ComponentUtils.setEnabled(timeSelectorPanel, notSync);
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
        jo.put("frame", frame);
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
