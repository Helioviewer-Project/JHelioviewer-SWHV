package org.helioviewer.jhv.plugins.swek;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.event.JHVEvent;
import org.helioviewer.jhv.event.RelatedEventsTest;
import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKCatalog;
import org.helioviewer.jhv.event.SWEKGroup;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.plugins.swek.sources.HEKHandler;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

// Actual supplier configuration and captured records, with geometry expectations from pre-series ee8e1ca19.
public final class HEKCatalogTest {

    private static final class Handler extends HEKHandler {
        RemotePage read(JSONObject page, SWEKSupplier supplier) throws Exception {
            return parseRemotePage(page, supplier);
        }

        String query(SWEKSupplier supplier, long start, long end) throws Exception {
            return createURI(supplier, start, end, 0).getQuery();
        }
    }

    public static void main(String[] args) throws Exception {
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();
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
                check(records.containsKey(supplier), "missing fixture for " + SWEKCatalog.key(supplier));
                records.get(supplier).forEach(record -> results.put(record.getJSONObject("event")));
            }
            for (SWEKSupplier supplier : SWEKCatalog.getSuppliers(group)) {
                supplierCount++;
                List<JSONObject> expected = records.get(supplier);
                var page = handler.read(new JSONObject().put("overmax", false).put("result", results), supplier);
                check(page.events().size() == expected.size(), "supplier filtering: " + SWEKCatalog.key(supplier));
                check(EventDatabase.storeRemotePage(page, supplier), "store " + SWEKCatalog.key(supplier));
                check(EventDatabase.storeRemotePage(page, supplier), "idempotent store");
                long start = page.events().stream().mapToLong(event -> event.start()).min().orElseThrow();
                long end = page.events().stream().mapToLong(event -> event.end()).max().orElseThrow();
                String query = handler.query(supplier, start, end);
                check(query.contains("event_type=" + expected.getFirst().getJSONObject("event").getString("event_type")), "event type query code");
                check(!query.contains("param") && query.endsWith("page=1"), "local supplier filtering and one-based pagination for every supplier");
                List<JHVEvent> loaded = EventDatabase.events2Program(start, end, supplier, List.of());
                check(loaded.size() == expected.size(), "database must retain every selected event: " + SWEKCatalog.key(supplier));
                Set<Integer> ids = new HashSet<>();
                for (JHVEvent event : loaded) {
                    check(ids.add(event.getUniqueID()), "unique database identity");
                    JSONObject record = expected.stream().filter(r -> TimeUtils.parse(r.getJSONObject("event").getString("event_starttime")) == event.start
                            && TimeUtils.parse(r.getJSONObject("event").getString("event_endtime")) == event.end).findFirst().orElseThrow();
                    verify(event, record.getJSONObject("expected"), false);
                    verify(EventDatabase.getEventDetails(event.getUniqueID(), supplier).event(), record.getJSONObject("expected"), true);
                    if (supplier.isCactus()) {
                        check(event.getPositionInformation().getEarth() != null, "CACTus rendering and picking need the Earth position");
                        double speed = record.getJSONObject("event").getDouble("cme_radiallinvel");
                        check(SWEKData.readCMESpeed(event) == speed, "CACTus speed reaches the renderer");
                        check(Double.isFinite(SWEKData.cactusDistance(event, event.end)), "finite CME propagation distance");
                        check(SWEKData.readCMEPrincipalAngleDegree(event) == record.getJSONObject("event").getDouble("event_coord1"), "CME principal angle");
                        check(SWEKData.readCMEAngularWidthDegree(event) == record.getJSONObject("event").getDouble("cme_angularwidth"), "CME angular width");
                    }
                    eventCount++;
                }
                RelatedEventsTest.checkLoadedEvents(loaded);
                for (SWEK.Parameter parameter : supplier.getParameterList()) {
                    if (parameter.filter() == null) continue;
                    double threshold = parameter.filter().startValue();
                    long expectedCount = page.events().stream().filter(event -> event.paramList().stream().anyMatch(p -> p.name().equals(parameter.name())
                            && p.value() instanceof Number value && value.doubleValue() >= threshold)).count();
                    check(EventDatabase.events2Program(start, end, supplier, List.of(new SWEK.Param(parameter.name(), threshold, SWEK.Operand.BIGGER_OR_EQUAL))).size()
                            == expectedCount, "configured numeric filter: " + parameter.name());
                }
                check(EventDatabase.addStoredInterval(start, end, supplier), "completed interval");
                check(EventDatabase.isStored(start, end, supplier), "reuse completed interval");
            }
        }
        check(groups.size() == 11 && supplierCount == 14, "all configured HEK event types and suppliers are covered");
        System.out.println("HEKCatalogTest passed: " + groups.size() + " event types, " + supplierCount + " suppliers, " + eventCount + " records");
    }

    private static void verify(JHVEvent event, JSONObject expected, boolean full) {
        check(event.getAllEventParameters().length == expected.getInt(full ? "full_parameters" : "normal_parameters"), "parameter retention");
        var position = event.getPositionInformation();
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

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
