package org.helioviewer.jhv.io;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.thread.Task;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

public final class SoarClient {

    private static final String querySoops = "SELECT DISTINCT soop_name FROM soop ORDER BY soop_name";
    private static final UriTemplate queryTemplate = new UriTemplate("https://soar.esac.esa.int/soar-sl-tap/tap/sync",
            UriTemplate.vars().set("REQUEST", "doQuery").set("LANG", "ADQL").set("FORMAT", "json"));
    private static final UriTemplate loadTemplate = new UriTemplate("https://soar.esac.esa.int/soar-sl-tap/data",
            UriTemplate.vars().set("retrieval_type", "LAST_PRODUCT").set("product_type", "SCIENCE"));

    enum SoarFileFormat {CDF, FITS, JP2}

    public record DataItem(String id, SoarFileFormat format, long size) {
        @Override
        public String toString() {
            return id;
        }
    }

    private static void doDataSearch(@Nonnull Consumer<List<DataItem>> onResult, String adql) {
        Task.submitBackground("soar", () -> toDataItems(query(adql)), onResult, "Error getting the data");
    }

    public static void submitSearchTime(@Nonnull Consumer<List<DataItem>> onResult, @Nonnull List<String> descriptors, @Nonnull String level, long start, long end) {
        doDataSearch(onResult, adqlSearchTime(descriptors, level, start, end));
    }

    public static void submitSearchSoop(@Nonnull Consumer<List<DataItem>> onResult, @Nonnull List<String> descriptors, @Nonnull String level, String soop) {
        doDataSearch(onResult, adqlSearchSoop(descriptors, level, soop));
    }

    public static void submitGetSoops(@Nonnull Consumer<List<String>> onResult) {
        Task.submitBackground("soar", () -> toSoops(query(querySoops)), onResult,
                "An error occurred querying the server");
    }

    public static void submitLoad(@Nonnull List<DataItem> items) {
        List<URI> imageUris = new ArrayList<>(items.size());
        List<URI> cdfUris = new ArrayList<>(items.size());

        for (DataItem item : items) {
            try {
                URI uri = new URI(loadTemplate.expand(UriTemplate.vars().set("data_item_id", item.id)));
                switch (item.format) {
                    case FITS, JP2 -> imageUris.add(uri);
                    case CDF -> cdfUris.add(uri);
                }
            } catch (Exception e) {
                Log.warn(e);
            }
        }
        Load.cdf(cdfUris);
        Load.image(imageUris);
    }

    static void submitTable(@Nonnull URI uri) {
        Task.submitBackground(uri.toString(), () -> SoarTable.get(uri), SoarClient::submitLoad, "An error occurred querying the server");
    }

    private static String adqlSearchTime(List<String> descriptors, String level, long start, long end) {
        String desc = String.join("' OR descriptor='", descriptors);
        return "SELECT data_item_id,file_format,filesize FROM v_sc_data_item WHERE " +
                "(descriptor='" + desc + "') AND " +
                "begin_time >= '" + TimeUtils.format(start) + "' AND end_time <= '" + TimeUtils.format(end) + "' AND " +
                "level='" + level + "' ORDER BY begin_time";
    }

    private static String adqlEscape(String value) {
        return value.replace("'", "''");
    }

    private static String adqlSearchSoop(List<String> descriptors, String level, String soop) {
        String desc = String.join("' OR descriptor='", descriptors);
        String escapedSoop = adqlEscape(soop);
        return "SELECT data_item_id,file_format,filesize FROM v_sc_data_item WHERE " +
                "(descriptor='" + desc + "') AND " +
                "soop_name LIKE '%" + escapedSoop + "%' AND " +
                "level='" + level + "' ORDER BY begin_time";
    }

    private static List<DataItem> toDataItems(JSONObject jo) {
        JSONArray data = jo.getJSONArray("data");
        int length = data.length();
        List<DataItem> result = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            JSONArray item = data.getJSONArray(i);
            try {
                result.add(new DataItem(item.getString(0), SoarFileFormat.valueOf(item.getString(1)), item.getLong(2)));
            } catch (Exception ignore) { // ignore unknown formats
            }
        }
        return result;
    }

    private static List<String> toSoops(JSONObject jo) {
        JSONArray data = jo.getJSONArray("data");
        int length = data.length();
        List<String> result = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            JSONArray item = data.getJSONArray(i);
            result.add(item.getString(0));
        }
        return result;
    }

    private static JSONObject query(String adql) throws Exception {
        URI uri = new URI(queryTemplate.expand(UriTemplate.vars().set("QUERY", adql)));
        return JSONUtils.get(uri);
    }

    private SoarClient() {}
}
