package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.camera.annotate.Annotateable;
import org.helioviewer.jhv.camera.annotate.AnnotationMode;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.input.KeyInputEvent;
import org.helioviewer.jhv.input.PointerEvent;
import org.helioviewer.jhv.layers.MovieDisplay;

class InteractionAnnotate implements Interaction.Type {

    private final Camera camera;

    InteractionAnnotate(Camera _camera) {
        camera = _camera;
    }

    @Override
    public void mousePressed(PointerEvent e, Viewport vp, AnnotationMode annotationMode) {
        Annotateable annotateable = annotationMode.generate(null);
        Annotations.start(annotateable);
        annotateable.mousePressed(camera, vp, e.x(), e.y());
        if (!annotateable.isDraggable()) {
            finishAnnotateable();
        }
        MovieDisplay.display();
    }

    @Override
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

    @Override
    public void mouseReleased(PointerEvent e) {
        finishAnnotateable();
    }

    @Override
    public void keyPressed(KeyInputEvent e) {
        if (e.key() == KeyInputEvent.Key.BACKSPACE || e.key() == KeyInputEvent.Key.DELETE) {
            Annotations.removeActive();
            MovieDisplay.display();
        } else if (e.key() == KeyInputEvent.Key.N && Annotations.selectNext()) {
            MovieDisplay.display();
        }
    }

}
