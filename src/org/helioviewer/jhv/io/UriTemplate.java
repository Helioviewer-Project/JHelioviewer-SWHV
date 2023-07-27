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

    public interface Variables {
        Variables set(String name, Object value);

        String expand(String base);
    }

    public static Variables vars() {
        return new VariablesImpl();
    }

    private static class VariablesImpl implements Variables {
        private final LinkedHashMap<String, String> params = new LinkedHashMap<>();

        @Override
        public Variables set(String param, Object value) {
            params.put('&' + param, '=' + URLEncoder.encode(value.toString(), StandardCharsets.UTF_8));
            return this;
        }

        @Override
        public String expand(String base) {
            StringBuilder builder = new StringBuilder(base);
            params.forEach((key, value) -> builder.append(key).append(value));
            return builder.toString();
        }
    }

}
