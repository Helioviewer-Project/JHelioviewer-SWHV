package ch.fhnw.jhv.plugins.vectors.control;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ch.fhnw.jhv.gui.components.CameraChooserControlPlugin;
import ch.fhnw.jhv.gui.controller.cam.CameraContainer;
import ch.fhnw.jhv.gui.controller.cam.CameraContainer.CameraType;
import ch.fhnw.jhv.gui.viewport.ViewPort;
import ch.fhnw.jhv.plugins.PluginManager;
import ch.fhnw.jhv.plugins.interfaces.AbstractPlugin;
import ch.fhnw.jhv.plugins.interfaces.ControlPlugin;
import ch.fhnw.jhv.plugins.interfaces.RenderPlugin.RenderPluginType;
import ch.fhnw.jhv.plugins.vectors.data.VectorFieldManager;
import ch.fhnw.jhv.plugins.vectors.rendering.VectorRenderer;
import ch.fhnw.jhv.plugins.vectors.rendering.VectorVisualization;
import ch.fhnw.jhv.plugins.vectors.rendering.VectorVisualization.VectorVisualizationType;

/**
 * SettingsControlPlugin contains all the Gui Elements for changing: - The
 * Length of the vector - The current averaging factor for the vectorfield - The
 * selection value for filtering vectors out - If it should be visualized on
 * plane or on the sun - Which Visualization is currently used
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class SettingsControlPlugin extends AbstractPlugin implements ControlPlugin {

    /**
     * Vector field manager
     */
    private VectorFieldManager vectorFieldManager = VectorFieldManager.getInstance();

    /**
     * Vector Length Slider
     */
    private JSlider sliderVectorLength;
    private float valueVectorLength = vectorFieldManager.getLengthScaleValue();

    /**
     * Vector Filter Slider
     */
    private JSlider sliderFilterVector;
    private JLabel lblValueFilterVector;
    private int valueFilterVector = vectorFieldManager.getFilterThresholdValue();
    private int min = 0;
    private int max = 1500;

    /**
     * Vector average Slider
     */
    private JSlider sliderVectorAverage;
    private JLabel lblValueVectorAverage;
    private double valueVectorAverage = vectorFieldManager.getAverageFactor();

    /**
     * Plane or Sphere
     */
    private JRadioButton radioButtonPlane;
    private JRadioButton radioButtonSun;

    /**
     * Visualization Types Combobox
     */
    private JComboBox visTypes;

    /**
     * Panel container
     */
    private JPanel container;

    /**
     * Return the title of the ControlPlugin
     * 
     * @return String title
     */
    public String getTitle() {
        return "Vector Field Settings";
    }

    /**
     * Constructor
     */
    public SettingsControlPlugin() {
        container = new JPanel();
        container.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.NONE;

        // Add the components to the main container
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        container.add(getLengthSlider(), c);

        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        container.add(getFilterSlider(), c);

        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;
        container.add(getAverageSlider(), c);

        c.gridx = 0;
        c.gridy = 3;
        c.anchor = GridBagConstraints.WEST;
        container.add(getPlaneToggle(), c);

        c.gridx = 0;
        c.gridy = 4;
        c.anchor = GridBagConstraints.WEST;
        container.add(getVisualizationChooser(), c);

        container.validate();
    }

    /**
     * Return the Gui elements of this ControlPlugin
     * 
     * @return JComponent Gui elements of the ControlPlugin
     */
    public JComponent getComponent() {
        return container;
    }

    /**
     * Return the FilterLengthSlider Container
     * 
     * @return JComponent
     */
    private JComponent getLengthSlider() {

        final float scaleFactor = 10000.0f;

        JPanel container = new JPanel();
        container.setLayout(new BorderLayout());

        JLabel length = new JLabel("\t\t Length: ");
        length.setPreferredSize(new Dimension(90, 20));

        container.add(length, BorderLayout.WEST);

        sliderVectorLength = new JSlider();
        sliderVectorLength.setPreferredSize(new Dimension(180, 50));
        sliderVectorLength.setMinimum(1);
        sliderVectorLength.setToolTipText("With this slider you can scale the vector length");
        sliderVectorLength.setMaximum(100);
        sliderVectorLength.setValue((int) (valueVectorLength * scaleFactor));
        sliderVectorLength.setToolTipText("Define the vector length");

        sliderVectorLength.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent event) {

                if (!((JSlider) event.getSource()).getValueIsAdjusting()) {
                    valueVectorLength = ((JSlider) event.getSource()).getValue() / scaleFactor;

                    new DecimalFormat("#.####");

                    // Propagate the changed value to the VectorRenderer
                    if (valueVectorLength != 0) {

                        vectorFieldManager.setLengthScaleValue(valueVectorLength);
                    }
                }
            }
        });

        container.add(sliderVectorLength, BorderLayout.CENTER);

        return container;
    }

    /**
     * Return the Average Slider Container
     * 
     * @return JComponent component
     */
    private JComponent getAverageSlider() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JLabel averaging = new JLabel("\t\t Averaging: ");
        averaging.setPreferredSize(new Dimension(90, 20));

        panel.add(averaging, BorderLayout.WEST);

        // create and configure slider
        sliderVectorAverage = new JSlider();
        sliderVectorAverage.setMinimum(1);
        sliderVectorAverage.setMaximum(32);
        sliderVectorAverage.setToolTipText("With this slider you can define how many vectores are combined together. With an average factor of 3 it means that 3x3 vectors are combined. ");
        sliderVectorAverage.setValueIsAdjusting(true);
        sliderVectorAverage.setPreferredSize(new Dimension(180, 50));
        sliderVectorAverage.setValue((int) valueVectorAverage);
        sliderVectorAverage.setPaintTicks(true);
        sliderVectorAverage.setPaintLabels(true);
        sliderVectorAverage.setMajorTickSpacing(4);
        sliderVectorAverage.setToolTipText("Averaging: ");
        sliderVectorAverage.setPaintLabels(true);

        sliderVectorAverage.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent event) {

                JSlider slider = (JSlider) event.getSource();

                if (!slider.getValueIsAdjusting()) {

                    int currentVal = slider.getValue();
                    valueVectorAverage = currentVal;

                    lblValueVectorAverage.setText("" + valueVectorAverage);

                    container.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                    // Propagate the new value to the VectorRendererPlugin
                    vectorFieldManager.setAverageFactor((int) valueVectorAverage);

                    container.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });

        panel.add(sliderVectorAverage, BorderLayout.CENTER);

        lblValueVectorAverage = new JLabel("" + valueVectorAverage);
        panel.add(lblValueVectorAverage, BorderLayout.EAST);

        sliderVectorAverage.validate();

        return panel;
    }

    /**
     * Update the AverageValue
     * 
     * @param averageFactora
     *            new averageFactor
     */
    public void setAverageFactor(int averageFactor) {
        this.valueVectorAverage = averageFactor;
        lblValueVectorAverage.setText(averageFactor + "");
        sliderVectorAverage.setValue(averageFactor);
    }

    /**
     * Return the FilterSlider Container
     * 
     * @return JComponent component
     */
    private JComponent getFilterSlider() {
        JPanel container = new JPanel();
        container.setLayout(new BorderLayout());

        JLabel filter = new JLabel("\t\t Filter: ");
        filter.setPreferredSize(new Dimension(90, 20));

        container.add(filter, BorderLayout.WEST);

        // create and configure slider
        sliderFilterVector = new JSlider();
        sliderFilterVector.setMinimum(min);
        sliderFilterVector.setPreferredSize(new Dimension(180, 50));
        sliderFilterVector.setMaximum(max);
        sliderFilterVector.setToolTipText("With this slider you can filter out short vectors.");
        sliderFilterVector.setValue(valueFilterVector);
        sliderFilterVector.setToolTipText("Filter vectors out");

        sliderFilterVector.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent event) {

                if (!((JSlider) event.getSource()).getValueIsAdjusting()) {
                    int currentVal = ((JSlider) event.getSource()).getValue();
                    valueFilterVector = currentVal;

                    lblValueFilterVector.setText("" + valueFilterVector);

                    // Propagate the new value to the VectorRendererPlugin
                    vectorFieldManager.setFilterThresholdValue(valueFilterVector);
                }
            }
        });

        container.add(sliderFilterVector, BorderLayout.CENTER);

        lblValueFilterVector = new JLabel("" + valueFilterVector);
        // container.add(lblValueFilterVector, BorderLayout.EAST);

        return container;

    }

    /**
     * Return the PlaneSphere RadioButtons
     * 
     * @return JComponent component
     */
    private JComponent getPlaneToggle() {
        JPanel pnlPlaneToggle = new JPanel();
        JLabel lblPlaneToggle = new JLabel("\t Visualize on ");
        lblPlaneToggle.setPreferredSize(new Dimension(90, 20));

        PluginManager.getInstance();

        radioButtonSun = new JRadioButton("Sun");
        radioButtonSun.setToolTipText("Visualize the vectors on the sun.");
        radioButtonPlane = new JRadioButton("Plane");
        radioButtonPlane.setToolTipText("Visualize the vectors on the plane.");

        radioButtonSun.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                e.getSource();

                PluginManager pluginManager = PluginManager.getInstance();

                // Switched to sun mode
                container.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                pluginManager.deactivateRenderPluginType(RenderPluginType.PLANE);
                pluginManager.activateRenderPluginType(RenderPluginType.SUN);

                ViewPort.getInstance().setActiveCamera(CameraContainer.getCamera(CameraType.ROTATION_SUN));

                VectorRenderer.getInstance().updateVBOS();
                container.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

                // Notify the CameraChooser Plugin to reload the camera items
                CameraChooserControlPlugin cameraChooserControlPlugin = (CameraChooserControlPlugin) pluginManager.getControlPluginByType(ControlPluginType.CAMERA_CHOOSER);
                cameraChooserControlPlugin.loadComboBox();
            }
        });

        radioButtonPlane.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                e.getSource();

                PluginManager pluginManager = PluginManager.getInstance();

                container.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                // Switched to plane mode
                pluginManager.deactivateRenderPluginType(RenderPluginType.SUN);
                pluginManager.activateRenderPluginType(RenderPluginType.PLANE);

                ViewPort.getInstance().setActiveCamera(CameraContainer.getCamera(CameraType.ROTATION_PLANE));

                VectorRenderer.getInstance().updateVBOS();

                container.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

                // Notify the CameraChooser Plugin to reload the camera items
                CameraChooserControlPlugin cameraChooserControlPlugin = (CameraChooserControlPlugin) pluginManager.getControlPluginByType(ControlPluginType.CAMERA_CHOOSER);
                cameraChooserControlPlugin.loadComboBox();
            }
        });

        pnlPlaneToggle.add(lblPlaneToggle);

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(radioButtonSun);
        buttonGroup.add(radioButtonPlane);

        pnlPlaneToggle.add(radioButtonSun);
        pnlPlaneToggle.add(radioButtonPlane);

        return pnlPlaneToggle;
    }

    /**
     * Return the JComponent which contains the VisualizationChooserCombobox
     * 
     * @return JComponent component
     */
    private JComponent getVisualizationChooser() {

        JPanel panel = new JPanel();
        JLabel lblVisualize = new JLabel("\t Visualize with ");

        String[] values = new String[VectorVisualizationType.values().length];

        int i = 0;
        int currSelctedIndex = -1;
        VectorVisualization currVis = VectorRenderer.getInstance().getVectorVisualization();
        for (VectorVisualizationType type : VectorVisualizationType.values()) {
            values[i] = type.getLabel();

            if (type.getVisualization() == currVis) {
                currSelctedIndex = i;
            }

            i++;
        }

        visTypes = new JComboBox(values);
        visTypes.setSelectedIndex(currSelctedIndex);
        visTypes.setToolTipText("You can choose between several different vector visualization methods.");
        visTypes.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox) e.getSource();
                String vis = (String) cb.getSelectedItem();

                for (VectorVisualizationType type : VectorVisualizationType.values()) {

                    if (type.getLabel().equals(vis)) {

                        // Tell the VectorRenderer, which Visualization
                        // implementation to use..
                        VectorRenderer.getInstance().setVectorVis(type);
                        VectorRenderer.getInstance().updateVBOS();

                        return;
                    }
                }
            }
        });

        panel.add(lblVisualize);
        panel.add(visTypes);

        return panel;
    }

    /**
     * Return the ControlPluginType
     * 
     * @return ControlPluginType type
     */
    public ControlPluginType getType() {
        return ControlPluginType.SETTINGS;
    }

    /**
     * Should the JComponent be expanded on statup
     * 
     * @return boolean should it be expanded on startup
     */
    public boolean shouldStartExpanded() {
        return false;
    }

    /**
     * Set the gui components enabled or not
     * 
     * @param enabled
     *            should they be enabled
     */

    public void setEnabled(boolean enabled) {
        sliderFilterVector.setEnabled(enabled);
        sliderVectorAverage.setEnabled(enabled);
        sliderVectorLength.setEnabled(enabled);
        radioButtonSun.setEnabled(enabled);
        radioButtonPlane.setEnabled(enabled);
        visTypes.setEnabled(enabled);

        // If it gets enabled again we have to check which visualization was on
        if (enabled) {
            PluginManager pluginManager = PluginManager.getInstance();

            // Determine which one is active at the start
            if ((pluginManager.getRenderPluginByType(RenderPluginType.SUN) != null)) {
                // sun is active
                radioButtonSun.setSelected(true);
            } else if (pluginManager.getRenderPluginByType(RenderPluginType.PLANE) != null) {
                // plane is active
                radioButtonPlane.setSelected(true);
            }
        }
    }
}
