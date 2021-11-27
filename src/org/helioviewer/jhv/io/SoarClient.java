package org.helioviewer.jhv.io;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONArray;

import com.google.common.util.concurrent.FutureCallback;

public class SoarClient {

    public static void submitSearch(@Nonnull SoarReceiver receiver, @Nonnull String descriptor, @Nonnull String level, long start, long end) {
        EventQueueCallbackExecutor.pool.submit(new SoarSearch(descriptor, level, start, end), new Callback(receiver));
    }

    private static final String SOAR_URL = "http://soar.esac.esa.int/soar-sl-tap/tap/sync?REQUEST=doQuery&LANG=ADQL&FORMAT=json&QUERY=";

    private record SoarSearch(String descriptor, String level, long start,
                              long end) implements Callable<Map<String, String>> {
        @Override
        public Map<String, String> call() throws Exception {
            String select = "SELECT data_item_id,filename from v_sc_data_item " +
                    "WHERE descriptor='" + descriptor +
                    "' AND begin_time >= '" + TimeUtils.format(start) + "' and begin_time <= '" + TimeUtils.format(end) +
                    "' and level='" + level + "' ORDER BY begin_time";
            URI uri = new URI(SOAR_URL + URLEncoder.encode(select, StandardCharsets.UTF_8));
            JSONArray data = JSONUtils.get(uri).getJSONArray("data");

            int length = data.length();
            Map<String, String> result = new LinkedHashMap<>(length);
            for (int i = 0; i < length; i++) {
                JSONArray item = data.getJSONArray(i);
                result.put(item.getString(0), item.getString(1));
            }

            return result;
        }
    }

    private record Callback(SoarReceiver receiver) implements FutureCallback<Map<String, String>> {

        @Override
        public void onSuccess(Map<String, String> result) {
            receiver.setSoarItems(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error("An error occurred while opening the remote file:", t);
            Message.err("An error occurred while opening the remote file:", t.getMessage(), false);
        }

    }

}
