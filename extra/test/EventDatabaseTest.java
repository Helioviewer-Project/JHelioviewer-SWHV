package org.helioviewer.jhv.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.helioviewer.jhv.event.JHVEvent;
import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKCatalog;
import org.helioviewer.jhv.event.SWEKGroup;
import org.helioviewer.jhv.event.SWEKHandler;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.plugins.swek.sources.HEKHandler;

import org.json.JSONObject;

// Runs with an isolated user.home supplied by run_hek_tests.py.
public final class EventDatabaseTest {

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of(Directories.CACHE.getPath()));
        SWEKSupplier supplier = new SWEKSupplier(new SWEKGroup("Flare", ""), "SWPC", "SWPC",
                new SWEK.Source("HEK", List.of(), new HEKHandler()), "test_swpc",
                List.of(new SWEK.Parameter("jhv_goesflux", "Flux",
                        new SWEK.ParameterFilter("", 0, 1, 0, 1, "", "REAL"), false)));
        SWEKCatalog.add(supplier);
        SWEKCatalog.setRelatedEvents(List.of());
        boolean reload = args[0].equals("reload");
        Path timestampFile = Path.of(Directories.CACHE.getPath(), "test-timestamp.txt");
        long recent = reload ? Long.parseLong(Files.readString(timestampFile)) : System.currentTimeMillis() - 3_600_000;
        long historical = recent - 30L * 24 * 60 * 60 * 1000;
        if (reload) {
            check(!EventDatabase.isStored(recent, recent + 1, supplier), "recent empty coverage expires on reload");
            check(EventDatabase.isStored(historical, historical + 1, supplier), "historical empty coverage survives reload");
            System.out.println("EventDatabaseTest reload passed");
            return;
        }

        Files.writeString(timestampFile, Long.toString(recent));
        check(EventDatabase.addStoredInterval(historical - 60_000, historical + 60_000, supplier), "store historical empty coverage");
        check(EventDatabase.addStoredInterval(recent - 60_000, recent + 60_000, supplier), "store empty coverage");
        check(EventDatabase.isStored(recent, recent + 1, supplier), "reuse successful empty response within session");
        // Keep this supplier empty for the second JVM's invalidation regression.
        SWEKSupplier dataSupplier = new SWEKSupplier(supplier.group(), "test", "test", supplier.source(),
                "test_data", supplier.getParameterList());
        SWEKCatalog.add(dataSupplier);
        SWEKCatalog.setRelatedEvents(List.of());
        byte[] json;
        try (var output = JSONUtils.compressJSON(new JSONObject().put("noposition", true).put("event_title", "test"))) {
            json = output.toByteArray();
        }
        SWEKHandler.RemoteEvent first = new SWEKHandler.RemoteEvent(json, 100, 300, 100, "first",
                List.of(new SWEKHandler.RemoteParameter("jhv_goesflux", 1e-5)));
        SWEKHandler.RemoteEvent second = new SWEKHandler.RemoteEvent(json, 200, 400, 100, "second",
                List.of(new SWEKHandler.RemoteParameter("jhv_goesflux", 1e-6)));
        SWEKHandler.RemotePage page = new SWEKHandler.RemotePage(false, List.of(first, second),
                List.of(new JHVEvent.LinkRef("first", "second")));
        check(EventDatabase.storeRemotePage(page, dataSupplier), "store page transaction");
        check(EventDatabase.storeRemotePage(page, dataSupplier), "idempotent repeat");
        check(EventDatabase.events2Program(250, 250, dataSupplier, List.of()).size() == 2, "SQL overlap and UID deduplication");
        check(EventDatabase.events2Program(250, 250, dataSupplier,
                List.of(new SWEK.Param("jhv_goesflux", 1e-5, SWEK.Operand.BIGGER_OR_EQUAL))).size() == 1, "SQL flux filter");
        check(EventDatabase.associations2Program(250, 250, dataSupplier).size() == 1, "association deduplication");
        try {
            EventDatabase.events2Program(250, 250, dataSupplier,
                    List.of(new SWEK.Param("missing_column", 1, SWEK.Operand.BIGGER_OR_EQUAL)));
            throw new AssertionError("database failure was reported as an empty successful result");
        } catch (java.util.concurrent.ExecutionException expected) {
            // Worker can now receive and report this failure.
        }
        System.out.println("EventDatabaseTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }
}
