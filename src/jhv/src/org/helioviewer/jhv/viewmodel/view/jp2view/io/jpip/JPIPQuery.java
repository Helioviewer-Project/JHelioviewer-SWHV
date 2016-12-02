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

    private final HashMap<String, String> fields = new HashMap<>();

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

        fields.putIfAbsent("len", Integer.toString(JPIPConstants.MIN_REQUEST_LEN));
    }

    public void setField(String key, String value) {
        fields.put(key, value);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            buf.append(entry.getKey()).append('=').append(entry.getValue()).append('&');
        }

        String ret = buf.toString();
        if (!ret.isEmpty())
            ret = ret.substring(0, ret.length() - 1);

        return ret;
    }

}
