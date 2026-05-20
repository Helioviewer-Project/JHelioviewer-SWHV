package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;

public record MapContext(Position viewpoint, Viewport vp, ProjectionScale scale, GridType gridType) {}
