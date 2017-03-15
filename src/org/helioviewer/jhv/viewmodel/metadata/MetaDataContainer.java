package org.helioviewer.jhv.viewmodel.metadata;

/**
 * Object containing the raw meta data.
 * 
 * <p>
 * Usually, this is the image itself. This interface provides the capability to
 * read the meta data, assuming it is given as pairs of key and value.
 * 
 * @author Ludwig Schmidt
 * 
 */
public interface MetaDataContainer {
    /**
     * Gets the value for a given key as a string.
     * 
     * If the key does not exist, returns null.
     * 
     * @param key
     *            Search for this key
     * @return value corresponding to the key, null if the key does not exist
     */
    String get(String key);
    /**
     * Gets the value for a given key as an integer value.
     * 
     * If the key does not exist or is not an integer value , returns 0.
     * 
     * @param key
     *            Search for this key
     * @return value corresponding to the key, if it is an integer value, 0
     *         otherwise
     */
    int tryGetInt(String key);
    /**
     * Gets the value for a given key as a double value.
     * 
     * If the key does not exist or is not a double value , returns 0.
     * 
     * @param key
     *            Search for this key
     * @return value corresponding to the key, if it is a double value, 0
     *         otherwise
     */
    double tryGetDouble(String key);

}
