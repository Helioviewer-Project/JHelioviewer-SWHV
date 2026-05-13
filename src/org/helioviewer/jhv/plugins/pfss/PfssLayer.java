package org.helioviewer.jhv.plugins.pfss;

import java.awt.Component;

import javax.annotation.Nullable;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeListener;

import org.json.JSONObject;

public class PfssLayer extends AbstractLayer implements TimeListener.Change, TimeListener.Range { // has to be public for state

    private static final double LINEWIDTH = 2 * GLSLLine.LINEWIDTH_BASIC;

    private final PfssCache cache = PfssPlugin.getPfssCache();
    private final GLSLLine glslLine = new GLSLLine(true);
    private final BufVertex lineBuf = new BufVertex(3276 * GLSLLine.stride); // pre-allocate 64k

    private int detail = 0;
    private boolean fixedColor = false;
    private double radius = PfssSettings.MAX_RADIUS;

    private PfssLoader.Data lastData;
    private JHVTime pfssTime;
    private long currentTime;

    public PfssLayer(JSONObject jo) {
        if (jo != null) {
            detail = Math.clamp(jo.optInt("detail", detail), 0, PfssSettings.MAX_DETAIL);
            fixedColor = jo.optBoolean("fixedColor", fixedColor);
            radius = Math.clamp(jo.optDouble("radius", radius), 1.1, PfssSettings.MAX_RADIUS);
        }
    }

    @Override
    public void serialize(JSONObject jo) {
        jo.put("detail", detail);
        jo.put("fixedColor", fixedColor);
        jo.put("radius", radius);
    }

    @Override
    public void render(Camera camera, Viewport vp) {
        if (!isVisible[vp.idx])
            return;

        PfssLoader.Data data;
        if ((data = cache.getNearestData(currentTime)) == null)
            return;
        renderData(vp, data);
        lastData = data;
    }

    @Override
    public void remove() {
        setEnabled(false);
        dispose();
    }

    @Override
    public Component getOptionsPanel() {
        return null;
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
    public void init() {
        glslLine.init();
    }

    @Override
    public void dispose() {
        glslLine.dispose();
    }

    @Override
    public boolean isDownloading() {
        return cache.isDownloading();
    }

    private int lastDetail;
    private boolean lastFixedColor;
    private double lastRadius;
    private boolean lastWhiteBackground;

    private void renderData(Viewport vp, PfssLoader.Data data) {
        boolean whiteBackground = Display.whiteBackground;

        if (lastData != data || lastDetail != detail || lastFixedColor != fixedColor || lastRadius != radius || lastWhiteBackground != whiteBackground) {
            lastDetail = detail;
            lastFixedColor = fixedColor;
            lastRadius = radius;
            lastWhiteBackground = whiteBackground;

            PfssLine.calculatePositions(data, detail, fixedColor, radius, whiteBackground, lineBuf);
            glslLine.setVertex(lineBuf);

            pfssTime = data.dateObs();
            Layers.fireTimeUpdated(this);
        }
        glslLine.renderLine(vp, LINEWIDTH);
    }

    int getDetail() {
        return detail;
    }

    void setDetail(int _detail) {
        detail = _detail;
        MovieDisplay.display();
    }

    boolean getFixedColor() {
        return fixedColor;
    }

    void setFixedColor(boolean _fixedColor) {
        fixedColor = _fixedColor;
        MovieDisplay.display();
    }

    double getRadius() {
        return radius;
    }

    void setRadius(double _radius) {
        radius = _radius;
        MovieDisplay.display();
    }

}
