package org.helioviewer.jhv.plugins.eveplugin.radio.data;

import java.awt.image.BufferedImage;

import org.helioviewer.jhv.plugins.eveplugin.radio.model.DrawableAreaMap;

public interface RadioDataManagerListener {
    public abstract void drawBufferedImage(BufferedImage image, DrawableAreaMap map);

    public abstract void removeDownloadRequestData();

    public abstract void changeVisibility();
}
