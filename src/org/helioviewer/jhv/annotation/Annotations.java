package org.helioviewer.jhv.annotation;

import java.util.ArrayList;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.MapView;
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
    public static final int MIN_THICKNESS = 1;
    public static final int MAX_THICKNESS = 8;
    public static final int DEFAULT_THICKNESS = 1;
    public static final Colors.NamedColor[] BASE_COLORS = {
            Colors.NamedColor.Blue,
            Colors.NamedColor.Yellow,
            Colors.NamedColor.Cyan,
            Colors.NamedColor.Magenta,
            Colors.NamedColor.White
    };

    private static final ArrayList<Annotateable> annotations = new ArrayList<>();
    private static final GLSLLine annotationsLine = new GLSLLine(true);
    private static final BufVertex annotationsBuf = new BufVertex(3276 * GLSLLine.stride); // pre-allocate 64kB
    private static final GLSLLine transformedLine = new GLSLLine(true);
    private static final BufVertex transformedBuf = new BufVertex(512 * GLSLLine.stride); // pre-allocate 5 FOV
    private static final GLSLShape center = new GLSLShape(true);
    private static final BufVertex centerBuf = new BufVertex(8 * GLSLShape.stride);

    private static Annotateable pending;
    private static int activeIndex = -1;
    private static double thickness = DEFAULT_THICKNESS * LINEWIDTH;
    private static Colors.NamedColor baseColor = Colors.NamedColor.Blue;

    public static void start(Annotateable annotateable) {
        pending = annotateable;
    }

    public static int getThicknessValue() {
        return (int) Math.round(thickness / LINEWIDTH);
    }

    static double getThickness() {
        return thickness;
    }

    static double getDefaultThickness() {
        return DEFAULT_THICKNESS * LINEWIDTH;
    }

    public static void setThicknessValue(int value) {
        thickness = Math.clamp(value, MIN_THICKNESS, MAX_THICKNESS) * LINEWIDTH;
    }

    public static Colors.NamedColor getBaseColor() {
        return baseColor;
    }

    public static void setBaseColor(Colors.NamedColor color) {
        baseColor = color;
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

    public static void render(MapView mv, Viewport vp) {
        if (pending == null && annotations.isEmpty())
            return;

        Annotateable activeAnnotation = activeIndex >= 0 && activeIndex < annotations.size() ? annotations.get(activeIndex) : null;

        annotations.forEach(annotation -> renderAnnotation(mv, vp, annotation, annotation == activeAnnotation));
        if (pending != null)
            renderAnnotation(mv, vp, pending, false);

        double pixFactor = ViewportMath.getPixelFactor(vp, mv.cameraWidth(vp));

        Transform.pushView();
        if (mv.isOrthographic())
            Transform.rotateViewInverse(mv.viewpoint().toQuat());
        annotations.forEach(annotation -> renderTransformedAnnotation(mv, vp, pixFactor, annotation, annotation == activeAnnotation));
        if (pending != null)
            renderTransformedAnnotation(mv, vp, pixFactor, pending, false);
        Transform.popView();
    }

    private static void renderAnnotation(MapView mv, Viewport vp, Annotateable annotation, boolean active) {
        annotation.draw(mv, vp, active, annotationsBuf);
        annotationsLine.setVertex(annotationsBuf);
        annotationsLine.renderLine(vp, annotation.thickness());
    }

    private static void renderTransformedAnnotation(MapView mv, Viewport vp, double pixFactor, Annotateable annotation, boolean active) {
        annotation.drawTransformed(mv, active, transformedBuf, centerBuf);
        transformedLine.setVertex(transformedBuf);
        transformedLine.renderLine(vp, annotation.thickness());
        center.setVertex(centerBuf);
        center.renderPoints(pixFactor);
    }

    public static void drawCross(MapView mv, Viewport vp, double longitude, double latitude, byte[] color, BufVertex vexBuf) {
        AnnotateCross.drawCross(mv, vp, longitude, latitude, color, vexBuf);
    }

    @Nullable
    public static String getAnnotationData() {
        if (pending == null)
            return activeIndex >= 0 && activeIndex < annotations.size() ? annotations.get(activeIndex).getData() : null;
        else
            return pending.getData();
    }

    public static void zoom() {
        Annotateable activeAnnotation = activeIndex >= 0 && activeIndex < annotations.size() ? annotations.get(activeIndex) : null;
        if (activeAnnotation instanceof AnnotateFOV annotationFOV)
            annotationFOV.zoom(Display.getActiveViewport());
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

    private Annotations() {}
}
