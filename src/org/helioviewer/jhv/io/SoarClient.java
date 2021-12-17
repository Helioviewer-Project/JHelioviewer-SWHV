package org.helioviewer.jhv.io;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log2;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONArray;

import com.google.common.util.concurrent.FutureCallback;

public class SoarClient {

    public interface Receiver {
        void setDataItems(List<DataItem> items);
    }

    private static final String SEARCH_URL = "http://soar.esac.esa.int/soar-sl-tap/tap/sync?REQUEST=doQuery&LANG=ADQL&FORMAT=json&QUERY=";
    private static final String LOAD_URL = "http://soar.esac.esa.int/soar-sl-tap/data?retrieval_type=LAST_PRODUCT&product_type=SCIENCE&data_item_id=";

    private enum FileFormat {CDF, FITS, JP2}

    public record DataItem(String id, FileFormat format, long size) {
        @Override
        public String toString() {
            return id;
        }
    }

    public static void submitSearch(@Nonnull Receiver receiver, @Nonnull List<String> descriptors, @Nonnull String level, long start, long end) {
        EventQueueCallbackExecutor.pool.submit(new SoarSearch(descriptors, level, start, end), new Callback(receiver));
    }

    public static void submitLoad(@Nonnull List<DataItem> items) {
        List<URI> fitsUris = new ArrayList<>();
        List<URI> jp2Uris = new ArrayList<>();
        List<URI> cdfUris = new ArrayList<>();

        for (DataItem item : items) {
            try {
                URI uri = new URI(LOAD_URL + item.id);
                switch (item.format) {
                    case CDF -> cdfUris.add(uri);
                    case FITS -> fitsUris.add(uri);
                    case JP2 -> jp2Uris.add(uri);
                }
            } catch (Exception e) {
                Log2.warn(e);
            }
        }
        Load.CDF.getAll(cdfUris);
        Load.FITS.getAll(fitsUris);
        Load.Image.getAll(jp2Uris);
    }

    private record SoarSearch(List<String> descriptors, String level, long start, long end)
            implements Callable<List<DataItem>> {
        @Override
        public List<DataItem> call() throws Exception {
            String sqldesc = String.join("' OR descriptor='", descriptors);
            String select = "SELECT data_item_id,file_format,filesize FROM v_sc_data_item WHERE " +
                    "(descriptor='" + sqldesc + "') AND " +
                    "begin_time >= '" + TimeUtils.format(start) + "' AND end_time <= '" + TimeUtils.format(end) + "' AND " +
                    "level='" + level + "' ORDER BY begin_time";
            URI uri = new URI(SEARCH_URL + URLEncoder.encode(select, StandardCharsets.UTF_8));
            JSONArray data = JSONUtils.get(uri).getJSONArray("data");

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
    }

    private record Callback(Receiver receiver) implements FutureCallback<List<DataItem>> {

        @Override
        public void onSuccess(List<DataItem> result) {
            receiver.setDataItems(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log2.error(t);
            Message.err("An error occurred while opening the remote file:", t.getMessage(), false);
        }

    }

}
