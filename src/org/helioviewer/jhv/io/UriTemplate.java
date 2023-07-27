package org.helioviewer.jhv.io;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class UriTemplate {

    private final String base;
    private final HashMap<String, String> params = new HashMap<>();

    public UriTemplate(String _base) {
        base = _base;
    }

    public UriTemplate set(String param, Object value) {
        params.put(param, URLEncoder.encode(value.toString(), StandardCharsets.UTF_8));
        return this;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(base + '?');
        params.forEach((key, value) -> builder.append('&').append(key).append('=').append(value));
        return builder.toString();
    }

}
