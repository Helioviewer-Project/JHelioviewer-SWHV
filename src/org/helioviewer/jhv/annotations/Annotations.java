package org.helioviewer.jhv.annotations;

import java.util.ArrayList;

import javax.annotation.Nullable;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.display.ViewportMath;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.opengl.Transform;

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

    public static void start(Annotateable annotateable) {
        pending = annotateable;
    }

    @Nullable
    public static Annotateable pending() {
        return pending;
    }

    public static boolean hasPending() {
        return pending != null;
    }

    public static void finishPending() {
        if (pending != null && pending.beingDragged()) {
            pending.mouseReleased();
            annotations.add(pending);
            activeIndex = annotations.size() - 1;
        }
        pending = null;
    }

    public static void removeActive() {
        if (activeIndex >= 0 && activeIndex < annotations.size()) {
            annotations.remove(activeIndex);
            activeIndex = annotations.size() - 1;
        }
    }

    public static boolean selectNext() {
        if (activeIndex < 0 || annotations.isEmpty())
            return false;

        activeIndex++;
        activeIndex %= annotations.size();
        return true;
    }

    public static boolean selectPrevious() {
        if (activeIndex < 0 || annotations.isEmpty())
            return false;

        activeIndex--;
        if (activeIndex < 0)
            activeIndex = annotations.size() - 1;
        return true;
    }

    public static void render(MapView mv, Viewport vp, MapScale scale) {
        if (pending == null && annotations.isEmpty())
            return;

        Annotateable activeAnnotation = activeIndex >= 0 && activeIndex < annotations.size() ? annotations.get(activeIndex) : null;

        annotations.forEach(annotation -> {
            boolean active = annotation == activeAnnotation;
            annotation.draw(mv, vp, scale, active, annotationsBuf);
            annotation.drawTransformed(mv, active, transformedBuf, centerBuf);
        });
        if (pending != null) {
            pending.draw(mv, vp, scale, false, annotationsBuf);
            pending.drawTransformed(mv, false, transformedBuf, centerBuf);
        }
        annotationsLine.setVertex(annotationsBuf);
        annotationsLine.renderLine(vp, LINEWIDTH);

        double pixFactor = ViewportMath.getPixelFactor(vp, mv.cameraWidth(vp));

        Transform.pushView();
        if (mv.isOrthographic())
            Transform.rotateViewInverse(mv.viewpoint().toQuat());
        transformedLine.setVertex(transformedBuf);
        transformedLine.renderLine(vp, LINEWIDTH);
        center.setVertex(centerBuf);
        center.renderPoints(pixFactor);

        Transform.popView();
    }

    public static void drawCross(MapView mv, Viewport vp, MapScale scale, double longitude, double latitude, byte[] color, BufVertex vexBuf) {
        AnnotateCross.drawCross(mv, vp, scale, longitude, latitude, color, vexBuf);
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
