package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

public class MapContext {

    protected final Camera camera;
    protected final Viewport vp;
    protected final GridType gridType;

    // Compatibility fields to support step-by-step migration of legacy callers
    private final Position tempViewpoint;
    private final ProjectionScale tempScale;
    private final Quat tempRotation;

    @Deprecated
    public MapContext(Position viewpoint, Viewport vp, ProjectionScale scale, GridType gridType) {
        this.camera = null;
        this.vp = vp;
        this.gridType = gridType;
        this.tempViewpoint = viewpoint;
        this.tempScale = scale;
        this.tempRotation = scale.isOrtho() ? null : gridType.mapRotation(viewpoint);
    }

    protected MapContext(Camera camera, Viewport vp, GridType gridType) {
        this.camera = camera;
        this.vp = vp;
        this.gridType = gridType;
        this.tempViewpoint = null;
        this.tempScale = null;
        this.tempRotation = null;
    }

    public Camera camera() {
        return camera;
    }

    public Viewport vp() {
        return vp;
    }

    public GridType gridType() {
        return gridType;
    }

    public Position viewpoint() {
        return camera != null ? camera.getViewpoint() : tempViewpoint;
    }

    public Quat rotation() {
        return tempRotation;
    }

    public ProjectionScale scale() {
        return tempScale;
    }

    public boolean isOrthographic() {
        return tempScale == ProjectionScale.ortho;
    }

    public Vec2 projectToScreen(Vec3 v) {
        return Display.mode.projectToScreen(this, v);
    }

    public Vec3 mouseToSurface(int x, int y) {
        return Display.mode.mouseToSurface(camera, vp, gridType, x, y);
    }

    public Vec2 mouseToGrid(int x, int y) {
        return Display.mode.mouseToGrid(camera, vp, gridType, x, y);
    }

    public Vec2 mouseToScreen(int x, int y) {
        return Display.mode.mouseToScreen(camera, this, x, y);
    }

    public Vec2 emitMapVertex(Vec3 vertex, Vec2 previous, boolean first, boolean last, double radius, byte[] color, BufVertex vexBuf) {
        return Display.mode.emitMapVertex(this, vertex, previous, first, last, radius, color, vexBuf);
    }

    public void emitMapPoint(Vec3 vertex, double size, double radius, byte[] color, BufVertex vexBuf) {
        Display.mode.emitMapPoint(this, vertex, size, radius, color, vexBuf);
    }
}
