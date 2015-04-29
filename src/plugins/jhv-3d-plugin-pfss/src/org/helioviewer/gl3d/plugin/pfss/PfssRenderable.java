package org.helioviewer.gl3d.plugin.pfss;

import java.awt.Component;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.helioviewer.gl3d.plugin.pfss.data.PfssData;
import org.helioviewer.gl3d.plugin.pfss.data.PfssNewDataLoader;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.plugin.renderable.Renderable;
import org.helioviewer.jhv.plugin.renderable.RenderableType;
import org.helioviewer.jhv.plugins.pfssplugin.PfssPlugin;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.TimedMovieView;

import com.jogamp.opengl.GL2;

/**
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssRenderable implements Renderable, LayersListener {

    private final static ExecutorService pfssNewLoadPool = Executors.newFixedThreadPool(1);

    private boolean isVisible = false;
    private final RenderableType type;
    private final PfssPluginPanel optionsPanel;

    /**
     * Default constructor.
     */
    public PfssRenderable() {
        type = new RenderableType("PFSS plugin");
        this.optionsPanel = new PfssPluginPanel();
        Displayer.getRenderableContainer().addRenderable(this);
        Displayer.getLayersModel().addLayersListener(this);
    }

    @Override
    public void init(GL2 gl) {
    }

    @Override
    public void render(GL2 gl) {
        if (isVisible) {
            PfssData pfssData;
            TimedMovieView movie = LinkedMovieManager.getSingletonInstance().getMasterMovie();
            if (movie != null && (pfssData = PfssPlugin.getPfsscache().getData(movie.getCurrentDateMillis())) != null) {
                pfssData.setInit(false);
                pfssData.init(gl);
                if (pfssData.isInit()) {
                    pfssData.display(gl);
                    datetime = pfssData.getDateString();
                    Displayer.getRenderableContainer().fireTimeUpdated(this);
                }
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

}
