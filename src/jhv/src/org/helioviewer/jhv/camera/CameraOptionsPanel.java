package org.helioviewer.jhv.camera;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.components.base.TerminatedFormatterFactory;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.gui.dialogs.TextDialog;

@SuppressWarnings("serial")
public class CameraOptionsPanel extends JPanel {

    private CameraOptionPanel currentOptionPanel;

    private static final String[] cameras = new String[] { "Observer View", "Earth View", "Arbitrary View" };
    private static final String explanation = "Observer: view from observer.\nCamera time defined by timestamps of the master layer.\n\n" +
                                              "Earth: view from Earth.\nCamera time defined by timestamps of the master layer.\n\n" +
                                              "Arbitrary: view from selected object.\nCamera time defined by timestamps of the master layer, unless " +
                                              "\"Use master layer timestamps\" is off. In that case, camera time is interpolated in the configured time interval.";

    public CameraOptionsPanel(final Camera camera) {
        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;

        final JComboBox comboBox = new JComboBox(cameras);
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedItem = (String) comboBox.getSelectedItem();
                if (selectedItem.equals(cameras[0]))
                    changeCamera(camera, Camera.CameraMode.OBSERVER);
                else if (selectedItem.equals(cameras[1]))
                    changeCamera(camera, Camera.CameraMode.EARTH);
                else
                    changeCamera(camera, Camera.CameraMode.EXPERT);
            }
        });

        add(comboBox, c);
        c.gridx = 1;
        c.weightx = 0;

        AbstractAction showInfoAction = new AbstractAction() {
            {
                putValue(SHORT_DESCRIPTION, "Show viewpoint info");
                putValue(SMALL_ICON, IconBank.getIcon(JHVIcon.INFO));
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                TextDialog td = new TextDialog("Viewpoint options information", explanation);
                td.showDialog();
            }
        };
        JButton infoButton = new JButton(showInfoAction);
        infoButton.setBorder(null);
        infoButton.setText(null);
        infoButton.setBorderPainted(false);
        infoButton.setFocusPainted(false);
        infoButton.setContentAreaFilled(false);
        add(infoButton, c);

        // fov
        double min = 0, max = 180;

        JPanel fovPanel = new JPanel();
        fovPanel.setLayout(new BoxLayout(fovPanel, BoxLayout.LINE_AXIS));
        fovPanel.add(new JLabel("FOV angle"));

        final JSpinner fovSpinner = new JSpinner();
        fovSpinner.setModel(new SpinnerNumberModel(Double.valueOf(0.8), Double.valueOf(min), Double.valueOf(max), Double.valueOf(0.01)));
        JFormattedTextField f = ((JSpinner.DefaultEditor) fovSpinner.getEditor()).getTextField();
        f.setFormatterFactory(new TerminatedFormatterFactory("%.2f", "\u00B0", min, max));

        camera.setFOVangleDegrees((Double) fovSpinner.getValue());

        fovSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                camera.setFOVangleDegrees((Double) fovSpinner.getValue());
                Displayer.display();
            }
        });
        WheelSupport.installMouseWheelSupport(fovSpinner);
        fovPanel.add(fovSpinner);

        fovSpinner.setMaximumSize(new Dimension(6, 22));
        fovPanel.add(Box.createHorizontalGlue());

        c.weightx = 1;
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 1;
        add(fovPanel, c);
    }

    private void switchOptionsPanel(CameraOptionPanel newOptionPanel) {
        if (currentOptionPanel != null) {
            currentOptionPanel.deactivate();
            remove(currentOptionPanel);
        }

        if (newOptionPanel != null) {
            currentOptionPanel = newOptionPanel;
            currentOptionPanel.activate();
            currentOptionPanel.syncWithLayer();

            GridBagConstraints c = new GridBagConstraints();
            c.weightx = 1;
            c.weighty = 1;
            c.gridwidth = 2;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = 2;
            add(currentOptionPanel, c);
        }
        revalidate();
    }

    private void changeCamera(Camera camera, Camera.CameraMode mode) {
        camera.setMode(mode);
        switchOptionsPanel(camera.getOptionPanel());
        Displayer.setActiveCamera(camera);
    }

}
