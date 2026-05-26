package org.helioviewer.jhv.layers;

import java.util.HashSet;
import java.util.Set;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.ViewpointListener;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.display.ViewportMath;
import org.helioviewer.jhv.layers.stars.GaiaClient;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.opengl.Transform;

import org.json.JSONObject;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public final class StarLayer extends AbstractLayer implements ViewpointListener, GaiaClient.Receiver {

    private final Cache<Position, BufVertex> cache = Caffeine.newBuilder().softValues().build();
    private final Set<Position> pending = new HashSet<>();
    private final GLSLShape points = new GLSLShape(true);
    private BufVertex uploadedBuf;

    @Override
    public void serialize(JSONObject jo) {}

    public StarLayer(JSONObject jo) {}

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
        DisplayController.display();
    }

    @Override
    public void setStarsFailed(Position viewpoint) {
        pending.remove(viewpoint);
    }

    @Override
    public void render(MapView ctx, Viewport vp, MapScale scale) {
        if (!isVisible[vp.idx])
            return;

        Position viewpoint = ctx.viewpoint();
        BufVertex vexBuf = cache.getIfPresent(viewpoint);
        if (vexBuf == null)
            return;

        if (vexBuf != uploadedBuf) {
            points.setVertexRepeatable(vexBuf);
            uploadedBuf = vexBuf;
        }

        Transform.pushView();
        Transform.rotateViewInverse(viewpoint.toQuat()); // viewpoint was interpolated for Viewpoint->Location
        points.renderPoints(ViewportMath.getPixelFactor(vp, ctx.cameraWidth(vp)));
        Transform.popView();
    }

    @Override
    public void setEnabled(boolean _enabled) {
        super.setEnabled(_enabled);
        if (enabled) {
            DisplayController.addViewpointListener(this);
        } else {
            DisplayController.removeViewpointListener(this);
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
    public String getName() {
        return "Stars";
    }

}
