package org.helioviewer.jhv.layers;

import java.io.IOException;
import java.net.URI;

import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.io.NetFileCache;
import org.helioviewer.jhv.opengl.GLSLModel;
import org.helioviewer.jhv.opengl.model.AssimpModelLoader;
import org.helioviewer.jhv.opengl.model.ModelScene;
import org.helioviewer.jhv.time.JHVTime;

import org.json.JSONObject;

public final class ModelLayer extends AbstractLayer {

    private final URI source;
    private final String name;
    private final JHVTime time;
    private final GLSLModel model;

    public ModelLayer(URI uri) throws IOException {
        this(NetFileCache.get(uri));
    }

    private ModelLayer(DataUri data) throws IOException {
        source = data.sourceUri();
        ModelScene scene = AssimpModelLoader.load(data);
        name = scene.name();
        time = scene.time();
        model = new GLSLModel(scene);
        setEnabled(true);
    }

    public ModelLayer(JSONObject jo) throws IOException {
        this(URI.create(jo.getString("uri")));
    }

    @Override
    public void serialize(JSONObject jo) {
        jo.put("uri", source.toString());
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
        return "file".equalsIgnoreCase(source.getScheme());
    }

}
