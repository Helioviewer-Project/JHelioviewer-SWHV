package org.helioviewer.jhv.camera;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.DateTimePanel;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.view.View;

@SuppressWarnings("serial")
public class CameraOptionPanelExpert extends CameraOptionPanel implements LayersListener, PositionLoadFire {

    private final JLabel loadedLabel = new JLabel("Status: Not loaded");
    private final JCheckBox exactDateCheckBox = new JCheckBox("Use master layer timestamps", true);
    private final DateTimePanel startDateTimePanel = new DateTimePanel("Start");
    private final DateTimePanel endDateTimePanel = new DateTimePanel("End");
    private final PositionLoad positionLoad;

    CameraOptionPanelExpert(String frame, UpdateViewpoint uv) {
        positionLoad = new PositionLoad(frame);
        uv.setPositionLoad(positionLoad);

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(new JSeparator(SwingConstants.HORIZONTAL), c);

        JPanel loadedLabelPanel = new JPanel();
        loadedLabelPanel.setLayout(new BoxLayout(loadedLabelPanel, BoxLayout.LINE_AXIS));
        loadedLabelPanel.add(loadedLabel);
        c.gridy = 1;
        add(loadedLabelPanel, c);
        c.gridy = 2;
        add(new JSeparator(SwingConstants.HORIZONTAL), c);
        c.gridy = 3;
        addObjectList(c);
        c.gridy = 4;
        add(exactDateCheckBox, c);
        c.gridy = 5;
        startDateTimePanel.add(Box.createRigidArea(new Dimension(40, 0)));
        add(startDateTimePanel, c);
        c.gridy = 6;
        endDateTimePanel.add(Box.createRigidArea(new Dimension(40, 0)));
        add(endDateTimePanel, c);
        c.gridy = 7;
        JPanel buttonPanel = syncButtons();
        add(buttonPanel, c);

        startDateTimePanel.addListener(e -> request());
        endDateTimePanel.addListener(e -> request());

        startDateTimePanel.setVisible(false);
        endDateTimePanel.setVisible(false);
        buttonPanel.setVisible(false);
        exactDateCheckBox.addActionListener(e -> {
            boolean selected = !exactDateCheckBox.isSelected();
            startDateTimePanel.setVisible(selected);
            endDateTimePanel.setVisible(selected);
            buttonPanel.setVisible(selected);
            if (selected)
                request();
        });

        ComponentUtils.smallVariant(this);
    }

    private JPanel syncButtons() {
        JButton synchronizeWithLayersButton = new JButton("Sync");
        synchronizeWithLayersButton.setToolTipText("Fill selected layer dates");
        synchronizeWithLayersButton.addActionListener(e -> syncWithLayer());

        JButton synchronizeWithNowButton = new JButton("Now");
        synchronizeWithNowButton.setToolTipText("Fill twice current time");
        synchronizeWithNowButton.addActionListener(e -> syncBothLayerNow());

        JButton synchronizeWithCurrentButton = new JButton("Current");
        synchronizeWithCurrentButton.setToolTipText("Fill twice selected layer time");
        synchronizeWithCurrentButton.addActionListener(e -> syncWithLayerCurrentTime());

        JPanel panel = new JPanel(new GridLayout(0, 3));
        panel.add(synchronizeWithLayersButton);
        panel.add(synchronizeWithCurrentButton);
        panel.add(synchronizeWithNowButton);
        return panel;
    }

    @Override
    void activate() {
        Layers.addLayersListener(this);
    }

    @Override
    void deactivate() {
        Layers.removeLayersListener(this);
    }

    @Override
    public void activeLayerChanged(View view) {
        if (exactDateCheckBox.isSelected())
            syncWithLayer();
    }

    private void addObjectList(GridBagConstraints c) {
        JList<SpaceObject> objectList = new JList<>(SpaceObject.getObjectArray());
        objectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        objectList.setSelectedValue(SpaceObject.Earth, true);
        objectList.setCellRenderer(new SpaceObject.CellRenderer());
        objectList.addListSelectionListener(e -> {
            for (SpaceObject object : objectList.getSelectedValuesList()) {
                target = object.getUrlName();
                request();
                break;
            }
        });

        JScrollPane jsp = new JScrollPane(objectList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jsp.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        jsp.getViewport().setBackground(objectList.getBackground());
        add(jsp, c);
    }

    @Override
    void syncWithLayer() {
        View view = Layers.getActiveView();
        if (view == null)
            return;

        startDateTimePanel.setTime(view.getFirstTime().milli);
        endDateTimePanel.setTime(view.getLastTime().milli);
        request();
    }

    private void syncBothLayerNow() {
        long now = System.currentTimeMillis();
        startDateTimePanel.setTime(now);
        endDateTimePanel.setTime(now);
        request();
    }

    private void syncWithLayerCurrentTime() {
        long now = Layers.getLastUpdatedTimestamp().milli;
        startDateTimePanel.setTime(now);
        endDateTimePanel.setTime(now);
        request();
    }

    @Override
    public void fireLoaded(String state) {
        loadedLabel.setText("<html><body style='width: 200px'>Status: " + state);
        Displayer.getCamera().refresh();
    }

    private String target = "Earth";

    private void request() {
        positionLoad.request(this, target, startDateTimePanel.getTime(), endDateTimePanel.getTime());
    }

}
