package org.helioviewer.jhv.io;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.util.concurrent.FutureCallback;

public class SoarClient {

    private static final String QUERY_URL = "http://soar.esac.esa.int/soar-sl-tap/tap/sync?REQUEST=doQuery&LANG=ADQL&FORMAT=json&QUERY=";
    private static final String LOAD_URL = "http://soar.esac.esa.int/soar-sl-tap/data?retrieval_type=LAST_PRODUCT&product_type=SCIENCE&data_item_id=";

    enum FileFormat {CDF, FITS, JP2}

    public record DataItem(String id, FileFormat format, long size) {
        @Override
        public String toString() {
            return id;
        }
    }

    private static void doDataSearch(@Nonnull ReceiverItems receiver, String adql) {
        String url = QUERY_URL + URLEncoder.encode(adql, StandardCharsets.UTF_8);
        EventQueueCallbackExecutor.pool.submit(new QueryItems(url), new CallbackItems(receiver));
    }

    public static void submitSearchTime(@Nonnull ReceiverItems receiver, @Nonnull List<String> descriptors, @Nonnull String level, long start, long end) {
        doDataSearch(receiver, adqlSearchTime(descriptors, level, start, end));
    }

    public static void submitSearchSoop(@Nonnull ReceiverItems receiver, @Nonnull List<String> descriptors, @Nonnull String level, String soop) {
        doDataSearch(receiver, adqlSearchSoop(descriptors, level, soop));
    }

    public static void submitGetSoops(@Nonnull ReceiverSoops receiver) {
        String adql = "SELECT DISTINCT soop_name FROM soop ORDER BY soop_name";
        String url = QUERY_URL + URLEncoder.encode(adql, StandardCharsets.UTF_8);
        EventQueueCallbackExecutor.pool.submit(new QuerySoops(url), new CallbackSoops(receiver));
    }

    public static void submitLoad(@Nonnull List<DataItem> items) {
        List<URI> fitsUris = new ArrayList<>();
        List<URI> jp2Uris = new ArrayList<>();
        List<URI> cdfUris = new ArrayList<>();

        for (DataItem item : items) {
            try {
                URI uri = new URI(LOAD_URL + URLEncoder.encode(item.id, StandardCharsets.UTF_8));
                switch (item.format) {
                    case CDF -> cdfUris.add(uri);
                    case FITS -> fitsUris.add(uri);
                    case JP2 -> jp2Uris.add(uri);
                }
            } catch (Exception e) {
                Log.warn(e);
            }
        }
        Load.CDF.getAll(cdfUris);
        Load.FITS.getAll(fitsUris);
        Load.Image.getAll(jp2Uris);
    }

    static void submitTable(@Nonnull URI uri) {
        EventQueueCallbackExecutor.pool.submit(new QueryTable(uri), new CallbackTable());
    }

    private static String adqlSearchTime(List<String> descriptors, String level, long start, long end) {
        String desc = String.join("' OR descriptor='", descriptors);
        return "SELECT data_item_id,file_format,filesize FROM v_sc_data_item WHERE " +
                "(descriptor='" + desc + "') AND " +
                "begin_time >= '" + TimeUtils.format(start) + "' AND end_time <= '" + TimeUtils.format(end) + "' AND " +
                "level='" + level + "' ORDER BY begin_time";
    }

    private static String adqlSearchSoop(List<String> descriptors, String level, String soop) {
        String desc = String.join("' OR descriptor='", descriptors);
        return "SELECT data_item_id,file_format,filesize FROM v_sc_data_item WHERE " +
                "(descriptor='" + desc + "') AND " +
                "soop_name LIKE '%" + soop + "%' AND " +
                "level='" + level + "' ORDER BY begin_time";
    }

    private static List<DataItem> json2DataItems(JSONObject jo) {
        JSONArray data = jo.getJSONArray("data");
        int length = data.length();
        List<DataItem> result = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            JSONArray item = data.getJSONArray(i);
            try {
                result.add(new DataItem(item.getString(0), FileFormat.valueOf(item.getString(1)), item.getLong(2)));
            } catch (Exception ignore) { // ignore unknown formats
            }
        }
        return result;
    }

    private static List<String> json2Soops(JSONObject jo) {
        JSONArray data = jo.getJSONArray("data");
        int length = data.length();
        List<String> result = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            JSONArray item = data.getJSONArray(i);
            result.add(item.getString(0));
        }
        return result;
    }

    public interface ReceiverItems {
        void setSoarResponseItems(List<DataItem> list);
    }

    public interface ReceiverSoops {
        void setSoarResponseSoops(List<String> list);
    }

    private record QueryItems(String url) implements Callable<List<DataItem>> {
        @Override
        public List<DataItem> call() throws Exception {
            JSONObject jo = JSONUtils.get(new URI(url));
            return json2DataItems(jo);
        }
    }

    private record QuerySoops(String url) implements Callable<List<String>> {
        @Override
        public List<String> call() throws Exception {
            JSONObject jo = JSONUtils.get(new URI(url));
            return json2Soops(jo);
        }
    }

    private record QueryTable(URI uri) implements Callable<List<DataItem>> {
        @Override
        public List<DataItem> call() throws Exception {
            return SoarTable.get(uri);
        }
    }

    private record CallbackItems(ReceiverItems receiver) implements FutureCallback<List<DataItem>> {

        @Override
        public void onSuccess(List<DataItem> result) {
            receiver.setSoarResponseItems(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error(t);
            Message.err("An error occurred querying the server", t.getMessage());
        }

    }

    private record CallbackSoops(ReceiverSoops receiver) implements FutureCallback<List<String>> {

        @Override
        public void onSuccess(List<String> result) {
            receiver.setSoarResponseSoops(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error(t);
            Message.err("An error occurred querying the server", t.getMessage());
        }

    }

    private static class CallbackTable implements FutureCallback<List<DataItem>> {

        @Override
        public void onSuccess(List<DataItem> result) {
            submitLoad(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error(t);
            Message.err("An error occurred querying the server", t.getMessage());
        }

    }

}
