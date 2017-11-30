package org.helioviewer.jhv.base;

import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import org.helioviewer.jhv.log.Log;
import org.json.JSONObject;
import org.json.JSONTokener;

public class JSONUtils {

    private static final int BUFSIZ = 65536;

    public static JSONObject getJSONStream(InputStream in) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8), BUFSIZ)) {
            return new JSONObject(new JSONTokener(reader));
        } catch (Exception e) {
            Log.error("Invalid JSON response " + e);
            return new JSONObject();
        }
    }

    public static ByteArrayOutputStream compressJSON(JSONObject json) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(baos, BUFSIZ);
             OutputStreamWriter out = new OutputStreamWriter(gz, StandardCharsets.UTF_8)) {
            json.write(out);
        }
        return baos;
    }

}
