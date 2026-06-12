package org.helioviewer.jhv.plugins.pfss;

import javax.annotation.Nullable;

import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.movie.Movie;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeListener;

import org.json.JSONObject;

public class PfssLayer extends AbstractLayer implements TimeListener.Range { // has to be public for state

    private static final double LINEWIDTH = 2 * GLSLLine.LINEWIDTH_BASIC;

    private final PfssCache cache = PfssPlugin.getPfssCache();
    private final GLSLLine glslLine = new GLSLLine(true);

    private int detail = 0;
    private boolean fixedColor = false;
    private double radius = PfssSettings.MAX_RADIUS;

    private JHVTime pfssTime;

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

    private final PfssLineWorker lineWorker = new PfssLineWorker(this::lineReady);
    private PfssLineWorker.Parameters uploadedParameters;
    private PfssLineWorker.Line readyLine;

    @Override
    public void render(MapView mv, Viewport vp) {
        if (!isVisible[vp.idx])
            return;

        PfssLoader.Data data;
        if ((data = cache.getNearestData(mv.viewpoint().time.milli)) == null)
            return;

        PfssLineWorker.Parameters parameters = new PfssLineWorker.Parameters(data, detail, fixedColor, radius, Display.whiteBackground);
        uploadReadyLine(parameters);

        if (!parameters.equals(uploadedParameters))
            lineWorker.submit(parameters);

        if (uploadedParameters != null)
            glslLine.renderLine(vp, LINEWIDTH);
    }

    private void lineReady(PfssLineWorker.Line line) {
        readyLine = line;
        DisplayController.display();
    }

    private void uploadReadyLine(PfssLineWorker.Parameters parameters) {
        if (readyLine == null)
            return;

        if (readyLine.parameters().equals(parameters)) {
            glslLine.setVertexRepeatable(readyLine.vertices());
            uploadedParameters = readyLine.parameters();
            pfssTime = uploadedParameters.data().dateObs();
            Layers.fireTimeUpdated(this);
        }
        readyLine = null;
    }

    @Override
    public void remove() {
        setEnabled(false);
        dispose();
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
            Movie.addTimeRangeListener(this);
        } else {
            Movie.removeTimeRangeListener(this);
            clearLineWorker();
        }
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
        clearLineWorker();
        glslLine.dispose();
    }

    private void clearLineWorker() {
        readyLine = null;
        uploadedParameters = null;
        pfssTime = null;
        lineWorker.cancel();
    }

    @Override
    public boolean isDownloading() {
        return cache.isDownloading();
    }

    int getDetail() {
        return detail;
    }

    void setDetail(int _detail) {
        detail = _detail;
        DisplayController.display();
    }

    boolean getFixedColor() {
        return fixedColor;
    }

    void setFixedColor(boolean _fixedColor) {
        fixedColor = _fixedColor;
        DisplayController.display();
    }

    double getRadius() {
        return radius;
    }

    void setRadius(double _radius) {
        radius = _radius;
        DisplayController.display();
    }
}
