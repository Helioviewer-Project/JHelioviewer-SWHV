package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.annotations.Annotations;
import org.helioviewer.jhv.annotations.Annotateable;
import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.display.DisplayFrame;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.input.KeyInputEvent;
import org.helioviewer.jhv.input.PointerEvent;

class InteractionAnnotate {

    private final Camera camera;

    InteractionAnnotate(Camera _camera) {
        camera = _camera;
    }

    public void mousePressed(PointerEvent e, Viewport vp) {
        Annotateable annotateable = ViewState.getAnnotationMode().generate(null);
        Annotations.start(annotateable);
        annotateable.mousePressed(camera, vp, e.x(), e.y());
        if (!annotateable.isDraggable()) {
            finishAnnotateable();
        }
        DisplayFrame.display();
    }

    public void mouseDragged(PointerEvent e, Viewport vp) {
        Annotateable pending = Annotations.pending();
        if (pending != null && pending.isDraggable()) {
            pending.mouseDragged(camera, vp, e.x(), e.y());
            DisplayFrame.display();
        }
    }

    private void finishAnnotateable() {
        Annotations.finishPending();
        DisplayFrame.display();
    }

    public void mouseReleased() {
        finishAnnotateable();
    }

    public void keyPressed(KeyInputEvent e) {
        if (e.key() == KeyInputEvent.Key.BACKSPACE || e.key() == KeyInputEvent.Key.DELETE) {
            Annotations.removeActive();
            DisplayFrame.display();
        } else if (e.key() == KeyInputEvent.Key.N && Annotations.selectNext()) {
            DisplayFrame.display();
        } else if (e.key() == KeyInputEvent.Key.P && Annotations.selectPrevious()) {
            DisplayFrame.display();
        }
    }

}
