package org.helioviewer.viewmodel.metadata;

/**
 * Interface to represent non constant meta data.
 * 
 * Non constant meta data is meta data, which can change while modifying a view.
 * This does not affect values, which are not constant by nature, for example
 * the time stamp for a movie, but it does affect values, which are usually are
 * constant, like the resolution of a frame in a movie.
 * 
 * @author Markus Langenberg
 * 
 */
public interface NonConstantMetaData extends MetaData {

    /**
     * Checks the meta data container for modifications.
     * 
     * If the response from the container has changed, the meta object will be
     * updated.
     * 
     * @return True, if there were modifications, false otherwise
     */
    boolean checkForModifications();
}
