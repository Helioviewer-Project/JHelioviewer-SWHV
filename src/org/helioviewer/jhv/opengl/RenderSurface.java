package org.helioviewer.jhv.opengl;

public interface RenderSurface {
    void requestRender();

    void setWhiteBackground(boolean whiteBackground);

    int getFramerate();
}
