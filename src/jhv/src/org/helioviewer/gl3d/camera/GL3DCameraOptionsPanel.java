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
                System.out.println("selected" + index);
                if (index == 0) {
                    cameraSelectorModel.getCurrentCamera().deactivate();
                    cameraSelectorModel.setCurrentCamera(cameraSelectorModel.getTrackballCamera());
                    cameraSelectorModel.getCurrentCamera().activate();
                    optionsPanel = cameraOptionsAttributeManager.getCameraOptionAttributePanel(cameraSelectorModel.getCurrentCamera());
                    Displayer.getSingletonInstance().display();
                }
                if (index == 1) {
                    cameraSelectorModel.getCurrentCamera().deactivate();
                    cameraSelectorModel.setCurrentCamera(cameraSelectorModel.getEarthCamera());
                    cameraSelectorModel.getCurrentCamera().activate();
                    optionsPanel = cameraOptionsAttributeManager.getCameraOptionAttributePanel(cameraSelectorModel.getCurrentCamera());
                    Displayer.getSingletonInstance().display();
                }
                if (index == 2) {
                    cameraSelectorModel.getCurrentCamera().deactivate();
                    cameraSelectorModel.setCurrentCamera(cameraSelectorModel.getFollowObjectCamera());
                    cameraSelectorModel.getCurrentCamera().activate();
                    optionsPanel = cameraOptionsAttributeManager.getCameraOptionAttributePanel(cameraSelectorModel.getCurrentCamera());
                    Displayer.getSingletonInstance().display();
                }
                if (index == 3) {
                    optionsPanel = infoPanel();
                }
                tab.setComponentAt(index, optionsPanel);
            }
        });
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
