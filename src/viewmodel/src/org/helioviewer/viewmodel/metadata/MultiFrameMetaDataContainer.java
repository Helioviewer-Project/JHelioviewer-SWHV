package org.helioviewer.viewmodel.metadata;

import java.io.IOException;

/**
 * Extension of MetaDataContainer, to allow accessing multiple frames.
 * 
 * The functions from MetaDataContainer should refer to he current frame,
 * whereas the functions from MultiFrameMetaDataContainer also allow to access
 * different frames than the current one.
 * 
 * @author Markus Langenberg
 * 
 */
public interface MultiFrameMetaDataContainer extends MetaDataContainer {

    /**
     * Gets the value for a given key as a string.
     * 
     * If the key does not exist, returns null. If the frame number does not
     * exists, choses the closest one.
     * 
     * @param key
     *            Search for this key
     * @param frameNumber
     *            frame number to search key, starting with 0 for the first
     *            frame.
     * @return value corresponding to the key, null if the key does not exist
     */
    public String get(String key, int frameNumber) throws IOException;
}
