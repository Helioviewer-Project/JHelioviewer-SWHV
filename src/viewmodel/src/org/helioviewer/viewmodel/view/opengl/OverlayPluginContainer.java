package org.helioviewer.viewmodel.view.opengl;


public class OverlayPluginContainer {

    private boolean postRender = true;

    public void setPostRender(boolean postRender) {
        this.postRender = postRender;
    }

    public boolean getPostRender() {
        return this.postRender;
    }
}
