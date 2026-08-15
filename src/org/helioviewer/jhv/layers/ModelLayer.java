package org.helioviewer.jhv.layers;

import java.io.IOException;
import java.nio.file.Path;

import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.opengl.GLSLModel;
import org.helioviewer.jhv.opengl.model.AssimpModelLoader;
import org.helioviewer.jhv.opengl.model.ModelScene;

import org.json.JSONObject;

public final class ModelLayer extends AbstractLayer {

    private final Path path;
    private final String name;
    private final GLSLModel model;

    public ModelLayer(Path _path) throws IOException {
        path = _path.toAbsolutePath().normalize();
        ModelScene scene = AssimpModelLoader.load(path);
        name = scene.name();
        model = new GLSLModel(scene);
        setEnabled(true);
    }

    public ModelLayer(JSONObject jo) throws IOException {
        this(Path.of(jo.getString("path")));
    }

    @Override
    public void serialize(JSONObject jo) {
        jo.put("path", path.toString());
    }

    @Override
    public void render(MapView mv, Viewport vp) {
        if (isVisible[vp.idx])
            model.render(mv, vp);
    }

    @Override
    public void init() {
        model.init();
    }

    @Override
    public void dispose() {
        model.dispose();
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
    public boolean isDeletable() {
        return true;
    }

    @Override
    public boolean isLocal() {
        return true;
    }

}
