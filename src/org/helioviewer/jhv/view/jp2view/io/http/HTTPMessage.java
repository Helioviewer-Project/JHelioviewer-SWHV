package org.helioviewer.jhv.view.jp2view.io.http;

import java.util.HashMap;
import java.util.Map;

public class HTTPMessage {

    private final HashMap<String, String> headers = new HashMap<>();

    public String getHeader(String key) {
        return headers.get(key);
    }

    public void setHeader(String key, String val) {
        headers.put(key, val);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            str.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        return str.toString();
    }

}
