package org.helioviewer.gl3d.camera;

import java.awt.EventQueue;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.gl3d.gui.GL3DCameraSelectorModel;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

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
                addCameraTabs();
            }
        });
    }

    private void addCameraTabs() {
        final JTabbedPane tab = new JTabbedPane();
        tab.add("Observer", new JPanel());
        tab.add("Earth", new JPanel());
        tab.add("Expert", new JPanel());
        tab.add(new JPanel());
        tab.setTabComponentAt(3, new JLabel(IconBank.getIcon(JHVIcon.INFO)));
        add(tab);
        cameraSelectorModel = GL3DCameraSelectorModel.getInstance();
        this.cameraComboBox = new JComboBox(cameraSelectorModel);
        add(this.cameraComboBox);
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

    private void changeCamera(GL3DCamera newCamera) {
        boolean hidden = cameraSelectorModel.getCurrentCamera().getGrid().getDrawBits().get(Bit.Hidden);
        int resx = cameraSelectorModel.getCurrentCamera().getGridResolutionX();
        int resy = cameraSelectorModel.getCurrentCamera().getGridResolutionY();

        cameraSelectorModel.getCurrentCamera().deactivate();
        cameraSelectorModel.setCurrentCamera(newCamera);
        cameraSelectorModel.getCurrentCamera().activate();
        cameraSelectorModel.getCurrentCamera().getGrid().getDrawBits().set(Bit.Hidden, hidden);
        optionsPanel = cameraOptionsAttributeManager.getCameraOptionAttributePanel(cameraSelectorModel.getCurrentCamera());
        ((GL3DCameraOptionPanel) optionsPanel).getGridVisibleCheckbox().setSelected(!hidden);
        ((GL3DCameraOptionPanel) optionsPanel).getGridResolutionXSpinner().setValue(resx);
        ((GL3DCameraOptionPanel) optionsPanel).getGridResolutionYSpinner().setValue(resy);

        Displayer.getSingletonInstance().display();
    }

    private JPanel infoPanel() {
        final JEditorPane infoText = new JEditorPane();
        JPanel infoExplainPanel = new JPanel();
        infoExplainPanel.setLayout(new BoxLayout(infoExplainPanel, BoxLayout.PAGE_AXIS));
        infoExplainPanel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        infoText.setText("This is a long text that should explain how this thing works. It is not yet fully written though.");

        infoExplainPanel.add(infoText);
        add(infoExplainPanel);
        return infoExplainPanel;
    }

    public JPanel createFollowObjectCamera() {
        JPanel panel1 = new JPanel();
        return panel1;
    }
}
