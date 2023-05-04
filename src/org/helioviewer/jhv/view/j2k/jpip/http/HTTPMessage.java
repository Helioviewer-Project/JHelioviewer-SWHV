package org.helioviewer.jhv.view.j2k.jpip.http;

import java.util.HashMap;

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
        headers.forEach((key, value) -> str.append(key).append(": ").append(value).append("\r\n"));
        return str.toString();
    }

}
