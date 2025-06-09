package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.util.Optional;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.camera.Transform;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.stars.GaiaClient;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.json.JSONObject;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jogamp.opengl.GL3;

public final class StarLayer extends AbstractLayer implements Camera.Listener, GaiaClient.Receiver {

    private final Cache<Position, Optional<BufVertex>> cache = Caffeine.newBuilder().softValues().build();
    private final GLSLShape points = new GLSLShape(true);

    @Override
    public void serialize(JSONObject jo) {
    }

    public StarLayer(JSONObject jo) {
    }

    @Override
    public void viewpointChanged(Position viewpoint) {
        if (cache.getIfPresent(viewpoint) != null) // avoid repeated calls
            return;
        cache.put(viewpoint, Optional.empty()); // promise
        GaiaClient.submitSearch(this, viewpoint);
    }

    @Override
    public void setStars(Position viewpoint, BufVertex pointsBuf) {
        cache.put(viewpoint, Optional.of(pointsBuf));
        MovieDisplay.display();
    }

    @Override
    public void render(Camera camera, Viewport vp, GL3 gl) {
        if (!isVisible[vp.idx])
            return;

        Position viewpoint = camera.getViewpoint();
        Optional<BufVertex> optBuf = cache.getIfPresent(viewpoint);
        if (optBuf == null || optBuf.isEmpty())
            return;

        points.setVertexRepeatable(gl, optBuf.get());

        Transform.pushView();
        Transform.rotateViewInverse(viewpoint.toQuat()); // viewpoint was interpolated for Viewpoint->Location
        points.renderPoints(gl, CameraHelper.getPixelFactor(camera, vp));
        Transform.popView();
    }

    @Override
    public void setEnabled(boolean _enabled) {
        super.setEnabled(_enabled);
        if (enabled) {
            Display.getCamera().addListener(this);
        } else {
            Display.getCamera().removeListener(this);
        }
    }

    @Override
    public void init(GL3 gl) {
        points.init(gl);
    }

    @Override
    public void dispose(GL3 gl) {
        points.dispose(gl);
    }

    @Override
    public void remove(GL3 gl) {
        dispose(gl);
    }

    @Override
    public Component getOptionsPanel() {
        return null;
    }

    @Override
    public String getName() {
        return "Stars";
    }

}
