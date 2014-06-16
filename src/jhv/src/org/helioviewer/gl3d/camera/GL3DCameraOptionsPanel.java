package org.helioviewer.gl3d.camera;

import java.awt.EventQueue;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.gl3d.gui.GL3DCameraSelectorModel;

public class GL3DCameraOptionsPanel extends JPanel {
    private static final long serialVersionUID = 3942154069677445408L;
    private JComboBox cameraComboBox;
    private GL3DCameraSelectorModel cameraSelectorModel;
    private GL3DCameraOptionPanel optionsPanel;
    private final GL3DCameraOptionsAttributeManager cameraOptionsAttributeManager = GL3DCameraOptionsAttributeManager.getSingletonInstance();

    public GL3DCameraOptionsPanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                addCameraTabs();
            }
        });
    }

    private void addCameraTabs() {
        final JTabbedPane tab = new JTabbedPane();
        tab.add("Observer", new JPanel());
        tab.add("Earth", new JPanel());
        tab.add("Expert", new JPanel());
        add(tab);
        cameraSelectorModel = GL3DCameraSelectorModel.getInstance();
        this.cameraComboBox = new JComboBox(cameraSelectorModel);
        add(this.cameraComboBox);
        tab.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int index = tab.getSelectedIndex();
                System.out.println("selected" + index);
                if (index == 0) {
                    cameraSelectorModel.getCurrentCamera().deactivate();
                    cameraSelectorModel.setCurrentCamera(cameraSelectorModel.getTrackballCamera());
                    optionsPanel = cameraOptionsAttributeManager.getCameraOptionAttributePanel(cameraSelectorModel.getCurrentCamera());
                }
                if (index == 1) {
                    cameraSelectorModel.getCurrentCamera().deactivate();
                    cameraSelectorModel.setCurrentCamera(cameraSelectorModel.getEarthCamera());
                    optionsPanel = cameraOptionsAttributeManager.getCameraOptionAttributePanel(cameraSelectorModel.getCurrentCamera());
                }
                if (index == 2) {
                    cameraSelectorModel.getCurrentCamera().deactivate();
                    cameraSelectorModel.setCurrentCamera(cameraSelectorModel.getFollowObjectCamera());
                    optionsPanel = cameraOptionsAttributeManager.getCameraOptionAttributePanel(cameraSelectorModel.getCurrentCamera());
                }
                tab.setComponentAt(index, optionsPanel);
            }
        });
    }

    public JPanel createFollowObjectCamera() {
        JPanel panel1 = new JPanel();
        return panel1;
    }
}
