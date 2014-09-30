package org.helioviewer.jhv.plugins.swek;

/**
 * Locks of the
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class SWEKPluginLocks {
    /** Lock for the downloads */
    public static final Object downloadLock = new Object();

    /** Lock for the requests */
    public static final Object requestLock = new Object();

    /** Lock for tree selection */
    public static final Object treeSelectionLock = new Object();

    /** Filter manager lock */
    public static final Object filterManagerLock = new Object();
}
