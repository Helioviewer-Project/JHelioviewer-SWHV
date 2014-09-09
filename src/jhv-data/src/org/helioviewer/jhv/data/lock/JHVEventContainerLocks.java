package org.helioviewer.jhv.data.lock;

public class JHVEventContainerLocks {
    public static final Object dateLock = new Object();

    public static final Object intervalLock = new Object();

    public static final Object cacheLock = new Object();

    public static final Object eventHandlerCacheLock = new Object();
}
