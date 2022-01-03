package org.helioviewer.jhv.layers;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.helioviewer.jhv.astronomy.Frame;
import org.helioviewer.jhv.astronomy.PositionLoad;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.gui.components.base.JHVSlider;
import org.helioviewer.jhv.layers.spaceobject.SpaceObjectContainer;
import org.json.JSONArray;
import org.json.JSONObject;

@SuppressWarnings("serial")
class ViewpointLayerOptionsExpert extends JPanel {

    private final SpaceObjectContainer container;

    private static final int MIN_SPEED_SPIRAL = 200;
    private static final int MAX_SPEED_SPIRAL = 2000;
    private int spiralSpeed = 500;
    private int spiralMult = 0;
    private boolean relative = false;

    private Frame frame;

    ViewpointLayerOptionsExpert(JSONObject jo, UpdateViewpoint uv, SpaceObject observer, Frame _frame, long start, long end, boolean exclusive) {
        frame = _frame;

        JSONArray ja = null;
        if (jo != null) {
            try {
                frame = Frame.valueOf(jo.optString("frame"));
            } catch (Exception ignore) {
            }
            relative = jo.optBoolean("relativeLongitude", relative);
            ja = jo.optJSONArray("objects");
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
        JHVSlider spiralSlider = new JHVSlider(MIN_SPEED_SPIRAL, MAX_SPEED_SPIRAL, spiralSpeed);
        spiralSlider.addChangeListener(e -> {
            spiralSpeed = spiralSlider.getValue();
            spiralLabel.setText(spiralSpeed + " km/s");
            MovieDisplay.display();
        });

        JPanel spiralPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        spiralPanel.add(spiralCheckBox);
        spiralPanel.add(spiralSlider);
        spiralPanel.add(spiralLabel);

        JPanel framePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        framePanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        framePanel.add(new JLabel("Frame", JLabel.RIGHT));
        ButtonGroup modeGroup = new ButtonGroup();
        for (Frame f : List.of(Frame.SOLO_HCI, Frame.SOLO_HEEQ, Frame.SOLO_HEE)) {
            JRadioButton radio = new JRadioButton(f.uiString(), f == frame);
            radio.addItemListener(e -> {
                if (radio.isSelected()) {
                    frame = f;
                    container.setFrame(frame);
                }
            });
            framePanel.add(radio);
            modeGroup.add(radio);
        }
        JCheckBox relativeCheckBox = new JCheckBox("Relative longitude", relative);
        relativeCheckBox.addActionListener(e -> {
            relative = !relative;
            Display.getCamera().refresh(); // full camera refresh to update viewpoint
        });
        framePanel.add(relativeCheckBox);

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1.;
        c.weighty = 1.;
        c.gridx = 0;
        c.fill = GridBagConstraints.BOTH;

        c.gridy = 0;
        add(container, c);
        if (!exclusive) {
            c.gridy = 1;
            add(framePanel, c);
            c.gridy = 2;
            add(spiralPanel, c);
        }
    }

    void setTimespan(long start, long end) {
        container.setTime(start, end);
    }

    boolean isDownloading() {
        return container.isDownloading();
    }

    JSONObject toJson() {
        JSONObject jo = new JSONObject();
        jo.put("frame", frame);
        jo.put("relativeLongitude", relative);
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
