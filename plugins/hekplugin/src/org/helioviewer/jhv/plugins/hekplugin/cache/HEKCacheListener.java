package org.helioviewer.jhv.plugins.hekplugin.cache;

public interface HEKCacheListener {

    public void cacheStateChanged();

    public void eventsChanged(HEKPath path);

    public void structureChanged(HEKPath path);

}
