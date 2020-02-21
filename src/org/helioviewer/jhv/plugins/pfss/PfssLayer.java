package org.helioviewer.jhv.plugins.pfss;

import java.awt.Component;

import javax.annotation.Nullable;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.layers.TimeListener;
import org.helioviewer.jhv.layers.TimespanListener;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.plugins.pfss.data.PfssData;
import org.helioviewer.jhv.plugins.pfss.data.PfssNewDataLoader;
import org.helioviewer.jhv.time.JHVTime;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class PfssLayer extends AbstractLayer implements TimeListener, TimespanListener { // has to be public for state

    private static final double LINEWIDTH = 2 * GLSLLine.LINEWIDTH_BASIC;

    private final PfssLayerOptions optionsPanel;
    private final PfssLine pfssLine = new PfssLine();
    private final GLSLLine glslLine = new GLSLLine(true);
    private final BufVertex lineBuf = new BufVertex(3276 * GLSLLine.stride); // pre-allocate 64k

    private PfssData previousPfssData;
    private JHVTime pfssTime;
    private long currentTime;

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

        PfssData pfssData = PfssPlugin.getPfsscache().getNearestData(currentTime);
        if (pfssData != null) {
            renderData(camera, vp, gl, pfssData);
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

    @Nullable
    @Override
    public String getTimeString() {
        return pfssTime == null ? null : pfssTime.toString();
    }

    @Override
    public void setEnabled(boolean _enabled) {
        super.setEnabled(_enabled);

        if (enabled) {
            Movie.addTimeListener(this);
            Movie.addTimespanListener(this);
        } else {
            Movie.removeTimeListener(this);
            Movie.removeTimespanListener(this);
            pfssTime = null;
            previousPfssData = null;
        }
    }

    @Override
    public void timeChanged(long milli) {
        currentTime = milli;
    }

    @Override
    public void timespanChanged(long start, long end) {
        PfssNewDataLoader.submit(start, end);
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

    private void renderData(Camera camera, Viewport vp, GL2 gl, PfssData data) {
        int detail = optionsPanel.getDetail();
        boolean fixedColor = optionsPanel.getFixedColor();
        double radius = optionsPanel.getRadius();

        if (data != previousPfssData || lastDetail != detail || lastFixedColor != fixedColor || lastRadius != radius) {
            lastDetail = detail;
            lastFixedColor = fixedColor;
            lastRadius = radius;

            pfssLine.calculatePositions(data, detail, fixedColor, radius, lineBuf);
            glslLine.setData(gl, lineBuf);

            pfssTime = data.dateObs;
            JHVFrame.getLayers().fireTimeUpdated(this);
        }

        Transform.pushView();
        CameraHelper.rotate2Carrington(camera.getViewpoint());
        glslLine.render(gl, vp.aspect, LINEWIDTH);
        Transform.popView();
    }

}
