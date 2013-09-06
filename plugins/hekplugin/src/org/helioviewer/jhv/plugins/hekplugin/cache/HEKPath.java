package org.helioviewer.jhv.plugins.hekplugin.cache;

import java.util.Comparator;
import java.util.Vector;

import org.helioviewer.base.logging.Log;

/**
 * Class used to categorize events
 * 
 * @author Malte Nuhn
 * 
 */

// TODO JAVADOC
public class HEKPath {

    public static final Comparator<HEKPath> LASTPARTCOMPARATOR = new Comparator<HEKPath>() {
        public int compare(HEKPath a, HEKPath b) {
            String partA = a.getLastPart();
            String partB = b.getLastPart();

            if (partA == null || partB == null) {
                Log.fatal("Path Lastpart is null. This should not happen!");
            }
            return partA.compareTo(partB);
        }
    };

    /**
     * Reference to the cache object this path belongs to
     */
    private HEKCache cache;

    /**
     * List of Subdirectories
     */
    private Vector<String> path = new Vector<String>();

    /**
     * Flag if this path directly exists in the cache, or if it is an
     * intermediate path,
     * <p>
     * If for example only "[ TYPE, FRM ]" exists in the cache, [] and [TYPE]
     * are virtual directories that would also exist
     */
    private boolean virtual = false;

    /**
     * Object that this path is pointing to
     */
    private Object object;

    /**
     * Constructor.
     * 
     * @param root
     * @param obj
     * @param catalogue
     * @param type
     * @param frm
     */
    public HEKPath(HEKCache root, Object obj, String catalogue, String type, String frm) {
        this.virtual = false;
        this.cache = root;
        this.object = obj;
        path.add(catalogue);
        path.add(type);
        path.add(frm);
    }

    /**
     * Constructor.
     * 
     * @param hekCache
     */
    public HEKPath(HEKCache hekCache) {
        cache = hekCache;
        object = hekCache;
    }

    /**
     * Constructor.
     * 
     * @param other
     */
    public HEKPath(HEKPath other) {
        this.cache = other.cache;
        this.object = other.object;
        this.path.addAll(other.path);
    }

    /**
     * Constructor.
     * 
     * @param other
     * @param additional
     * @param obj
     */
    public HEKPath(HEKPath other, String additional, Object obj) {
        this(other);
        this.object = obj;
        this.path.add(additional);
    }

    /**
     * Constructor
     * 
     * @param hekCache
     * @param path
     */
    public HEKPath(HEKCache hekCache, String[] path) {
        this.cache = hekCache;
        for (int i = 0; i < path.length; i++) {
            this.path.add(path[i]);
        }
    }

    /**
     * Constructor.
     * 
     * @param other
     * @param additional
     */
    public HEKPath(HEKPath other, String additional) {
        this(other);
        this.path.add(additional);
    }

    /**
     * Overwritten equals function.
     * 
     * Two HEKPathes are equal iff all parts of the containing "path" vector
     * equal.
     */
    public boolean equals(Object other) {
        if (other instanceof HEKPath) {
            HEKPath otherType = (HEKPath) other;
            if (otherType.path.size() != this.path.size())
                return false;
            for (int i = 0; i < this.path.size(); i++) {
                if (!this.path.get(i).equals(otherType.path.get(i)))
                    return false;
            }
            return true;
        } else {
            return false;
        }
    }

    public String getFRM() {
        if (this.path.size() > 2)
            return path.get(2);
        return null;
    }

    public String getType() {
        if (this.path.size() > 1)
            return path.get(1);
        return null;
    }

    /**
     * @see HEKPath#isSubPathOf(HEKPath)
     * @param other
     *            - Path the current path should be compared to
     * @param delta
     *            - defines how many intermediate paths may exists between the
     *            given path and the current one
     * @param exact
     *            - if false, this path must be AT LEAST the (delta)th children,
     *            else it must be exacty the (delta)th children
     * @return true if the current path is a child of the given path and there
     *         are no more than (delta-1) intermediate children inbetween
     */
    public boolean isSubPathOf(HEKPath other, int delta, boolean exact) {

        if (this.path.size() < other.path.size()) {
            return false;
        }

        int lenDelta = this.path.size() - other.path.size();

        // if in exact mode
        if (exact && lenDelta != delta) {
            return false;
        }

        // if not in exact mode
        if (!exact && lenDelta < delta) {
            return false;
        }

        for (int i = 0; i < other.path.size(); i++) {
            if (!this.path.get(i).equals(other.path.get(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * @see HEKPath#isSubPathOf(HEKPath)
     * @param other
     *            - Path the current path should be compared to
     * @return
     */
    public boolean isSubPathOf(HEKPath other) {
        return isSubPathOf(other, 1, false);
    }

    public HEKPath getParent() {

        if (getDepth() == 0) {
            return null;
        }

        HEKPath result = new HEKPath(this.cache);
        result.path.addAll(path);
        result.path.removeElementAt(result.path.size() - 1);
        result.virtual = !this.cache.getTrackPaths().contains(result); // if the
                                                                       // cache
        result.object = this.cache.getTrack(result); // try to get the object
                                                     // behind it
        return result;
    }

    public String toString() {
        if (!path.isEmpty()) {
            return path.toString();
        } else {
            return "ROOT";
        }
    }

    public int hashCode() {
        return this.toString().hashCode();
    }

    public void setLastPart(String lastPart) {
        if (this.path.size() > 0) {
            this.path.set(this.path.size() - 1, lastPart);
        }
    }

    public String getLastPart() {
        if (this.path.size() == 0) {
            return "ROOT";
        } else {
            return this.path.lastElement();
        }
    }

    public HEKCache getCache() {
        return cache;
    }

    public void setVirtual(boolean virtual) {
        this.virtual = virtual;
    }

    public boolean isVirtual() {
        return virtual;
    }

    public int getDepth() {
        return path.size();
    }

    public HEKPath truncate(int len) {
        HEKPath result = new HEKPath(this);
        for (int i = len; i < this.path.size(); i++) {
            result.path.removeElementAt(result.path.size() - 1);
        }
        return result;
    }

    public void setObject(Object obj) {
        this.object = obj;
    }

    public Object getObject() {
        return this.object;
    }

    public Object[] getTreePath() {
        Vector<HEKPath> result = new Vector<HEKPath>();
        getTreePath(result);
        return result.toArray();
    }

    private void getTreePath(Vector<HEKPath> result) {
        HEKPath parent = this.getParent();
        if (parent != null)
            parent.getTreePath(result);
        result.add(this);
    }

}
