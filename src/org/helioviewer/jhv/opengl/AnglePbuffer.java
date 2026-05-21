package org.helioviewer.jhv.opengl;

import java.awt.EventQueue;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.opengl.angle.AngleRenderer;

public final class AnglePbuffer {

    private static final int WIDTH = 1024;
    private static final int HEIGHT = 1024;

    private AngleRenderer angleRenderer;
    private boolean renderPending;

    public void requestRender(Position viewpoint) {
        if (renderPending)
            return;

        renderPending = true;
        EventQueue.invokeLater(() -> {
            renderPending = false;
            renderNow(viewpoint);
        });
    }

    private void renderNow(Position viewpoint) {
        if (angleRenderer == null) {
            angleRenderer = AngleRenderer.pbuffer(WIDTH, HEIGHT);
            GLRenderer.reshape(WIDTH, HEIGHT);
        }
        angleRenderer.render(viewpoint);
    }

}
