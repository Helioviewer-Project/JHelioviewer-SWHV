package org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip;

import java.util.Map;
import java.util.HashMap;

/**
 * A class that helps build a JPIP query string.
 * 
 * @author caplins
 * @author Juan Pablo
 */
public class JPIPQuery {

    /** HashMap holding the jpip-request-fields */
    private final HashMap<String, String> fields = new HashMap<>();

    public JPIPQuery() {
        fields.put("len", Integer.toString(JPIPConstants.MIN_REQUEST_LEN));
    }

    /**
     * This constructor allows to initialize the query by means of a string list
     * of pairs: key1, value1, key2, value2, ...
     * 
     * @param values
     */
    public JPIPQuery(String... values) {
        String key = null;
        boolean isKey = true;

        for (String val : values) {
            if (isKey)
                key = val;
            else
                fields.put(key, val);
            isKey = !isKey;
        }

        if (fields.get("len") == null)
            fields.put("len", Integer.toString(JPIPConstants.MIN_REQUEST_LEN));
    }

    /**
     * Sets the specified field to the specified value.
     * 
     * @param _key
     * @param _value
     */
    public void setField(String key, String value) {
        fields.put(key, value);
    }

    /**
     * Removes a specified pair (key, value).
     * 
     * @param key
     */
    public void removeField(String key) {
        fields.remove(key);
    }

    /**
     * Gets the value of the specified field. Returns null if it does not exist.
     * 
     * @param key
     * @return Value corresponding to given key
     */
    public String getValue(String key) {
        return fields.get(key);
    }

    /** Returns a String representing this query. */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            buf.append(entry.getKey()).append('=').append(entry.getValue()).append('&');
        }

        String ret = buf.toString();
        if (ret.length() > 0)
            ret = ret.substring(0, ret.length() - 1);

        return ret;
    }

}
