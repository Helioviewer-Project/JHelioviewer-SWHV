package org.helioviewer.viewmodel.view;

import org.helioviewer.viewmodel.imagedata.ImageData;

public interface AbstractViewDataHandler {

    public abstract void handleData(AbstractView view, ImageData imageData);

}
