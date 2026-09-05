package org.helioviewer.jhv.io;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

public class UriTemplate {

    private final String root;

    public UriTemplate(String _root) {
        root = _root + '?';
    }

    public UriTemplate(String _root, Variables v) {
        root = v.expand(_root + '?');
    }

    public String expand(Variables v) {
        return v.expand(root);
    }

    public static Variables vars() {
        return new Variables();
    }

    public static final class Variables {
        private final LinkedHashMap<String, String> vars = new LinkedHashMap<>();

        private Variables() {}

        public Variables set(String key, Object value) {
            vars.put('&' + key, '=' + URLEncoder.encode(value.toString(), StandardCharsets.UTF_8));
            return this;
        }

        public String expand(String base) {
            StringBuilder builder = new StringBuilder(base);
            vars.forEach((key, value) -> builder.append(key).append(value));
            return builder.toString();
        }
    }

}
