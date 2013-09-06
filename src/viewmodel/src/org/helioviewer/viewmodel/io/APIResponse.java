package org.helioviewer.viewmodel.io;

import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;

import org.helioviewer.base.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.XML;

/**
 * Wrapper for JSONObject.
 * 
 * The idea behind this class is simply to get a better name and to prevent the
 * user from handling all the JSONExceptions.
 * 
 * @author Markus Langenberg
 */
public class APIResponse {
    /**
     * Complete answer
     */
    private JSONObject data;
    /**
     * URI of the response
     */
    private URI uri;

    /**
     * Constructor with a reader as source
     * 
     * @param source
     *            - Reader from which the JSON object will be read
     */
    public APIResponse(Reader source) {
        try {
            data = new JSONObject(new JSONTokener(source));
            uri = new URI(data.getString("uri"));
        } catch (JSONException e) {
            Log.error("Invalid JSON response " + data, e);
        } catch (URISyntaxException e) {
            Log.error("Invalid uri in response " + data, e);
        }
    }

    /**
     * Constructor with a complete string.
     * 
     * @param source
     *            string containing the formated JSON object
     */
    public APIResponse(String source) {
        try {
            data = new JSONObject(source);
            uri = new URI(data.getString("uri"));
        } catch (JSONException e) {
            Log.error("Invalid JSON response " + data, e);
        } catch (URISyntaxException e) {
            Log.error("Invalid uri in response " + data, e);
        }
    }

    /**
     * Constructor with a complete XML string
     * 
     * @param source
     *            - string containing the XML or JSON formatted representation
     * @param isXML
     *            - true if the string contains a XML representation, false if
     *            it contains a JSON representation
     */
    public APIResponse(String source, boolean isXML) {
        try {
            if (isXML) {
                data = XML.toJSONObject(source);
            } else {
                data = new JSONObject(source);
            }
            uri = new URI(data.getString("uri"));
        } catch (JSONException e) {
            Log.error("Invalid JSON response " + data, e);
        } catch (URISyntaxException e) {
            Log.error("Invalid uri in response " + data, e);
        }
    }

    /**
     * Returns the value for a given key.
     * 
     * If the key does not exist, returns null.
     * 
     * @param key
     *            Key to search for
     * @return value for given key
     */
    public String getString(String key) {
        try {
            return data.getString(key);
        } catch (JSONException e) {
        }
        return null;
    }

    /**
     * Return a String representation of the whole API response
     * 
     * @return String representing the API response
     */
    public String getXMLRepresentation() {
        try {
            return XML.toString(this.data);
        } catch (JSONException e) {
            return "";
        }
    }

    /**
     * Returns the URI of the image which results from this API response.
     * 
     * The URI is used as the unique identifier for the response.
     * 
     * @return unique URI corresponding to this response
     */
    public URI getURI() {
        return uri;
    }

    /**
     * Checks if a JSON object could be created
     * 
     * @return true if a valid JSON object has been created
     */
    public boolean hasData() {
        return data != null;
    }
}
