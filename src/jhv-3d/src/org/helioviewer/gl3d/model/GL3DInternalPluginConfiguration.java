package org.helioviewer.gl3d.model;

import java.util.ArrayList;
import java.util.List;

import org.helioviewer.gl3d.plugin.GL3DModelPlugin;
import org.helioviewer.gl3d.plugin.GL3DPluginConfiguration;
import org.helioviewer.gl3d.plugin.pfss.PfssPlugin;
import org.helioviewer.gl3d.plugin.vectors.VectorsPlugin;

/**
 * A static plugin configuration that includes the two internal plugins - PFSS -
 * Vectors
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DInternalPluginConfiguration implements GL3DPluginConfiguration {

    public List<GL3DModelPlugin> findPlugins() {
        List<GL3DModelPlugin> plugins = new ArrayList<GL3DModelPlugin>();
        plugins.add(new PfssPlugin());
        plugins.add(new VectorsPlugin());
        return plugins;
    }
}
