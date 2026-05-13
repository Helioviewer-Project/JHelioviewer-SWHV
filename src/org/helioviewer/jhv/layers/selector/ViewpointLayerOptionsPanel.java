package org.helioviewer.jhv.layers.selector;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.dialogs.TextDialog;
import org.helioviewer.jhv.layers.ViewpointLayer;
import org.helioviewer.jhv.layers.ViewpointLayerOptions;
import org.helioviewer.jhv.layers.ViewpointLayerOptions.CameraMode;

import com.jidesoft.swing.JideButton;

@SuppressWarnings("serial")
final class ViewpointLayerOptionsPanel extends JPanel {

    private static final String explanation = """
            <b>Observer at 1au</b>: view from the active observer, but at 1au fixed distance.
            <b>Location</b>: view from selected object.
            <b>Heliosphere</b>: view onto the solar equatorial plane.
            
            If "Use movie time interval" is unselected, the viewpoint time is interpolated in the configured time interval.""";

    private final ViewpointLayerOptions options;
    private final ViewpointLayerOptionsExpertPanel locationPanel;
    private final ViewpointLayerOptionsExpertPanel equatorialPanel;
    private Component currentOptionPanel;

    ViewpointLayerOptionsPanel(ViewpointLayer layer) {
        options = layer.getOptions();
        locationPanel = new ViewpointLayerOptionsExpertPanel(options.getLocationOptions());
        equatorialPanel = new ViewpointLayerOptionsExpertPanel(options.getEquatorialOptions());
        setLayout(new GridBagLayout());

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        ButtonGroup modeGroup = new ButtonGroup();
        for (CameraMode mode : CameraMode.values()) {
            JRadioButton radio = new JRadioButton(mode.toString(), mode == options.getCameraMode());
            radio.setHorizontalTextPosition(SwingConstants.LEFT);
            radio.addItemListener(e -> {
                if (radio.isSelected()) {
                    options.setCameraMode(mode, Camera.ViewpointApplyMode.RESET);
                    switchOptionsPanel(getCurrentOptionPanel());
                }
            });
            radioPanel.add(radio);
            modeGroup.add(radio);
        }

        JideButton info = new JideButton(Buttons.info);
        info.setToolTipText("Show viewpoint info");
        info.addActionListener(e -> new TextDialog("Viewpoint Options Information", explanation, false).showDialog());
        radioPanel.add(info);

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.weightx = 1;
        c.weighty = 1;

        c.gridy = 0;
        add(radioPanel, c);

        switchOptionsPanel(getCurrentOptionPanel());
    }

    private Component getCurrentOptionPanel() {
        return switch (options.getCameraMode()) {
            case ObserverAt1au -> null;
            case Location -> locationPanel;
            case Heliosphere -> equatorialPanel;
        };
    }

    private void switchOptionsPanel(Component newOptionPanel) {
        if (currentOptionPanel == newOptionPanel)
            return;

        if (currentOptionPanel != null)
            remove(currentOptionPanel);

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

}
