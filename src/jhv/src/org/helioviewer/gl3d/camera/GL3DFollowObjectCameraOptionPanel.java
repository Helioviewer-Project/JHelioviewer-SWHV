package org.helioviewer.gl3d.camera;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.basegui.components.TimeTextField;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarEvent;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarListener;

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
        addDatePanel(beginDateLabel, beginDatetimePanel, beginDatePicker, beginTimePicker);
        endDateLabel = new JLabel("End date");
        addDatePanel(endDateLabel, endDatetimePanel, endDatePicker, endTimePicker);
    }

    private void addObjectCombobox() {
        objectCombobox = new JComboBox();
        objectCombobox.addItem("Solar Orbiter");
        objectCombobox.addItem("Venus");
        add(objectCombobox);
    }

    private void addDatePanel(JLabel dateLabel, JPanel datetimePanel, JHVCalendarDatePicker datePicker, TimeTextField timePicker) {
        add(dateLabel);
        datetimePanel = new JPanel();
        datetimePanel.setLayout(new GridLayout(0, 2));
        datePicker = new JHVCalendarDatePicker();
        datePicker.addJHVCalendarListener(new JHVCalendarListener() {
            @Override
            public void actionPerformed(JHVCalendarEvent e) {
                Displayer.getSingletonInstance().display();
            }
        });
        Date startDate = new Date(System.currentTimeMillis());
        datePicker.setDate(startDate);
        timePicker = new TimeTextField();
        timePicker.setText(TimeTextField.formatter.format(startDate));
        timePicker.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Displayer.getSingletonInstance().display();
            }
        });
        datetimePanel.add(datePicker);
        datetimePanel.add(timePicker);
        add(datetimePanel);
    }
}
