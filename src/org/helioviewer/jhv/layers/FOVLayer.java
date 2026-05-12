package org.helioviewer.jhv.layers;

import java.awt.Component;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.fov.FOVCatalog;
import org.helioviewer.jhv.layers.fov.FOVTreePane;

import org.json.JSONObject;

public class FOVLayer extends AbstractLayer {

    private final FOVCatalog catalog;
    private FOVTreePane treePane;

    @Override
    public void serialize(JSONObject jo) {
        catalog.serialize(jo);
    }

    public FOVLayer(JSONObject jo) {
        catalog = new FOVCatalog(jo);
    }

    @Override
    public void render(Camera camera, Viewport vp) {
        if (!isVisible[vp.idx])
            return;
        catalog.render(camera, vp);
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
    public Component getOptionsPanel() {
        if (treePane == null)
            treePane = new FOVTreePane(catalog);
        return treePane;
    }

    @Override
    public String getName() {
        return "FOV";
    }

}
