package org.helioviewer.gl3d.camera;

import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Date;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.basegui.components.TimeTextField;
import org.helioviewer.gl3d.gui.GL3DCameraSelectorModel;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarEvent;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarListener;

public class GL3DCameraOptionsPanel extends JPanel {
    private static final long serialVersionUID = 3942154069677445408L;
    private long timeDelay;
    private JPanel timedelayPanel;
    private JLabel timedelayLabel;
    private JHVCalendarDatePicker timedelayDate;
    private TimeTextField timedelayTime;
    private JComboBox cameraComboBox;
    private GL3DCamera currentCamera;
    private GL3DCameraSelectorModel cameraSelectorModel;

    public GL3DCameraOptionsPanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                addCameraComboBox();
            }
        });
    }

    private void addCameraComboBox() {
        cameraSelectorModel = GL3DCameraSelectorModel.getInstance();
        this.cameraComboBox = new JComboBox(cameraSelectorModel);
        add(this.cameraComboBox);
        this.cameraComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    GL3DCamera selectedCamera = (GL3DCamera) event.getItem();
                    if (selectedCamera != null) {
                        cameraSelectorModel.getCurrentCamera().deactivate();
                        cameraSelectorModel.setCurrentCamera(selectedCamera);
                        cameraSelectorModel.getCurrentCamera().activate();
                    }
                }
            }
        });
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
    }

    public void computeTimedelayTime() {
        timeDelay = timedelayDate.getDate().getTime();
        timeDelay += timedelayTime.getValue().getTime();
    }

}
