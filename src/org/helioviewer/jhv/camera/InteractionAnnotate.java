package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.camera.annotate.Annotateable;
import org.helioviewer.jhv.camera.annotate.AnnotationMode;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.input.KeyInputEvent;
import org.helioviewer.jhv.input.PointerEvent;
import org.helioviewer.jhv.layers.MovieDisplay;

class InteractionAnnotate implements Interaction.Type {

    private final Camera camera;
    private final Annotations annotations;

    InteractionAnnotate(Camera _camera, Annotations _annotations) {
        camera = _camera;
        annotations = _annotations;
    }

    @Override
    public void mousePressed(PointerEvent e, Viewport vp, AnnotationMode annotationMode) {
        Annotateable annotateable = annotationMode.generate(null);
        annotations.start(annotateable);
        annotateable.mousePressed(camera, vp, e.x(), e.y());
        if (!annotateable.isDraggable()) {
            finishAnnotateable();
        }
        MovieDisplay.display();
    }

    @Override
    public void mouseDragged(PointerEvent e, Viewport vp) {
        Annotateable pending = annotations.pending();
        if (pending != null && pending.isDraggable()) {
            pending.mouseDragged(camera, vp, e.x(), e.y());
            MovieDisplay.display();
        }
    }

    private void finishAnnotateable() {
        annotations.finishPending();
        MovieDisplay.display();
    }

    boolean hasPendingAnnotateable() {
        return annotations.hasPending();
    }

    @Override
    public void mouseReleased(PointerEvent e) {
        finishAnnotateable();
    }

    @Override
    public void keyPressed(KeyInputEvent e) {
        if (e.key() == KeyInputEvent.Key.BACKSPACE || e.key() == KeyInputEvent.Key.DELETE) {
            annotations.removeActive();
            MovieDisplay.display();
        } else if (e.key() == KeyInputEvent.Key.N && annotations.selectNext()) {
            MovieDisplay.display();
        }
    }

}
