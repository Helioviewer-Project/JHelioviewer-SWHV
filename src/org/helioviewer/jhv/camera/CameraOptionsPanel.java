package org.helioviewer.jhv.camera;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.ButtonGroup;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.components.base.TerminatedFormatterFactory;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.gui.dialogs.TextDialog;
import org.helioviewer.jhv.math.MathUtils;
import org.json.JSONObject;

import com.jidesoft.swing.JideButton;

@SuppressWarnings("serial")
public class CameraOptionsPanel extends JPanel {

    private enum CameraMode {
        Observer, Earth, Equatorial, Other
    }

    private double fovAngle = 0.8;

    private final ButtonGroup modeGroup = new ButtonGroup();
    private final CameraOptionPanelExpert expertOptionPanel;
    private final CameraOptionPanelExpert equatorialOptionPanel;
    private CameraOptionPanel currentOptionPanel;

    private static final String explanation = "<b>Observer</b>: view from observer.\nCamera time defined by timestamps of the master layer.\n\n" +
                                              "<b>Earth</b>: view from Earth.\nCamera time defined by timestamps of the master layer.\n\n" +
                                              "<b>Equatorial</b>: view onto the solar equatorial plane.\nCamera time defined by timestamps of the master layer.\n" +
                                              "Object positions are interpolated in the time interval of the master layer, unless " +
                                              "\"Use master layer time interval\" is off.\nIn that case, the positions are interpolated in the configured time interval.\n\n" +
                                              "<b>Other</b>: view from selected object.\nCamera time is interpolated in the time interval of the master layer, unless " +
                                              "\"Use master layer time interval\" is off.\nIn that case, camera time is interpolated in the configured time interval.";

    public CameraOptionsPanel(JSONObject jo) {
        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 0;

        CameraMode defaultMode = CameraMode.Observer;
        double fovMin = 0, fovMax = 180;
        JSONObject joExpert = null;
        JSONObject joEquatorial = null;
        if (jo != null) {
            fovAngle = MathUtils.clip(jo.optDouble("fovAngle", fovAngle), fovMin, fovMax);
            String strMode = jo.optString("mode", defaultMode.toString());
            try {
                defaultMode = CameraMode.valueOf(strMode);
            } catch (Exception ignore) {
            }
            joExpert = jo.optJSONObject("expert");
            joEquatorial = jo.optJSONObject("equatorial");
        }

        expertOptionPanel = new CameraOptionPanelExpert(joExpert, UpdateViewpoint.expert, "HEEQ", true);
        equatorialOptionPanel = new CameraOptionPanelExpert(joEquatorial, UpdateViewpoint.equatorial, "HEEQ", false);

        JPanel radio = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
        radio.add(new JLabel("View", JLabel.RIGHT));
        for (CameraMode mode : CameraMode.values()) {
            JRadioButton item = new JRadioButton(mode.toString());
            item.addActionListener(e -> changeCamera(mode));
            item.setActionCommand(mode.toString());
            radio.add(item);
            modeGroup.add(item);
            if (mode == defaultMode)
                item.doClick();
        }
        add(radio, c);

        JideButton info = new JideButton(Buttons.info);
        info.setToolTipText("Show viewpoint info");
        info.addActionListener(e -> new TextDialog("Viewpoint options information", explanation, false).showDialog());

        c.gridx = 1;
        c.weightx = 0;
        add(info, c);

        JPanel fovPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
        fovPanel.add(new JLabel("FOV angle", JLabel.RIGHT));

        JSpinner fovSpinner = new JSpinner(new SpinnerNumberModel(Double.valueOf(fovAngle), Double.valueOf(fovMin), Double.valueOf(fovMax), Double.valueOf(0.01)));
        fovSpinner.setMaximumSize(new Dimension(6, 22));
        fovSpinner.addChangeListener(e -> {
            fovAngle = (Double) fovSpinner.getValue();
            Displayer.display();
        });

        JFormattedTextField f = ((JSpinner.DefaultEditor) fovSpinner.getEditor()).getTextField();
        f.setFormatterFactory(new TerminatedFormatterFactory("%.2f", "\u00B0", fovMin, fovMax));

        WheelSupport.installMouseWheelSupport(fovSpinner);
        fovPanel.add(fovSpinner);

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 1;
        add(fovPanel, c);

        ComponentUtils.smallVariant(this);
    }

    public double getFOVAngle() {
        return fovAngle * (Math.PI / 180.);
    }

    public void serialize(JSONObject jo) {
        jo.put("mode", modeGroup.getSelection().getActionCommand());
        jo.put("fovAngle", fovAngle);
        jo.put("expert", expertOptionPanel.toJson());
        jo.put("equatorial", equatorialOptionPanel.toJson());
    }

    private void switchOptionsPanel(CameraOptionPanel newOptionPanel) {
        if (currentOptionPanel == newOptionPanel)
            return;

        if (currentOptionPanel != null) {
            currentOptionPanel.deactivate();
            remove(currentOptionPanel);
        }

        if (newOptionPanel != null) {
            newOptionPanel.activate();

            GridBagConstraints c = new GridBagConstraints();
            c.weightx = 1;
            c.weighty = 1;
            c.gridwidth = 2;
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 0;
            c.gridy = 2;
            add(newOptionPanel, c);
        }
        currentOptionPanel = newOptionPanel;
        revalidate();
        repaint();
    }

    private void changeCamera(CameraMode mode) {
        UpdateViewpoint update;
        CameraOptionPanel panel = null;

        switch (mode) {
            case Other:
                update = UpdateViewpoint.expert;
                panel = expertOptionPanel;
            break;
            case Earth:
                update = UpdateViewpoint.earth;
            break;
            case Equatorial:
                UpdateViewpoint.equatorial.setDistance(2 * Sun.MeanEarthDistance / Math.tan(0.5 * Displayer.getCamera().getFOV()));
                update = UpdateViewpoint.equatorial;
                panel = equatorialOptionPanel;
            break;
            default:
                update = UpdateViewpoint.observer;
        }
        Displayer.setViewpointUpdate(update);
        Displayer.getCamera().reset();

        switchOptionsPanel(panel);
    }

}
