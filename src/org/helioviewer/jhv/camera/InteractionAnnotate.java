package org.helioviewer.jhv.camera;

import java.util.ArrayList;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.camera.annotate.AnnotateFOV;
import org.helioviewer.jhv.camera.annotate.Annotateable;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.MapContext;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.input.KeyInputEvent;
import org.helioviewer.jhv.input.PointerEvent;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;

import org.json.JSONArray;
import org.json.JSONObject;

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

    void draw(Viewport vp) {
        if (newAnnotateable == null && anns.isEmpty())
            return;

        Annotateable activeAnn = activeIndex >= 0 && activeIndex < anns.size() ? anns.get(activeIndex) : null;

        Position viewpoint = camera.getViewpoint();
        MapContext ctx = new MapContext(viewpoint, vp, Display.gridType);
        anns.forEach(annotateable -> {
            boolean active = annotateable == activeAnn;
            annotateable.draw(ctx, active, annsBuf);
            annotateable.drawTransformed(active, transBuf, centerBuf);
        });
        if (newAnnotateable != null) {
            newAnnotateable.draw(ctx, false, annsBuf);
            newAnnotateable.drawTransformed(false, transBuf, centerBuf);
        }
        annsLine.setVertex(annsBuf);
        annsLine.renderLine(vp, LINEWIDTH);

        double pixFactor = CameraHelper.getPixelFactor(camera, vp);

        Transform.pushView();
        if (Display.mode.isOrthographic())
            Transform.rotateViewInverse(viewpoint.toQuat());
        transLine.setVertex(transBuf);
        transLine.renderLine(vp, LINEWIDTH);
        center.setVertex(centerBuf);
        center.renderPoints(pixFactor);

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
            annFOV.zoom(camera, Display.getActiveViewport());
    }

    @Override
    public void mousePressed(PointerEvent e, Viewport vp, Interaction.AnnotationMode annotationMode) {
        newAnnotateable = annotationMode.generate(null);
        newAnnotateable.mousePressed(camera, vp, e.x(), e.y());
        if (!newAnnotateable.isDraggable()) {
            finishAnnotateable();
        }
        MovieDisplay.display();
    }

    @Override
    public void mouseDragged(PointerEvent e, Viewport vp) {
        if (newAnnotateable != null && newAnnotateable.isDraggable()) {
            newAnnotateable.mouseDragged(camera, vp, e.x(), e.y());
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

    boolean hasPendingAnnotateable() {
        return newAnnotateable != null;
    }

    @Override
    public void mouseReleased(PointerEvent e) {
        finishAnnotateable();
    }

    @Override
    public void keyPressed(KeyInputEvent e) {
        if (e.key() == KeyInputEvent.Key.BACKSPACE || e.key() == KeyInputEvent.Key.DELETE) {
            remove();
            MovieDisplay.display();
        } else if (e.key() == KeyInputEvent.Key.N && activeIndex >= 0 && !anns.isEmpty()) {
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
        return Interaction.AnnotationMode.generate(jo.optString("type", Interaction.AnnotationMode.Rectangle.toString()), jo);
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
            if (anns.isEmpty())
                activeIndex = -1;
            else if (activeIndex < 0 || activeIndex >= anns.size())
                activeIndex = 0;
        }
    }

    void init() {
        annsLine.init();
        transLine.init();
        center.init();
    }

    void dispose() {
        annsLine.dispose();
        transLine.dispose();
        center.dispose();
    }

}
