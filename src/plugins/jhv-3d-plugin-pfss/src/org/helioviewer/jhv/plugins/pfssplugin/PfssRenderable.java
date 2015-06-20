package org.helioviewer.jhv.plugins.pfssplugin;

import java.awt.Component;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssData;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssNewDataLoader;
import org.helioviewer.jhv.renderable.gui.Renderable;
import org.helioviewer.jhv.renderable.gui.RenderableType;
import org.helioviewer.viewmodel.view.View;

import com.jogamp.opengl.GL2;

/**
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssRenderable implements Renderable, LayersListener {

    private final static ExecutorService pfssNewLoadPool = Executors.newFixedThreadPool(1);

    private boolean isVisible = false;
    private final RenderableType type;
    private final PfssPluginPanel optionsPanel;
    private PfssData previousPfssData = null;

    /**
     * Default constructor.
     */
    public PfssRenderable() {
        type = new RenderableType("PFSS plugin");
        optionsPanel = new PfssPluginPanel();
    }

    @Override
    public void render(GL2 gl) {
        View view;
        if (isVisible && (view = Layers.getActiveView()) != null) {
            PfssData pfssData;

            long millis = view.getImageData().getMetaData().getDateObs().getMillis();
            if ((pfssData = PfssPlugin.getPfsscache().getData(millis)) != null) {
                if (previousPfssData != null && previousPfssData != pfssData && previousPfssData.isInit()) {
                    previousPfssData.clear(gl);
                }
                if (!pfssData.isInit())
                    pfssData.init(gl);
                if (pfssData.isInit()) {
                    pfssData.display(gl);
                    datetime = pfssData.getDateString();
                    ImageViewerGui.getRenderableContainer().fireTimeUpdated(this);
                }
                previousPfssData = pfssData;
            }
        }
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
    }

    @Override
    public RenderableType getType() {
        return type;
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public String getName() {
        return "PFSS model";
    }

    @Override
    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    private String datetime = null;

    @Override
    public String getTimeString() {
        return datetime;
    }

    @Override
    public void layerAdded(View view) {
        PfssPlugin.getPfsscache().clear();
        Date start = Layers.getFirstDate();
        Date end = Layers.getLastDate();
        Thread t = new Thread(new PfssNewDataLoader(start, end), "PFFSLoader");
        pfssNewLoadPool.submit(t);
    }

    @Override
    public void activeLayerChanged(View view) {
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public boolean isActiveImageLayer() {
        return false;
    }

    @Override
    public void init(GL2 gl) {
    }

    @Override
    public void dispose(GL2 gl) {
        PfssPlugin.getPfsscache().destroy(gl);
    }

}
