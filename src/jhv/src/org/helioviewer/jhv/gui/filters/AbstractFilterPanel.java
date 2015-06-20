package org.helioviewer.jhv.gui.filters;

import org.helioviewer.jhv.opengl.GLImage;

public abstract class AbstractFilterPanel {

    protected GLImage image;

    protected void setGLImage(GLImage _image) {
        image = _image;
    }

}
