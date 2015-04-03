package org.helioviewer.gl3d.plugin.pfss;

import java.awt.Component;

import javax.media.opengl.GL2;
import javax.swing.JPanel;

import org.helioviewer.gl3d.plugin.pfss.data.PfssCache;
import org.helioviewer.gl3d.plugin.pfss.data.PfssData;
import org.helioviewer.gl3d.plugin.pfss.data.PfssFitsFile;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.plugin.renderable.Renderable;
import org.helioviewer.jhv.plugin.renderable.RenderableType;

/**
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssPlugin3dRenderer implements Renderable {

    private PfssCache pfssCache = null;
    private boolean isVisible = false;
    private final RenderableType type;

    /**
     * Default constructor.
     */
    public PfssPlugin3dRenderer(PfssCache pfssCache) {
        type = new RenderableType("PFSS plugin");
        this.pfssCache = pfssCache;
        Displayer.getRenderablecontainer().addRenderable(this);
    }

    @Override
    public void init(GL3DState state) {
        // TODO Auto-generated method stub

    }

    @Override
    public void render(GL3DState state) {
        if (isVisible) {
            GL2 gl = GL3DState.get().gl;
            PfssFitsFile fitsToClear = pfssCache.getFitsToDelete();
            if (fitsToClear != null)
                fitsToClear.clear(gl);
            PfssData pfssData = pfssCache.getData();
            if (pfssData != null) {
                pfssData.setInit(false);
                pfssData.init(gl);
                if (pfssData.isInit()) {
                    pfssData.display(gl);
                }
            }
        }
    }

    @Override
    public void remove(GL3DState state) {

    }

    @Override
    public RenderableType getType() {
        return type;
    }

    @Override
    public Component getOptionsPanel() {
        return new JPanel();
    }

    @Override
    public String getName() {
        return "PFSS data";
    }

    @Override
    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;

    }

    @Override
    public String getTimeString() {
        PfssData pfssData = pfssCache.getData();
        if (pfssData != null) {
            return pfssData.getDateString();
        }
        return "";

    }

}
