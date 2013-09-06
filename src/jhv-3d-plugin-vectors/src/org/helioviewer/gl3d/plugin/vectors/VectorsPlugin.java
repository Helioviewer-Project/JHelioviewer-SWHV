package org.helioviewer.gl3d.plugin.vectors;

import javax.swing.JComponent;

import org.helioviewer.gl3d.plugin.GL3DAbstractModelPlugin;
import org.helioviewer.gl3d.plugin.GL3DModelPlugin;

/**
 * This plugin visualizes vector fields derived from SOT/Hinode images using
 * colored vectors. It can read FITS files to import vector fields.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public class VectorsPlugin extends GL3DAbstractModelPlugin implements GL3DModelPlugin {
    private VectorsConfigPanel configPanel;

    public void load() {
        this.configPanel = new VectorsConfigPanel(this);
        this.configPanel.init();
    }

    public void unload() {

    }

    public String getPluginDescription() {
        return "Vectorfield";
    }

    public String getPluginName() {
        return "Vectorfield";
    }

    public JComponent getConfigurationComponent() {
        return this.configPanel;
    }
}
