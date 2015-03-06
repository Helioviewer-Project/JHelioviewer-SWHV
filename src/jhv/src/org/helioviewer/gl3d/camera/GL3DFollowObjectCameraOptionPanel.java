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
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.basegui.components.TimeTextField;
import org.helioviewer.basegui.components.WheelSupport;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarEvent;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

public class GL3DFollowObjectCameraOptionPanel extends GL3DCameraOptionPanel implements GL3DFollowObjectCameraListener {

    private static final long serialVersionUID = 1L;

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
    private final JLabel cameraTime;
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private final String DISABLED_TEXT = "----";
    private JPanel addBeginDatePanel;
    private JPanel addEndDatePanel;
    private JButton synchronizeWithLayersButton;
    private JButton synchronizeWithNowButton;
    private JButton synchronizeWithCurrentButton;
    private JPanel fovPanel;
    private JSpinner fovSpinner;

    private JPanel buttonPanel;

    private final JCheckBox exactDateCheckBox;

    protected boolean firstComboChanged = false;

    private JButton visibleFovButton;

    protected boolean fovVisible = true;

    public GL3DFollowObjectCameraOptionPanel(final GL3DFollowObjectCamera camera) {
        super(camera);
        this.camera = camera;
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        this.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        this.createGridOptions();
        add(new JSeparator(SwingConstants.HORIZONTAL));
        this.createFOV();

        add(new JSeparator(SwingConstants.HORIZONTAL));
        JPanel cameraTimePanel = new JPanel();
        cameraTimePanel.setLayout(new BoxLayout(cameraTimePanel, BoxLayout.LINE_AXIS));
        cameraTime = new JLabel("Camera time: " + DISABLED_TEXT);
        cameraTimePanel.add(cameraTime);
        cameraTimePanel.add(Box.createHorizontalGlue());
        add(cameraTimePanel);
        JPanel loadedLabelPanel = new JPanel();
        loadedLabelPanel.setMaximumSize(new Dimension(338, 40));

        loadedLabelPanel.setLayout(new BoxLayout(loadedLabelPanel, BoxLayout.LINE_AXIS));

        loadedLabel = new JLabel("Status: Not loaded");
        //loadedLabel.setEditable(false);
        //loadedLabel.setLineWrap(true);
        loadedLabel.setOpaque(false);
        //loadedLabel.setEditable(false);

        loadedLabelPanel.add(loadedLabel);
        loadedLabelPanel.add(Box.createHorizontalGlue());
        add(loadedLabelPanel);

        add(new JSeparator(SwingConstants.HORIZONTAL));
        addObjectCombobox();
        exactDateCheckBox = new JCheckBox("Use active layer timestamps", true);
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
        this.syncWithLayerBeginTime(false);
        this.syncWithLayerEndTime(true);
    }

    private void createFOV() {
        this.fovPanel = new JPanel();
        this.fovPanel.setLayout(new BoxLayout(fovPanel, BoxLayout.LINE_AXIS));
        this.fovPanel.add(new JLabel("FOV angle"));
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
        this.fovPanel.add(new JLabel("degree"));

        this.fovSpinner.setMaximumSize(new Dimension(6, 22));
        this.fovPanel.add(Box.createHorizontalGlue());
        visibleFovButton = new JButton(IconBank.getIcon(JHVIcon.VISIBLE));
        visibleFovButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (fovVisible) {
                    camera.cameraFOVDraw.getDrawBits().on(Bit.Hidden);
                    visibleFovButton.setIcon(IconBank.getIcon(JHVIcon.HIDDEN));
                } else {
                    camera.cameraFOVDraw.getDrawBits().off(Bit.Hidden);
                    visibleFovButton.setIcon(IconBank.getIcon(JHVIcon.VISIBLE));
                }
                fovVisible = !fovVisible;
                Displayer.getSingletonInstance().display();
            }
        });
        visibleFovButton.setToolTipText("Toggle visibility");
        this.fovPanel.add(visibleFovButton);

        add(this.fovPanel);
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
        syncWithLayerBeginTime(false);
        syncWithLayerEndTime(true);
    }

    private void syncWithLayerBeginTime(boolean applyChanges) {
        Date startDate = null;
        startDate = LayersModel.getSingletonInstance().getFirstDate();
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

    private void syncWithLayerEndTime(boolean applyChanges) {
        Date endDate = null;
        endDate = LayersModel.getSingletonInstance().getLastDate();
        if (endDate == null) {
            endDate = new Date(System.currentTimeMillis());
        }
        endDatePicker.setDate(new Date(endDate.getTime() - endDate.getTime() % (60 * 60 * 24 * 1000)));
        endTimePicker.setText(TimeTextField.formatter.format(endDate));
        setEndTime(applyChanges);
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
        String htmlstart = "<html><body style='width: 200px'>";
        String htmlend = "</body></html>";

        this.loadedLabel.setText(htmlstart + "Status: " + state + htmlend);
    }

    @Override
    public void fireCameraTime(Date cameraDate) {
        this.cameraTime.setText("Camera time: " + format.format(cameraDate));
    }

    @Override
    public void fireNewDate(Date date) {
        this.cameraTime.setText(this.format.format(date));
    }

}
