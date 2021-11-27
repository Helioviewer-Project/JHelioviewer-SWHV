package org.helioviewer.jhv.io;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONArray;

import com.google.common.util.concurrent.FutureCallback;

public class SoarClient {

    private static final String SEARCH_URL = "http://soar.esac.esa.int/soar-sl-tap/tap/sync?REQUEST=doQuery&LANG=ADQL&FORMAT=json&QUERY=";
    private static final String LOAD_URL = "http://soar.esac.esa.int/soar-sl-tap/data?retrieval_type=LAST_PRODUCT&product_type=SCIENCE&data_item_id=";

    public static void submitSearch(@Nonnull SoarReceiver receiver, @Nonnull String descriptor, @Nonnull String level, long start, long end) {
        EventQueueCallbackExecutor.pool.submit(new SoarSearch(descriptor, level, start, end), new Callback(receiver));
    }

    public static void submitLoad(@Nonnull List<String> descriptors) {
        List<URI> uris = new ArrayList<>(descriptors.size());
        for (String descriptor : descriptors) {
            try {
                uris.add(new URI(LOAD_URL + descriptor));
            } catch (Exception e) {
                Log.error(e);
            }
        }
        Load.FITS.getAll(uris);
    }

    private record SoarSearch(String descriptor, String level, long start, long end) implements Callable<List<String>> {
        @Override
        public List<String> call() throws Exception {
            String select = "SELECT data_item_id from v_sc_data_item " +
                    "WHERE descriptor='" + descriptor +
                    "' AND begin_time >= '" + TimeUtils.format(start) + "' and begin_time <= '" + TimeUtils.format(end) +
                    "' and level='" + level + "' ORDER BY begin_time";
            URI uri = new URI(SEARCH_URL + URLEncoder.encode(select, StandardCharsets.UTF_8));
            JSONArray data = JSONUtils.get(uri).getJSONArray("data");

            int length = data.length();
            List<String> result = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                JSONArray item = data.getJSONArray(i);
                result.add(item.getString(0));
            }
            return result;
        }
    }

    private record Callback(SoarReceiver receiver) implements FutureCallback<List<String>> {

        @Override
        public void onSuccess(List<String> result) {
            receiver.setSoarItems(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error("An error occurred while opening the remote file:", t);
            Message.err("An error occurred while opening the remote file:", t.getMessage(), false);
        }

    }

}
