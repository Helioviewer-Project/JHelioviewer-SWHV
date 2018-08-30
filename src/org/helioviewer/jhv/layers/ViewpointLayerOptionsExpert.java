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
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.timeselector.TimeSelectorListener;
import org.helioviewer.jhv.gui.components.timeselector.TimeSelectorPanel;
import org.helioviewer.jhv.layers.spaceobject.SpaceObjectComboBox;
import org.helioviewer.jhv.layers.spaceobject.SpaceObjectContainer;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

@SuppressWarnings("serial")
class ViewpointLayerOptionsExpert extends JPanel implements TimeSelectorListener {

    private final SpaceObjectContainer container;
    private final SpaceObjectComboBox objectCombo;
    private final JCheckBox syncCheckBox;
    private final TimeSelectorPanel timeSelectorPanel = new TimeSelectorPanel();

    private Frame frame;

    ViewpointLayerOptionsExpert(JSONObject jo, boolean viewFrom, UpdateViewpoint uv, SpaceObject observer, Frame _frame) {
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
            ja = new JSONArray(new String[]{"Earth"});


        uv.clear();
        // if viewFrom
        objectCombo = new SpaceObjectComboBox(ja, viewFrom, uv, observer, frame, start, end);
        container = new SpaceObjectContainer(ja, !viewFrom, uv, observer, frame, start, end);

        // if !viewFrom
        JPanel framePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        framePanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        framePanel.add(new JLabel("Frame", JLabel.RIGHT));
        ButtonGroup modeGroup = new ButtonGroup();
        for (Frame f : Frame.values()) {
            JRadioButton radio = new JRadioButton(f.toString(), f == frame);
            radio.addItemListener(e -> {
                if (radio.isSelected()) {
                    frame = f;
                    container.setFrame(frame);
                }
            });
            framePanel.add(radio);
            modeGroup.add(radio);
        }

        syncCheckBox = new JCheckBox("Use movie time interval", sync);
        syncCheckBox.addActionListener(e -> setTimespan(Movie.getStartTime(), Movie.getEndTime()));

        timeSelectorPanel.setTime(start, end);
        timeSelectorPanel.setVisible(!sync);
        timeSelectorPanel.addListener(this);

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1.;
        c.weighty = 1.;
        c.gridx = 0;
        c.fill = GridBagConstraints.BOTH;

        c.gridy = 0;
        add(viewFrom ? objectCombo : framePanel, c);
        c.gridy++;
        add(container, c);
        c.gridy++;
        add(syncCheckBox, c);
        c.gridy++;
        add(timeSelectorPanel, c);

        ComponentUtils.smallVariant(this);
    }

    void setTimespan(long start, long end) {
        boolean notSync = !syncCheckBox.isSelected();
        timeSelectorPanel.setVisible(notSync);
        if (notSync)
            return;
        timeSelectorPanel.setTime(start, end);
    }

    @Override
    public void timeSelectionChanged(long start, long end) {
        container.setTime(start, end);
        objectCombo.setTime(start, end);
    }

    boolean isDownloading() {
        return objectCombo.isDownloading() || container.isDownloading();
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
        // also SpaceObjectComboBox
        return jo;
    }

}
