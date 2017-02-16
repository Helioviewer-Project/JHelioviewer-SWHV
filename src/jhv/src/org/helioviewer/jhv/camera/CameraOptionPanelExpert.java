package org.helioviewer.jhv.camera;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.DateTimePanel;
import org.helioviewer.jhv.gui.components.base.JSeparatorComboBox;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.viewmodel.view.View;

@SuppressWarnings("serial")
public class CameraOptionPanelExpert extends CameraOptionPanel implements LayersListener {

    private final PositionLoad positionLoad;

    private final JLabel loadedLabel = new JLabel("Status: Not loaded");
    private final JCheckBox exactDateCheckBox = new JCheckBox("Use master layer timestamps", true);
    private final DateTimePanel startDateTimePanel = new DateTimePanel("Start");
    private final DateTimePanel endDateTimePanel = new DateTimePanel("End");

    private JPanel buttonPanel;

    CameraOptionPanelExpert(PositionLoad _positionLoad) {
        positionLoad = _positionLoad;

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
        loadedLabelPanel.add(loadedLabel);
        c.gridy = 1;
        add(loadedLabelPanel, c);
        c.gridy = 2;
        add(new JSeparator(SwingConstants.HORIZONTAL), c);
        c.gridy = 3;
        addObjectCombobox(c);
        c.gridy = 4;
        add(exactDateCheckBox, c);
        c.gridy = 5;
        add(startDateTimePanel, c);
        c.gridy = 6;
        add(endDateTimePanel, c);
        c.gridy = 7;
        addSyncButtons(c);

        startDateTimePanel.addListener(e -> setStartTime(true));
        endDateTimePanel.addListener(e -> setEndTime(true));

        startDateTimePanel.setVisible(false);
        endDateTimePanel.setVisible(false);
        buttonPanel.setVisible(false);
        exactDateCheckBox.addActionListener(e -> {
            boolean selected = !exactDateCheckBox.isSelected();
            startDateTimePanel.setVisible(selected);
            endDateTimePanel.setVisible(selected);
            buttonPanel.setVisible(selected);
            if (selected) {
                setStartTime(false);
                setEndTime(true);
            }
        });

        ComponentUtils.smallVariant(this);
    }

    private void addSyncButtons(GridBagConstraints c) {
        JButton synchronizeWithLayersButton = new JButton("Sync");
        synchronizeWithLayersButton.setToolTipText("Fill selected layer dates");
        synchronizeWithLayersButton.addActionListener(e -> syncWithLayer());

        JButton synchronizeWithNowButton = new JButton("Now");
        synchronizeWithNowButton.setToolTipText("Fill twice current time");
        synchronizeWithNowButton.addActionListener(e -> syncBothLayerNow());

        JButton synchronizeWithCurrentButton = new JButton("Current");
        synchronizeWithCurrentButton.setToolTipText("Fill twice selected layer time");
        synchronizeWithCurrentButton.addActionListener(e -> syncWithLayerCurrentTime());

        buttonPanel = new JPanel(new GridLayout(0, 3));

        buttonPanel.add(synchronizeWithLayersButton);
        buttonPanel.add(synchronizeWithCurrentButton);
        buttonPanel.add(synchronizeWithNowButton);

        add(buttonPanel, c);
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
        if (exactDateCheckBox.isSelected()) {
            syncWithLayer();
        }
    }

    private void addObjectCombobox(GridBagConstraints c) {
        JSeparatorComboBox objectCombobox = new JSeparatorComboBox(SpaceObject.getObjectList().toArray());
        objectCombobox.setSelectedItem(SpaceObject.earth);
        objectCombobox.addActionListener(e -> {
            String object = ((SpaceObject) objectCombobox.getSelectedItem()).getUrlName();
            positionLoad.setObserver(object, true);
            // Displayer.render();
        });
        add(objectCombobox, c);
    }

    private void setStartTime(boolean applyChanges) {
        positionLoad.setBeginTime(startDateTimePanel.getTime(), applyChanges);
    }

    private void setEndTime(boolean applyChanges) {
        positionLoad.setEndTime(endDateTimePanel.getTime(), applyChanges);
    }

    @Override
    void syncWithLayer() {
        View view = Layers.getActiveView();
        if (view == null)
            return;

        startDateTimePanel.setTime(view.getFirstTime().milli);
        endDateTimePanel.setTime(view.getLastTime().milli);
        setStartTime(false);
        setEndTime(true);
    }

    private void syncBothLayerNow() {
        long now = System.currentTimeMillis();

        startDateTimePanel.setTime(now);
        endDateTimePanel.setTime(now);
        setStartTime(false);
        setEndTime(true);
    }

    private void syncWithLayerCurrentTime() {
        long now = Layers.getLastUpdatedTimestamp().milli;

        startDateTimePanel.setTime(now);
        endDateTimePanel.setTime(now);
        setStartTime(false);
        setEndTime(true);
    }

    void fireLoaded(String state) {
        loadedLabel.setText("<html><body style='width: 200px'>Status: " + state);
    }

}
