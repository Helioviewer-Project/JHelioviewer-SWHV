package org.helioviewer.gl3d.plugin.pfss;

import javax.media.opengl.GL;

import org.helioviewer.gl3d.plugin.pfss.data.PfssCache;
import org.helioviewer.gl3d.plugin.pfss.data.PfssData;
import org.helioviewer.gl3d.plugin.pfss.data.PfssFitsFile;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderGraphics;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderer3d;
import org.helioviewer.viewmodel.view.View;

/**
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssPlugin3dRenderer extends PhysicalRenderer3d {
    private PfssCache pfssCache = null;
    private GL lastGl = null;

    /**
     * Default constructor.
     */
    public PfssPlugin3dRenderer(PfssCache pfssCache) {
        this.pfssCache = pfssCache;
    }

    /**
     * {@inheritDoc}
     * 
     * Draws all available and visible solar events with there associated icon.
     */
    @Override
    public void render(PhysicalRenderGraphics g) {
        if (pfssCache.isVisible()) {
            GL gl = g.getGL();
            PfssFitsFile fitsToClear = pfssCache.getFitsToDelete();
            if (fitsToClear != null)
                fitsToClear.clear(gl);
            PfssData pfssData = pfssCache.getData();

            if (pfssData != null) {
                if (lastGl != gl)
                    pfssData.setInit(false);
                pfssData.init(gl);
                lastGl = gl;
                if (pfssData.isInit()) {
                    pfssData.display(gl);
                }
            }

        }
    }

    public void setVisible() {

    }

    @Override
    public void viewChanged(View view) {

    }

}
