package org.helioviewer.jhv.base;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

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

}
