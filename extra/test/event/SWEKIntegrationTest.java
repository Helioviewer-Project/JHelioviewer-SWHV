package org.helioviewer.jhv.event;

import java.awt.EventQueue;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.event.EventBatch;
import org.helioviewer.jhv.event.EventCacheTest;
import org.helioviewer.jhv.event.EventDatabase;
import org.helioviewer.jhv.event.EventGeometry;
import org.helioviewer.jhv.event.EventMetadata;
import org.helioviewer.jhv.event.EventParameter;
import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKCatalog;
import org.helioviewer.jhv.event.SWEKGroup;
import org.helioviewer.jhv.event.SWEKHandler;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.event.SolarEvent;
import org.helioviewer.jhv.event.sources.HEKHandler;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

public final class SWEKIntegrationTest {
    private static SWEKCatalog catalog;
    private static EventDatabase database;
    private static final List<SWEKSupplier> definitions = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        if (args[0].startsWith("config-")) {
            checkConfigLoad(args[0]);
            System.out.println(args[0] + " passed");
            return;
        }
        AppInit.loadSpice();
        checkCatalogDefinitions();
        checkCapturedCatalog(args);
        System.out.println("SWEKIntegrationTest passed");
    }

    private static void checkConfigLoad(String scenario) throws Exception {
        catalog = SWEKConfig.load(Map.of("HEK", new HEKHandler()));
        List<SWEKGroup> groups = catalog.getGroups();
        boolean disabledSource = scenario.equals("config-disabled-source");
        check(groups.size() == (disabledSource ? 11 : 0), "configuration must load completely or return no groups");
        try (InputStream in = FileUtils.getResource("/settings/SWEK.json")) {
            JSONObject config = JSONUtils.get(in);
            for (Object value : config.getJSONArray("events_types")) {
                JSONObject group = (JSONObject) value;
                for (Object entry : group.getJSONArray("suppliers")) {
                    JSONObject definition = (JSONObject) entry;
                    SWEKSupplier supplier = catalog.getSupplier(definition.getString("id"));
                    boolean expected = disabledSource && definition.getString("source").equals("HEK");
                    check((supplier != null) == expected, "supplier registration after " + scenario + ": " + definition.getString("id"));
                    if (supplier != null)
                        check(groups.contains(supplier.group()), "registered supplier must belong to a returned group");
                }
            }
        }
        check(disabledSource || catalog.getRelations().isEmpty(), "failed configuration must leave no relations");
        if (disabledSource)
            check(!catalog.getRelations().isEmpty(), "disabled sources must not discard enabled groups' relations");
    }

    private static final class Handler extends HEKHandler {
        RemotePage read(JSONObject page, SWEKSupplier supplier) throws Exception {
            return parseRemotePage(page, supplier, catalog.indexedParameters(supplier));
        }

        String query(SWEKSupplier supplier, long start, long end) throws Exception {
            return createURI(supplier, start, end, 0).getQuery();
        }
    }

    private static void checkCapturedCatalog(String[] args) throws Exception {
        catalog = SWEKConfig.load(Map.of("HEK", new HEKHandler()));
        List<SWEKGroup> groups = catalog.getGroups();
        database = new EventDatabase(catalog);
        Handler handler = new Handler();
        JSONObject fixture = new JSONObject(Files.readString(Path.of(args[0])));
        Map<SWEKSupplier, List<JSONObject>> records = new HashMap<>();
        for (Object value : fixture.getJSONArray("records")) {
            JSONObject record = (JSONObject) value;
            SWEKSupplier supplier = catalog.getSupplier(record.getString("catalog_key"));
            check(supplier != null, "fixture supplier must exist in configuration");
            records.computeIfAbsent(supplier, _ -> new ArrayList<>()).add(record);
        }
        int supplierCount = 0, eventCount = 0;
        for (SWEKGroup group : groups) {
            JSONArray results = new JSONArray();
            for (SWEKSupplier supplier : catalog.getSuppliers(group)) {
                check(records.containsKey(supplier), "missing fixture for " + supplier.id());
                records.get(supplier).forEach(record -> results.put(record.getJSONObject("event")));
            }
            for (SWEKSupplier supplier : catalog.getSuppliers(group)) {
                supplierCount++;
                List<JSONObject> expected = records.get(supplier);
                Map<String, JSONObject> expectedById = new HashMap<>();
                for (JSONObject record : expected)
                    check(expectedById.put(record.getJSONObject("event").getString("kb_archivid"), record) == null, "unique fixture archive ID");
                SWEKHandler.RemotePage page = handler.read(new JSONObject().put("overmax", false).put("result", results), supplier);
                check(page.events().size() == expected.size(), "supplier filtering: " + supplier.id());
                check(page.events().stream().map(SWEKHandler.RemoteEvent::uid).collect(Collectors.toSet()).equals(expectedById.keySet()),
                        "parser retains exact archive IDs: " + supplier.id());
                check(database.storeRemotePage(page, supplier), "store " + supplier.id());
                check(database.storeRemotePage(page, supplier), "idempotent store");
                long start = page.events().stream().mapToLong(event -> event.start()).min().orElseThrow();
                long end = page.events().stream().mapToLong(event -> event.end()).max().orElseThrow();
                String query = handler.query(supplier, start, end);
                check(query.contains("event_type=" + expected.getFirst().getJSONObject("event").getString("event_type")), "event type query code");
                check(!query.contains("param") && query.endsWith("page=1"), "local supplier filtering and one-based pagination for every supplier");
                List<SolarEvent> loaded = database.loadEvents(start, end, supplier, List.of()).events();
                check(loaded.size() == expected.size(), "database must retain every selected event: " + supplier.id());
                Set<Integer> ids = new HashSet<>();
                Set<String> archiveIds = new HashSet<>();
                for (SolarEvent event : loaded) {
                    check(ids.add(event.getUniqueID()), "unique database identity");
                    SolarEvent details = database.getEventDetails(event.getUniqueID()).event();
                    String archiveId = Arrays.stream(details.getAllEventParameters()).filter(parameter -> parameter.getParameterName().equals("kb_archivid"))
                            .map(EventParameter::getParameterValue).findFirst().orElseThrow();
                    check(archiveIds.add(archiveId), "unique loaded archive ID: " + archiveId);
                    JSONObject record = expectedById.get(archiveId);
                    check(record != null, "unexpected loaded archive ID: " + archiveId);
                    check(event.start == TimeUtils.parse(record.getJSONObject("event").getString("event_starttime"))
                            && event.end == TimeUtils.parse(record.getJSONObject("event").getString("event_endtime")), "archive ID retains its event times");
                    Set<String> visibleNames = Arrays.stream(details.getVisibleEventParameters()).map(EventParameter::getParameterName).collect(Collectors.toSet());
                    long removed = Arrays.stream(details.getAllEventParameters()).map(EventParameter::getParameterName)
                            .filter(name -> Set.of("cme_radiallinvel", "event_coord1", "cme_angularwidth").contains(name) && !visibleNames.contains(name)).count();
                    verify(event, record.getJSONObject("expected"), false, (int) removed);
                    verify(details, record.getJSONObject("expected"), true, 0);
                    check(event.getAllEventParameters().length == event.getVisibleEventParameters().length, "normal metadata contains only visible fields");
                    if (supplier.isCactus()) {
                        check(event.getPositionInformation().getEarth() != null, "CACTus rendering and picking need the Earth position");
                        double speed = record.getJSONObject("event").getDouble("cme_radiallinvel");
                        check(event.getCMEParameters().speedKmPerSecond() == speed, "CACTus speed reaches the renderer");
                        org.helioviewer.jhv.plugins.swek.SWEKLayerTest.checkPropagation(event);
                        check(event.getCMEParameters().principalAngleDegree() == record.getJSONObject("event").getDouble("event_coord1"), "CME principal angle");
                        check(event.getCMEParameters().angularWidthDegree() == record.getJSONObject("event").getDouble("cme_angularwidth"), "CME angular width");
                    }
                    eventCount++;
                }
                check(archiveIds.equals(expectedById.keySet()), "database retains exact archive IDs: " + supplier.id());
                try (SWEKSession session = new SWEKSession(catalog)) {
                    EventCacheTest.checkLoadedEvents(session, loaded);
                }
                for (SWEK.Parameter parameter : supplier.getParameterList()) {
                    if (parameter.filter() == null) continue;
                    double threshold = parameter.filter().startValue();
                    long expectedCount = page.events().stream().filter(event -> event.indexedValues().get(parameter.name()) != null
                            && event.indexedValues().get(parameter.name()).doubleValue() >= threshold).count();
                    check(database.loadEvents(start, end, supplier, List.of(new SWEK.Param(parameter.name(), threshold, SWEK.Operand.BIGGER_OR_EQUAL))).events().size()
                            == expectedCount, "configured numeric filter: " + parameter.name());
                }
                check(database.addStoredInterval(start, end, supplier), "completed interval");
                check(database.isStored(start, end, supplier), "reuse completed interval");
            }
        }
        check(groups.size() == 11 && supplierCount == 14, "all configured HEK event types and suppliers are covered");
        System.out.println("Captured HEK catalog passed: " + groups.size() + " event types, " + supplierCount + " suppliers, " + eventCount + " records");
    }

    private static void verify(SolarEvent event, JSONObject expected, boolean full, int removed) {
        check(event.getAllEventParameters().length == expected.getInt(full ? "full_parameters" : "normal_parameters") - removed, "parameter retention");
        EventGeometry position = event.getPositionInformation();
        check((position != null) == expected.getBoolean("has_position"), "position presence");
        if (position == null) return;
        check((position.centralPoint() == null) == expected.isNull("point"), "central point presence");
        if (position.centralPoint() != null) {
            JSONArray point = expected.getJSONArray("point");
            check(Math.abs(position.centralPoint().x - point.getDouble(0)) < 1e-9 && Math.abs(position.centralPoint().y - point.getDouble(1)) < 1e-9
                    && Math.abs(position.centralPoint().z - point.getDouble(2)) < 1e-9, "central point matches baseline");
        }
        JSONArray boundary = expected.getJSONArray("boundary");
        check(position.getBoundBox().length == boundary.length(), "boundary vertex count");
        for (int i = 0; i < boundary.length(); i++)
            check(Math.abs(position.getBoundBox()[i] - boundary.getDouble(i)) < 1e-6, "boundary matches baseline");
        check((position.getEarth() == null) == expected.isNull("earth"), "observer presence");
        if (position.getEarth() != null)
            check(Math.abs(position.getEarth().lon - expected.getDouble("earth")) < 1e-9, "observer matches baseline");
    }

    private static void checkCatalogDefinitions() {
        SWEKGroup activeRegion = new SWEKGroup("Active Region", ""), flare = new SWEKGroup("Flare", "");
        SWEK.Source source = new SWEK.Source("test", List.of(), null, Map.of("region", SWEK.NumericType.INTEGER));
        SWEK.Source targetSource = new SWEK.Source("target", List.of(), null, Map.of("target_region", SWEK.NumericType.INTEGER, "measurement", SWEK.NumericType.DECIMAL));
        SWEKSupplier supplier = new SWEKSupplier(activeRegion, "test", "test", source, "test", List.of(
                new SWEK.Parameter("Region", "Region", new SWEK.ParameterFilter("", 0, 100, 0, 1, ""), false)));
        SWEKSupplier target = new SWEKSupplier(flare, "target", "target", targetSource, "target", List.of());
        definitions.clear();
        definitions.add(supplier);
        definitions.add(target);
        catalog = new SWEKCatalog(0, definitions, List.of());
        check(catalog.getSupplier("test") == supplier, "catalog lookup uses the explicit supplier ID");
        SWEKSupplier sibling = new SWEKSupplier(activeRegion, "sibling", "sibling", source, "sibling", List.of());
        SWEKCatalog ordered = new SWEKCatalog(0, List.of(supplier, target, sibling), List.of());
        check(ordered.getGroups().equals(List.of(activeRegion, flare)), "groups retain first-seen order with interleaved suppliers");
        check(ordered.getSuppliers(activeRegion).equals(List.of(supplier, sibling)), "suppliers retain definition order within each group");
        try {
            new SWEKCatalog(0, List.of(supplier, target, new SWEKSupplier(flare, "Different supplier", "Different display", targetSource, "test", List.of())), List.of());
            throw new AssertionError("duplicate supplier ID was accepted");
        } catch (IllegalArgumentException expected) {
        }
        List<SWEK.Relation> valid = List.of(new SWEK.Relation(activeRegion, flare, List.of(new SWEK.RelatedOn("region", "target_region"))));
        catalog = new SWEKCatalog(0, definitions, valid);
        Map<String, SWEK.NumericType> indexed = catalog.indexedParameters(supplier);
        check(indexed.containsKey("REGION") && indexed.get("region") == SWEK.NumericType.INTEGER, "indexed lookup preserves case-insensitive names");
        check(List.copyOf(indexed.keySet()).equals(List.of("Region")), "lookup preserves the configured spelling");
        try {
            indexed.put("new", SWEK.NumericType.DECIMAL);
            throw new AssertionError("indexed definitions are mutable");
        } catch (UnsupportedOperationException expected) {
        }
        check(catalog.indexedParameters(supplier).equals(Map.of("Region", SWEK.NumericType.INTEGER)), "filter and relationship share the source definition");
        check(catalog.indexedParameters(target).equals(Map.of("target_region", SWEK.NumericType.INTEGER)), "target uses its own source definition");
        expectFailure(List.of(new SWEK.Relation(activeRegion, flare, List.of(new SWEK.RelatedOn("region", "measurement")))), "Incompatible");
        expectFailure(List.of(new SWEK.Relation(activeRegion, flare, List.of(new SWEK.RelatedOn("unknown", "target_region")))), "Missing");
        try {
            new SWEK.Source("bad", List.of(), null, Map.of("value", SWEK.NumericType.INTEGER, "VALUE", SWEK.NumericType.DECIMAL));
            throw new AssertionError("conflicting source definitions were accepted");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("Conflicting"), "source conflict is explained");
        }
        definitions.clear();
        definitions.add(new SWEKSupplier(activeRegion, "test", "test", source, "test", List.of(
                new SWEK.Parameter("unknown", "Unknown", new SWEK.ParameterFilter("", 0, 100, 0, 1, ""), false))));
        expectFailure(List.of(), "Missing");
        definitions.clear();
    }

    private static void expectFailure(List<SWEK.Relation> relations, String explanation) {
        try {
            new SWEKCatalog(0, definitions, relations);
            throw new AssertionError("invalid parameter references were accepted");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains(explanation), "configuration failure is explained");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
