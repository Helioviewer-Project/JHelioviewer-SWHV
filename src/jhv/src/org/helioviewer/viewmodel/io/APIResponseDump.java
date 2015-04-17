package org.helioviewer.viewmodel.io;

import java.net.URI;
import java.util.HashMap;

/**
 * Dump for APIResponses.
 * 
 * This class stores API responses, to that other classes all over the
 * application can get additional information provided by the API.
 * 
 * @author Markus Langenberg
 */
public class APIResponseDump {

    private static final APIResponseDump singletonInstance = new APIResponseDump();
    private HashMap<URI, APIResponse> dump = new HashMap<URI, APIResponse>();

    private APIResponseDump() {
    }

    /**
     * Gets singleton object
     * 
     * @return singleton object for the APIResponseDump
     */
    public static APIResponseDump getSingletonInstance() {
        return singletonInstance;
    }

    /**
     * Puts a new response on the dump.
     * 
     * @param newResponse
     *            new response
     */
    public void putResponse(APIResponse newResponse) {
        dump.put(newResponse.getURI(), newResponse);
    }

    /**
     * Gets the response corresponding to the given URI.
     * 
     * The response is removed from the dump afterwards.
     * 
     * @param uri
     *            URI from corresponding response
     * @return the api response
     */
    public APIResponse getResponse(URI uri) {
        return getResponse(uri, true);
    }

    /**
     * Gets the response corresponding to the given URI.
     * 
     * @param uri
     *            URI from corresponding response
     * @param keepResponse
     *            if true, the response stays on the dump, otherwise it is
     *            removed
     * @return the api response
     */
    public APIResponse getResponse(URI uri, boolean keepResponse) {
        APIResponse response = dump.get(uri);
        if (!keepResponse) {
            dump.remove(uri);
        }
        return response;
    }

    /**
     * Removes the response corresponding to the given URI.
     * 
     * @param uri
     */
    public void removeResponse(URI uri) {
        dump.remove(uri);
    }
}
