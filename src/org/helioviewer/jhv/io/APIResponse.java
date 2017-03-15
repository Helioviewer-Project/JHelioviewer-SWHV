package org.helioviewer.jhv.io;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.logging.Log;
// import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class APIResponse {

    private JSONObject data;
    private URI uri;
//    private long[] frames;

    public APIResponse(InputStream in) {
        try {
            data = JSONUtils.getJSONStream(in);
            if (!data.isNull("uri")) {
                uri = new URI(data.getString("uri"));
            }
/*
            if (!data.isNull("frames")) {
                JSONArray arr = data.getJSONArray("frames");
                int len = arr.length();

                frames = new long[len];
                for (int i = 0; i < len; i++)
                    frames[i] = arr.getLong(i) * 1000L;
            }
*/
            Log.debug("answer : " + data);
        } catch (JSONException e) {
            Log.error("Invalid JSON response " + data, e);
        } catch (URISyntaxException e) {
            Log.error("Invalid URI in response " + data, e);
        }
    }

    /**
     * Returns the value for a given key.
     *
     * Returns null if the key does not exist or the value is not a string.
     *
     * @param key
     *            Key to search for
     * @return value for given key
     */
    public String getString(String key) {
        try {
            return data.getString(key);
        } catch (JSONException ignore) {
        }
        return null;
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
/*
    public long[] getFrames() {
        return frames;
    }
*/
    /**
     * Checks if a JSON object could be created
     *
     * @return true if a valid JSON object has been created
     */
    public boolean hasData() {
        return data != null;
    }

}
