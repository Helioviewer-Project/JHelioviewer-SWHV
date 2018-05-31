package org.helioviewer.jhv.camera;

import java.util.ArrayList;
import java.util.HashSet;

import org.helioviewer.jhv.camera.annotate.AnnotateCircle;
import org.helioviewer.jhv.camera.annotate.AnnotateCross;
import org.helioviewer.jhv.camera.annotate.AnnotateFOV;
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
    private final HashSet<Annotateable> removed = new HashSet<>();
    private final HashSet<Annotateable> added = new HashSet<>();

    private Annotateable newAnnotateable = null;
    private AnnotationMode mode = AnnotationMode.Rectangle;
    private int activeIndex = -1;

    public InteractionAnnotate(Camera _camera) {
        super(_camera);
    }

    private void add(Annotateable ann) {
        anns.add(ann);
        added.add(ann);
    }

    private void remove() {
        if (activeIndex >= 0 && activeIndex < anns.size()) {
            removed.add(anns.remove(activeIndex));
            activeIndex = anns.size() - 1;
        }
    }

    public void drawAnnotations(Viewport vp, GL2 gl) {
        for (Annotateable ann : removed) {
            ann.dispose(gl);
        }
        removed.clear();
        for (Annotateable ann : added) {
            ann.init(gl);
        }
        added.clear();
        if (newAnnotateable != null) {
            newAnnotateable.init(gl);
        }

        Annotateable activeAnn = null;
        if (activeIndex >= 0 && activeIndex < anns.size())
            activeAnn = anns.get(activeIndex);
        for (Annotateable ann : anns) {
            ann.render(camera, vp, gl, ann == activeAnn);
        }
        if (newAnnotateable != null) {
            newAnnotateable.render(camera, vp, gl, false);
        }
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
            add(newAnnotateable);
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
        removed.addAll(anns);
        anns.clear();
        activeIndex = -1;
    }

    private Annotateable generate(JSONObject jo) {
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
                add(generate(ja.getJSONObject(i)));
            }
        }
    }

}
