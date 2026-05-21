package org.helioviewer.jhv.layers;

import org.helioviewer.jhv.astronomy.Position;

@FunctionalInterface
public interface RenderRequestHandler {
    void requestRender(Position viewpoint);
}
