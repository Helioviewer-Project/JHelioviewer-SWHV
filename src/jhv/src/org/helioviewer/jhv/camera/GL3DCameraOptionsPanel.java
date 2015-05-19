package org.helioviewer.jhv.camera;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataListener;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.components.base.DegreeFormatterFactory;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.gui.dialogs.TextDialog;

@SuppressWarnings({"unchecked","rawtypes","serial"})
public class GL3DCameraOptionsPanel extends JPanel {

    private GL3DCamera previousCamera;
    private final JComboBox comboBox;

    private GL3DCameraOptionPanel currentOptionPanel;

    private JPanel fovPanel;
    private JSpinner fovSpinner;
    private final CameraComboboxModel comboBoxModel;

    public GL3DCameraOptionsPanel(GL3DCamera newCamera) {
        setLayout(new GridBagLayout());
        comboBoxModel = new CameraComboboxModel();

        Displayer.setActiveCamera(newCamera);
        addInitialCameraTypes();
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        comboBox = new JComboBox(comboBoxModel);
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int i = 0;
                Object selectedItem = comboBox.getSelectedItem();
                while (selectedItem != comboBoxModel.getComboBoxTitle(i) && i < comboBoxModel.getCurrentCameras()) {
                    i++;
                }
                if (i != comboBoxModel.getCurrentCameras()) {
                    Class<?> cls = comboBoxModel.getCombolistClass(i);
                    GL3DCamera camera;
                    try {
                        camera = (GL3DCamera) cls.newInstance();
                        changeCamera(camera);
                    } catch (InstantiationException e1) {
                        e1.printStackTrace();
                    } catch (IllegalAccessException e1) {
                        e1.printStackTrace();
                    }
                }
                Displayer.display();

            }
        });

        add(comboBox, c);
        c.gridx = 1;
        c.weightx = 0;

        AbstractAction showInfoAction = new AbstractAction() {
            {
                putValue(SHORT_DESCRIPTION, "Show camera info");
                putValue(SMALL_ICON, IconBank.getIcon(JHVIcon.INFO));
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                TextDialog td = new TextDialog("Camera options information", comboBoxModel.getExplanation());
                td.showDialog();
            }
        };
        JButton infoButton = new JButton(showInfoAction);
        infoButton.setBorder(null);
        infoButton.setText(null);
        infoButton.setBorderPainted(false);
        infoButton.setFocusPainted(false);
        infoButton.setContentAreaFilled(false);
        add(infoButton, c);
        c.weightx = 1;

        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 1;
        createFOV(c);
        previousCamera = newCamera;
        changeCamera(newCamera);
    }

    public void addInitialCameraTypes() {
        addCameraType("Observer Camera", GL3DObserverCamera.class, "Observer camera: view from observer.\nCamera time defined by timestamps of the active layer.\n\n");
        addCameraType("Earth Camera", GL3DEarthCamera.class, "Earth camera: view from Earth.\nCamera time defined by timestamps of the active layer.\n\n");
        addCameraType("Expert Camera", GL3DFollowObjectCamera.class, "Expert camera: view from selected object.\nCamera time defined by timestamps of the active layer, unless \"Use active layer timestamps\" is off. In that case, camera time is interpolated in the configured time interval.");
    }

    public void addCameraType(String comboBoxTitle, Class<?> cls, String explanation) {
        comboBoxModel.addCameraType(comboBoxTitle, cls, explanation);
    }

    public void removeCameraType(Class<?> cls) {
        comboBoxModel.removeCameraType(cls);
    }

    private void switchOptionsPanel(GL3DCameraOptionPanel newOptionPanel) {
        if (currentOptionPanel != null) {
            remove(currentOptionPanel);
        }
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 2;
        this.add(newOptionPanel, c);
        currentOptionPanel = newOptionPanel;
        revalidate();
    }

    private void changeCamera(GL3DCamera newCamera) {
        boolean trackingMode = previousCamera.getTrackingMode();
        newCamera.setTrackingMode(trackingMode);
        this.switchOptionsPanel(newCamera.getOptionPanel());

        Displayer.setActiveCamera(newCamera);
        Displayer.display();
        previousCamera = newCamera;
    }

    private void createFOV(GridBagConstraints c) {
        fovPanel = new JPanel();
        fovPanel.setLayout(new BoxLayout(fovPanel, BoxLayout.LINE_AXIS));
        fovPanel.add(new JLabel("FOV angle"));
        fovSpinner = new JSpinner();
        fovSpinner.setModel(new SpinnerNumberModel(new Double(0.8), new Double(0.0), new Double(180.), new Double(0.01)));
        JFormattedTextField f = ((JSpinner.DefaultEditor) fovSpinner.getEditor()).getTextField();
        f.setFormatterFactory(new DegreeFormatterFactory("%.2f\u00B0"));

        if (Displayer.getActiveCamera() != null)
            Displayer.getActiveCamera().setFOVangleDegrees((Double) fovSpinner.getValue());

        this.fovSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Displayer.getActiveCamera().setFOVangleDegrees((Double) fovSpinner.getValue());
                Displayer.display();
            }
        });
        WheelSupport.installMouseWheelSupport(this.fovSpinner);
        fovPanel.add(this.fovSpinner);

        fovSpinner.setMaximumSize(new Dimension(6, 22));
        fovPanel.add(Box.createHorizontalGlue());
        add(fovPanel, c);
    }

    private class CameraComboboxModel implements ComboBoxModel {
        private Object selectedItem;
        private final ArrayList<ListDataListener> listDataListeners = new ArrayList<ListDataListener>();
        private final ArrayList<String> combolist = new ArrayList<String>();
        private final ArrayList<Class<?>> combolistClass = new ArrayList<Class<?>>();
        private final ArrayList<String> explanations = new ArrayList<String>();

        public CameraComboboxModel() {

        }

        public int getCurrentCameras() {
            return combolist.size();
        }

        public Class<?> getCombolistClass(int i) {
            return combolistClass.get(i);
        }

        public Object getComboBoxTitle(int i) {
            return combolist.get(i);
        }

        @Override
        public int getSize() {
            return combolist.size();
        }

        @Override
        public Object getElementAt(int index) {
            return combolist.get(index);
        }

        @Override
        public void addListDataListener(ListDataListener l) {
            this.listDataListeners.add(l);
        }

        @Override
        public void removeListDataListener(ListDataListener l) {
            this.listDataListeners.remove(l);
        }

        @Override
        public void setSelectedItem(Object anItem) {
            this.selectedItem = anItem;
        }

        @Override
        public Object getSelectedItem() {
            return this.selectedItem;
        }

        public void addCameraType(String comboBoxTitle, Class<?> cls, String explanation) {
            combolist.add(comboBoxTitle);
            explanations.add(explanation);
            combolistClass.add(cls);
            if (this.selectedItem == null) {
                this.selectedItem = combolist.get(0);
            }
        }

        public void removeCameraType(Class<?> cls) {
            int idx = combolistClass.indexOf(cls);

            if (idx == -1) {
                Log.error("Trying to remove unexisting element from camera list");
            } else {
                if (this.selectedItem == combolist.get(idx) && combolist.size() > 0) {
                    this.selectedItem = combolist.get(0);
                }
                combolist.remove(idx);
                explanations.remove(idx);
                combolistClass.remove(idx);
            }
        }

        private String getExplanation() {
            String full = "";
            for (String expl : explanations) {
                full += expl;
            }
            return full;
        }
    }
}
