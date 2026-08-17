package org.helioviewer.jhv.layers;

import java.io.IOException;
import java.nio.file.Path;

import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.opengl.GLSLVolume;
import org.helioviewer.jhv.opengl.volume.FitsVolumeLoader;
import org.helioviewer.jhv.opengl.volume.VolumeData;

import org.json.JSONObject;

public final class VolumeLayer extends AbstractLayer {

    private final Path path;
    private final VolumeData data;
    private final GLSLVolume volume;

    public VolumeLayer(Path _path) throws IOException {
        path = _path.toAbsolutePath().normalize();
        data = FitsVolumeLoader.load(path);
        volume = new GLSLVolume(data);
        setEnabled(true);
    }

    public VolumeLayer(JSONObject jo) throws IOException {
        this(Path.of(jo.getString("path")));
    }

    @Override
    public void serialize(JSONObject jo) {
        jo.put("path", path.toString());
    }

    @Override
    public void render(MapView mv, Viewport vp) {
        if (isVisible[vp.idx] && mv.isOrthographic())
            volume.render();
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
        return data.name();
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
