package org.helioviewer.jhv.io;

import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class JSONUtils {

    private static final int BUFSIZ = 65536;

    public static JSONObject get(InputStream in) throws IOException, JSONException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8), BUFSIZ)) {
            return new JSONObject(new JSONTokener(reader));
        }
    }

    public static JSONObject get(Reader in) throws JSONException {
        return new JSONObject(new JSONTokener(in));
    }

    public static JSONObject get(URI uri) throws IOException, JSONException {
        try (NetClient nc = NetClient.of(uri)) {
            return get(nc.getReader());
        }
    }

    public static JSONObject get(String uri) throws IOException, JSONException {
        try (NetClient nc = NetClient.of(uri)) {
            return get(nc.getReader());
        }
    }

    public static ByteArrayOutputStream compressJSON(JSONObject json) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFSIZ);
        try (GZIPOutputStream gz = new GZIPOutputStream(baos, BUFSIZ);
             OutputStreamWriter out = new OutputStreamWriter(gz, StandardCharsets.UTF_8)) {
            json.write(out);
        }
        return baos;
    }

}
