package org.helioviewer.jhv.io;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.threads.EDTCallbackExecutor;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.util.concurrent.FutureCallback;

public class SoarClient {

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

    private static void doDataSearch(@Nonnull ReceiverItems receiver, String adql) {
        EDTCallbackExecutor.pool.submit(new QueryItems(adql), new CallbackItems(receiver));
    }

    public static void submitSearchTime(@Nonnull ReceiverItems receiver, @Nonnull List<String> descriptors, @Nonnull String level, long start, long end) {
        doDataSearch(receiver, adqlSearchTime(descriptors, level, start, end));
    }

    public static void submitSearchSoop(@Nonnull ReceiverItems receiver, @Nonnull List<String> descriptors, @Nonnull String level, String soop) {
        doDataSearch(receiver, adqlSearchSoop(descriptors, level, soop));
    }

    public static void submitGetSoops(@Nonnull ReceiverSoops receiver) {
        EDTCallbackExecutor.pool.submit(new QuerySoops("SELECT DISTINCT soop_name FROM soop ORDER BY soop_name"), new CallbackSoops(receiver));
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
        Load.CDF.getAll(cdfUris);
        Load.Image.getAll(imageUris);
    }

    static void submitTable(@Nonnull URI uri) {
        EDTCallbackExecutor.pool.submit(new QueryTable(uri), new CallbackTable());
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

    private static List<DataItem> json2DataItems(JSONObject jo) {
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

    private record QueryItems(String adql) implements Callable<List<DataItem>> {
        @Override
        public List<DataItem> call() throws Exception {
            URI uri = new URI(queryTemplate.expand(UriTemplate.vars().set("QUERY", adql)));
            JSONObject jo = JSONUtils.get(uri);
            return json2DataItems(jo);
        }
    }

    private record QuerySoops(String adql) implements Callable<List<String>> {
        @Override
        public List<String> call() throws Exception {
            URI uri = new URI(queryTemplate.expand(UriTemplate.vars().set("QUERY", adql)));
            JSONObject jo = JSONUtils.get(uri);
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
        public void onSuccess(@Nonnull List<DataItem> result) {
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
        public void onSuccess(@Nonnull List<String> result) {
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
        public void onSuccess(@Nonnull List<DataItem> result) {
            submitLoad(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error(t);
            Message.err("An error occurred querying the server", t.getMessage());
        }
    }

}
