package org.helioviewer.gl3d.camera;

import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.basegui.components.TimeTextField;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarEvent;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarListener;

public class GL3DFixedTimeCameraOptionPanel extends GL3DCameraOptionPanel {

    private static final long serialVersionUID = 1L;
    private JLabel timedelayLabel;
    private JPanel timedelayPanel;
    private JHVCalendarDatePicker timedelayDate;
    private TimeTextField timedelayTime;
    private long timeDelay;

    public GL3DFixedTimeCameraOptionPanel(GL3DFixedTimeCamera camera) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                addTimedelayPanel();
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

    @Override
    public void deactivate() {
        // TODO Auto-generated method stub

    }

}
