package org.helioviewer.gl3d.camera;

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.basegui.components.WheelSupport;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.jhv.display.Displayer;

public class GL3DEarthCameraOptionPanel extends GL3DCameraOptionPanel {

    private final GL3DEarthCamera camera;
    private JPanel gridPanel;
    private JSpinner gridResolutionXSpinner;
    private JSpinner gridResolutionYSpinner;
    private JCheckBox fovCheckbox;

    public GL3DEarthCameraOptionPanel(GL3DEarthCamera camera) {
        this.camera = camera;
        createGridOptions();
    }

    public void createGridOptions() {
        this.gridPanel = new JPanel();
        this.gridPanel.setLayout(new BoxLayout(gridPanel, BoxLayout.LINE_AXIS));
        this.gridPanel.add(new JLabel("Grid "));
        this.createGridResolutionX();
        this.createGridResolutionY();

        this.gridResolutionXSpinner.setMaximumSize(new Dimension(6, 22));
        this.gridPanel.add(this.gridResolutionXSpinner);
        this.gridPanel.add(Box.createHorizontalGlue());

        this.gridResolutionYSpinner.setMaximumSize(new Dimension(6, 22));
        this.gridPanel.add(this.gridResolutionYSpinner);
        this.gridPanel.add(Box.createHorizontalGlue());

        this.gridPanel.add(new JSeparator(SwingConstants.VERTICAL));
        this.gridPanel.add(Box.createHorizontalGlue());

        createVisibleCheckBox();
        this.gridPanel.add(fovCheckbox);

        add(this.gridPanel);
    }

    private void createVisibleCheckBox() {
        fovCheckbox = new JCheckBox("Visible");
        fovCheckbox.setSelected(true);
        fovCheckbox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.DESELECTED) {
                    camera.getGrid().getDrawBits().on(Bit.Hidden);
                } else {
                    camera.getGrid().getDrawBits().off(Bit.Hidden);
                }
                Displayer.getSingletonInstance().display();

            }
        });
    }

    public void createGridResolutionX() {
        this.gridResolutionXSpinner = new JSpinner();
        this.gridResolutionXSpinner.setModel(new SpinnerNumberModel(new Integer(20), new Integer(2), new Integer(250), new Integer(1)));
        camera.setGridResolutionX((Integer) gridResolutionXSpinner.getValue());
        this.gridResolutionXSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                camera.setGridResolutionX((Integer) gridResolutionXSpinner.getValue());
                Displayer.getSingletonInstance().render();
            }
        });
        WheelSupport.installMouseWheelSupport(this.gridResolutionXSpinner);
    }

    public void createGridResolutionY() {
        this.gridResolutionYSpinner = new JSpinner();
        this.gridResolutionYSpinner.setModel(new SpinnerNumberModel(new Integer(20), new Integer(2), new Integer(250), new Integer(1)));
        camera.setGridResolutionY((Integer) gridResolutionYSpinner.getValue());
        this.gridResolutionYSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                camera.setGridResolutionY((Integer) gridResolutionYSpinner.getValue());
                Displayer.getSingletonInstance().render();
            }
        });
        WheelSupport.installMouseWheelSupport(this.gridResolutionYSpinner);
    }

    @Override
    public void deactivate() {

    }

}
