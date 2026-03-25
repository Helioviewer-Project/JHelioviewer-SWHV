package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;

public record MapContext(Position viewpoint, GridType gridType, Viewport vp) {
}
