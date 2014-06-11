package org.helioviewer.gl3d.camera;

import java.awt.EventQueue;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.helioviewer.gl3d.gui.GL3DCameraSelectorModel;

public class GL3DCameraOptionsPanel extends JPanel {
    private static final long serialVersionUID = 3942154069677445408L;
    private JComboBox cameraComboBox;
    private GL3DCameraSelectorModel cameraSelectorModel;
    private JPanel optionsPanel;
    private final GL3DCameraOptionsAttributeManager cameraOptionsAttributeManager = GL3DCameraOptionsAttributeManager.getSingletonInstance();

    public GL3DCameraOptionsPanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                addCameraComboBox();
            }
        });
    }

    private void addCameraComboBox() {
        cameraSelectorModel = GL3DCameraSelectorModel.getInstance();
        this.cameraComboBox = new JComboBox(cameraSelectorModel);
        add(this.cameraComboBox);
        this.cameraComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    GL3DCamera selectedCamera = (GL3DCamera) event.getItem();
                    if (selectedCamera != null) {
                        cameraSelectorModel.getCurrentCamera().deactivate();
                        cameraSelectorModel.setCurrentCamera(selectedCamera);
                        cameraSelectorModel.getCurrentCamera().activate();
                        if (optionsPanel != null) {
                            remove(optionsPanel);
                        }
                        optionsPanel = cameraOptionsAttributeManager.getCameraOptionAttributePanel(selectedCamera);
                        add(optionsPanel);
                        revalidate();
                    }
                }
            }
        });
    }
}
