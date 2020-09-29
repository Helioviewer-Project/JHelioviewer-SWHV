package org.helioviewer.jhv.layers;

import java.awt.Component;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Spice;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.fov.FOVTreePane;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.FOVShape;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.text.JhvTextRenderer;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class FOVLayer extends AbstractLayer {

    private static final double LINEWIDTH_FOV = GLSLLine.LINEWIDTH_BASIC;

    private final FOVShape fov = new FOVShape();
    private final byte[] fovColor = Colors.Blue;
    private final GLSLLine fovLine = new GLSLLine(true);
    private final BufVertex lineBuf = new BufVertex((4 * (FOVShape.RECT_SUBDIVS + 1) + 2) * GLSLLine.stride);
    private final GLSLShape center = new GLSLShape(true);
    private final BufVertex centerBuf = new BufVertex(GLSLShape.stride);

    private final FOVTreePane treePane = new FOVTreePane();

    @Override
    public void serialize(JSONObject jo) {
    }

    public FOVLayer(JSONObject jo) {
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;
        if (!treePane.hasEnabled())
            return;

        double pixFactor = CameraHelper.getPixelFactor(camera, vp);
        Position viewpoint = camera.getViewpoint();

        Position fovObserver = Spice.getCarrington("SUN", "SOLO", viewpoint.time);
        if (fovObserver == null)
            return;
        double distance = fovObserver.distance;

        Transform.pushView();
        Transform.rotateViewInverse(fovObserver.toQuat());

        boolean far = Camera.useWideProjection(distance);
        if (far) {
            Transform.pushProjection();
            camera.projectionOrthoWide(vp.aspect);
        }

        JhvTextRenderer renderer = GLText.getRenderer(48);
        renderer.setColor(Colors.WhiteFloat);
        renderer.begin3DRendering();
        renderer.setSurfacePut();

        treePane.putFOV(fov, distance, lineBuf, centerBuf, renderer);

        renderer.setDirectPut();
        renderer.end3DRendering();

        center.setData(gl, centerBuf);
        center.renderPoints(gl, pixFactor);

        fovLine.setData(gl, lineBuf);
        fovLine.render(gl, vp.aspect, LINEWIDTH_FOV);

        if (far) {
            Transform.popProjection();
        }
        Transform.popView();
    }

    @Override
    public void renderFloat(Camera camera, Viewport vp, GL2 gl) {
    }

    @Override
    public void init(GL2 gl) {
        fovLine.init(gl);
        center.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        fovLine.dispose(gl);
        center.dispose(gl);
    }

    @Override
    public void remove(GL2 gl) {
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

    @Nullable
    @Override
    public String getTimeString() {
        return null;
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

}
