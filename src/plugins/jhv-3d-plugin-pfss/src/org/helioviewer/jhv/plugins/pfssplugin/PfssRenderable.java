package org.helioviewer.jhv.plugins.pfssplugin;

import java.awt.Component;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.plugin.renderable.Renderable;
import org.helioviewer.jhv.plugin.renderable.RenderableType;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssData;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssNewDataLoader;
import org.helioviewer.viewmodel.view.AbstractView;

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
        this.optionsPanel = new PfssPluginPanel();
        ImageViewerGui.getRenderableContainer().addRenderable(this);
        Displayer.getLayersModel().addLayersListener(this);
    }

    @Override
    public void init(GL2 gl) {
    }

    @Override
    public void render(GL2 gl) {
        AbstractView view;
        if (isVisible && (view = Displayer.getLayersModel().getActiveView()) != null) {
            PfssData pfssData;

            long millis = view.getMetaData().getDateTime().getMillis();
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

    String datetime = "";

    @Override
    public String getTimeString() {
        return datetime;
    }

    @Override
    public void layerAdded(int idx) {
        PfssPlugin.getPfsscache().clear();
        Date start = Displayer.getLayersModel().getFirstDate();
        Date end = Displayer.getLayersModel().getLastDate();
        Thread t = new Thread(new PfssNewDataLoader(start, end), "PFFSLoader");
        pfssNewLoadPool.submit(t);
    }

    @Override
    public void layerRemoved(int oldIdx) {
    }

    @Override
    public void activeLayerChanged(AbstractView view) {
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
    public void reInit(GL2 gl) {
        PfssPlugin.getPfsscache().unInit();
    }

}
