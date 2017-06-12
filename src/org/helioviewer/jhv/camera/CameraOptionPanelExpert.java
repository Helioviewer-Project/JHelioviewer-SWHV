package org.helioviewer.jhv.camera;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.camera.object.SpaceObjectContainer;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.DateTimePanel;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.view.View;

@SuppressWarnings("serial")
public class CameraOptionPanelExpert extends CameraOptionPanel implements LayersListener {

    private final JCheckBox exactDateCheckBox = new JCheckBox("Use master layer timestamps", true);
    private final DateTimePanel startDateTimePanel = new DateTimePanel("Start");
    private final DateTimePanel endDateTimePanel = new DateTimePanel("End");

    private final String frame;
    private final UpdateViewpoint uv;
    private final SpaceObjectContainer container;

    CameraOptionPanelExpert(String _frame, UpdateViewpoint _uv, boolean exclusive) {
        frame = _frame;
        uv = _uv;

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(new JSeparator(SwingConstants.HORIZONTAL), c);

        c.gridy = 1;
        container = new SpaceObjectContainer(uv, frame, exclusive);
        container.selectObject(SpaceObject.Earth);
        add(container, c);

        c.gridy = 2;
        add(exactDateCheckBox, c);
        c.gridy = 3;
        startDateTimePanel.addListener(e -> request());
        startDateTimePanel.add(Box.createRigidArea(new Dimension(40, 0)));
        add(startDateTimePanel, c);
        c.gridy = 4;
        endDateTimePanel.addListener(e -> request());
        endDateTimePanel.add(Box.createRigidArea(new Dimension(40, 0)));
        add(endDateTimePanel, c);

        c.gridy = 5;
        JButton synchronizeWithLayersButton = new JButton("Sync");
        synchronizeWithLayersButton.setToolTipText("Fill selected layer dates");
        synchronizeWithLayersButton.addActionListener(e -> syncWithLayer());

        JButton synchronizeWithNowButton = new JButton("Now");
        synchronizeWithNowButton.setToolTipText("Fill twice current time");
        synchronizeWithNowButton.addActionListener(e -> syncBothLayerNow());

        JButton synchronizeWithCurrentButton = new JButton("Current");
        synchronizeWithCurrentButton.setToolTipText("Fill twice selected layer time");
        synchronizeWithCurrentButton.addActionListener(e -> syncWithLayerCurrentTime());

        JPanel buttonPanel = new JPanel(new GridLayout(0, 3));
        buttonPanel.add(synchronizeWithLayersButton);
        buttonPanel.add(synchronizeWithCurrentButton);
        buttonPanel.add(synchronizeWithNowButton);
        add(buttonPanel, c);

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

    private void request() {
        container.setStartTime(startDateTimePanel.getTime());
        container.setEndTime(endDateTimePanel.getTime());
    }

}
