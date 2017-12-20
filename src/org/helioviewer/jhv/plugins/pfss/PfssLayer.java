package org.helioviewer.jhv.plugins.pfss;

import java.awt.Component;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.TimespanListener;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.plugins.pfss.data.PfssData;
import org.helioviewer.jhv.plugins.pfss.data.PfssNewDataLoader;
import org.helioviewer.jhv.threads.CancelTask;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class PfssLayer extends AbstractLayer implements TimespanListener {

    private final PfssLayerOptions optionsPanel;
    private final PfssLine line = new PfssLine();
    private PfssData previousPfssData;

    public PfssLayer(JSONObject jo) {
        int detail = 0;
        boolean fixedColor = false;
        double radius = PfssSettings.MAX_RADIUS;

        if (jo != null) {
            detail = MathUtils.clip(jo.optInt("detail", detail), 0, PfssSettings.MAX_DETAIL);
            fixedColor = jo.optBoolean("fixedColor", fixedColor);
            radius = MathUtils.clip(jo.optDouble("radius", radius), 1.1, PfssSettings.MAX_RADIUS);
        }
        optionsPanel = new PfssLayerOptions(detail, fixedColor, radius);
    }

    @Override
    public void serialize(JSONObject jo) {
        jo.put("detail", optionsPanel.getDetail());
        jo.put("fixedColor", optionsPanel.getFixedColor());
        jo.put("radius", optionsPanel.getRadius());
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;

        PfssData pfssData = PfssPlugin.getPfsscache().getNearestData(Layers.getLastUpdatedTimestamp().milli);
        if (pfssData != null) {
            renderData(gl, pfssData, vp.aspect);
            previousPfssData = pfssData;
        }
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public String getName() {
        return "PFSS Model";
    }

    private String timeString = null;

    @Override
    public String getTimeString() {
        return timeString;
    }

    @Override
    public void setEnabled(boolean _enabled) {
        super.setEnabled(_enabled);

        if (!_enabled)
            timeString = null;
        else if (previousPfssData != null)
            timeString = previousPfssData.dateObs.toString();
    }

    @Override
    public void timespanChanged(long start, long end) {
        FutureTask<Void> dataLoaderTask = new FutureTask<>(new PfssNewDataLoader(start, end), null);
        PfssPlugin.pfssNewLoadPool.execute(dataLoaderTask);
        PfssPlugin.pfssReaperPool.schedule(new CancelTask(dataLoaderTask), PfssSettings.TIMEOUT_DOWNLOAD, TimeUnit.SECONDS);
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void init(GL2 gl) {
        line.init(gl);
        Layers.addTimespanListener(this);
    }

    @Override
    public void dispose(GL2 gl) {
        Layers.removeTimespanListener(this);
        previousPfssData = null;
        line.dispose(gl);
    }

    private int lastDetail;
    private boolean lastFixedColor;
    private double lastRadius;

    private void renderData(GL2 gl, PfssData data, double aspect) {
        int detail = optionsPanel.getDetail();
        boolean fixedColor = optionsPanel.getFixedColor();
        double radius = optionsPanel.getRadius();

        if (data != previousPfssData || lastDetail != detail || lastFixedColor != fixedColor || lastRadius != radius) {
            lastDetail = detail;
            lastFixedColor = fixedColor;
            lastRadius = radius;

            line.calculatePositions(gl, data, detail, fixedColor, radius);

            timeString = data.dateObs.toString();
            ImageViewerGui.getLayersContainer().fireTimeUpdated(this);
        }
        line.render(gl, aspect);
    }

}
