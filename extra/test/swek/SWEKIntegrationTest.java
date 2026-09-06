package org.helioviewer.jhv.plugins.swek;

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
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.event.EventBatch;
import org.helioviewer.jhv.event.EventCache;
import org.helioviewer.jhv.event.EventCacheTest;
import org.helioviewer.jhv.event.EventGeometry;
import org.helioviewer.jhv.event.EventMetadata;
import org.helioviewer.jhv.event.EventParameter;
import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKCatalog;
import org.helioviewer.jhv.event.SWEKGroup;
import org.helioviewer.jhv.event.SWEKHandler;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.event.SolarEvent;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.plugins.swek.sources.HEKHandler;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

public final class SWEKIntegrationTest {

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
        checkLabels();
        checkActiveEvents();
        checkCapturedCatalog(args);
        System.out.println("SWEKIntegrationTest passed");
    }

    private static void checkConfigLoad(String scenario) throws Exception {
        List<SWEKGroup> groups = SWEKConfig.load();
        boolean disabledSource = scenario.equals("config-disabled-source");
        check(groups.size() == (disabledSource ? 11 : 0), "configuration must load completely or return no groups");
        try (InputStream in = FileUtils.getResource("/settings/SWEK.json")) {
            JSONObject config = JSONUtils.get(in);
            for (Object value : config.getJSONArray("events_types")) {
                JSONObject group = (JSONObject) value;
                for (Object entry : group.getJSONArray("suppliers")) {
                    JSONObject definition = (JSONObject) entry;
                    SWEKSupplier supplier = SWEKCatalog.getSupplier(definition.getString("id"));
                    boolean expected = disabledSource && definition.getString("source").equals("HEK");
                    check((supplier != null) == expected, "supplier registration after " + scenario + ": " + definition.getString("id"));
                    if (supplier != null)
                        check(groups.contains(supplier.group()), "registered supplier must belong to a returned group");
                }
            }
        }
        check(disabledSource || SWEKCatalog.getRelations().isEmpty(), "failed configuration must leave no relations");
        if (disabledSource)
            check(!SWEKCatalog.getRelations().isEmpty(), "disabled sources must not discard enabled groups' relations");
    }

    private static final class Handler extends HEKHandler {
        RemotePage read(JSONObject page, SWEKSupplier supplier) throws Exception {
            return parseRemotePage(page, supplier);
        }

        String query(SWEKSupplier supplier, long start, long end) throws Exception {
            return createURI(supplier, start, end, 0).getQuery();
        }
    }

    private static void checkCapturedCatalog(String[] args) throws Exception {
        List<SWEKGroup> groups = SWEKConfig.load();
        Handler handler = new Handler();
        JSONObject fixture = new JSONObject(Files.readString(Path.of(args[0])));
        Map<SWEKSupplier, List<JSONObject>> records = new HashMap<>();
        for (Object value : fixture.getJSONArray("records")) {
            JSONObject record = (JSONObject) value;
            SWEKSupplier supplier = SWEKCatalog.getSupplier(record.getString("catalog_key"));
            check(supplier != null, "fixture supplier must exist in configuration");
            records.computeIfAbsent(supplier, _ -> new ArrayList<>()).add(record);
        }
        int supplierCount = 0, eventCount = 0;
        for (SWEKGroup group : groups) {
            JSONArray results = new JSONArray();
            for (SWEKSupplier supplier : SWEKCatalog.getSuppliers(group)) {
                check(records.containsKey(supplier), "missing fixture for " + supplier.id());
                records.get(supplier).forEach(record -> results.put(record.getJSONObject("event")));
            }
            for (SWEKSupplier supplier : SWEKCatalog.getSuppliers(group)) {
                supplierCount++;
                List<JSONObject> expected = records.get(supplier);
                Map<String, JSONObject> expectedById = new HashMap<>();
                for (JSONObject record : expected)
                    check(expectedById.put(record.getJSONObject("event").getString("kb_archivid"), record) == null, "unique fixture archive ID");
                SWEKHandler.RemotePage page = handler.read(new JSONObject().put("overmax", false).put("result", results), supplier);
                check(page.events().size() == expected.size(), "supplier filtering: " + supplier.id());
                check(page.events().stream().map(SWEKHandler.RemoteEvent::uid).collect(Collectors.toSet()).equals(expectedById.keySet()),
                        "parser retains exact archive IDs: " + supplier.id());
                check(EventDatabase.storeRemotePage(page, supplier), "store " + supplier.id());
                check(EventDatabase.storeRemotePage(page, supplier), "idempotent store");
                long start = page.events().stream().mapToLong(event -> event.start()).min().orElseThrow();
                long end = page.events().stream().mapToLong(event -> event.end()).max().orElseThrow();
                String query = handler.query(supplier, start, end);
                check(query.contains("event_type=" + expected.getFirst().getJSONObject("event").getString("event_type")), "event type query code");
                check(!query.contains("param") && query.endsWith("page=1"), "local supplier filtering and one-based pagination for every supplier");
                List<SolarEvent> loaded = EventDatabase.loadEvents(start, end, supplier, List.of()).events();
                check(loaded.size() == expected.size(), "database must retain every selected event: " + supplier.id());
                Set<Integer> ids = new HashSet<>();
                Set<String> archiveIds = new HashSet<>();
                for (SolarEvent event : loaded) {
                    check(ids.add(event.getUniqueID()), "unique database identity");
                    SolarEvent details = EventDatabase.getEventDetails(event.getUniqueID()).event();
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
                        check(Double.isFinite(SWEKData.cactusDistance(event, event.end)), "finite CME propagation distance");
                        check(event.getCMEParameters().principalAngleDegree() == record.getJSONObject("event").getDouble("event_coord1"), "CME principal angle");
                        check(event.getCMEParameters().angularWidthDegree() == record.getJSONObject("event").getDouble("cme_angularwidth"), "CME angular width");
                    }
                    eventCount++;
                }
                check(archiveIds.equals(expectedById.keySet()), "database retains exact archive IDs: " + supplier.id());
                EventCacheTest.checkLoadedEvents(loaded);
                for (SWEK.Parameter parameter : supplier.getParameterList()) {
                    if (parameter.filter() == null) continue;
                    double threshold = parameter.filter().startValue();
                    long expectedCount = page.events().stream().filter(event -> event.indexedValues().get(parameter.name()) != null
                            && event.indexedValues().get(parameter.name()).doubleValue() >= threshold).count();
                    check(EventDatabase.loadEvents(start, end, supplier, List.of(new SWEK.Param(parameter.name(), threshold, SWEK.Operand.BIGGER_OR_EQUAL))).events().size()
                            == expectedCount, "configured numeric filter: " + parameter.name());
                }
                check(EventDatabase.addStoredInterval(start, end, supplier), "completed interval");
                check(EventDatabase.isStored(start, end, supplier), "reuse completed interval");
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

    private static void checkActiveEvents() throws Exception {
        EventQueue.invokeAndWait(() -> {
            try {
                run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void run() throws Exception {
        SWEKSupplier supplier = new SWEKSupplier(new SWEKGroup("Test", ""), "test", "Test",
                new SWEK.Source("test", List.of(), null, Map.of()), "test", List.of());
        Method publish = EventCache.class.getDeclaredMethod("replaceEvents", EventBatch.class);
        publish.setAccessible(true);
        SWEKLayer layer = new SWEKLayer(null);
        layer.setEnabled(true);
        SolarEvent first = new SolarEvent(supplier, 1, 100, 200);
        SolarEvent second = new SolarEvent(supplier, 2, 150, 300);
        SolarEvent third = new SolarEvent(supplier, 3, 400, 500);
        List<SolarEvent.Link> links = List.of(new SolarEvent.Link(1, 2), new SolarEvent.Link(2, 3));
        publish.invoke(null, new EventBatch(1, List.of(first, second, third), links));
        check(layer, 150, first);
        List<SWEKLayer.ActiveEvent> active = layer.activeEvents(150);
        EventCache.highlight(active.getFirst().relatedEvents());
        if (active != layer.activeEvents(150) || !active.getFirst().relatedEvents().isHighlighted())
            throw new AssertionError("highlight remains live without rebuilding selections");
        EventCache.highlight(null);
        check(layer, 200, first);
        check(layer, 201, second);
        check(layer, 300, second);
        check(layer, 301, null);
        check(layer, 400, third);
        check(layer, 150, first);
        SolarEvent refreshed = new SolarEvent(supplier, 1, 100, 200);
        publish.invoke(null, new EventBatch(2, List.of(refreshed), links));
        check(layer, 150, second); // Refreshed observation moves to the end of insertion order.
        check(layer, 100, refreshed);
        layer.timeRangeChanged(100, 500);
        check(layer, 100, refreshed);
        layer.setEnabled(false);
        SolarEvent replacement = new SolarEvent(supplier, 1, 100, 200);
        publish.invoke(null, new EventBatch(3, List.of(replacement), links));
        layer.setEnabled(true);
        check(layer, 100, replacement);
        Method remove = EventCache.class.getDeclaredMethod("removeSupplier", SWEKSupplier.class);
        remove.setAccessible(true);
        remove.invoke(null, supplier);
        check(layer, 100, null);
        layer.setEnabled(false);
    }

    private static void check(SWEKLayer layer, long time, SolarEvent expected) {
        List<SWEKLayer.ActiveEvent> active = layer.activeEvents(time);
        if (active != layer.activeEvents(time)) throw new AssertionError("cache reuse");
        if (expected == null) {
            if (!active.isEmpty()) throw new AssertionError("unexpected active event at " + time);
            return;
        }
        if (active.size() != 1) throw new AssertionError("group count");
        SWEKLayer.ActiveEvent item = active.getFirst();
        SolarEvent actual = item.event();
        if (item.relatedEvents() != EventCache.getRelatedEvents(expected.getUniqueID())) throw new AssertionError("group identity");
        if (actual != expected) throw new AssertionError("selection at " + time);
    }

    private static void checkLabels() {
        SWEKSupplier supplier = new SWEKSupplier(new SWEKGroup("Test", ""), "test", "Test",
                new SWEK.Source("test", List.of(), null, Map.of()), "test", List.of());
        EventMetadata.Builder metadata = new EventMetadata.Builder(supplier, true);
        metadata.add("event_title", "Title", "Excluded title", true);
        metadata.add("event_description", "Description", "Excluded description", true);
        metadata.add("link", "Link", "https://example.com/event", true);
        metadata.add("speed", "Speed", "679.9", true);
        metadata.add("text", "Text", "https:// is not a valid URL", true);
        metadata.add("hidden", "Hidden", "Hidden detail", false);
        SolarEvent event = new SolarEvent(supplier, 1, 0, 1, null, SolarEvent.CMEParameters.DEFAULT, metadata.build());
        metadata.add("later", "Later", "Not in the built event", true);
        List<String> expected = List.of("Speed : 6.799E2", "Text : https:// is not a valid URL");
        check(SWEKData.visibleParameterLines(event).equals(expected), "hover labels preserve order, formatting and exclusions");
        check(SWEKData.visibleParameterLines(event).equals(expected), "repeated labels preserve their values");
        check(event.getAllEventParameters().length == 6 && event.getVisibleEventParameters().length == 5, "detail lists retain URLs and hidden metadata");
        EventParameter link = event.getVisibleEventParameters()[2];
        check(link.isUrl(), "URL classification");
        check(link.getDisplayParameterValue().equals("<a href=\"https://example.com/event\">Open URL</a>"), "detail link formatting is unchanged");
        EventParameter region = new EventParameter("ar_noaanum", "Region", "13664");
        check(region.getDisplayParameterValue().contains("NOAA%2013664") && !region.isUrl(), "generated region links remain distinct from URL values");
    }

    private static void checkCatalogDefinitions() {
        SWEKGroup activeRegion = new SWEKGroup("Active Region", ""), flare = new SWEKGroup("Flare", "");
        SWEK.Source source = new SWEK.Source("test", List.of(), null, Map.of("region", SWEK.NumericType.INTEGER));
        SWEK.Source targetSource = new SWEK.Source("target", List.of(), null, Map.of("target_region", SWEK.NumericType.INTEGER, "measurement", SWEK.NumericType.DECIMAL));
        SWEKSupplier supplier = new SWEKSupplier(activeRegion, "test", "test", source, "test", List.of(
                new SWEK.Parameter("Region", "Region", new SWEK.ParameterFilter("", 0, 100, 0, 1, ""), false)));
        SWEKSupplier target = new SWEKSupplier(flare, "target", "target", targetSource, "target", List.of());
        SWEKCatalog.clear();
        SWEKCatalog.add(supplier);
        SWEKCatalog.add(target);
        check(SWEKCatalog.getSupplier("test") == supplier, "catalog lookup uses the explicit supplier ID");
        try {
            SWEKCatalog.add(new SWEKSupplier(flare, "Different supplier", "Different display", targetSource, "test", List.of()));
            throw new AssertionError("duplicate supplier ID was accepted");
        } catch (IllegalArgumentException expected) {
            check(SWEKCatalog.getSupplier("test") == supplier, "duplicate ID does not replace the original supplier");
            check(SWEKCatalog.getSuppliers(flare).equals(List.of(target)), "duplicate ID does not enter a group");
        }
        List<SWEK.Relation> valid = List.of(new SWEK.Relation(activeRegion, flare, List.of(new SWEK.RelatedOn("region", "target_region"))));
        SWEKCatalog.setRelations(valid);
        Map<String, SWEK.NumericType> indexed = SWEKCatalog.indexedParameters(supplier);
        check(indexed.containsKey("REGION") && indexed.get("region") == SWEK.NumericType.INTEGER, "indexed lookup preserves case-insensitive names");
        check(List.copyOf(indexed.keySet()).equals(List.of("Region")), "lookup preserves the configured spelling");
        try {
            indexed.put("new", SWEK.NumericType.DECIMAL);
            throw new AssertionError("indexed definitions are mutable");
        } catch (UnsupportedOperationException expected) {
            check(!indexed.containsKey("new"), "indexed definitions remain unchanged");
        }
        check(SWEKCatalog.indexedParameters(supplier).equals(Map.of("Region", SWEK.NumericType.INTEGER)), "filter and relationship share the source definition");
        check(SWEKCatalog.indexedParameters(target).equals(Map.of("target_region", SWEK.NumericType.INTEGER)), "target uses its own source definition");
        expectFailure(List.of(new SWEK.Relation(activeRegion, flare, List.of(new SWEK.RelatedOn("region", "measurement")))), "Incompatible");
        expectFailure(List.of(new SWEK.Relation(activeRegion, flare, List.of(new SWEK.RelatedOn("unknown", "target_region")))), "Missing");
        check(SWEKCatalog.getRelations().equals(valid), "rejected declarations preserve relationships");
        check(SWEKCatalog.indexedParameters(supplier).equals(Map.of("Region", SWEK.NumericType.INTEGER)), "rejected declarations preserve field definitions");
        try {
            new SWEK.Source("bad", List.of(), null, Map.of("value", SWEK.NumericType.INTEGER, "VALUE", SWEK.NumericType.DECIMAL));
            throw new AssertionError("conflicting source definitions were accepted");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("Conflicting"), "source conflict is explained");
        }
        SWEKCatalog.clear();
        SWEKCatalog.add(new SWEKSupplier(activeRegion, "test", "test", source, "test", List.of(
                new SWEK.Parameter("unknown", "Unknown", new SWEK.ParameterFilter("", 0, 100, 0, 1, ""), false))));
        expectFailure(List.of(), "Missing");
        SWEKCatalog.clear();
    }

    private static void expectFailure(List<SWEK.Relation> relations, String explanation) {
        try {
            SWEKCatalog.setRelations(relations);
            throw new AssertionError("invalid parameter references were accepted");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains(explanation), "configuration failure is explained");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
