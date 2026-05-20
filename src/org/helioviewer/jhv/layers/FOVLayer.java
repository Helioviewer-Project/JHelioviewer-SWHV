package org.helioviewer.jhv.layers;

import org.helioviewer.jhv.display.MapContext;
import org.helioviewer.jhv.layers.fov.FOVCatalog;

import org.json.JSONObject;

public class FOVLayer extends AbstractLayer {

    private final FOVCatalog catalog;

    @Override
    public void serialize(JSONObject jo) {
        catalog.serialize(jo);
    }

    public FOVLayer(JSONObject jo) {
        catalog = new FOVCatalog(jo);
    }

    @Override
    public void render(MapContext ctx) {
        if (!isVisible[ctx.vp().idx])
            return;
        catalog.render(ctx);
    }

    @Override
    public void init() {
        catalog.init();
    }

    @Override
    public void dispose() {
        catalog.dispose();
    }

    @Override
    public void remove() {
        dispose();
    }

    @Override
    public String getName() {
        return "FOV";
    }

    public FOVCatalog getCatalog() {
        return catalog;
    }

}
