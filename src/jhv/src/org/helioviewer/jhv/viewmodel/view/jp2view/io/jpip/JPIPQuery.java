package org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip;

import java.util.Map.Entry;
import java.util.Hashtable;

/**
 * A class that helps build a JPIP query string.
 * 
 * @author caplins
 * @author Juan Pablo
 */
public class JPIPQuery implements Cloneable {
    /** The hashtable holding the jpip-request-fields */
    protected Hashtable<String, String> fields = new Hashtable<String, String>();

    /** Default constructor. */
    public JPIPQuery() {
        fields.put("len", Integer.toString(JPIPConstants.MIN_REQUEST_LEN));
    }

    /**
     * This constructor allows to initialize the query by means of a string list
     * of pairs: key1, value1, key2, value2, ...
     * 
     * @param _values
     */
    public JPIPQuery(String... _values) {
        String key = null;
        boolean isKey = true;
        boolean hasLen = false;

        for (String val : _values) {
            if (isKey)
                key = val;
            else {
                fields.put(key, val);
                hasLen = key.equals("len");
            }

            isKey = !isKey;
        }

        if (!hasLen) {
            fields.put("len", Integer.toString(JPIPConstants.MIN_REQUEST_LEN));
        }
    }

    /**
     * Sets the specified field to the specified value.
     * 
     * @param _key
     * @param _value
     */
    public void setField(String _key, String _value) {
        fields.put(_key, _value);
    }

    /**
     * Removes a specified pair (key, value).
     * 
     * @param _key
     */
    public void removeField(String _key) {
        fields.remove(_key);
    }

    /**
     * Gets the value of the specified field. Returns null if non exists.
     * 
     * @param _key
     * @return Value corresponding to given key
     */
    public String getValue(String _key) {
        return fields.get(_key);
    }

    /** Clones the query. */
    @Override
    public JPIPQuery clone() throws CloneNotSupportedException {
        super.clone();

        JPIPQuery ret = new JPIPQuery();
        ret.fields.putAll(this.fields);
        return ret;
    }

    /** Returns a String representing this query. */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (Entry<String, String> entry : fields.entrySet()) {
            buf.append(entry.getKey());
            buf.append("=");
            buf.append(entry.getValue());
            buf.append("&");
        }

        String ret = buf.toString();
        if (ret.length() > 0)
            ret = ret.substring(0, ret.length() - 1);

        return ret;
    }

}
