package org.helioviewer.jhv.base;

import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.zip.GZIPOutputStream;

import org.json.JSONObject;
import org.json.JSONTokener;

public class JSONUtils {

    private static final int BUFSIZ = 65536;

    public static JSONObject getJSONStream(InputStream in) {
        try {
            return new JSONObject(new JSONTokener(new BufferedReader(new InputStreamReader(in, "UTF-8"), BUFSIZ)));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public static byte[] writeJSONCompressed(JSONObject json) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter out = new OutputStreamWriter(new GZIPOutputStream(baos, BUFSIZ), "UTF-8");
        json.write(out);
        out.close();

        return baos.toByteArray();
    }

}
