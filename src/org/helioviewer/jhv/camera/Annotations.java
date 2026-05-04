package org.helioviewer.jhv.camera;

import java.util.ArrayList;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.camera.annotate.AnnotateFOV;
import org.helioviewer.jhv.camera.annotate.Annotateable;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.MapContext;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;

import org.json.JSONArray;
import org.json.JSONObject;

final class Annotations {

    private static final double LINEWIDTH = GLSLLine.LINEWIDTH_BASIC;

    private final ArrayList<Annotateable> annotations = new ArrayList<>();
    private final GLSLLine annotationsLine = new GLSLLine(true);
    private final BufVertex annotationsBuf = new BufVertex(3276 * GLSLLine.stride); // pre-allocate 64kB
    private final GLSLLine transformedLine = new GLSLLine(true);
    private final BufVertex transformedBuf = new BufVertex(512 * GLSLLine.stride); // pre-allocate 5 FOV
    private final GLSLShape center = new GLSLShape(true);
    private final BufVertex centerBuf = new BufVertex(8 * GLSLShape.stride);

    private Annotateable pending;
    private int activeIndex = -1;

    void start(Annotateable annotateable) {
        pending = annotateable;
    }

    @Nullable
    Annotateable pending() {
        return pending;
    }

    boolean hasPending() {
        return pending != null;
    }

    void finishPending() {
        if (pending != null && pending.beingDragged()) {
            pending.mouseReleased();
            annotations.add(pending);
            activeIndex = annotations.size() - 1;
        }
        pending = null;
    }

    void removeActive() {
        if (activeIndex >= 0 && activeIndex < annotations.size()) {
            annotations.remove(activeIndex);
            activeIndex = annotations.size() - 1;
        }
    }

    boolean selectNext() {
        if (activeIndex < 0 || annotations.isEmpty())
            return false;

        activeIndex++;
        activeIndex %= annotations.size();
        return true;
    }

    void render(Camera camera, Viewport vp) {
        if (pending == null && annotations.isEmpty())
            return;

        Annotateable activeAnnotation = activeIndex >= 0 && activeIndex < annotations.size() ? annotations.get(activeIndex) : null;

        Position viewpoint = camera.getViewpoint();
        MapContext ctx = new MapContext(viewpoint, vp, Display.gridType);
        annotations.forEach(annotation -> {
            boolean active = annotation == activeAnnotation;
            annotation.draw(ctx, active, annotationsBuf);
            annotation.drawTransformed(active, transformedBuf, centerBuf);
        });
        if (pending != null) {
            pending.draw(ctx, false, annotationsBuf);
            pending.drawTransformed(false, transformedBuf, centerBuf);
        }
        annotationsLine.setVertex(annotationsBuf);
        annotationsLine.renderLine(vp, LINEWIDTH);

        double pixFactor = CameraHelper.getPixelFactor(camera, vp);

        Transform.pushView();
        if (Display.mode.isOrthographic())
            Transform.rotateViewInverse(viewpoint.toQuat());
        transformedLine.setVertex(transformedBuf);
        transformedLine.renderLine(vp, LINEWIDTH);
        center.setVertex(centerBuf);
        center.renderPoints(pixFactor);

        Transform.popView();
    }

    @Nullable
    Object getAnnotationData() {
        if (pending == null)
            return activeIndex >= 0 && activeIndex < annotations.size() ? annotations.get(activeIndex).getData() : null;
        else
            return pending.getData();
    }

    void zoom(Camera camera) {
        Annotateable activeAnnotation = activeIndex >= 0 && activeIndex < annotations.size() ? annotations.get(activeIndex) : null;
        if (activeAnnotation instanceof AnnotateFOV annotationFOV)
            annotationFOV.zoom(camera, Display.getActiveViewport());
    }

    void clear() {
        pending = null;
        annotations.clear();
        activeIndex = -1;
    }

    private static Annotateable generate(JSONObject jo) {
        return Interaction.AnnotationMode.generate(jo.optString("type", Interaction.AnnotationMode.Rectangle.toString()), jo);
    }

    JSONObject toJson() {
        JSONArray ja = new JSONArray();
        annotations.forEach(annotation -> ja.put(annotation.toJson()));
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
                annotations.add(generate(ja.getJSONObject(i)));
            }
            if (annotations.isEmpty())
                activeIndex = -1;
            else if (activeIndex < 0 || activeIndex >= annotations.size())
                activeIndex = 0;
        }
    }

    void init() {
        annotationsLine.init();
        transformedLine.init();
        center.init();
    }

    void dispose() {
        annotationsLine.dispose();
        transformedLine.dispose();
        center.dispose();
    }

}
