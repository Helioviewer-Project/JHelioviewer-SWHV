package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.app.state.ViewState;
import org.helioviewer.jhv.camera.annotate.Annotateable;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.input.KeyInputEvent;
import org.helioviewer.jhv.input.PointerEvent;
import org.helioviewer.jhv.layers.MovieDisplay;

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
        MovieDisplay.display();
    }

    public void mouseDragged(PointerEvent e, Viewport vp) {
        Annotateable pending = Annotations.pending();
        if (pending != null && pending.isDraggable()) {
            pending.mouseDragged(camera, vp, e.x(), e.y());
            MovieDisplay.display();
        }
    }

    private void finishAnnotateable() {
        Annotations.finishPending();
        MovieDisplay.display();
    }

    public void mouseReleased() {
        finishAnnotateable();
    }

    public void keyPressed(KeyInputEvent e) {
        if (e.key() == KeyInputEvent.Key.BACKSPACE || e.key() == KeyInputEvent.Key.DELETE) {
            Annotations.removeActive();
            MovieDisplay.display();
        } else if (e.key() == KeyInputEvent.Key.N && Annotations.selectNext()) {
            MovieDisplay.display();
        } else if (e.key() == KeyInputEvent.Key.P && Annotations.selectPrevious()) {
            MovieDisplay.display();
        }
    }

}
