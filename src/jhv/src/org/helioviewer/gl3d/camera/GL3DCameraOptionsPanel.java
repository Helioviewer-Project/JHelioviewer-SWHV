package org.helioviewer.gl3d.camera;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.gl3d.gui.GL3DCameraSelectorModel;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.display.Displayer;

public class GL3DCameraOptionsPanel extends JPanel {
    private static final long serialVersionUID = 3942154069677445408L;
    private final JSpinner timedelaySpinner;
    private long timeDelay;

    public GL3DCameraOptionsPanel() {
        String earthCameraButtonString = "View from earth";
        String stonyhurstCameraButtonString = "Stonyhurst view";
        String fixedTimeCameraButtonString = "View from earth at fixed time";

        JRadioButton earthCameraButton = new JRadioButton(earthCameraButtonString);
        earthCameraButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GL3DCameraSelectorModel selector = GL3DCameraSelectorModel.getInstance();
                selector.setCurrentCamera(selector.getTrackballCamera());
                Displayer.getSingletonInstance().display();
            }
        });
        JRadioButton stonyhurstCameraButton = new JRadioButton(stonyhurstCameraButtonString);
        stonyhurstCameraButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GL3DCameraSelectorModel selector = GL3DCameraSelectorModel.getInstance();
                selector.setCurrentCamera(selector.getSolarRotationCamera());
                Displayer.getSingletonInstance().display();
            }
        });

        JRadioButton fixedTimeCameraButton = new JRadioButton(fixedTimeCameraButtonString);
        fixedTimeCameraButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GL3DCameraSelectorModel selector = GL3DCameraSelectorModel.getInstance();
                selector.setCurrentCamera(selector.getTrackballCamera());
                Displayer.getSingletonInstance().display();
            }
        });
        ButtonGroup group = new ButtonGroup();
        group.add(earthCameraButton);
        group.add(stonyhurstCameraButton);
        group.add(fixedTimeCameraButton);
        JPanel radioPanel = new JPanel(new GridLayout(0, 1));
        radioPanel.add(earthCameraButton);
        radioPanel.add(stonyhurstCameraButton);
        radioPanel.add(fixedTimeCameraButton);
        this.add(radioPanel);
        add(new JLabel("Set time delay"));
        timedelaySpinner = new JSpinner();
        timedelaySpinner.setModel(new SpinnerNumberModel(new Float(0.2f), new Float(-5000), new Float(5000), new Float(0.01f)));
        timedelaySpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                GL3DState state = GL3DState.get();
                timeDelay = (long) ((SpinnerNumberModel) timedelaySpinner.getModel()).getNumber().floatValue() * 60 * 60 * 24 * 1000;
                state.getActiveCamera().setTimeDelay(timeDelay);
            }
        });
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(timedelaySpinner);
        timedelaySpinner.setEditor(editor);
        timedelaySpinner.setVisible(false);
        this.add(timedelaySpinner);
    }

}
