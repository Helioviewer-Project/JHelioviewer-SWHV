package org.helioviewer.jhv.layers;

import java.awt.Component;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.fov.FOVTreePane;
import org.json.JSONObject;

import com.jogamp.opengl.GL3;

public class FOVLayer extends AbstractLayer {

    private final FOVTreePane treePane;

    @Override
    public void serialize(JSONObject jo) {
        treePane.serialize(jo);
    }

    public FOVLayer(JSONObject jo) {
        treePane = new FOVTreePane(jo);
    }

    @Override
    public void render(Camera camera, Viewport vp, GL3 gl) {
        if (!isVisible[vp.idx])
            return;
        treePane.render(camera, vp);
    }

    @Override
    public void init() {
        treePane.init();
    }

    @Override
    public void dispose() {
        treePane.dispose();
    }

    @Override
    public void remove() {
        dispose();
    }

    @Override
    public Component getOptionsPanel() {
        return treePane;
    }

    @Override
    public String getName() {
        return "FOV";
    }

}
