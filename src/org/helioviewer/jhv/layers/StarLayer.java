package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.util.HashSet;
import java.util.Set;

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

public final class StarLayer extends AbstractLayer implements Camera.Listener, GaiaClient.Receiver {

    private final Cache<Position, BufVertex> cache = Caffeine.newBuilder().softValues().build();
    private final Set<Position> pending = new HashSet<>();
    private final GLSLShape points = new GLSLShape(true);
    private BufVertex uploadedBuf;

    @Override
    public void serialize(JSONObject jo) {
    }

    public StarLayer(JSONObject jo) {
    }

    @Override
    public void viewpointChanged(Position viewpoint) {
        if (cache.getIfPresent(viewpoint) != null) // avoid repeated calls
            return;
        if (!pending.add(viewpoint))
            return;
        GaiaClient.submitSearch(this, viewpoint);
    }

    @Override
    public void setStars(Position viewpoint, BufVertex pointsBuf) {
        pending.remove(viewpoint);
        cache.put(viewpoint, pointsBuf);
        MovieDisplay.display();
    }

    @Override
    public void setStarsFailed(Position viewpoint) {
        pending.remove(viewpoint);
    }

    @Override
    public void render(Camera camera, Viewport vp) {
        if (!isVisible[vp.idx])
            return;

        Position viewpoint = camera.getViewpoint();
        BufVertex vexBuf = cache.getIfPresent(viewpoint);
        if (vexBuf == null)
            return;

        if (vexBuf != uploadedBuf) {
            points.setVertexRepeatable(vexBuf);
            uploadedBuf = vexBuf;
        }

        Transform.pushView();
        Transform.rotateViewInverse(viewpoint.toQuat()); // viewpoint was interpolated for Viewpoint->Location
        points.renderPoints(CameraHelper.getPixelFactor(camera, vp));
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
    public void init() {
        points.init();
    }

    @Override
    public void dispose() {
        uploadedBuf = null;
        points.dispose();
    }

    @Override
    public void remove() {
        setEnabled(false);
        dispose();
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
