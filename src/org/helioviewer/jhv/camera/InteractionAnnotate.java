package org.helioviewer.jhv.camera;

import java.util.ArrayList;

import org.helioviewer.jhv.camera.annotate.AnnotateCircle;
import org.helioviewer.jhv.camera.annotate.AnnotateCross;
import org.helioviewer.jhv.camera.annotate.AnnotateRectangle;
import org.helioviewer.jhv.camera.annotate.Annotateable;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.json.JSONArray;
import org.json.JSONObject;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.GL2;

public class InteractionAnnotate extends Interaction {

    public enum AnnotationMode {
        Rectangle(AnnotateRectangle.class), Circle(AnnotateCircle.class), Cross(AnnotateCross.class);

        private final Class<? extends Annotateable> clazz;

        AnnotationMode(Class<? extends Annotateable> _clazz) {
            clazz = _clazz;
        }

        Annotateable generate(Camera _camera, JSONObject _jo) {
            try {
                return clazz.getConstructor(Camera.class, JSONObject.class).newInstance(_camera, _jo);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new AnnotateRectangle(_camera, _jo);
        }
    }

    private final ArrayList<Annotateable> annotateables = new ArrayList<>();
    private Annotateable newAnnotateable = null;
    private AnnotationMode mode = AnnotationMode.Rectangle;
    private int activeIndex = -1;

    public InteractionAnnotate(Camera _camera) {
        super(_camera);
    }

    public void drawAnnotations(Viewport vp, GL2 gl) {
        Annotateable activeAnnotateable = null;
        if (activeIndex >= 0)
            activeAnnotateable = annotateables.get(activeIndex);
        for (Annotateable ann : annotateables) {
            ann.render(vp, gl, ann == activeAnnotateable);
        }
        if (newAnnotateable != null) {
            newAnnotateable.render(vp, gl, false);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        newAnnotateable = mode.generate(camera, null);
        newAnnotateable.mousePressed(e.getX(), e.getY());
        if (!newAnnotateable.isDraggable()) {
            finishAnnotateable();
        }
        Display.display();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (newAnnotateable != null && newAnnotateable.isDraggable()) {
            newAnnotateable.mouseDragged(e.getX(), e.getY());
            Display.display();
        }
    }

    private void finishAnnotateable() {
        if (newAnnotateable != null && newAnnotateable.beingDragged()) {
            newAnnotateable.mouseReleased();
            annotateables.add(newAnnotateable);
            activeIndex = annotateables.size() - 1;
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
            if (activeIndex >= 0) {
                annotateables.remove(activeIndex);
            }
            activeIndex = annotateables.size() - 1;
            Display.display();
        } else if (code == KeyEvent.VK_N && activeIndex >= 0) {
            activeIndex++;
            activeIndex %= annotateables.size();
            Display.display();
        }
    }

    public void setMode(AnnotationMode newMode) {
        mode = newMode;
    }

    public void clear() {
        newAnnotateable = null;
        annotateables.clear();
        activeIndex = -1;
    }

    private Annotateable generate(JSONObject jo) {
        try {
            return AnnotationMode.valueOf(jo.getString("type")).generate(camera, jo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new AnnotateRectangle(camera, jo);
    }

    public JSONObject toJson() {
        JSONArray ja = new JSONArray();
        for (Annotateable annotateable : annotateables)
            ja.put(annotateable.toJson());
        return new JSONObject().put("activeIndex", activeIndex).put("annotateables", ja);
    }

}
