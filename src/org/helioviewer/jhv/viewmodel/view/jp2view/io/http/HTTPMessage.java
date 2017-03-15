package org.helioviewer.jhv.viewmodel.view.jp2view.io.http;

import java.util.HashMap;
import java.util.Map;

public class HTTPMessage {

    private final HashMap<String, String> headers = new HashMap<>();

    public final String getHeader(String key) {
        return headers.get(key);
    }

    public final void setHeader(String key, String val) {
        headers.put(key, val);
    }

    public final String toString() {
        StringBuilder str = new StringBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            str.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        return str.toString();
    }

}
