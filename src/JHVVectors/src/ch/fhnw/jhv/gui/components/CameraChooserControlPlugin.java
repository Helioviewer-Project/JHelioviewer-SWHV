package ch.fhnw.jhv.gui.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SpringLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ch.fhnw.jhv.gui.controller.cam.Camera;
import ch.fhnw.jhv.gui.controller.cam.CameraContainer;
import ch.fhnw.jhv.gui.viewport.ViewPort;
import ch.fhnw.jhv.plugins.PluginManager;
import ch.fhnw.jhv.plugins.interfaces.AbstractPlugin;
import ch.fhnw.jhv.plugins.interfaces.ControlPlugin;
import ch.fhnw.jhv.plugins.interfaces.RenderPlugin.RenderPluginType;
import ch.fhnw.jhv.plugins.vectors.control.SpringUtilities;

/**
 * CameraChooserControlPlugin defines the currently used Camera.
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class CameraChooserControlPlugin extends AbstractPlugin implements ControlPlugin, ChangeListener {

    /**
     * ComboBox which contains the different cameras
     */
    private JComboBox cameraSelection;

    /**
     * Sensitivity Slider for adjusting the sensitiviy of the camera rotation
     * speed
     */
    private JSlider sensitivitySlider;

    /**
     * Default empty constructor
     */
    public CameraChooserControlPlugin() {

    }

    /**
     * Return the title of the Plugin
     * 
     * @return String title of the ControlPlugin
     */
    public String getTitle() {
        return "Camera Chooser";
    }

    /**
     * Return the ControlPlugin component
     * 
     * @return JComponent The Component which contains the gui for the
     *         ControlPlugin
     */
    public JComponent getComponent() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        cameraSelection = new JComboBox();
        cameraSelection.setToolTipText("Choose between different Camera implementations");

        // Load all Camera values into the combo
        // box and set the current selected index
        loadComboBox();

        // Action Listener for changing the camera, when the selection got
        // changed
        cameraSelection.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox) e.getSource();
                String cam = (String) cb.getSelectedItem();

                ArrayList<Camera> list = (PluginManager.getInstance().getRenderPluginByType(RenderPluginType.SUN) != null) ? CameraContainer.getCamsSun() : CameraContainer.getCamsPlane();

                for (Camera val : list) {

                    if (val.getLabel().equals(cam)) {

                        // Change the current active camera
                        ViewPort.getInstance().setActiveCamera(val);

                        return;
                    }
                }
            }
        });

        panel.add(cameraSelection);

        JPanel form = new JPanel(new SpringLayout());
        form.add(new JLabel("Sensitivity"));
        sensitivitySlider = new JSlider();
        sensitivitySlider.setMinimum(10);
        sensitivitySlider.setMaximum(100);
        sensitivitySlider.setToolTipText("Define the sensitivity of the mouse rotation speed");
        form.add(sensitivitySlider);
        sensitivitySlider.addChangeListener(this);
        sensitivitySlider.setValueIsAdjusting(true);
        sensitivitySlider.setValue((int) (ViewPort.getInstance().getActiveCamera().getSensitivity() * 100));
        sensitivitySlider.setValueIsAdjusting(false);

        // Lay out the panel
        SpringUtilities.makeCompactGrid(form, 1, 2, // rows, cols
                6, 6, // initX, initY
                6, 6); // xPad, yPad

        panel.add(form);

        return panel;
    }

    /**
     * Load the possible camera for the JComboBox
     * 
     * @return String[] values
     */
    public void loadComboBox() {

        // clear current combobox values
        cameraSelection.removeAllItems();

        int i = 0;
        int currSelctedIndex = -1;

        ArrayList<Camera> list = (PluginManager.getInstance().getRenderPluginByType(RenderPluginType.SUN) != null) ? CameraContainer.getCamsSun() : CameraContainer.getCamsPlane();

        String[] values = new String[list.size()];

        Camera currCamera = ViewPort.getInstance().getActiveCamera();

        // Set the dropdown list value to the current active camera
        for (Camera cam : list) {
            values[i] = cam.getLabel();

            cameraSelection.addItem(cam.getLabel());

            if (cam.getLabel().equals(currCamera.getLabel())) {
                currSelctedIndex = i;
            }

            i++;
        }

        cameraSelection.setSelectedIndex(currSelctedIndex);
    }

    /**
     * Return the Type of the ControlPlugin
     * 
     * @return ControlPluginType type
     */
    public ControlPluginType getType() {
        return ControlPluginType.CAMERA_CHOOSER;
    }

    /**
     * Should the ControlPlugin start in expanded mode
     * 
     * @return boolean If it should start in expanded mode
     */
    public boolean shouldStartExpanded() {
        return false;
    }

    /**
     * Handles Changes of SensitivitySlider
     * 
     * @param e
     *            change event
     */
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == sensitivitySlider && !sensitivitySlider.getValueIsAdjusting()) {
            // change mouse sensitivity
            Camera cam = ViewPort.getInstance().getActiveCamera();
            if (cam != null) {
                float sense = (float) sensitivitySlider.getValue() / 100;
                cam.setSensitivity(sense);
            }
        }
    }
}
