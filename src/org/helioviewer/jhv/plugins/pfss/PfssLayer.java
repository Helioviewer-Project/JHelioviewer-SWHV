package org.helioviewer.jhv.plugins.pfss;

import java.awt.Component;

import javax.annotation.Nullable;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeListener;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class PfssLayer extends AbstractLayer implements TimeListener.Change, TimeListener.Range { // has to be public for state

    private static final double LINEWIDTH = 2 * GLSLLine.LINEWIDTH_BASIC;

    private final PfssLayerOptions optionsPanel;
    private final GLSLLine glslLine = new GLSLLine(true);
    private final BufVertex lineBuf = new BufVertex(3276 * GLSLLine.stride); // pre-allocate 64k

    private PfssLoader.Data lastData;
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

        PfssLoader.Data data = PfssPlugin.getPfsscache().getNearestData(currentTime);
        if (data != null) {
            renderData(gl, vp, data);
            lastData = data;
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
            Movie.addTimeRangeListener(this);
        } else {
            Movie.removeTimeListener(this);
            Movie.removeTimeRangeListener(this);
            pfssTime = null;
            lastData = null;
        }
    }

    @Override
    public void timeChanged(long milli) {
        currentTime = milli;
    }

    @Override
    public void timeRangeChanged(long start, long end) {
        PfssLoader.submitList(start, end);
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

    private void renderData(GL2 gl, Viewport vp, PfssLoader.Data data) {
        int detail = optionsPanel.getDetail();
        boolean fixedColor = optionsPanel.getFixedColor();
        double radius = optionsPanel.getRadius();

        if (lastData != data || lastDetail != detail || lastFixedColor != fixedColor || lastRadius != radius) {
            lastDetail = detail;
            lastFixedColor = fixedColor;
            lastRadius = radius;

            PfssLine.calculatePositions(data, detail, fixedColor, radius, lineBuf);
            glslLine.setVertex(gl, lineBuf);

            pfssTime = data.dateObs();
            JHVFrame.getLayers().fireTimeUpdated(this);
        }
        glslLine.renderLine(gl, vp.aspect, LINEWIDTH);
    }

}
