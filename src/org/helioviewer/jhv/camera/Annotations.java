package org.helioviewer.jhv.camera;

import java.util.ArrayList;

import javax.annotation.Nullable;

import org.helioviewer.jhv.camera.annotate.Annotateable;
import org.helioviewer.jhv.camera.annotate.AnnotateFOV;
import org.helioviewer.jhv.camera.annotate.AnnotationMode;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.MapContext;
import org.helioviewer.jhv.display.ProjectionScale;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;

import org.json.JSONArray;
import org.json.JSONObject;

public final class Annotations {

    private static final double LINEWIDTH = GLSLLine.LINEWIDTH_BASIC;

    private static final ArrayList<Annotateable> annotations = new ArrayList<>();
    private static final GLSLLine annotationsLine = new GLSLLine(true);
    private static final BufVertex annotationsBuf = new BufVertex(3276 * GLSLLine.stride); // pre-allocate 64kB
    private static final GLSLLine transformedLine = new GLSLLine(true);
    private static final BufVertex transformedBuf = new BufVertex(512 * GLSLLine.stride); // pre-allocate 5 FOV
    private static final GLSLShape center = new GLSLShape(true);
    private static final BufVertex centerBuf = new BufVertex(8 * GLSLShape.stride);

    private static Annotateable pending;
    private static int activeIndex = -1;

    private Annotations() {}

    static void start(Annotateable annotateable) {
        pending = annotateable;
    }

    @Nullable
    static Annotateable pending() {
        return pending;
    }

    static boolean hasPending() {
        return pending != null;
    }

    static void finishPending() {
        if (pending != null && pending.beingDragged()) {
            pending.mouseReleased();
            annotations.add(pending);
            activeIndex = annotations.size() - 1;
        }
        pending = null;
    }

    static void removeActive() {
        if (activeIndex >= 0 && activeIndex < annotations.size()) {
            annotations.remove(activeIndex);
            activeIndex = annotations.size() - 1;
        }
    }

    static boolean selectNext() {
        if (activeIndex < 0 || annotations.isEmpty())
            return false;

        activeIndex++;
        activeIndex %= annotations.size();
        return true;
    }

    static boolean selectPrevious() {
        if (activeIndex < 0 || annotations.isEmpty())
            return false;

        activeIndex--;
        if (activeIndex < 0)
            activeIndex = annotations.size() - 1;
        return true;
    }

    public static void render(MapContext ctx, Viewport vp, ProjectionScale scale) {
        if (pending == null && annotations.isEmpty())
            return;

        Annotateable activeAnnotation = activeIndex >= 0 && activeIndex < annotations.size() ? annotations.get(activeIndex) : null;

        annotations.forEach(annotation -> {
            boolean active = annotation == activeAnnotation;
            annotation.draw(ctx, vp, scale, active, annotationsBuf);
            annotation.drawTransformed(ctx, active, transformedBuf, centerBuf);
        });
        if (pending != null) {
            pending.draw(ctx, vp, scale, false, annotationsBuf);
            pending.drawTransformed(ctx, false, transformedBuf, centerBuf);
        }
        annotationsLine.setVertex(annotationsBuf);
        annotationsLine.renderLine(vp, LINEWIDTH);

        double pixFactor = CameraHelper.getPixelFactor(vp, ctx.cameraWidth(vp));

        Transform.pushView();
        if (ctx.isOrthographic())
            Transform.rotateViewInverse(ctx.viewpoint().toQuat());
        transformedLine.setVertex(transformedBuf);
        transformedLine.renderLine(vp, LINEWIDTH);
        center.setVertex(centerBuf);
        center.renderPoints(pixFactor);

        Transform.popView();
    }

    @Nullable
    public static Object getAnnotationData() {
        if (pending == null)
            return activeIndex >= 0 && activeIndex < annotations.size() ? annotations.get(activeIndex).getData() : null;
        else
            return pending.getData();
    }

    public static void zoom(Camera camera) {
        Annotateable activeAnnotation = activeIndex >= 0 && activeIndex < annotations.size() ? annotations.get(activeIndex) : null;
        if (activeAnnotation instanceof AnnotateFOV annotationFOV)
            annotationFOV.zoom(camera, Display.getActiveViewport());
    }

    public static void clear() {
        pending = null;
        annotations.clear();
        activeIndex = -1;
    }

    private static Annotateable generate(JSONObject jo) {
        return AnnotationMode.generate(jo.optString("type", AnnotationMode.Rectangle.toString()), jo);
    }

    public static JSONObject toJson() {
        JSONArray ja = new JSONArray();
        annotations.forEach(annotation -> ja.put(annotation.toJson()));
        return new JSONObject().put("activeIndex", activeIndex).put("annotateables", ja);
    }

    public static void fromJson(JSONObject jo) {
        clear();
        if (jo == null)
            return;

        JSONArray ja = jo.optJSONArray("annotateables");
        if (ja != null) {
            activeIndex = jo.optInt("activeIndex", activeIndex);
            int len = ja.length();
            for (int i = 0; i < len; i++) {
                JSONObject obj = ja.optJSONObject(i);
                if (obj != null) {
                    annotations.add(generate(obj));
                }
            }
            if (annotations.isEmpty())
                activeIndex = -1;
            else if (activeIndex < 0 || activeIndex >= annotations.size())
                activeIndex = 0;
        }
    }

    public static void init() {
        annotationsLine.init();
        transformedLine.init();
        center.init();
    }

    public static void dispose() {
        annotationsLine.dispose();
        transformedLine.dispose();
        center.dispose();
    }

}
