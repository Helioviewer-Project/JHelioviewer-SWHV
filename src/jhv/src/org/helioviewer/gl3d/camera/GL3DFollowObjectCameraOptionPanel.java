package org.helioviewer.gl3d.camera;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.basegui.components.TimeTextField;
import org.helioviewer.basegui.components.WheelSupport;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarEvent;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

public class GL3DFollowObjectCameraOptionPanel extends GL3DCameraOptionPanel implements GL3DFollowObjectCameraListener {
    private static final long serialVersionUID = 1L;

    private final JTextArea loadedLabel;
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
    private final JLabel cameraTime;
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private final String DISABLED_TEXT = "----";
    private JPanel addBeginDatePanel;
    private JPanel addEndDatePanel;
    private JButton synchronizeWithLayersButton;
    private JButton synchronizeWithNowButton;
    private JButton synchronizeWithCurrentButton;
    private final JPanel fovPanel;
    private final JSpinner fovSpinner;

    private JPanel buttonPanel;

    public GL3DFollowObjectCameraOptionPanel(final GL3DFollowObjectCamera camera) {
        super(camera);
        this.camera = camera;
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        this.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        add(new JSeparator(SwingConstants.HORIZONTAL));
        JPanel cameraTimePanel = new JPanel();
        cameraTimePanel.setLayout(new BoxLayout(cameraTimePanel, BoxLayout.LINE_AXIS));
        cameraTime = new JLabel("Camera date: " + DISABLED_TEXT);
        cameraTimePanel.add(cameraTime);
        cameraTimePanel.add(Box.createHorizontalGlue());
        add(cameraTimePanel);
        JPanel loadedLabelPanel = new JPanel();
        loadedLabelPanel.setMaximumSize(new Dimension(338, 40));

        loadedLabelPanel.setLayout(new BoxLayout(loadedLabelPanel, BoxLayout.LINE_AXIS));
        loadedLabel = new JTextArea("Status: Not loaded");
        loadedLabel.setEditable(false);
        loadedLabel.setLineWrap(true);
        loadedLabel.setOpaque(false);
        loadedLabelPanel.add(loadedLabel);
        loadedLabelPanel.add(Box.createHorizontalGlue());
        add(loadedLabelPanel);

        add(new JSeparator(SwingConstants.HORIZONTAL));
        this.createGridOptions();
        add(new JSeparator(SwingConstants.HORIZONTAL));
        this.fovPanel = new JPanel();
        this.fovPanel.setLayout(new BoxLayout(fovPanel, BoxLayout.LINE_AXIS));
        this.fovPanel.add(new JLabel("FOV angle (degree) "));
        this.fovSpinner = new JSpinner();
        this.fovSpinner.setModel(new SpinnerNumberModel(new Double(0.8), new Double(0.0), new Double(180.), new Double(0.01)));
        camera.setFOVangleDegrees((Double) fovSpinner.getValue());

        this.fovSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                camera.setFOVangleDegrees((Double) fovSpinner.getValue());
                Displayer.getSingletonInstance().render();
            }
        });
        WheelSupport.installMouseWheelSupport(this.fovSpinner);
        this.fovPanel.add(this.fovSpinner);
        this.fovSpinner.setMaximumSize(new Dimension(6, 22));
        this.fovPanel.add(Box.createHorizontalGlue());
        this.fovPanel.add(new JSeparator(SwingConstants.VERTICAL));
        this.fovPanel.add(Box.createHorizontalGlue());
        JCheckBox fovCheckbox = new JCheckBox("Visible");
        fovCheckbox.setSelected(true);
        fovCheckbox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.DESELECTED) {
                    camera.cameraFOV.getDrawBits().on(Bit.Hidden);
                } else {
                    camera.cameraFOV.getDrawBits().off(Bit.Hidden);
                }
                Displayer.getSingletonInstance().display();
            }
        });
        this.fovPanel.add(fovCheckbox);

        add(this.fovPanel);
        add(new JSeparator(SwingConstants.HORIZONTAL));
        addObjectCombobox();
        final JCheckBox exactDateCheckBox = new JCheckBox("Use active layer timestamps", true);
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.add(exactDateCheckBox);
        add(checkboxPanel);
        addBeginDatePanel();
        addEndDatePanel();
        addBeginDatePanel.setVisible(false);
        addEndDatePanel.setVisible(false);
        addSyncButtons();
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
        this.syncWithLayerBeginTime();
        this.syncWithLayerEndTime();
    }

    public void addSyncButtons() {
        this.synchronizeWithLayersButton = new JButton("Sync");
        this.synchronizeWithLayersButton.setToolTipText("Fill the dates based on the current active layer.");
        this.synchronizeWithLayersButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                syncWithLayer();
            }
        });
        this.synchronizeWithNowButton = new JButton("Now");
        this.synchronizeWithNowButton.setToolTipText("Fill twice now.");
        this.synchronizeWithNowButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                syncBothLayerNow();
            }
        });
        this.synchronizeWithCurrentButton = new JButton("Current");
        this.synchronizeWithCurrentButton.setToolTipText("Fill twice current layer time.");
        this.synchronizeWithCurrentButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                syncWithLayerCurrentTime();
                Displayer.getSingletonInstance().display();
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

        add(buttonPanel);
    }

    @Override
    public void deactivate() {
        this.camera.removeFollowObjectCameraListener(this);
        cameraTime.setText(DISABLED_TEXT);
    }

    private void addObjectCombobox() {
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
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    GL3DSpaceObject object = (GL3DSpaceObject) event.getItem();
                    if (object != null) {
                        camera.setObservingObject(object.getUrlName(), true);
                        revalidate();
                    }
                }
            }
        });
        add(objectCombobox);
        objectCombobox.setSelectedItem(GL3DSpaceObject.earth);
    }

    private void addBeginDatePanel() {
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
                Displayer.getSingletonInstance().render();
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
                Displayer.getSingletonInstance().render();
            }
        });
        syncWithLayerBeginTime();
        beginTimePicker.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setBeginTime(true);
                Displayer.getSingletonInstance().render();
            }
        });
        addBeginDatePanel.add(beginDatePicker);
        addBeginDatePanel.add(beginTimePicker);
        addBeginDatePanel.add(Box.createRigidArea(new Dimension(40, 0)));
        add(addBeginDatePanel);
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
        setBeginTime(true);
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
        ImmutableDateTime helpDate = null;
        helpDate = LayersModel.getSingletonInstance().getCurrentFrameTimestamp(LayersModel.getSingletonInstance().getActiveLayer());
        Date currentDate = null;
        if (helpDate != null) {
            currentDate = helpDate.getTime();
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

    private void syncWithLayerEndTime() {
        Date endDate = null;
        endDate = LayersModel.getSingletonInstance().getLastDate();
        if (endDate == null) {
            endDate = new Date(System.currentTimeMillis());
        }
        endDatePicker.setDate(new Date(endDate.getTime() - endDate.getTime() % (60 * 60 * 24 * 1000)));
        endTimePicker.setText(TimeTextField.formatter.format(endDate));
        setEndTime(true);
    }

    private void addEndDatePanel() {
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
                Displayer.getSingletonInstance().render();
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
                Displayer.getSingletonInstance().render();
            }
        });
        syncWithLayerEndTime();
        endTimePicker.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setEndTime(true);
                Displayer.getSingletonInstance().render();
            }
        });
        addEndDatePanel.add(endDatePicker);
        addEndDatePanel.add(endTimePicker);
        addEndDatePanel.add(Box.createRigidArea(new Dimension(40, 0)));

        add(addEndDatePanel);
    }

    @Override
    public void fireLoaded(String state) {
        this.loadedLabel.setText("Status: " + state);
    }

    @Override
    public void fireCameraTime(Date cameraDate) {
        this.cameraTime.setText("Camera date: " + format.format(cameraDate));
    }

    @Override
    public void fireNewDate(Date date) {
        this.cameraTime.setText(this.format.format(date));
    }
}
