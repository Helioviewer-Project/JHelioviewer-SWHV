package org.helioviewer.jhv.layers;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.helioviewer.jhv.astronomy.Frame;
import org.helioviewer.jhv.astronomy.PositionLoad;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.components.base.JHVButton;
import org.helioviewer.jhv.gui.dialogs.TextDialog;
import org.json.JSONObject;

@SuppressWarnings("serial")
class ViewpointLayerOptions extends JPanel implements TimespanListener {

    private enum CameraMode {
        Observer(UpdateViewpoint.observer), Location(UpdateViewpoint.location), Heliosphere(UpdateViewpoint.equatorial);

        final UpdateViewpoint update;

        CameraMode(UpdateViewpoint _update) {
            update = _update;
        }
    }

    private final ViewpointLayerOptionsExpert locationOptionPanel;
    private final ViewpointLayerOptionsExpert equatorialOptionPanel;

    private CameraMode cameraMode;
    private ViewpointLayerOptionsExpert currentOptionPanel;

    private static final String explanation = "<b>Observer</b>: view from observer.\nCamera time defined by timestamps of the master layer.\n\n" +
            "<b>Location</b>: view from selected object.\n" +
            "If \"Use movie time interval\" is unselected, the camera time is interpolated in the configured time interval.\n\n" +
            "<b>Heliosphere</b>: view onto the solar equatorial plane.\n" +
            "If \"Use movie time interval\" is unselected, the positions of objects are interpolated in the configured time interval.";

    ViewpointLayerOptions(JSONObject jo) {
        setLayout(new GridBagLayout());

        // create panels before potential camera change
        JSONObject joLocation = null;
        JSONObject joEquatorial = null;
        if (jo != null) {
            joLocation = jo.optJSONObject("location");
            joEquatorial = jo.optJSONObject("equatorial");
        }
        locationOptionPanel = new ViewpointLayerOptionsExpert(joLocation, UpdateViewpoint.location, SpaceObject.Sol, Frame.SOLO_IAU_SUN_2009, true);
        equatorialOptionPanel = new ViewpointLayerOptionsExpert(joEquatorial, UpdateViewpoint.equatorial, SpaceObject.Sol, Frame.HCI, false);

        cameraMode = CameraMode.Observer;
        if (jo != null) {
            try {
                cameraMode = CameraMode.valueOf(jo.optString("mode"));
            } catch (Exception ignore) {
            }
            JSONObject jc = jo.optJSONObject("camera");
            if (jc != null)
                Display.getCamera().fromJson(jc);
        }

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        radioPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        radioPanel.add(new JLabel("View", JLabel.RIGHT));
        ButtonGroup modeGroup = new ButtonGroup();
        for (CameraMode mode : CameraMode.values()) {
            JRadioButton radio = new JRadioButton(mode.toString(), mode == cameraMode);
            radio.addItemListener(e -> {
                if (radio.isSelected()) {
                    cameraMode = mode;
                    syncViewpoint();
                }
            });
            radioPanel.add(radio);
            modeGroup.add(radio);
        }

        JHVButton info = new JHVButton(Buttons.info);
        info.setToolTipText("Show viewpoint info");
        info.addActionListener(e -> new TextDialog("Viewpoint Options Information", explanation, false).showDialog());
        radioPanel.add(info);

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.weightx = 1.;
        c.weighty = 1.;

        c.gridy = 0;
        add(radioPanel, c);

        ComponentUtils.smallVariant(this);

        syncViewpoint();
    }

    void serialize(JSONObject jo) {
        jo.put("mode", cameraMode);
        jo.put("camera", Display.getCamera().toJson());
        jo.put("location", locationOptionPanel.toJson());
        jo.put("equatorial", equatorialOptionPanel.toJson());
    }

    private void switchOptionsPanel(ViewpointLayerOptionsExpert newOptionPanel) {
        if (currentOptionPanel == newOptionPanel)
            return;

        if (currentOptionPanel != null) {
            remove(currentOptionPanel);
        }

        if (newOptionPanel != null) {
            GridBagConstraints c = new GridBagConstraints();
            c.weightx = 1;
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 0;
            c.gridy = 2;
            add(newOptionPanel, c);
        }
        currentOptionPanel = newOptionPanel;
        revalidate();
        repaint();
    }

    boolean isDownloading() {
        return locationOptionPanel.isDownloading() || equatorialOptionPanel.isDownloading();
    }

    void syncViewpoint() {
        ViewpointLayerOptionsExpert panel = null;
        if (cameraMode == CameraMode.Location)
            panel = locationOptionPanel;
        else if (cameraMode == CameraMode.Heliosphere)
            panel = equatorialOptionPanel;
        switchOptionsPanel(panel);

        Display.getCamera().setViewpointUpdate(cameraMode.update);
    }

    void activate() {
        Movie.addTimespanListener(this);
    }

    void deactivate() {
        Movie.removeTimespanListener(this);
    }

    @Override
    public void timespanChanged(long start, long end) {
        locationOptionPanel.setTimespan(start, end);
        equatorialOptionPanel.setTimespan(start, end);
    }

    boolean isHeliospheric() {
        return currentOptionPanel == equatorialOptionPanel;
    }

    @Nullable
    PositionLoad getHighlightedLoad() {
        return currentOptionPanel == equatorialOptionPanel ? equatorialOptionPanel.getHighlightedLoad() : null;
    }

    int getSpiralSpeed() {
        return currentOptionPanel == equatorialOptionPanel ? equatorialOptionPanel.getSpiralSpeed() : 0;
    }

    boolean isRelative() {
        return currentOptionPanel == equatorialOptionPanel && equatorialOptionPanel.isRelative();
    }

}
