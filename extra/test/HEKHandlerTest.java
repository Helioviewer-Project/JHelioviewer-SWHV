package org.helioviewer.jhv.plugins.swek.sources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKGroup;
import org.helioviewer.jhv.event.SWEKHandler;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class HEKHandlerTest {

    public static void main(String[] args) throws Exception {
        HEKHandler handler = new HEKHandler();
        SWEKSupplier supplier = new SWEKSupplier(new SWEKGroup("Flare", ""), "SWPC", "NOAA SWPC",
                new SWEK.Source("HEK", List.of(), handler), "test_swpc", List.of());
        JSONObject fixture = new JSONObject(Files.readString(Path.of("extra/test/data/hek-events.json")));
        SWEKHandler.RemotePage page = handler.parseRemotePage(fixture, supplier);
        check(page.events().size() == 1, "defensive supplier filtering");
        SWEKHandler.RemoteEvent remote = page.events().getFirst();
        check(remote.start() == TimeUtils.parse("2024-05-10T00:10:00"), "UTC start");
        check(remote.end() == TimeUtils.parse("2024-05-10T00:22:00"), "UTC end");
        JSONObject swpc;
        try (GZIPInputStream stream = new GZIPInputStream(new ByteArrayInputStream(remote.compressedJson()))) {
            swpc = new JSONObject(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
        check(Math.abs(swpc.getDouble("jhv_goesflux") - 1.3e-5) < 1e-12, "GOES class to flux");
        JSONObject bad = new JSONObject(swpc.toString()).put("event_endtime", "bad");
        expectFailure(() -> handler.parseRemotePage(new JSONObject().put("overmax", false)
                .put("result", new JSONArray().put(bad)), supplier), IOException.class, "bad required event data fails page");
        expectFailure(() -> handler.parseRemotePage(new JSONObject().put("result", new JSONArray()), supplier),
                JSONException.class, "missing pagination metadata fails page");
        for (String field : List.of("kb_archivid", "frm_name", "event_starttime", "event_endtime")) {
            JSONObject missing = new JSONObject(swpc.toString());
            missing.remove(field);
            expectFailure(() -> handler.parseRemotePage(new JSONObject().put("overmax", false)
                    .put("result", new JSONArray().put(missing)), supplier), IOException.class, "missing required field: " + field);
        }
        JSONObject blankId = new JSONObject(swpc.toString()).put("kb_archivid", " ");
        expectFailure(() -> handler.parseRemotePage(new JSONObject().put("overmax", false)
                .put("result", new JSONArray().put(blankId)), supplier), IOException.class, "blank ID");

        SWEKSupplier spocaSupplier = new SWEKSupplier(new SWEKGroup("Coronal Hole", ""), "SPoCA", "SPoCA",
                supplier.source(), "test_spoca", List.of());
        SWEKHandler.RemotePage spocaPage = handler.parseRemotePage(fixture, spocaSupplier);
        check(spocaPage.events().size() == 1 && !spocaPage.associations().isEmpty(), "live association to unloaded neighbor retained");
        System.out.println("HEKHandlerTest passed");
    }

    private interface CheckedAction { void run() throws Exception; }

    private static void expectFailure(CheckedAction action, Class<? extends Exception> type, String message) throws Exception {
        try {
            action.run();
        } catch (Exception expected) {
            if (type.isInstance(expected))
                return;
            throw expected;
        }
        throw new AssertionError(message);
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }
}
