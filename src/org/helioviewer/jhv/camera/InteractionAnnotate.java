package org.helioviewer.jhv.camera;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import java.util.ArrayList;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.camera.annotate.AnnotateFOV;
import org.helioviewer.jhv.camera.annotate.AnnotateRectangle;
import org.helioviewer.jhv.camera.annotate.Annotateable;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.json.JSONArray;
import org.json.JSONObject;

import com.jogamp.opengl.GL3;

class InteractionAnnotate implements Interaction.Type {

    private final Camera camera;
    private final ArrayList<Annotateable> anns = new ArrayList<>();

    private static final double LINEWIDTH = GLSLLine.LINEWIDTH_BASIC;
    private final GLSLLine annsLine = new GLSLLine(true);
    private final BufVertex annsBuf = new BufVertex(3276 * GLSLLine.stride); // pre-allocate 64kB
    private final GLSLLine transLine = new GLSLLine(true);
    private final BufVertex transBuf = new BufVertex(512 * GLSLLine.stride); // pre-allocate 5 FOV
    private final GLSLShape center = new GLSLShape(true);
    private final BufVertex centerBuf = new BufVertex(8 * GLSLShape.stride);

    private Annotateable newAnnotateable = null;
    private int activeIndex = -1;

    InteractionAnnotate(Camera _camera) {
        camera = _camera;
    }

    private void remove() {
        if (activeIndex >= 0 && activeIndex < anns.size()) {
            anns.remove(activeIndex);
            activeIndex = anns.size() - 1;
        }
    }

    void draw(Viewport vp, GL3 gl) {
        if (newAnnotateable == null && anns.isEmpty())
            return;

        Annotateable activeAnn = activeIndex >= 0 && activeIndex < anns.size() ? anns.get(activeIndex) : null;

        Position viewpoint = camera.getViewpoint();
        Quat q = Display.gridType.toGrid(viewpoint);
        anns.forEach(annotateable -> {
            boolean active = annotateable == activeAnn;
            annotateable.draw(q, vp, active, annsBuf);
            annotateable.drawTransformed(active, transBuf, centerBuf);
        });
        if (newAnnotateable != null) {
            newAnnotateable.draw(q, vp, false, annsBuf);
            newAnnotateable.drawTransformed(false, transBuf, centerBuf);
        }
        annsLine.setVertex(gl, annsBuf);
        annsLine.renderLine(gl, vp.aspect, LINEWIDTH);

        double pixFactor = CameraHelper.getPixelFactor(camera, vp);

        Transform.pushView();
        Transform.rotateViewInverse(viewpoint.toQuat());

        transLine.setVertex(gl, transBuf);
        transLine.renderLine(gl, vp.aspect, LINEWIDTH);
        center.setVertex(gl, centerBuf);
        center.renderPoints(gl, pixFactor);

        Transform.popView();
    }

    @Nullable
    Object getAnnotationData() {
        if (newAnnotateable == null)
            return activeIndex >= 0 && activeIndex < anns.size() ? anns.get(activeIndex).getData() : null;
        else
            return newAnnotateable.getData();
    }

    void zoom() {
        Annotateable activeAnn = activeIndex >= 0 && activeIndex < anns.size() ? anns.get(activeIndex) : null;
        if (activeAnn instanceof AnnotateFOV annFOV)
            annFOV.zoom(camera);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        newAnnotateable = JHVFrame.getInteraction().getAnnotationMode().generate(null);
        newAnnotateable.mousePressed(camera, e.getX(), e.getY());
        if (!newAnnotateable.isDraggable()) {
            finishAnnotateable();
        }
        MovieDisplay.display();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (newAnnotateable != null && newAnnotateable.isDraggable()) {
            newAnnotateable.mouseDragged(camera, e.getX(), e.getY());
            MovieDisplay.display();
        }
    }

    private void finishAnnotateable() {
        if (newAnnotateable != null && newAnnotateable.beingDragged()) {
            newAnnotateable.mouseReleased();
            anns.add(newAnnotateable);
            activeIndex = anns.size() - 1;
        }
        newAnnotateable = null;
        MovieDisplay.display();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        finishAnnotateable();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_DELETE) {
            remove();
            MovieDisplay.display();
        } else if (code == KeyEvent.VK_N && activeIndex >= 0) {
            activeIndex++;
            activeIndex %= anns.size();
            MovieDisplay.display();
        }
    }

    void clear() {
        newAnnotateable = null;
        anns.clear();
        activeIndex = -1;
    }

    private static Annotateable generate(JSONObject jo) {
        try {
            return Interaction.AnnotationMode.valueOf(jo.getString("type")).generate(jo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new AnnotateRectangle(jo);
    }

    JSONObject toJson() {
        JSONArray ja = new JSONArray();
        anns.forEach(annotateable -> ja.put(annotateable.toJson()));
        return new JSONObject().put("activeIndex", activeIndex).put("annotateables", ja);
    }

    void fromJson(JSONObject jo) {
        clear();
        if (jo == null)
            return;

        JSONArray ja = jo.optJSONArray("annotateables");
        if (ja != null) {
            activeIndex = jo.optInt("activeIndex", activeIndex);
            int len = ja.length();
            for (int i = 0; i < len; i++) {
                anns.add(generate(ja.getJSONObject(i)));
            }
        }
    }

    void init(GL3 gl) {
        annsLine.init(gl);
        transLine.init(gl);
        center.init(gl);
    }

    void dispose(GL3 gl) {
        annsLine.dispose(gl);
        transLine.dispose(gl);
        center.dispose(gl);
    }

}
