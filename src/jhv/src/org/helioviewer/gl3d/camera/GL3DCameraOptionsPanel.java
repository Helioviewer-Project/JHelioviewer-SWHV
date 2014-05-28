package org.helioviewer.gl3d.camera;

import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;

import org.helioviewer.basegui.components.TimeTextField;
import org.helioviewer.gl3d.gui.GL3DCameraSelectorModel;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarEvent;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarListener;

public class GL3DCameraOptionsPanel extends JPanel {
    private static final long serialVersionUID = 3942154069677445408L;
    private JSpinner timedelaySpinner;
    private long timeDelay;
    private JPanel timedelayPanel;
    private JLabel timedelayLabel;
    private JHVCalendarDatePicker timedelayDate;
    private TimeTextField timedelayTime;

    public GL3DCameraOptionsPanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                addCameraChoiceRadioButtons();
                addTimedelayPanel();
            }
        });
    }

    private void addCameraChoiceRadioButtons() {
        String earthCameraButtonString = "View from earth";
        String stonyhurstCameraButtonString = "Stonyhurst view";
        String fixedTimeCameraButtonString = "View from earth at fixed time";

        JRadioButton earthCameraButton = new JRadioButton(earthCameraButtonString);
        earthCameraButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GL3DCameraSelectorModel selector = GL3DCameraSelectorModel.getInstance();
                selector.setCurrentCamera(selector.getTrackballCamera());
                hideTimedelayComponents();
                Displayer.getSingletonInstance().display();
            }
        });

        JRadioButton stonyhurstCameraButton = new JRadioButton(stonyhurstCameraButtonString);
        stonyhurstCameraButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GL3DCameraSelectorModel selector = GL3DCameraSelectorModel.getInstance();
                selector.setCurrentCamera(selector.getSolarRotationCamera());
                hideTimedelayComponents();
                Displayer.getSingletonInstance().display();
            }
        });

        JRadioButton fixedTimeCameraButton = new JRadioButton(fixedTimeCameraButtonString);
        fixedTimeCameraButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GL3DCameraSelectorModel selector = GL3DCameraSelectorModel.getInstance();
                selector.setCurrentCamera(selector.getFixedTimeCamera());
                showTimedelayComponents();
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
    }

    private void addTimedelayPanel() {
        timedelayLabel = new JLabel("Set time delay");
        add(timedelayLabel);
        timedelayPanel = new JPanel();
        timedelayPanel.setLayout(new GridLayout(0, 2));
        timedelayDate = new JHVCalendarDatePicker();
        timedelayDate.addJHVCalendarListener(new JHVCalendarListener() {
            @Override
            public void actionPerformed(JHVCalendarEvent e) {
                GL3DState state = GL3DState.get();
                computeTimedelayTime();
                state.getActiveCamera().setTimeDelay(timeDelay);
                Displayer.getSingletonInstance().display();
            }
        });
        Date startDate = new Date(System.currentTimeMillis());
        timedelayDate.setDate(startDate);
        timedelayTime = new TimeTextField();
        timedelayTime.setText(TimeTextField.formatter.format(startDate));
        timedelayTime.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GL3DState state = GL3DState.get();
                computeTimedelayTime();
                state.getActiveCamera().setTimeDelay(timeDelay);
                Displayer.getSingletonInstance().display();
            }
        });
        timedelayPanel.add(timedelayDate);
        timedelayPanel.add(timedelayTime);
        add(timedelayPanel);
        hideTimedelayComponents();

    }

    public void computeTimedelayTime() {
        timeDelay = timedelayDate.getDate().getTime();
        timeDelay += timedelayTime.getValue().getTime();
        System.out.println(timeDelay);
    }

    private void showTimedelayComponents() {
        timedelayPanel.setVisible(true);
    }

    private void hideTimedelayComponents() {
        timedelayPanel.setVisible(false);
    }
}
