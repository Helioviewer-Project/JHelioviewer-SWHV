package org.helioviewer.jhv.layers;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;

import org.helioviewer.jhv.astronomy.Frame;
import org.helioviewer.jhv.astronomy.PositionLoad;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.gui.components.timeselector.TimeSelectorListener;
import org.helioviewer.jhv.gui.components.timeselector.TimeSelectorPanel;
import org.helioviewer.jhv.layers.spaceobject.SpaceObjectContainer;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

@SuppressWarnings("serial")
class ViewpointLayerOptionsExpert extends JPanel implements TimeSelectorListener {

    private final SpaceObjectContainer container;
    private final JCheckBox syncCheckBox;
    private final JCheckBox relativeCheckBox;
    private final TimeSelectorPanel timeSelectorPanel = new TimeSelectorPanel();

    private static final int MIN_SPEED_SPIRAL = 200;
    private static final int MAX_SPEED_SPIRAL = 2000;
    private int spiralSpeed = 500;
    private int spiralMult = 0;
    private boolean relative;

    private Frame frame;

    ViewpointLayerOptionsExpert(JSONObject jo, UpdateViewpoint uv, SpaceObject observer, Frame _frame, boolean exclusive) {
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

        container = new SpaceObjectContainer(ja, exclusive, uv, observer, frame, start, end);

        JCheckBox spiralCheckBox = new JCheckBox("Spiral", false);
        spiralCheckBox.addActionListener(e -> {
            spiralMult = spiralCheckBox.isSelected() ? 1 : 0;
            MovieDisplay.display();
        });

        JLabel spiralLabel = new JLabel(spiralSpeed + " km/s");
        JSlider spiralSlider = new JSlider(JSlider.HORIZONTAL, MIN_SPEED_SPIRAL, MAX_SPEED_SPIRAL, spiralSpeed);
        spiralSlider.addChangeListener(e -> {
            spiralSpeed = spiralSlider.getValue();
            spiralLabel.setText(spiralSpeed + " km/s");
            MovieDisplay.display();
        });
        WheelSupport.installMouseWheelSupport(spiralSlider);

        JPanel spiralPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        spiralPanel.add(spiralCheckBox);
        spiralPanel.add(spiralSlider);
        spiralPanel.add(spiralLabel);

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
        relativeCheckBox = new JCheckBox("Relative longitude");
        relativeCheckBox.addActionListener(e -> {
            relative = !relative;
            MovieDisplay.display();
        });
        framePanel.add(relativeCheckBox);

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
        add(container, c);
        c.gridy = 1;
        add(syncCheckBox, c);
        c.gridy = 2;
        add(timeSelectorPanel, c);
        if (!exclusive) {
            c.gridy = 3;
            add(framePanel, c);
            c.gridy = 4;
            add(spiralPanel, c);
        }

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
            jo.put("startTime", new JHVTime(timeSelectorPanel.getStartTime()));
            jo.put("endTime", new JHVTime(timeSelectorPanel.getEndTime()));
        }
        jo.put("objects", container.toJson());
        return jo;
    }

    @Nullable
    PositionLoad getHighlightedLoad() {
        return container.getHighlightedLoad();
    }

    int getSpiralSpeed() {
        return spiralMult * spiralSpeed;
    }

    boolean isRelative() {
        return relative;
    }

}
