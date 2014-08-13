package org.helioviewer.jhv.plugins.swek;

public class SWEKPluginLocks {
    /** Lock for the downloads */
    public static final Object downloadLock = new Object();

    /** Lock for the requests */
    public static final Object requestLock = new Object();

    /** Lock for tree selection */
    public static final Object treeSelectionLock = new Object();
}
