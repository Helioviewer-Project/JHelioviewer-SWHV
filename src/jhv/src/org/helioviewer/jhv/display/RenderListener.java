package org.helioviewer.jhv.display;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.Viewpoint;
import org.helioviewer.jhv.display.Viewport;

public interface RenderListener {

    public void render(Camera camera, Viewport vp, double factor);

}
