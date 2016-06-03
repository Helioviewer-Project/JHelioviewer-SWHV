package org.helioviewer.jhv.renderable.components.ImageLayer;

import org.helioviewer.jhv.opengl.GLImage;

public abstract class AbstractFilterPanel {

    protected GLImage image;

    protected void setGLImage(GLImage _image) {
        image = _image;
    }

    protected void refresh() {
        setGLImage(image);
    }

}
