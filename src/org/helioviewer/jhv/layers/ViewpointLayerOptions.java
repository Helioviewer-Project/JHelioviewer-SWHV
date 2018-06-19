package org.helioviewer.jhv.layers;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.helioviewer.jhv.astronomy.Frame;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.components.base.TerminatedFormatterFactory;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.gui.dialogs.TextDialog;
import org.helioviewer.jhv.math.MathUtils;
import org.json.JSONObject;

import com.jidesoft.swing.JideButton;

@SuppressWarnings("serial")
class ViewpointLayerOptions extends JPanel implements TimespanListener {

    private enum CameraMode {
        Observer(UpdateViewpoint.observer), Earth(UpdateViewpoint.earth), Equatorial(UpdateViewpoint.equatorial), Other(UpdateViewpoint.expert);

        final UpdateViewpoint update;

        CameraMode(UpdateViewpoint _update) {
            update = _update;
        }
    }

    private double fovAngle = Camera.INITFOV / Math.PI * 180;

    private final ViewpointLayerOptionsExpert expertOptionPanel;
    private final ViewpointLayerOptionsExpert equatorialOptionPanel;

    private CameraMode cameraMode;
    private ViewpointLayerOptionsExpert currentOptionPanel;

    private static final String explanation = "<b>Observer</b>: view from observer.\nCamera time defined by timestamps of the master layer.\n\n" +
                                              "<b>Earth</b>: view from Earth.\nCamera time defined by timestamps of the master layer.\n\n" +
                                              "<b>Equatorial</b>: view onto the solar equatorial plane.\nCamera time defined by timestamps of the master layer.\n" +
                                              "If \"Use movie time interval\" is unselected, the positions of objects are interpolated in the configured time interval.\n\n" +
                                              "<b>Other</b>: view from selected object.\n" +
                                              "If \"Use movie time interval\" is unselected, the camera time is interpolated in the configured time interval.";

    ViewpointLayerOptions(JSONObject jo) {
        setLayout(new GridBagLayout());

        // create panels before potential camera change
        JSONObject joExpert = null;
        JSONObject joEquatorial = null;
        if (jo != null) {
            joExpert = jo.optJSONObject("expert");
            joEquatorial = jo.optJSONObject("equatorial");
        }
        expertOptionPanel = new ViewpointLayerOptionsExpert(joExpert, UpdateViewpoint.expert, Frame.HEEQ, true);
        equatorialOptionPanel = new ViewpointLayerOptionsExpert(joEquatorial, UpdateViewpoint.equatorial, Frame.HCI, false);

        double fovMin = 0, fovMax = 180;
        cameraMode = CameraMode.Observer;
        if (jo != null) {
            fovAngle = MathUtils.clip(jo.optDouble("fovAngle", fovAngle), fovMin, fovMax);
            try {
                cameraMode = CameraMode.valueOf(jo.optString("mode"));
            } catch (Exception ignore) {
            }
            JSONObject jc = jo.optJSONObject("camera");
            if (jc != null)
                Display.getCamera().fromJson(jc);
        }

        JPanel fovPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        fovPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        fovPanel.add(new JLabel("FOV angle", JLabel.RIGHT));

        JSpinner fovSpinner = new JSpinner(new SpinnerNumberModel(Double.valueOf(fovAngle), Double.valueOf(fovMin), Double.valueOf(fovMax), Double.valueOf(0.01)));
        fovSpinner.setMaximumSize(new Dimension(6, 22));
        fovSpinner.addChangeListener(e -> {
            fovAngle = (Double) fovSpinner.getValue();
            Display.display();
        });

        JFormattedTextField f = ((JSpinner.DefaultEditor) fovSpinner.getEditor()).getTextField();
        f.setFormatterFactory(new TerminatedFormatterFactory("%.2f", "\u00B0", fovMin, fovMax));

        WheelSupport.installMouseWheelSupport(fovSpinner);
        fovPanel.add(fovSpinner);

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

        JideButton info = new JideButton(Buttons.info);
        info.setToolTipText("Show viewpoint info");
        info.addActionListener(e -> new TextDialog("Viewpoint options information", explanation, false).showDialog());
        radioPanel.add(info);

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.weightx = 1.;
        c.weighty = 1.;

        c.gridy = 0;
        add(fovPanel, c);
        c.gridy = 1;
        add(radioPanel, c);

        ComponentUtils.smallVariant(this);

        syncViewpoint();
    }

    double getFOVAngle() {
        return fovAngle * (Math.PI / 180.);
    }

    void serialize(JSONObject jo) {
        jo.put("mode", cameraMode);
        jo.put("camera", Display.getCamera().toJson());
        jo.put("fovAngle", fovAngle);
        jo.put("expert", expertOptionPanel.toJson());
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
        return expertOptionPanel.isDownloading() || equatorialOptionPanel.isDownloading();
    }

    void syncViewpoint() {
        ViewpointLayerOptionsExpert panel = null;
        if (cameraMode == CameraMode.Other)
            panel = expertOptionPanel;
        else if (cameraMode == CameraMode.Equatorial)
            panel = equatorialOptionPanel;
        switchOptionsPanel(panel);

        Display.getCamera().setViewpointUpdate(cameraMode.update);
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
        expertOptionPanel.setTimespan(start, end);
        equatorialOptionPanel.setTimespan(start, end);
    }

}
