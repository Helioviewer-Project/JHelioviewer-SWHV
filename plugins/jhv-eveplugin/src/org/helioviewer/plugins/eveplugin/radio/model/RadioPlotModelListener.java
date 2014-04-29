package org.helioviewer.plugins.eveplugin.radio.model;

import java.awt.image.BufferedImage;

public interface RadioPlotModelListener {
    // public abstract void newDataAvalable();
    // public abstract void donwloadFinished();
    public abstract void drawBufferedImage(BufferedImage image, DrawableAreaMap map);

    public abstract void removeDownloadRequestData(long iD);

    public abstract void changeVisibility(long iD);
}
