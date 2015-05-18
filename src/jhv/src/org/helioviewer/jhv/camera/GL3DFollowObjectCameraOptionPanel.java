package org.helioviewer.jhv.camera;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.base.TimeTextField;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarEvent;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarListener;

public class GL3DFollowObjectCameraOptionPanel extends GL3DCameraOptionPanel implements GL3DFollowObjectCameraListener {

    private final JLabel loadedLabel;
    private JLabel beginDateLabel;
    private JPanel beginDatetimePanel;
    JHVCalendarDatePicker beginDatePicker;
    TimeTextField beginTimePicker;

    private JLabel endDateLabel;
    private JPanel endDatetimePanel;
    JHVCalendarDatePicker endDatePicker;
    TimeTextField endTimePicker;
    JSeparatorComboBox objectCombobox;
    private final GL3DFollowObjectCamera camera;
    private JPanel addBeginDatePanel;
    private JPanel addEndDatePanel;
    private JButton synchronizeWithLayersButton;
    private JButton synchronizeWithNowButton;
    private JButton synchronizeWithCurrentButton;

    private JPanel buttonPanel;

    private final JCheckBox exactDateCheckBox;

    protected boolean firstComboChanged = false;

    protected boolean fovVisible = true;

    public GL3DFollowObjectCameraOptionPanel(final GL3DFollowObjectCamera camera) {
        super();
        this.camera = camera;
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(new JSeparator(SwingConstants.HORIZONTAL), c);

        JPanel loadedLabelPanel = new JPanel();
        loadedLabelPanel.setLayout(new BoxLayout(loadedLabelPanel, BoxLayout.LINE_AXIS));

        loadedLabel = new JLabel("Status: Not loaded");
        loadedLabelPanel.add(loadedLabel);
        c.gridy = 1;
        add(loadedLabelPanel, c);
        c.gridy = 2;
        add(new JSeparator(SwingConstants.HORIZONTAL), c);
        c.gridy = 3;

        addObjectCombobox(c);
        exactDateCheckBox = new JCheckBox("Use active layer timestamps", true);
        c.gridy = 4;
        add(exactDateCheckBox, c);
        c.gridy = 5;
        addBeginDatePanel(c);
        c.gridy = 6;
        addEndDatePanel(c);
        addBeginDatePanel.setVisible(false);
        addEndDatePanel.setVisible(false);
        c.gridy = 7;

        addSyncButtons(c);
        buttonPanel.setVisible(false);
        camera.setInterpolation(false);
        exactDateCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                boolean selected = !exactDateCheckBox.isSelected();
                addBeginDatePanel.setVisible(selected);
                addEndDatePanel.setVisible(selected);
                buttonPanel.setVisible(selected);
                camera.setInterpolation(selected);
            }
        });

        this.camera.addFollowObjectCameraListener(this);
        this.syncWithLayerBeginTime(false);
        this.syncWithLayerEndTime(true);
    }

    public void addSyncButtons(GridBagConstraints c) {
        this.synchronizeWithLayersButton = new JButton("Sync");
        this.synchronizeWithLayersButton.setToolTipText("Fill selected layer dates");
        this.synchronizeWithLayersButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                syncWithLayer();
            }
        });
        this.synchronizeWithNowButton = new JButton("Now");
        this.synchronizeWithNowButton.setToolTipText("Fill twice current time");
        this.synchronizeWithNowButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                syncBothLayerNow();
            }
        });
        this.synchronizeWithCurrentButton = new JButton("Current");
        this.synchronizeWithCurrentButton.setToolTipText("Fill twice selected layer time");
        this.synchronizeWithCurrentButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                syncWithLayerCurrentTime();
                Displayer.display();
            }
        });
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(0, 3));
        this.synchronizeWithLayersButton.getMaximumSize().width = 15;
        buttonPanel.add(this.synchronizeWithLayersButton);

        this.synchronizeWithCurrentButton.getMaximumSize().width = 15;
        buttonPanel.add(this.synchronizeWithCurrentButton);

        this.synchronizeWithNowButton.getMaximumSize().width = 15;
        buttonPanel.add(this.synchronizeWithNowButton);

        add(buttonPanel, c);
    }

    @Override
    public void deactivate() {
        this.camera.removeFollowObjectCameraListener(this);
    }

    private void addObjectCombobox(GridBagConstraints c) {
        objectCombobox = new JSeparatorComboBox();
        GL3DSpaceObject[] objectList = GL3DSpaceObject.getObjectList();
        for (int i = 0; i < objectList.length; i++) {
            objectCombobox.addItem(objectList[i]);
            if (i == GL3DSpaceObject.LINESEPSATS || i == GL3DSpaceObject.LINESEPPLANETS) {
                objectCombobox.addItem(new JSeparator());
            }
        }
        objectCombobox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED && firstComboChanged) {
                    GL3DSpaceObject object = (GL3DSpaceObject) event.getItem();
                    if (object != null) {
                        camera.setObservingObject(object.getUrlName(), true);
                        revalidate();
                    }
                }
                if (event.getStateChange() == ItemEvent.SELECTED && !firstComboChanged) {
                    firstComboChanged = true;
                }
            }
        });
        add(objectCombobox, c);
        objectCombobox.setSelectedItem(GL3DSpaceObject.earth);
    }

    private void addBeginDatePanel(GridBagConstraints c) {
        beginDateLabel = new JLabel("Begin");
        beginDatePicker = new JHVCalendarDatePicker();
        beginDatePicker.getTextField().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent arg0) {
            }

            @Override
            public void focusLost(FocusEvent arg0) {
                beginDatePicker.checkDateStringInTextField();
                setBeginTime(true);
                Displayer.display();
            }
        });
        beginTimePicker = new TimeTextField();
        addBeginDatePanel = new JPanel();
        addBeginDatePanel.setLayout(new BoxLayout(addBeginDatePanel, BoxLayout.LINE_AXIS));
        beginDateLabel.setPreferredSize(new Dimension(40, 0));

        addBeginDatePanel.add(beginDateLabel);
        beginDatetimePanel = new JPanel();
        beginDatetimePanel.setLayout(new GridLayout(0, 2));
        beginDatePicker.addJHVCalendarListener(new JHVCalendarListener() {
            @Override
            public void actionPerformed(JHVCalendarEvent e) {
                setBeginTime(true);
                Displayer.display();
            }
        });
        beginTimePicker.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setBeginTime(true);
                Displayer.display();
            }
        });
        addBeginDatePanel.add(beginDatePicker);
        addBeginDatePanel.add(beginTimePicker);
        addBeginDatePanel.add(Box.createRigidArea(new Dimension(40, 0)));
        add(addBeginDatePanel, c);
    }

    private void setEndTime(boolean applyChanges) {
        Date dt = endTimePicker.getValue();
        Date end_date = new Date(endDatePicker.getDate().getTime() + dt.getTime());
        camera.setEndDate(end_date, applyChanges);
    }

    private void setBeginTime(boolean applyChanges) {
        Date dt = beginTimePicker.getValue();
        Date begin_date = new Date(beginDatePicker.getDate().getTime() + dt.getTime());
        camera.setBeginDate(begin_date, applyChanges);
    }

    private void syncWithLayer() {
        syncWithLayerBeginTime(false);
        syncWithLayerEndTime(true);
    }

    private void syncWithLayerBeginTime(boolean applyChanges) {
        Date startDate = null;
        startDate = Displayer.getLayersModel().getFirstDate();
        if (startDate == null) {
            startDate = new Date(System.currentTimeMillis());
        }
        beginDatePicker.setDate(new Date(startDate.getTime() - startDate.getTime() % (60 * 60 * 24 * 1000)));
        beginTimePicker.setText(TimeTextField.formatter.format(startDate));
        setBeginTime(applyChanges);
    }

    private void syncBothLayerNow() {
        Date nowDate = new Date(System.currentTimeMillis());
        beginDatePicker.setDate(new Date(nowDate.getTime() - nowDate.getTime() % (60 * 60 * 24 * 1000)));
        beginTimePicker.setText(TimeTextField.formatter.format(nowDate));
        endDatePicker.setDate(new Date(nowDate.getTime() - nowDate.getTime() % (60 * 60 * 24 * 1000)));
        endTimePicker.setText(TimeTextField.formatter.format(nowDate));
        setBeginTime(false);
        setEndTime(true);
    }

    private void syncWithLayerCurrentTime() {
        Date lastDate = Displayer.getLastUpdatedTimestamp();
        Date currentDate = null;
        if (lastDate != null) {
            currentDate = lastDate;
        }
        if (currentDate == null) {
            currentDate = new Date(System.currentTimeMillis());
        }
        endDatePicker.setDate(new Date(currentDate.getTime() - currentDate.getTime() % (60 * 60 * 24 * 1000)));
        endTimePicker.setText(TimeTextField.formatter.format(currentDate));
        beginDatePicker.setDate(new Date(currentDate.getTime() - currentDate.getTime() % (60 * 60 * 24 * 1000)));
        beginTimePicker.setText(TimeTextField.formatter.format(currentDate));
        setBeginTime(false);
        setEndTime(true);
    }

    private void syncWithLayerEndTime(boolean applyChanges) {
        Date endDate = null;
        endDate = Displayer.getLayersModel().getLastDate();
        if (endDate == null) {
            endDate = new Date(System.currentTimeMillis());
        }
        endDatePicker.setDate(new Date(endDate.getTime() - endDate.getTime() % (60 * 60 * 24 * 1000)));
        endTimePicker.setText(TimeTextField.formatter.format(endDate));
        setEndTime(applyChanges);
    }

    private void addEndDatePanel(GridBagConstraints c) {
        endDateLabel = new JLabel("End");
        endDatePicker = new JHVCalendarDatePicker();
        endDatePicker.getTextField().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent arg0) {
            }

            @Override
            public void focusLost(FocusEvent arg0) {
                beginDatePicker.checkDateStringInTextField();
                setEndTime(true);
                Displayer.display();
            }
        });
        endTimePicker = new TimeTextField();
        addEndDatePanel = new JPanel();
        addEndDatePanel.setLayout(new BoxLayout(addEndDatePanel, BoxLayout.LINE_AXIS));
        endDateLabel.setPreferredSize(new Dimension(40, 0));
        addEndDatePanel.add(endDateLabel);
        endDatetimePanel = new JPanel();
        endDatetimePanel.setLayout(new GridLayout(0, 2));
        endDatePicker.addJHVCalendarListener(new JHVCalendarListener() {
            @Override
            public void actionPerformed(JHVCalendarEvent e) {
                setEndTime(true);
                Displayer.display();
            }
        });
        endTimePicker.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setEndTime(true);
                Displayer.display();
            }
        });
        addEndDatePanel.add(endDatePicker);
        addEndDatePanel.add(endTimePicker);
        addEndDatePanel.add(Box.createRigidArea(new Dimension(40, 0)));

        add(addEndDatePanel, c);
    }

    @Override
    public void fireLoaded(String state) {
        String htmlstart = "<html><body style='width: 200px'>";
        String htmlend = "</body></html>";
        this.loadedLabel.setText(htmlstart + "Status: " + state + htmlend);
    }

}
