package org.helioviewer.gl3d.camera;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.ParseException;
import java.util.Date;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.base.logging.Log;
import org.helioviewer.basegui.components.TimeTextField;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarEvent;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarListener;
import org.helioviewer.jhv.layers.LayersModel;

public class GL3DFollowObjectCameraOptionPanel extends GL3DCameraOptionPanel {
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

    public GL3DFollowObjectCameraOptionPanel(GL3DFollowObjectCamera camera) {
        this.camera = camera;
        setLayout(new GridLayout(0, 1));
        addObjectCombobox();
        beginDateLabel = new JLabel("Begin date");
        beginDatePicker = new JHVCalendarDatePicker();
        beginTimePicker = new TimeTextField();
        addBeginDatePanel();
        endDateLabel = new JLabel("End date");
        endDatePicker = new JHVCalendarDatePicker();
        endTimePicker = new TimeTextField();
        addEndDatePanel();

    }

    private void addObjectCombobox() {
        objectCombobox = new JComboBox();
        objectCombobox.addItem("Solar Orbiter");
        objectCombobox.addItem("Venus");
        objectCombobox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    String object = (String) event.getItem();
                    if (object != null) {
                        camera.setObservingObject(object);
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
        Date startDate = null;
        startDate = LayersModel.getSingletonInstance().getFirstDate();
        beginDatePicker.setDate(startDate);

        if (startDate == null) {
            startDate = new Date(System.currentTimeMillis());
        }
        beginTimePicker.setText(TimeTextField.formatter.format(startDate));
        beginTimePicker.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setEndTime();
                Displayer.getSingletonInstance().render();
            }
        });
        beginDatetimePanel.add(beginDatePicker);
        beginDatetimePanel.add(beginTimePicker);
        add(beginDatetimePanel);
    }

    protected void setEndTime() {
        try {
            Date dt = TimeTextField.formatter.parse(beginTimePicker.getText());
            camera.setBeginDate(new Date(beginDatePicker.getDate().getTime() + dt.getTime()));
            System.out.println(new Date(beginDatePicker.getDate().getTime() + dt.getTime()));
        } catch (ParseException e) {
            Log.error("Date parsing failed" + e);
        }
    }

    protected void setBeginTime() {
        try {
            Date dt = TimeTextField.formatter.parse(beginTimePicker.getText());
            camera.setBeginDate(new Date(beginDatePicker.getDate().getTime() + dt.getTime()));
            System.out.println(new Date(beginDatePicker.getDate().getTime() + dt.getTime()));
        } catch (ParseException e) {
            Log.error("Date parsing failed" + e);
        }
    }

    private void addEndDatePanel() {
        add(endDateLabel);
        endDatetimePanel = new JPanel();
        endDatetimePanel.setLayout(new GridLayout(0, 2));
        endDatePicker.addJHVCalendarListener(new JHVCalendarListener() {
            @Override
            public void actionPerformed(JHVCalendarEvent e) {
                try {
                    Date dt = TimeTextField.formatter.parse(endTimePicker.getText());
                    camera.setEndDate(new Date(endDatePicker.getDate().getTime() + dt.getTime()));

                } catch (ParseException e1) {
                    Log.error("Date parsing failed", e1);
                }
                Displayer.getSingletonInstance().render();
            }
        });
        Date startDate = null;
        startDate = LayersModel.getSingletonInstance().getLastDate();
        endDatePicker.setDate(startDate);
        if (startDate == null) {
            startDate = new Date(System.currentTimeMillis());
        }
        endTimePicker.setText(TimeTextField.formatter.format(startDate));
        endTimePicker.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Date dt = TimeTextField.formatter.parse(endTimePicker.getText());
                    camera.setBeginDate(new Date(endDatePicker.getDate().getTime() + dt.getTime()));
                } catch (ParseException e1) {
                    Log.error("Date parsing failed", e1);
                }
                Displayer.getSingletonInstance().render();
            }
        });
        endDatetimePanel.add(endDatePicker);
        endDatetimePanel.add(endTimePicker);
        add(endDatetimePanel);
    }
}
