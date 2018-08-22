package org.helioviewer.jhv.camera;

import java.util.ArrayList;

import org.helioviewer.jhv.base.Buf;
import org.helioviewer.jhv.camera.annotate.AnnotateCircle;
import org.helioviewer.jhv.camera.annotate.AnnotateCross;
import org.helioviewer.jhv.camera.annotate.AnnotateFOV;
import org.helioviewer.jhv.camera.annotate.AnnotateRectangle;
import org.helioviewer.jhv.camera.annotate.Annotateable;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.json.JSONArray;
import org.json.JSONObject;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.GL2;

public class InteractionAnnotate extends Interaction {

    public enum AnnotationMode {
        Rectangle(AnnotateRectangle.class), Circle(AnnotateCircle.class), Cross(AnnotateCross.class), FOV(AnnotateFOV.class);

        private final Class<? extends Annotateable> clazz;

        AnnotationMode(Class<? extends Annotateable> _clazz) {
            clazz = _clazz;
        }

        Annotateable generate(JSONObject jo) {
            try {
                return clazz.getConstructor(JSONObject.class).newInstance(jo);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new AnnotateRectangle(jo);
        }
    }

    private final ArrayList<Annotateable> anns = new ArrayList<>();

    private static final double LINEWIDTH = 0.002;
    private final GLSLLine annsLine = new GLSLLine(true);
    private final Buf annsBuf = new Buf(3276 * GLSLLine.stride);
    private final GLSLLine transLine = new GLSLLine(true);
    private final Buf transBuf = new Buf(512 * GLSLLine.stride);
    private final GLSLShape center = new GLSLShape(true);
    private final Buf centerBuf = new Buf(8 * GLSLShape.stride);

    private Annotateable newAnnotateable = null;
    private AnnotationMode mode = AnnotationMode.Rectangle;
    private int activeIndex = -1;

    public InteractionAnnotate(Camera _camera) {
        super(_camera);
    }

    private void remove() {
        if (activeIndex >= 0 && activeIndex < anns.size()) {
            anns.remove(activeIndex);
            activeIndex = anns.size() - 1;
        }
    }

    public void drawAnnotations(Viewport vp, GL2 gl) {
        if (newAnnotateable == null && anns.isEmpty())
            return;

        Annotateable activeAnn = activeIndex >= 0 && activeIndex < anns.size() ? anns.get(activeIndex) : null;

        for (Annotateable ann : anns) {
            ann.render(camera, vp, ann == activeAnn, annsBuf);
        }
        if (newAnnotateable != null) {
            newAnnotateable.render(camera, vp, false, annsBuf);
        }
        annsLine.setData(gl, annsBuf);
        annsLine.render(gl, vp, LINEWIDTH);

        Transform.pushView();
        Transform.rotateViewInverse(camera.getViewpoint().toQuat());
        {
            for (Annotateable ann : anns) {
                ann.renderTransformed(camera, vp, ann == activeAnn, transBuf, centerBuf);
            }
            if (newAnnotateable != null) {
                newAnnotateable.renderTransformed(camera, vp, false, transBuf, centerBuf);
            }
            transLine.setData(gl, transBuf);
            transLine.render(gl, vp, LINEWIDTH);

            double pointFactor = vp.height / (2 * camera.getWidth()) / 4;
            center.setData(gl, centerBuf);
            center.renderPoints(gl, pointFactor);
        }
        Transform.popView();
    }

    public void zoom() {
        Annotateable activeAnn = activeIndex >= 0 && activeIndex < anns.size() ? anns.get(activeIndex) : null;
        if (activeAnn instanceof AnnotateFOV)
            ((AnnotateFOV) activeAnn).zoom(camera);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        newAnnotateable = mode.generate(null);
        newAnnotateable.mousePressed(camera, e.getX(), e.getY());
        if (!newAnnotateable.isDraggable()) {
            finishAnnotateable();
        }
        Display.display();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (newAnnotateable != null && newAnnotateable.isDraggable()) {
            newAnnotateable.mouseDragged(camera, e.getX(), e.getY());
            Display.display();
        }
    }

    private void finishAnnotateable() {
        if (newAnnotateable != null && newAnnotateable.beingDragged()) {
            newAnnotateable.mouseReleased();
            anns.add(newAnnotateable);
            activeIndex = anns.size() - 1;
        }
        newAnnotateable = null;
        Display.display();
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
            Display.display();
        } else if (code == KeyEvent.VK_N && activeIndex >= 0) {
            activeIndex++;
            activeIndex %= anns.size();
            Display.display();
        }
    }

    public void setMode(AnnotationMode newMode) {
        mode = newMode;
    }

    public void clear() {
        newAnnotateable = null;
        anns.clear();
        activeIndex = -1;
    }

    private static Annotateable generate(JSONObject jo) {
        try {
            return AnnotationMode.valueOf(jo.getString("type")).generate(jo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new AnnotateRectangle(jo);
    }

    public JSONObject toJson() {
        JSONArray ja = new JSONArray();
        for (Annotateable ann : anns)
            ja.put(ann.toJson());
        return new JSONObject().put("activeIndex", activeIndex).put("annotateables", ja);
    }

    public void fromJson(JSONObject jo) {
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

    public void init(GL2 gl) {
        annsLine.init(gl);
        transLine.init(gl);
        center.init(gl);
    }

    public void dispose(GL2 gl) {
        annsLine.dispose(gl);
        transLine.dispose(gl);
        center.dispose(gl);
    }

}
