package org.helioviewer.jhv.base;

import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import org.helioviewer.jhv.base.logging.Log;
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

    public static byte[] compressJSON(JSONObject json) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter out = new OutputStreamWriter(new GZIPOutputStream(baos, BUFSIZ), StandardCharsets.UTF_8);
        json.write(out);
        out.close();

        return baos.toByteArray();
    }

}
