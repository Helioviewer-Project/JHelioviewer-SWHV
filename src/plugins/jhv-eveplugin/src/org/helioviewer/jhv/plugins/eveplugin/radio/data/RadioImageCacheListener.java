package org.helioviewer.jhv.plugins.eveplugin.radio.data;

public interface RadioImageCacheListener {

    public abstract void imageRemoved(long ID);

    public abstract void imageAdded(long ID);

}
