package org.helioviewer.gl3d.camera;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.basegui.components.TimeTextField;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarEvent;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarListener;
import org.helioviewer.jhv.layers.LayersModel;

public class GL3DFollowObjectCameraOptionPanel extends GL3DCameraOptionPanel implements GL3DFollowObjectCameraListener {
    private final JLabel loadedLabel;
    private final JLabel beginDateLabel;
    private JPanel beginDatetimePanel;
    JHVCalendarDatePicker beginDatePicker;
    TimeTextField beginTimePicker;

    private final JLabel endDateLabel;
    private JPanel endDatetimePanel;
    JHVCalendarDatePicker endDatePicker;
    TimeTextField endTimePicker;
    JComboBox objectCombobox;
    private final GL3DFollowObjectCamera camera;
    private final JLabel cameraTime;
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private final String DISABLED_TEXT = "----";
    private final JButton synchronizeWithLayersButton;

    public GL3DFollowObjectCameraOptionPanel(GL3DFollowObjectCamera camera) {
        this.camera = camera;
        setLayout(new GridLayout(0, 1));
        cameraTime = new JLabel(DISABLED_TEXT);
        add(this.cameraTime);
        this.loadedLabel = new JLabel("Not loaded");
        add(this.loadedLabel);
        this.synchronizeWithLayersButton = new JButton("Synchronize");
        this.synchronizeWithLayersButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                syncWithLayer();
            }
        });
        add(this.synchronizeWithLayersButton);
        addObjectCombobox();
        beginDateLabel = new JLabel("Begin date");
        beginDatePicker = new JHVCalendarDatePicker();
        beginTimePicker = new TimeTextField();
        addBeginDatePanel();
        endDateLabel = new JLabel("End date");
        endDatePicker = new JHVCalendarDatePicker();
        endTimePicker = new TimeTextField();
        addEndDatePanel();
        this.syncWithLayerBeginTime();
        this.syncWithLayerEndTime();
        this.camera.addFollowObjectCameraListener(this);
    }

    @Override
    public void deactivate() {
        this.camera.removeFollowObjectCameraListener(this);

        cameraTime.setText(DISABLED_TEXT);
    }

    private void addObjectCombobox() {
        objectCombobox = new JComboBox();
        GL3DSpaceObject[] objectList = GL3DSpaceObject.getObjectList();
        for (int i = 0; i < objectList.length; i++) {
            objectCombobox.addItem(objectList[i]);
        }
        objectCombobox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    GL3DSpaceObject object = (GL3DSpaceObject) event.getItem();
                    if (object != null) {
                        camera.setObservingObject(object.getUrlName());
                        revalidate();
                    }
                }
            }
        });
        add(objectCombobox);
    }

    private void addBeginDatePanel() {
        add(beginDateLabel);
        beginDatetimePanel = new JPanel();
        beginDatetimePanel.setLayout(new GridLayout(0, 2));
        beginDatePicker.addJHVCalendarListener(new JHVCalendarListener() {
            @Override
            public void actionPerformed(JHVCalendarEvent e) {
                setBeginTime();
                Displayer.getSingletonInstance().render();
            }
        });
        syncWithLayerBeginTime();
        beginTimePicker.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setBeginTime();
                Displayer.getSingletonInstance().render();
            }
        });
        beginDatetimePanel.add(beginDatePicker);
        beginDatetimePanel.add(beginTimePicker);
        add(beginDatetimePanel);
    }

    private void setEndTime() {
        Date dt = endTimePicker.getValue();
        Date end_date = new Date(endDatePicker.getDate().getTime() + dt.getTime());
        camera.setEndDate(end_date);
    }

    private void setBeginTime() {
        Date dt = beginTimePicker.getValue();
        Date begin_date = new Date(beginDatePicker.getDate().getTime() + dt.getTime());
        camera.setBeginDate(begin_date);
    }

    private void syncWithLayer() {
        syncWithLayerBeginTime();
        syncWithLayerEndTime();
    }

    private void syncWithLayerBeginTime() {
        Date startDate = null;
        startDate = LayersModel.getSingletonInstance().getFirstDate();
        if (startDate == null) {
            startDate = new Date(System.currentTimeMillis());
        }
        beginDatePicker.setDate(new Date(startDate.getTime() - startDate.getTime() % (60 * 60 * 24 * 1000)));
        beginTimePicker.setText(TimeTextField.formatter.format(startDate));
        setBeginTime();
    }

    private void syncWithLayerEndTime() {
        Date endDate = null;
        endDate = LayersModel.getSingletonInstance().getLastDate();
        if (endDate == null) {
            endDate = new Date(System.currentTimeMillis());
        }
        endDatePicker.setDate(new Date(endDate.getTime() - endDate.getTime() % (60 * 60 * 24 * 1000)));
        endTimePicker.setText(TimeTextField.formatter.format(endDate));
        setEndTime();
    }

    private void addEndDatePanel() {
        add(endDateLabel);
        endDatetimePanel = new JPanel();
        endDatetimePanel.setLayout(new GridLayout(0, 2));
        endDatePicker.addJHVCalendarListener(new JHVCalendarListener() {
            @Override
            public void actionPerformed(JHVCalendarEvent e) {
                setEndTime();
                Displayer.getSingletonInstance().render();
            }
        });
        syncWithLayerEndTime();
        endTimePicker.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setEndTime();
                Displayer.getSingletonInstance().render();
            }
        });
        endDatetimePanel.add(endDatePicker);
        endDatetimePanel.add(endTimePicker);
        add(endDatetimePanel);
    }

    @Override
    public void fireLoaded(String state) {
        this.loadedLabel.setText(state);
    }

    @Override
    public void fireCameraTime(Date cameraDate) {
        this.cameraTime.setText(format.format(cameraDate));
    }

    @Override
    public void fireNewDate(Date date) {
        this.cameraTime.setText(this.format.format(date));
    }
}
