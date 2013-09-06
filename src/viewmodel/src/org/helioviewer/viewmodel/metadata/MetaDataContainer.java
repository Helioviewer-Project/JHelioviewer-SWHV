package org.helioviewer.viewmodel.metadata;

/**
 * Object containing the raw meta data.
 * 
 * <p>
 * Usually, this is the image itself. This interface provides the capability to
 * read the meta data, assuming it is given as pairs of key and value.
 * 
 * <p>
 * Apart from that, a MetaDataContainer provides access to its dimensions in
 * pixel.
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
    public String get(String key);

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
    public int tryGetInt(String key);

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
    public double tryGetDouble(String key);

    /**
     * Returns the width of the image in pixels.
     * 
     * @return width of the image in pixels
     */
    public int getPixelWidth();

    /**
     * Returns the height of the image in pixels.
     * 
     * @return height of the image in pixels
     */
    public int getPixelHeight();
}
