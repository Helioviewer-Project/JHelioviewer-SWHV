package org.helioviewer.jhv.camera;

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

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.components.base.TerminatedFormatterFactory;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.gui.dialogs.TextDialog;

import com.jidesoft.swing.JideButton;

@SuppressWarnings("serial")
public class CameraOptionsPanel extends JPanel implements PositionLoadFire {

    private enum CameraMode {
        Observer, Earth, Other
    }

    private static final double FOVAngleDefault = 0.8;
    private double FOVAngle = FOVAngleDefault * Math.PI / 180.;

    private final CameraOptionPanelExpert expertOptionPanel;
    private CameraOptionPanel currentOptionPanel;

    private static final String explanation = "<b>Observer</b>: view from observer.\nCamera time defined by timestamps of the master layer.\n\n" +
                                              "<b>Earth</b>: view from Earth.\nCamera time defined by timestamps of the master layer.\n\n" +
                                              "<b>Other</b>: view from selected object.\nCamera time defined by timestamps of the master layer, unless " +
                                              "\"Use master layer timestamps\" is off.\nIn that case, camera time is interpolated in the configured time interval.";

    public CameraOptionsPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 0;

        JPanel radio = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
        radio.add(new JLabel("View", JLabel.RIGHT));

        ButtonGroup group = new ButtonGroup();
        for (CameraMode mode : CameraMode.values()) {
            JRadioButton item = new JRadioButton(mode.toString());
            if (mode == CameraMode.Observer)
                item.setSelected(true);
            item.addActionListener(e -> changeCamera(mode));
            group.add(item);
            radio.add(item);
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

        double min = 0, max = 180;
        JSpinner fovSpinner = new JSpinner(new SpinnerNumberModel(Double.valueOf(FOVAngleDefault), Double.valueOf(min), Double.valueOf(max), Double.valueOf(0.01)));
        fovSpinner.setMaximumSize(new Dimension(6, 22));
        fovSpinner.addChangeListener(e -> {
            FOVAngle = (Double) fovSpinner.getValue() * Math.PI / 180.;
            Displayer.display();
        });

        JFormattedTextField f = ((JSpinner.DefaultEditor) fovSpinner.getEditor()).getTextField();
        f.setFormatterFactory(new TerminatedFormatterFactory("%.2f", "\u00B0", min, max));

        WheelSupport.installMouseWheelSupport(fovSpinner);
        fovPanel.add(fovSpinner);

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 1;
        add(fovPanel, c);

        ComponentUtils.smallVariant(this);

        PositionLoad positionLoad = new PositionLoad(this);
        ((UpdateViewpoint.UpdateViewpointExpert) UpdateViewpoint.updateExpert).setPositionLoad(positionLoad);
        expertOptionPanel = new CameraOptionPanelExpert(positionLoad);
    }

    public double getFOVAngle() {
        return FOVAngle;
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
            newOptionPanel.syncWithLayer();

            GridBagConstraints c = new GridBagConstraints();
            c.weightx = 1;
            c.weighty = 1;
            c.gridwidth = 2;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = 2;
            add(newOptionPanel, c);
        }
        currentOptionPanel = newOptionPanel;
        revalidate();
    }

    private void changeCamera(CameraMode mode) {
        UpdateViewpoint update;
        CameraOptionPanel panel = null;

        switch (mode) {
            case Other:
                update = UpdateViewpoint.updateExpert;
                panel = expertOptionPanel;
            break;
            case Earth:
                update = UpdateViewpoint.updateEarth;
            break;
            default:
                update = UpdateViewpoint.updateObserver;
        }
        Displayer.setViewpointUpdate(update);
        Displayer.getCamera().reset();

        switchOptionsPanel(panel);
    }

    @Override
    public void fireLoaded(String state) {
        expertOptionPanel.fireLoaded(state);
        Displayer.getCamera().refresh();
    }

}
