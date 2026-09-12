package org.helioviewer.jhv.database;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.helioviewer.jhv.event.EventBatch;
import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKCatalog;
import org.helioviewer.jhv.event.SWEKGroup;
import org.helioviewer.jhv.event.SWEKHandler;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.event.SolarEvent;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.plugins.swek.sources.HEKHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class EventDatabaseTest {

    // Each scenario uses a fresh JVM and cache, except store/reload which share their cache.
    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "store", "reload" -> checkPersistence(args);
            case "parameters" -> checkParameters();
            case "relations" -> checkRelations();
            case "decoding" -> checkDecoding();
            default -> throw new IllegalArgumentException("Unknown database test: " + args[0]);
        }
        System.out.println("EventDatabaseTest " + args[0] + " passed");
    }

    private static void checkPersistence(String[] args) throws Exception {
        Files.createDirectories(Path.of(Directories.CACHE.getPath()));
        SWEKSupplier supplier = new SWEKSupplier(new SWEKGroup("Flare", ""), "SWPC", "SWPC",
                new SWEK.Source("HEK", List.of(), new HEKHandler(), Map.of(
                        "jhv_goesflux", SWEK.NumericType.DECIMAL, "cme_radiallinvel", SWEK.NumericType.DECIMAL,
                        "test_measurement", SWEK.NumericType.DECIMAL, "test_identifier", SWEK.NumericType.INTEGER)), "test_swpc",
                List.of(new SWEK.Parameter("jhv_goesflux", "Flux",
                        new SWEK.ParameterFilter("", 0, 1, 0, 1, ""), false)));
        SWEKCatalog.add(supplier);
        SWEKSupplier cactus = new SWEKSupplier(new SWEKGroup("Coronal Mass Ejection", ""), "CACTus", "CACTus", supplier.source(), "test_cactus",
                List.of(new SWEK.Parameter("cme_radiallinvel", "Speed", new SWEK.ParameterFilter("", 0, 3000, 1000, 100, "km/s"), false)));
        SWEKCatalog.add(cactus);
        SWEKSupplier configured = new SWEKSupplier(supplier.group(), "Configured", "Configured", supplier.source(), "test_configured", List.of(
                new SWEK.Parameter("Test_Measurement", "Measurement", new SWEK.ParameterFilter("", 0, 100, 0, 1, ""), false),
                new SWEK.Parameter("Test_Identifier", "Identifier", new SWEK.ParameterFilter("", 0, 100, 0, 1, ""), false)));
        SWEKCatalog.add(configured);
        SWEKCatalog.setRelations(List.of());
        boolean reload = args[0].equals("reload");
        Path timestampFile = Path.of(Directories.CACHE.getPath(), "test-timestamp.txt");
        long recent = reload ? Long.parseLong(Files.readString(timestampFile)) : System.currentTimeMillis() - 3_600_000;
        long historical = recent - 30L * 24 * 60 * 60 * 1000;
        if (reload) {
            check(!EventDatabase.isStored(recent, recent + 1, supplier), "recent empty coverage expires on reload");
            check(EventDatabase.isStored(historical, historical + 1, supplier), "historical empty coverage survives reload");
            checkFractionalSpeedFilter(cactus);
            checkConfiguredParameters(configured);
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
        SWEKCatalog.setRelations(List.of());
        byte[] json;
        try (ByteArrayOutputStream output = JSONUtils.compressJSON(new JSONObject().put("noposition", true).put("event_title", "test"))) {
            json = output.toByteArray();
        }
        SWEKHandler.RemoteEvent first = new SWEKHandler.RemoteEvent(json, 100, 300, 100, "first",
                Map.of("jhv_goesflux", 1e-5));
        SWEKHandler.RemoteEvent second = new SWEKHandler.RemoteEvent(json, 200, 400, 100, "second",
                Map.of("jhv_goesflux", 1e-6));
        SWEKHandler.RemotePage page = new SWEKHandler.RemotePage(false, List.of(first, second),
                List.of(new SolarEvent.LinkRef("first", "second")));
        check(EventDatabase.storeRemotePage(page, dataSupplier), "store page transaction");
        check(EventDatabase.storeRemotePage(page, dataSupplier), "idempotent repeat");
        check(EventDatabase.loadEvents(250, 250, dataSupplier, List.of()).events().size() == 2, "SQL overlap and UID deduplication");
        check(EventDatabase.loadEvents(250, 250, dataSupplier,
                List.of(new SWEK.Param("jhv_goesflux", 1e-5, SWEK.Operand.BIGGER_OR_EQUAL))).events().size() == 1, "SQL flux filter");
        check(EventDatabase.loadEvents(250, 250, dataSupplier, List.of()).associations().size() == 1, "association deduplication");
        SWEKHandler.RemoteEvent faster = new SWEKHandler.RemoteEvent(json, 100, 300, 100, "faster", Map.of("cme_radiallinvel", 679.9));
        SWEKHandler.RemoteEvent slower = new SWEKHandler.RemoteEvent(json, 100, 300, 100, "slower", Map.of("cme_radiallinvel", 679.1));
        check(EventDatabase.storeRemotePage(new SWEKHandler.RemotePage(false, List.of(faster, slower), List.of()), cactus), "store fractional CME speeds");
        checkFractionalSpeedFilter(cactus);
        JSONObject configuredEvent = new JSONObject().put("kb_archivid", "configured").put("frm_name", configured.supplierName())
                .put("event_starttime", "2024-05-10T00:00:00").put("event_endtime", "2024-05-10T01:00:00")
                .put("test_measurement", "12.75").put("test_identifier", "42");
        SWEKHandler.RemotePage configuredPage = new TestHandler().read(configuredEvent, configured);
        check(configuredPage.events().getFirst().indexedValues().get("Test_Identifier") instanceof Integer, "configured integer keeps its type");
        check(configuredPage.events().getFirst().indexedValues().get("Test_Measurement") instanceof Double, "configured measurement keeps its type");
        check(EventDatabase.storeRemotePage(configuredPage, configured), "store parameters without hardcoded field names");
        checkConfiguredParameters(configured);
        SWEKHandler.RemoteEvent cleared = new SWEKHandler.RemoteEvent(json, 100, 300, 100, "first", Map.of());
        check(EventDatabase.storeRemotePage(new SWEKHandler.RemotePage(false, List.of(cleared), List.of()), dataSupplier), "replace indexed value with absence");
        check(EventDatabase.loadEvents(250, 250, dataSupplier, List.of()).events().size() == 2, "cleared value does not remove the event");
        check(EventDatabase.loadEvents(250, 250, dataSupplier,
                List.of(new SWEK.Param("jhv_goesflux", 1e-5, SWEK.Operand.BIGGER_OR_EQUAL))).events().isEmpty(), "old indexed value is not retained");
        try {
            EventDatabase.loadEvents(250, 250, dataSupplier,
                    List.of(new SWEK.Param("missing_column", 1, SWEK.Operand.BIGGER_OR_EQUAL))).events();
            throw new AssertionError("database failure was reported as an empty successful result");
        } catch (java.util.concurrent.ExecutionException expected) {
            // Worker can now receive and report this failure.
        }
        int malformedCount = 0;
        for (String value : List.of("NaN", "Infinity", "-Infinity", "1e999")) {
            JSONObject malformed = new JSONObject().put("kb_archivid", "nonfinite-" + value).put("frm_name", cactus.supplierName())
                    .put("event_starttime", "2024-05-10T00:00:00").put("event_endtime", "2024-05-10T01:00:00")
                    .put("cme_radiallinvel", value);
            SWEKHandler.RemotePage malformedPage = new TestHandler().read(malformed, cactus);
            check(EventDatabase.storeRemotePage(malformedPage, cactus), "nonfinite optional speed must not reject the page: " + value);
            check(malformedPage.events().getFirst().indexedValues().isEmpty(), "nonfinite speed is not indexed: " + value);
            check(EventDatabase.loadEvents(0, Long.MAX_VALUE, cactus, List.of()).events().size() == 2 + ++malformedCount,
                    "event with nonfinite speed survives storage and decoding");
        }
    }

    private static void checkFractionalSpeedFilter(SWEKSupplier supplier) throws Exception {
        check(EventDatabase.loadEvents(250, 250, supplier, List.of()).events().size() == 2, "both CME events survive storage");
        EventBatch filtered = EventDatabase.loadEvents(250, 250, supplier,
                List.of(new SWEK.Param("cme_radiallinvel", 679.5, SWEK.Operand.BIGGER_OR_EQUAL)));
        check(filtered.events().size() == 1, "speed filter preserves fractional precision");
        check(EventDatabase.loadEvents(250, 250, supplier,
                List.of(new SWEK.Param("cme_radiallinvel", 679.1, SWEK.Operand.BIGGER_OR_EQUAL))).events().size() == 2, "speed filter includes its exact boundary");
    }

    private static void checkConfiguredParameters(SWEKSupplier supplier) throws Exception {
        check(EventDatabase.loadEvents(0, Long.MAX_VALUE, supplier, List.of(
                new SWEK.Param("Test_Measurement", 12.5, SWEK.Operand.BIGGER_OR_EQUAL),
                new SWEK.Param("Test_Identifier", 42, SWEK.Operand.BIGGER_OR_EQUAL))).events().size() == 1, "configured parameters work together in SQL filters");
        check(EventDatabase.loadEvents(0, Long.MAX_VALUE, supplier,
                List.of(new SWEK.Param("Test_Measurement", 13, SWEK.Operand.BIGGER_OR_EQUAL))).events().isEmpty(), "configured filter excludes values below threshold");
    }

    private static final class TestHandler extends HEKHandler {
        RemotePage read(JSONObject event, SWEKSupplier supplier) throws Exception {
            return parseRemotePage(new JSONObject().put("overmax", false).put("result", new JSONArray().put(event)), supplier);
        }
    }

    private static void checkParameters() throws Exception {
        Files.createDirectories(Path.of(Directories.CACHE.getPath()));
        SWEK.Source source = new SWEK.Source("HEK", List.of(), new HEKHandler(), Map.of("speed", SWEK.NumericType.DECIMAL, "region", SWEK.NumericType.INTEGER));
        SWEKSupplier first = supplier(source, "first"), second = supplier(source, "second");
        SWEKCatalog.setRelations(List.of());
        SWEKHandler.RemoteEvent indexedEvent = indexedEvent("a", Map.of("speed", 12.75, "region", 42));
        check(EventDatabase.storeRemotePage(new SWEKHandler.RemotePage(false, List.of(indexedEvent, indexedEvent("missing", Map.of())),
                List.of(new SolarEvent.LinkRef("a", "placeholder"))), first), "storeIndexed initial values and association placeholder");
        check(number("SELECT count(*) FROM event_parameter") == 2, "missing values have no rows");
        check(number("SELECT count(*) FROM event_parameter WHERE typeof(value)='real'") == 1, "decimal storage type survives");
        check(number("SELECT count(*) FROM event_parameter WHERE typeof(value)='integer'") == 1, "integer storage type survives");
        check(number("SELECT count(*) FROM sqlite_master WHERE name='test_first'") == 0, "supplier does not create a table");
        List<SWEK.Param> filters = List.of(new SWEK.Param("SPEED", 12.5, SWEK.Operand.BIGGER_OR_EQUAL),
                new SWEK.Param("speed", 13, SWEK.Operand.SMALLER_OR_EQUAL), new SWEK.Param("region", 42, SWEK.Operand.BIGGER_OR_EQUAL));
        check(EventDatabase.loadEvents(150, 150, first, filters).events().size() == 1, "simultaneous filters and repeated case-insensitive names");
        check(EventDatabase.loadEvents(150, 150, first, List.of()).events().size() == 2, "unfiltered events include missing values");
        long placeholder = number("SELECT id FROM events WHERE uid='placeholder'");
        check(storeIndexed(first, indexedEvent("placeholder", Map.of("speed", 20.0))), "fill placeholder");
        check(number("SELECT id FROM events WHERE uid='placeholder'") == placeholder, "placeholder retains identity");
        check(EventDatabase.loadEvents(150, 150, first, List.of()).associations().size() == 1, "placeholder association survives");
        check(storeIndexed(first, indexedEvent("a", Map.of("speed", 13.0))), "remove one indexed value");
        check(number("SELECT count(*) FROM event_parameter WHERE name='region'") == 0, "removed value is deleted");
        try (Statement statement = EventDatabaseThread.getConnection().createStatement()) {
            statement.executeUpdate("CREATE TRIGGER reject_parameter BEFORE INSERT ON event_parameter WHEN NEW.value=99 BEGIN SELECT RAISE(ABORT,'test rollback'); END");
        }
        EventDatabaseThread.getConnection().commit();
        check(!storeIndexed(first, indexedEvent("a", Map.of("speed", 99.0))), "parameter failure rolls back replacement");
        check(number("SELECT value FROM event_parameter JOIN events ON events.id=event_id WHERE uid='a'") == 13, "rollback restores deleted parameter rows");
        check(!EventDatabase.storeRemotePage(new SWEKHandler.RemotePage(false,
                List.of(indexedEvent("partial", Map.of("speed", 1.0)), indexedEvent("reject", Map.of("speed", 99.0))), List.of()), second), "failed first supplier page");
        check(number("SELECT count(*) FROM events WHERE uid IN ('partial','reject')") == 0, "rollback removes partial events");
        check(number("SELECT count(*) FROM event_type") == 1, "supplier creation participates in rollback");
        check(storeIndexed(second, indexedEvent("a", Map.of("speed", 14.0))), "retry and change event supplier");
        check(number("SELECT count(*) FROM event_parameter p JOIN events e ON e.id=p.event_id WHERE p.type_id!=e.type_id") == 0, "parameter supplier stays consistent");
        check(EventDatabase.loadEvents(150, 150, second, List.of()).events().size() == 1, "moved event belongs to new supplier");
    }

    private static SWEKSupplier supplier(SWEK.Source source, String name) {
        SWEKSupplier supplier = new SWEKSupplier(new SWEKGroup("Flare", ""), name, name, source, "test_" + name, List.of(
                new SWEK.Parameter("speed", "Speed", new SWEK.ParameterFilter("", 0, 100, 0, 1, ""), false),
                new SWEK.Parameter("region", "Region", new SWEK.ParameterFilter("", 0, 100, 0, 1, ""), false)));
        SWEKCatalog.add(supplier);
        return supplier;
    }

    private static SWEKHandler.RemoteEvent indexedEvent(String uid, Map<String, Number> values) throws Exception {
        return event(uid, 100, 200, values);
    }

    private static boolean storeIndexed(SWEKSupplier supplier, SWEKHandler.RemoteEvent indexedEvent) {
        return EventDatabase.storeRemotePage(new SWEKHandler.RemotePage(false, List.of(indexedEvent), List.of()), supplier);
    }

    private static long number(String sql) throws Exception {
        try (Statement statement = EventDatabaseThread.getConnection().createStatement(); ResultSet row = statement.executeQuery(sql)) {
            row.next();
            return row.getLong(1);
        }
    }

    private static void checkRelations() throws Exception {
        Files.createDirectories(Path.of(Directories.CACHE.getPath()));
        SWEK.Source source = new SWEK.Source("HEK", List.of(), new HEKHandler(), Map.of("ar_noaanum", SWEK.NumericType.INTEGER));
        SWEKGroup ar = new SWEKGroup("Active Region", ""), fl = new SWEKGroup("Flare", ""), ce = new SWEKGroup("Coronal Mass Ejection", "");
        SWEKSupplier first = supplier(ar, source, "first"), second = supplier(ar, source, "second"), flare = supplier(fl, source, "flare"), cme = supplier(ce, source, "cme");
        SWEKCatalog.setRelations(List.of(
                new SWEK.Relation(ar, ar, List.of(new SWEK.RelatedOn("ar_noaanum", "ar_noaanum"))),
                new SWEK.Relation(ar, fl, List.of(new SWEK.RelatedOn("ar_noaanum", "ar_noaanum")))));
        store(first, event("a1", 42), event("a2", 42), event("missing", null));
        store(second, event("b1", 42), event("b2", 43));
        store(flare, event("f1", 42));
        store(cme, event("c1", null));
        int a1 = id(first, "a1"), a2 = id(first, "a2"), b1 = id(second, "b1"), f1 = id(flare, "f1");
        List<SolarEvent.Link> links = EventDatabase.loadEvents(100, 200, first, List.of()).associations();
        check(links.size() == 1 && Set.of(links.getFirst().firstId(), links.getFirst().secondId()).equals(Set.of(a1, a2)), "inferred links stay within one supplier");
        check(EventDatabase.loadEvents(100, 200, second, List.of()).associations().isEmpty(), "another supplier is not merged");
        check(related(a1).equals(Set.of(b1, f1)), "AR details find other suppliers and flares");
        check(related(b1).equals(Set.of(a1, a2, f1)), "reverse supplier lookup");
        check(related(f1).equals(Set.of(a1, a2, b1)), "reverse event-type lookup");
        check(related(id(first, "missing")).isEmpty(), "null region is not a relationship");
        check(related(id(cme, "c1")).isEmpty(), "unrelated event type");
        store(first, event("a1", null));
        check(id(first, "a1") == a1, "upsert preserves event identity");
        check(related(a1).isEmpty(), "clearing region removes equality matches");
        check(EventDatabase.loadEvents(100, 200, first, List.of()).associations().isEmpty(), "clearing region removes inferred association");
        store(first, event("a1", 42));
        check(EventDatabase.loadEvents(100, 200, first, List.of()).associations().equals(links), "restoring value restores association");
        check(EventDatabase.storeRemotePage(new SWEKHandler.RemotePage(false, List.of(), List.of(new SolarEvent.LinkRef("a1", "a2"))), first), "store explicit association");
        check(EventDatabase.loadEvents(100, 200, first, List.of()).associations().equals(links), "explicit and inferred links are deduplicated");
        store(first, event("a1", null));
        check(EventDatabase.loadEvents(100, 200, first, List.of()).associations().equals(links), "explicit association survives cleared parameter");
        store(first, event("moved", 42));
        int moved = id(first, "moved");
        store(flare, event("moved", 42));
        check(id(flare, "moved") == moved, "supplier replacement retains event identity");
        check(EventDatabase.getEventDetails(moved).event().getSupplier() == flare, "stored row determines decoded supplier");
        check(related(moved).equals(Set.of(a2, b1)), "current supplier determines detail relationships");
        checkAsymmetricAssociations();
        checkAsymmetricDetails();

        try (Statement statement = EventDatabaseThread.getConnection().createStatement()) {
            statement.executeUpdate("CREATE TRIGGER reject_event BEFORE INSERT ON events WHEN NEW.uid='reject' BEGIN SELECT RAISE(ABORT, 'test rollback'); END");
        }
        EventDatabaseThread.getConnection().commit();
        SWEKSupplier failed = supplier(ce, source, "failed");
        SWEKCatalog.setRelations(SWEKCatalog.getRelations());
        check(!EventDatabase.storeRemotePage(new SWEKHandler.RemotePage(false, List.of(event("rollback", null), event("reject", null)), List.of()), failed), "storage failure reaches caller");
        check(EventDatabase.loadEvents(100, 200, failed, List.of()).events().isEmpty(), "failed page leaves no partial events");
        store(failed, event("rollback", null));
        check(EventDatabase.loadEvents(100, 200, failed, List.of()).events().size() == 1, "retry after rollback");
    }

    private static SWEKSupplier supplier(SWEKGroup group, SWEK.Source source, String name) {
        SWEKSupplier supplier = new SWEKSupplier(group, name, name, source, "test_" + name, List.of());
        SWEKCatalog.add(supplier);
        return supplier;
    }

    private static SWEKHandler.RemoteEvent event(String uid, Integer region) throws Exception {
        return event(uid, 100, 200, region == null ? Map.of() : Map.of("ar_noaanum", region));
    }

    private static SWEKHandler.RemoteEvent event(String uid, long start, long end, Map<String, Number> parameters) throws Exception {
        byte[] json;
        try (ByteArrayOutputStream output = JSONUtils.compressJSON(new JSONObject().put("event_title", uid))) {
            json = output.toByteArray();
        }
        return new SWEKHandler.RemoteEvent(json, start, end, 100, uid, parameters);
    }

    private static void checkAsymmetricAssociations() throws Exception {
        SWEK.Source source = new SWEK.Source("HEK", List.of(), new HEKHandler(), Map.of("from", SWEK.NumericType.INTEGER, "to", SWEK.NumericType.INTEGER));
        SWEKGroup group = new SWEKGroup("Asymmetric", "");
        SWEKSupplier supplier = supplier(group, source, "asymmetric");
        List<SWEK.Relation> relations = new ArrayList<>(SWEKCatalog.getRelations());
        relations.add(new SWEK.Relation(group, group, List.of(new SWEK.RelatedOn("from", "to"))));
        SWEKCatalog.setRelations(relations);
        store(supplier, event("left", 100, 200, Map.of("from", 7)), event("right", 300, 400, Map.of("to", 7)));
        List<SolarEvent.Link> links = EventDatabase.loadEvents(100, 400, supplier, List.of()).associations();
        check(links.size() == 1, "different parameter names match");
        check(EventDatabase.loadEvents(200, 200, supplier, List.of()).associations().equals(links), "association found at first endpoint end");
        check(EventDatabase.loadEvents(300, 300, supplier, List.of()).associations().equals(links), "association found at second endpoint start");
        check(EventDatabase.loadEvents(201, 299, supplier, List.of()).associations().isEmpty(), "neither endpoint in interval");
        store(supplier, event("right", 300, 400, Map.of("to", 8)));
        check(EventDatabase.loadEvents(100, 400, supplier, List.of()).associations().isEmpty(), "changing other endpoint removes inferred association");
    }

    private static void checkAsymmetricDetails() throws Exception {
        SWEK.Source source = new SWEK.Source("HEK", List.of(), new HEKHandler(), Map.of("from", SWEK.NumericType.INTEGER, "to", SWEK.NumericType.INTEGER));
        SWEKGroup leftGroup = new SWEKGroup("Detail Left", ""), rightGroup = new SWEKGroup("Detail Right", "");
        SWEKSupplier left = supplier(leftGroup, source, "detail_left");
        SWEKSupplier sameGroup = supplier(leftGroup, source, "detail_same_group");
        SWEKSupplier right = supplier(rightGroup, source, "detail_right");
        List<SWEK.Relation> relations = new ArrayList<>(SWEKCatalog.getRelations());
        relations.add(new SWEK.Relation(leftGroup, rightGroup, List.of(new SWEK.RelatedOn("from", "to"))));
        relations.add(new SWEK.Relation(leftGroup, leftGroup, List.of(new SWEK.RelatedOn("from", "to"))));
        SWEKCatalog.setRelations(relations);
        store(left, event("detail_left", 100, 200, Map.of("from", 7)));
        store(sameGroup, event("detail_same_group", 100, 200, Map.of("to", 7)));
        store(right, event("detail_right", 100, 200, Map.of("to", 7)), event("detail_unmatched", 100, 200, Map.of("to", 8)));
        int leftId = id(left, "detail_left"), sameId = id(sameGroup, "detail_same_group"), rightId = id(right, "detail_right");
        check(related(leftId).equals(Set.of(sameId, rightId)), "asymmetric detail relationships match both same and different groups");
        check(related(rightId).equals(Set.of(leftId)), "reverse asymmetric detail lookup swaps parameter names");
        check(related(sameId).equals(Set.of(leftId)), "reverse asymmetric detail lookup within one group");
        check(related(id(right, "detail_unmatched")).isEmpty(), "unmatched asymmetric detail value stays unrelated");
    }

    private static void store(SWEKSupplier supplier, SWEKHandler.RemoteEvent... events) {
        check(EventDatabase.storeRemotePage(new SWEKHandler.RemotePage(false, List.of(events), List.of()), supplier), "store events");
    }

    private static int id(SWEKSupplier supplier, String title) throws Exception {
        for (SolarEvent event : EventDatabase.loadEvents(100, 200, supplier, List.of()).events()) {
            SolarEvent full = EventDatabase.getEventDetails(event.getUniqueID()).event();
            if (Arrays.stream(full.getAllEventParameters()).anyMatch(p -> p.getParameterName().equals("event_title") && title.equals(p.getParameterValue()))) return event.getUniqueID();
        }
        throw new AssertionError("Event not found: " + title);
    }

    private static Set<Integer> related(int id) throws Exception {
        List<SolarEvent> events = EventDatabase.getEventDetails(id).relatedEvents();
        for (SolarEvent event : events)
            check(event.getSupplier() == EventDatabase.getEventDetails(event.getUniqueID()).event().getSupplier(), "related row retains its stored supplier");
        return events.stream().map(SolarEvent::getUniqueID).collect(Collectors.toSet());
    }

    private static void checkDecoding() throws Exception {
        Files.createDirectories(Path.of(Directories.CACHE.getPath()));
        BlockingHandler handler = new BlockingHandler();
        SWEKSupplier supplier = new SWEKSupplier(new SWEKGroup("Test", ""), "test", "Test",
                new SWEK.Source("test", List.of(), handler, Map.of()), "test", List.of());
        SWEKCatalog.add(supplier);
        SWEKCatalog.setRelations(List.of());
        check(EventDatabase.storeRemotePage(new SWEKHandler.RemotePage(false,
                List.of(decodingEvent("pause", 10), decodingEvent("malformed", 20), decodingEvent("last", 30)), List.of()), supplier), "store initial snapshot");
        ExecutorService callers = Executors.newFixedThreadPool(2);
        try {
            Future<EventBatch> decoded = callers.submit(() -> {
                handler.caller = Thread.currentThread();
                return EventDatabase.loadEvents(0, 100, supplier, List.of());
            });
            check(handler.entered.await(5, TimeUnit.SECONDS), "decoder started");
            Future<Boolean> write = callers.submit(() -> EventDatabase.storeRemotePage(
                    new SWEKHandler.RemotePage(false, List.of(decodingEvent("new", 40)), List.of(new SolarEvent.LinkRef("pause", "new"))), supplier));
            check(write.get(5, TimeUnit.SECONDS), "database remains available while decoding is paused");
            handler.release.countDown();
            EventBatch batch = decoded.get(5, TimeUnit.SECONDS);
            List<SolarEvent> events = batch.events();
            check(events.size() == 2 && events.get(0).start == 10 && events.get(1).start == 30, "snapshot order and malformed-event isolation are preserved");
            check(handler.onCaller, "decoding runs on the requesting worker");
            check(batch.associations().isEmpty(), "association snapshot excludes concurrent write");
            EventBatch updated = EventDatabase.loadEvents(0, 100, supplier, List.of());
            check(updated.sequence() > batch.sequence(), "snapshot sequence follows database read order");
            check(updated.events().size() == 3 && updated.associations().size() == 1, "subsequent snapshot sees both new observation and link");
        } finally {
            handler.release.countDown();
            callers.shutdownNow();
        }
    }

    private static SWEKHandler.RemoteEvent decodingEvent(String uid, long start) throws Exception {
        try (ByteArrayOutputStream output = JSONUtils.compressJSON(new JSONObject().put("title", uid))) {
            return new SWEKHandler.RemoteEvent(output.toByteArray(), start, start + 1, start, uid, Map.of());
        }
    }

    private static final class BlockingHandler extends HEKHandler {
        private final CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        private volatile Thread caller;
        private volatile boolean onCaller;

        @Override
        public SolarEvent parseEventJSON(JSONObject json, SWEKSupplier supplier, int id, long start, long end, boolean full) {
            if (json.getString("title").equals("malformed")) throw new JSONException("deliberately malformed event");
            if (json.getString("title").equals("pause") && release.getCount() != 0) {
                onCaller = Thread.currentThread() == caller;
                entered.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("decoder was not released");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
            }
            return new SolarEvent(supplier, id, start, end);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
