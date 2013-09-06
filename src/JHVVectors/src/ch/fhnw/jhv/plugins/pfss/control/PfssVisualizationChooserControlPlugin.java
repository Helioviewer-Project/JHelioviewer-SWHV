/**
 * 
 */
package ch.fhnw.jhv.plugins.pfss.control;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ch.fhnw.jhv.plugins.interfaces.AbstractPlugin;
import ch.fhnw.jhv.plugins.interfaces.ControlPlugin;
import ch.fhnw.jhv.plugins.pfss.rendering.PfssRenderer;
import ch.fhnw.jhv.plugins.pfss.rendering.PfssVisualization.PfssVisualizationType;

/**
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class PfssVisualizationChooserControlPlugin extends AbstractPlugin implements ControlPlugin {

    /**
     * Visualization Types Combobox
     */
    private JComboBox visTypes;

    /**
     * PFSS Renderer
     */
    private PfssRenderer renderer;

    /**
     * Current visualization
     */
    private String currVis = "Cylinder";

    /**
     * Constructor
     * 
     * @param PfssRenderer
     *            renderer
     */
    public PfssVisualizationChooserControlPlugin(PfssRenderer renderer) {
        this.renderer = renderer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.interfaces.ControlPlugin#getTitle()
     */
    public String getTitle() {
        return "PFSS Visualization Chooser:";
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.interfaces.ControlPlugin#getComponent()
     */
    public JComponent getComponent() {
        JPanel panel = new JPanel();
        JLabel lblVisualize = new JLabel("\t Visualize with ");

        String[] values = new String[PfssVisualizationType.values().length];

        int i = 0;
        for (PfssVisualizationType type : PfssVisualizationType.values()) {
            values[i] = type.getLabel();
            i++;
        }

        visTypes = new JComboBox(values);
        visTypes.setSelectedIndex(0);
        visTypes.setToolTipText("Choose a visualization method");
        visTypes.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox) e.getSource();
                String vis = (String) cb.getSelectedItem();

                if (!currVis.equals(vis)) {

                    for (PfssVisualizationType type : PfssVisualizationType.values()) {

                        if (type.getLabel().equals(vis)) {
                            // Tell the VectorRenderer, which Visualization
                            // implementation to use..
                            renderer.setPfssVisualization(type.getPfssVisualization());

                            currVis = type.getLabel();

                            return;
                        }
                    }
                }
            }
        });

        panel.add(lblVisualize);
        panel.add(visTypes);

        return panel;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.interfaces.ControlPlugin#getType()
     */
    public ControlPluginType getType() {
        return ControlPluginType.PFSS_VISUALIZATION_CHOOSER;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.interfaces.ControlPlugin#shouldStartExpanded()
     */
    public boolean shouldStartExpanded() {
        return false;
    }

}
