package org.helioviewer.jhv.display.interaction;

import org.helioviewer.jhv.annotations.Annotations;
import org.helioviewer.jhv.annotations.Annotateable;
import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.input.KeyInputEvent;
import org.helioviewer.jhv.input.PointerEvent;

final class InteractionAnnotate {

    private final Camera camera;

    InteractionAnnotate(Camera _camera) {
        camera = _camera;
    }

    void mousePressed(PointerEvent e, Viewport vp) {
        Annotateable annotateable = ViewState.getAnnotationMode().generate(null);
        Annotations.start(annotateable);
        annotateable.mousePressed(camera, vp, e.x(), e.y());
        if (!annotateable.isDraggable()) {
            finishAnnotateable();
        }
        DisplayController.display();
    }

    void mouseDragged(PointerEvent e, Viewport vp) {
        Annotateable pending = Annotations.pending();
        if (pending != null && pending.isDraggable()) {
            pending.mouseDragged(camera, vp, e.x(), e.y());
            DisplayController.display();
        }
    }

    private void finishAnnotateable() {
        Annotations.finishPending();
        DisplayController.display();
    }

    void mouseReleased() {
        finishAnnotateable();
    }

    void keyPressed(KeyInputEvent e) {
        if (e.key() == KeyInputEvent.Key.BACKSPACE || e.key() == KeyInputEvent.Key.DELETE) {
            Annotations.removeActive();
            DisplayController.display();
        } else if (e.key() == KeyInputEvent.Key.N && Annotations.selectNext()) {
            DisplayController.display();
        } else if (e.key() == KeyInputEvent.Key.P && Annotations.selectPrevious()) {
            DisplayController.display();
        }
    }

}
