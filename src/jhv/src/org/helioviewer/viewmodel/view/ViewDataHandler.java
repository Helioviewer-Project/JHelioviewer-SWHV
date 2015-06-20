package org.helioviewer.viewmodel.view;

import org.helioviewer.viewmodel.imagedata.ImageData;

public interface ViewDataHandler {

    public abstract void handleData(AbstractView view, ImageData imageData);

}
