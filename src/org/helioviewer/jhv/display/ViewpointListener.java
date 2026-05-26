package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;

public interface ViewpointListener {
    void viewpointChanged(Position viewpoint);
}
