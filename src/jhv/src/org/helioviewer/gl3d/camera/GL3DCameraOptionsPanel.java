package org.helioviewer.gl3d.camera;

import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;

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
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
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
    JTabbedPane tab;
    private boolean active;

    public GL3DCameraOptionsPanel() {
        LayersModel.getSingletonInstance().addLayersListener(this);
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                setModel();
                addCameraTabs();
            }
        });
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
        cameraSelectorModel.setCurrentCamera(newCamera);
        optionsPanel = cameraOptionsAttributeManager.getCameraOptionAttributePanel(newCamera);
        Displayer.getSingletonInstance().display();
        tab.setComponentAt(0, optionsPanel);
    }

    private void changeCamera(GL3DCamera newCamera) {
        boolean hidden = cameraSelectorModel.getCurrentCamera().getGrid().getDrawBits().get(Bit.Hidden);
        double resx = cameraSelectorModel.getCurrentCamera().getGridResolutionX();
        double resy = cameraSelectorModel.getCurrentCamera().getGridResolutionY();

        cameraSelectorModel.getCurrentCamera().deactivate();
        cameraSelectorModel.setCurrentCamera(newCamera);
        cameraSelectorModel.getCurrentCamera().activate();
        cameraSelectorModel.getCurrentCamera().getGrid().getDrawBits().set(Bit.Hidden, hidden);
        optionsPanel = cameraOptionsAttributeManager.getCameraOptionAttributePanel(cameraSelectorModel.getCurrentCamera());
        ((GL3DCameraOptionPanel) optionsPanel).getGridVisibleCheckbox().setSelected(!hidden);
        ((GL3DCameraOptionPanel) optionsPanel).getGridResolutionXSpinner().setValue(resx);
        ((GL3DCameraOptionPanel) optionsPanel).getGridResolutionYSpinner().setValue(resy);

        Displayer.getSingletonInstance().render();
    }

    private JPanel infoPanel() {

        JPanel infoExplainPanel = new JPanel();
        infoExplainPanel.setLayout(new BoxLayout(infoExplainPanel, BoxLayout.PAGE_AXIS));
        infoExplainPanel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        String explanation = "Observer camera: view from observer defined by active layer.\n\n";
        explanation += "Earth camera: view from earth defined by active layer.\n\n";
        explanation += "Expert camera: view from selected object. ";
        explanation += "By default the current time follows the current timestamp of the active video. ";
        explanation += "Optionally, when unchecking \"Use active layer timestamps\", the current camera time is interpolated in the chosen time interval";
        explanation += "from the current data time relative to the current data interval.";
        explanation += "E.g. choosing the two camera times equal will project the data ";
        explanation += "as seen from the chosen viewpoint at the chosen time";

        JTextArea infoText = new JTextArea(explanation);
        infoText.setEditable(false);
        infoText.setLineWrap(true);
        infoText.setWrapStyleWord(true);
        infoText.setOpaque(false);
        infoExplainPanel.add(infoText);
        return infoExplainPanel;
    }

    public JPanel createFollowObjectCamera() {
        JPanel panel1 = new JPanel();
        return panel1;
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
        if (LayersModel.getSingletonInstance().getNumLayers() >= 0) {
            this.visactivate();
        }
    }

    @Override
    public void layerRemoved(View oldView, int oldIdx) {
        if (LayersModel.getSingletonInstance().getNumLayers() == 0) {
            this.visdeactivate();
        }
    }

    @Override
    public void layerChanged(int idx) {
        // TODO Auto-generated method stub

    }

    @Override
    public void activeLayerChanged(int idx) {
        // TODO Auto-generated method stub

    }

    @Override
    public void viewportGeometryChanged() {
        // TODO Auto-generated method stub

    }

    @Override
    public void timestampChanged(int idx) {
        // TODO Auto-generated method stub

    }

    @Override
    public void subImageDataChanged() {
        // TODO Auto-generated method stub

    }

    @Override
    public void layerDownloaded(int idx) {
        // TODO Auto-generated method stub

    }
}
