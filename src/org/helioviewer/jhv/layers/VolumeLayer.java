package org.helioviewer.jhv.layers;

import java.io.IOException;
import java.nio.file.Path;

import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.image.lut.LUT;
import org.helioviewer.jhv.opengl.GLSLVolume;
import org.helioviewer.jhv.opengl.volume.FitsVolumeLoader;
import org.helioviewer.jhv.opengl.volume.VolumeData;
import org.helioviewer.jhv.time.JHVTime;

import org.json.JSONObject;

public final class VolumeLayer extends AbstractLayer {

    private final Path path;
    private final String name;
    private final JHVTime time;
    private final GLSLVolume volume;
    private double opacity = 1;
    private LUT lut = LUT.gray();
    private final float[] cropMin = {0, 0, 0};
    private final float[] cropMax = {1, 1, 1};

    public VolumeLayer(Path _path) throws IOException {
        path = _path.toAbsolutePath().normalize();
        VolumeData data = FitsVolumeLoader.load(path);
        name = data.name();
        time = data.time();
        volume = new GLSLVolume(data);
        setEnabled(true);
    }

    public VolumeLayer(JSONObject jo) throws IOException {
        this(Path.of(jo.getString("path")));
        opacity = Math.clamp(jo.optDouble("opacity", opacity), 0, 1);
        LUT configured = LUT.get(jo.optString("colormap", lut.name()));
        if (configured != null)
            lut = configured;
        for (int axis = 0; axis < 3; axis++) {
            int number = axis + 1;
            setCrop(axis, jo.optDouble("cropMin" + number, 0), jo.optDouble("cropMax" + number, 1));
        }
    }

    @Override
    public void serialize(JSONObject jo) {
        jo.put("path", path.toString());
        jo.put("opacity", opacity);
        jo.put("colormap", lut.name());
        for (int axis = 0; axis < 3; axis++) {
            int number = axis + 1;
            jo.put("cropMin" + number, cropMin[axis]);
            jo.put("cropMax" + number, cropMax[axis]);
        }
    }

    @Override
    public void render(MapView mv, Viewport vp) {
        if (isVisible[vp.idx] && mv.isOrthographic() && opacity > 0)
            volume.render(lut, opacity, cropMin, cropMax);
    }

    public double getOpacity() {
        return opacity;
    }

    public void setOpacity(double _opacity) {
        opacity = Math.clamp(_opacity, 0, 1);
    }

    public LUT getLUT() {
        return lut;
    }

    public void setLUT(LUT _lut) {
        lut = _lut == null ? LUT.gray() : _lut;
    }

    public double getCropMin(int axis) {
        return cropMin[axis];
    }

    public double getCropMax(int axis) {
        return cropMax[axis];
    }

    public void setCrop(int axis, double minimum, double maximum) {
        minimum = Math.clamp(minimum, 0, 1);
        maximum = Math.clamp(maximum, 0, 1);
        cropMin[axis] = (float) Math.min(minimum, maximum);
        cropMax[axis] = (float) Math.max(minimum, maximum);
    }

    @Override
    public void init() {
        volume.init();
    }

    @Override
    public void dispose() {
        volume.dispose();
    }

    @Override
    public void remove() {
        dispose();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getTimeString() {
        return time.toString();
    }

    @Override
    public boolean isDeletable() {
        return true;
    }

    @Override
    public boolean isLocal() {
        return true;
    }

}
