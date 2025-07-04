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
        treePane.render(camera, vp, gl);
    }

    @Override
    public void init(GL3 gl) {
        treePane.init(gl);
    }

    @Override
    public void dispose(GL3 gl) {
        treePane.dispose(gl);
    }

    @Override
    public void remove(GL3 gl) {
        dispose(gl);
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
