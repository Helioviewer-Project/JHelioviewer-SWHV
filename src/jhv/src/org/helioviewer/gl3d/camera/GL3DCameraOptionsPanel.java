package org.helioviewer.gl3d.camera;

import java.awt.Component;
import java.awt.Container;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.gl3d.gui.GL3DCameraSelectionModelListener;
import org.helioviewer.gl3d.gui.GL3DCameraSelectorModel;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.View;

public class GL3DCameraOptionsPanel extends JPanel implements GL3DCameraSelectionModelListener, LayersListener {

    private static final long serialVersionUID = 3942154069677445408L;
    private GL3DCameraSelectorModel cameraSelectorModel;
    private JPanel optionsPanel;
    private final GL3DCameraOptionsAttributeManager cameraOptionsAttributeManager = GL3DCameraOptionsAttributeManager.getSingletonInstance();
    private JTabbedPane tab;

    public GL3DCameraOptionsPanel() {
        LayersModel.getSingletonInstance().addLayersListener(this);
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setModel();
        addCameraTabs();
    }

    protected void setModel() {
        cameraSelectorModel = GL3DCameraSelectorModel.getInstance();
        cameraSelectorModel.addListener(this);
    }

    private void addCameraTabs() {
        tab = new JTabbedPane();
        tab.add("Observer", new JPanel());
        tab.add("Earth", new JPanel());
        tab.add("Expert", new JPanel());
        tab.add(new JPanel());
        tab.setTabComponentAt(3, new JLabel(IconBank.getIcon(JHVIcon.INFO)));
        add(tab);
        tab.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int index = tab.getSelectedIndex();
                if (index == 0) {
                    changeCamera(cameraSelectorModel.getObserverCamera());
                }
                if (index == 1) {
                    changeCamera(cameraSelectorModel.getEarthCamera());
                }
                if (index == 2) {
                    changeCamera(cameraSelectorModel.getFollowObjectCamera());
                }
                if (index == 3) {
                    optionsPanel = infoPanel();
                }
                tab.setComponentAt(index, optionsPanel);
            }
        });
    }

    private void initCamera(GL3DCamera newCamera) {
        changeCamera(newCamera);
        cameraSelectorModel.setCurrentCamera(newCamera);
        optionsPanel = cameraOptionsAttributeManager.getCameraOptionAttributePanel(newCamera);
        Displayer.display();
        tab.setComponentAt(0, optionsPanel);
        tab.setSelectedIndex(0);
    }

    private void changeCamera(GL3DCamera newCamera) {

        cameraSelectorModel.getCurrentCamera().deactivate();
        boolean trackingMode = cameraSelectorModel.getCurrentCamera().getTrackingMode();
        newCamera.setTrackingMode(trackingMode);
        cameraSelectorModel.setCurrentCamera(newCamera);
        cameraSelectorModel.getCurrentCamera().activate();
        optionsPanel = cameraOptionsAttributeManager.getCameraOptionAttributePanel(cameraSelectorModel.getCurrentCamera());
        Displayer.display();
    }

    private JPanel infoPanel() {
        JPanel infoExplainPanel = new JPanel();
        infoExplainPanel.setLayout(new BoxLayout(infoExplainPanel, BoxLayout.PAGE_AXIS));
        infoExplainPanel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));

        String explanation = "Observer camera: view from Observer.\n";
        explanation += "Camera time defined by the active layer current timestamp.\n\n";
        explanation += "Earth camera: view from Earth.\n";
        explanation += "Camera time defined by the active layer current timestamp.\n\n";
        explanation += "Expert camera: view from selected object.\n";
        explanation += "Camera time defined by the active layer current timestamp, ";
        explanation += "unless \"Use active layer timestamps\" is off. ";
        explanation += "In that case, camera time is interpolated in the time interval set.";

        JTextArea infoText = new JTextArea(explanation);
        infoText.setEditable(false);
        infoText.setLineWrap(true);
        infoText.setWrapStyleWord(true);
        infoText.setOpaque(false);
        infoExplainPanel.add(infoText);
        return infoExplainPanel;
    }

    public JPanel createFollowObjectCamera() {
        return new JPanel();
    }

    @Override
    public void fireInit() {
        this.initCamera(cameraSelectorModel.getObserverCamera());
        if (LayersModel.getSingletonInstance().getNumLayers() > 0) {
            visactivate();
        } else {
            visdeactivate();
        }
    }

    public void enableComponents(Container container, boolean enable) {
        Component[] components = container.getComponents();
        for (Component component : components) {
            component.setEnabled(enable);
            if (component instanceof Container) {
                enableComponents((Container) component, enable);
            }
        }
    }

    void visactivate() {
        GL3DCameraOptionPanel optionsPanel;
        enableComponents(tab, true);
        optionsPanel = cameraOptionsAttributeManager.getCameraOptionAttributePanel(cameraSelectorModel.getObserverCamera());
        enableComponents(optionsPanel, true);
        optionsPanel = cameraOptionsAttributeManager.getCameraOptionAttributePanel(cameraSelectorModel.getEarthCamera());
        enableComponents(optionsPanel, true);
        optionsPanel = cameraOptionsAttributeManager.getCameraOptionAttributePanel(cameraSelectorModel.getFollowObjectCamera());
        enableComponents(optionsPanel, true);
    }

    void visdeactivate() {
        enableComponents(tab, false);
        optionsPanel = cameraOptionsAttributeManager.getCameraOptionAttributePanel(cameraSelectorModel.getObserverCamera());
        enableComponents(optionsPanel, false);
        optionsPanel = cameraOptionsAttributeManager.getCameraOptionAttributePanel(cameraSelectorModel.getEarthCamera());
        enableComponents(optionsPanel, false);
        optionsPanel = cameraOptionsAttributeManager.getCameraOptionAttributePanel(cameraSelectorModel.getFollowObjectCamera());
        enableComponents(optionsPanel, false);
    }

    @Override
    public void layerAdded(int idx) {
        if (LayersModel.getSingletonInstance().getNumLayers() > 0) {
            this.visactivate();
        }
    }

    @Override
    public void layerRemoved(int oldIdx) {
        if (LayersModel.getSingletonInstance().getNumLayers() == 0) {
            this.visdeactivate();
        }
    }

    @Override
    public void activeLayerChanged(View view) {
    }

}
