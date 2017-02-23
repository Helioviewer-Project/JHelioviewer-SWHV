package org.helioviewer.jhv.camera.annotate;

import org.helioviewer.jhv.display.Viewport;
import org.jetbrains.annotations.NotNull;

import com.jogamp.opengl.GL2;

public interface Annotateable {

    void render(@NotNull Viewport vp, @NotNull GL2 gl, boolean active);

    void mousePressed(int x, int y);

    void mouseDragged(int x, int y);

    void mouseReleased();

    boolean beingDragged();

    boolean isDraggable();

}
