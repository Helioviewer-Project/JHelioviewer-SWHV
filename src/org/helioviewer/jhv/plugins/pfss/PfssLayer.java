package org.helioviewer.jhv.plugins.pfss;

import java.awt.Component;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Buf;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.layers.TimespanListener;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.plugins.pfss.data.PfssData;
import org.helioviewer.jhv.plugins.pfss.data.PfssNewDataLoader;
import org.helioviewer.jhv.threads.CancelTask;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

// has to be public for state
public class PfssLayer extends AbstractLayer implements TimespanListener {

    private static final double LINEWIDTH = 0.002;

    private final PfssLayerOptions optionsPanel;
    private final PfssLine pfssLine = new PfssLine();
    private final GLSLLine glslLine = new GLSLLine(true);
    private final Buf lineBuf = new Buf(3276 * GLSLLine.stride); // pre-allocate 64k
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

        PfssData pfssData = PfssPlugin.getPfsscache().getNearestData(Movie.getTime().milli);
        if (pfssData != null) {
            renderData(gl, vp, pfssData);
            previousPfssData = pfssData;
        }
    }

    @Override
    public void remove(GL2 gl) {
        setEnabled(false);
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

    @Nullable
    @Override
    public String getTimeString() {
        return timeString;
    }

    @Override
    public void setEnabled(boolean _enabled) {
        super.setEnabled(_enabled);

        if (enabled) {
            Movie.addTimespanListener(this);
            timespanChanged(Movie.getStartTime(), Movie.getEndTime());
        } else {
            Movie.removeTimespanListener(this);
            timeString = null;
            previousPfssData = null;
        }
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
        glslLine.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        glslLine.dispose(gl);
    }

    @Override
    public boolean isDownloading() {
        return PfssPlugin.downloads != 0;
    }

    private int lastDetail;
    private boolean lastFixedColor;
    private double lastRadius;

    private void renderData(GL2 gl, Viewport vp, PfssData data) {
        int detail = optionsPanel.getDetail();
        boolean fixedColor = optionsPanel.getFixedColor();
        double radius = optionsPanel.getRadius();

        if (data != previousPfssData || lastDetail != detail || lastFixedColor != fixedColor || lastRadius != radius) {
            lastDetail = detail;
            lastFixedColor = fixedColor;
            lastRadius = radius;

            pfssLine.calculatePositions(data, detail, fixedColor, radius, lineBuf);
            glslLine.setData(gl, lineBuf);

            timeString = data.dateObs.toString();
            ImageViewerGui.getLayers().fireTimeUpdated(this);
        }
        glslLine.render(gl, vp, LINEWIDTH);
    }

}
