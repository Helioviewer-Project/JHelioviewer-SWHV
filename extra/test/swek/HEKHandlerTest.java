package org.helioviewer.jhv.plugins.swek.sources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.event.EventGeometry;
import org.helioviewer.jhv.event.EventParameter;
import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKCatalog;
import org.helioviewer.jhv.event.SWEKGroup;
import org.helioviewer.jhv.event.SWEKHandler;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.event.SolarEvent;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class HEKHandlerTest {

    private static JSONObject fixture;

    public static void main(String[] args) throws Exception {
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();
        fixture = new JSONObject(Files.readString(Path.of(args[0])));
        checkQueries();
        checkResponses();
        checkIndexedValues();
        checkGeometry();
        checkCMEParameters();
        System.out.println("HEKHandlerTest passed");
    }

    private static void checkResponses() throws Exception {
        HEKHandler handler = new HEKHandler();
        SWEKSupplier supplier = new SWEKSupplier(new SWEKGroup("Flare", ""), "SWPC", "NOAA SWPC",
                new SWEK.Source("HEK", List.of(), handler, Map.of()), "test_swpc", List.of());
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

    private static void checkQueries() throws Exception {
        HEKHandler handler = new HEKHandler();
        SWEKSupplier supplier = new SWEKSupplier(new SWEKGroup("Flare", ""), "SWPC", "NOAA SWPC",
                new SWEK.Source("HEK", List.of(), handler, Map.of()), "test_swpc", List.of());
        long start = TimeUtils.parse("2024-05-10T00:00:00");
        long end = TimeUtils.parse("2024-05-10T02:00:00");
        Map<String, String> query = new HashMap<>();
        for (String part : handler.createURI(supplier, start, end, 0).getRawQuery().split("&")) {
            String[] pair = part.split("=", 2);
            query.put(pair[0], URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
        }
        check(!query.containsKey("param0"), "supplier filtering stays local");
        check(query.get("event_endtime").equals(TimeUtils.format(end)), "bounded query");
        check(query.get("page").equals("1"), "one-based first page");
        check(handler.createURI(supplier, start, end, 1).getQuery().endsWith("page=2"), "second page");
        SWEKSupplier cactus = new SWEKSupplier(new SWEKGroup("Coronal Mass Ejection", ""), "CACTus (Computer Aided CME Tracking)", "CACTus",
                supplier.source(), "test_cactus", List.of());
        check(!handler.createURI(cactus, start, end, 0).getQuery().contains("param"), "CACTus name is not sent through HEK's supplier predicate");

    }

    private static final HEKHandler indexHandler = new HEKHandler();

    private static void checkIndexedValues() throws Exception {
        SWEK.Source source = new SWEK.Source("HEK", List.of(), indexHandler, Map.of(
                "jhv_goesflux", SWEK.NumericType.DECIMAL, "cme_radiallinvel", SWEK.NumericType.DECIMAL, "ar_noaanum", SWEK.NumericType.INTEGER));
        SWEKGroup flare = new SWEKGroup("Flare", ""), activeRegion = new SWEKGroup("Active Region", "");
        SWEKSupplier swpc = new SWEKSupplier(flare, "SWPC", "SWPC", source, "test_swpc", List.of(
                new SWEK.Parameter("JHV_GOESFlux", "Flux", new SWEK.ParameterFilter("", 0, 1, 0, 1, ""), false)));
        SWEKSupplier cactus = new SWEKSupplier(new SWEKGroup("Coronal Mass Ejection", ""), "CACTus (Computer Aided CME Tracking)", "CACTus", source, "test_cactus", List.of(
                new SWEK.Parameter("CME_RadialLinVel", "Speed", new SWEK.ParameterFilter("", 0, 3000, 1000, 100, ""), false)));
        SWEKSupplier spoca = new SWEKSupplier(new SWEKGroup("Coronal Hole", ""), "SPoCA", "SPoCA", source, "test_spoca", List.of());
        for (SWEKSupplier supplier : List.of(swpc, cactus, spoca)) SWEKCatalog.add(supplier);
        SWEKCatalog.setRelatedEvents(List.of(new SWEK.RelatedEvents(activeRegion, flare, List.of(new SWEK.RelatedOn("ar_noaanum", "ar_noaanum")))));

        Map<String, Number> values = read(swpc, new JSONObject().put("fl_goescls", "M1.0").put("ar_noaanum", 13664).put("cme_radiallinvel", 679));
        check(Math.abs(values.get("JHV_GOESFlux").doubleValue() - 1e-5) < 1e-12 && values.get("ar_noaanum").doubleValue() == 13664, "GOES conversion and NOAA identity");
        check(values.get("CME_RadialLinVel") == null, "unconfigured speed is not indexed for flares");
        check(values.get("JHV_GOESFlux") instanceof Double && values.get("ar_noaanum") instanceof Integer, "database binding types");
        check(read(cactus, new JSONObject().put("cme_radiallinvel", 679.9)).get("CME_RadialLinVel").doubleValue() == 679.9, "fractional numeric speed is retained");
        check(read(cactus, new JSONObject().put("cme_radiallinvel", "680")).get("CME_RadialLinVel").doubleValue() == 680, "integer string speed");
        check(read(swpc, new JSONObject().put("ar_noaanum", "13664")).get("ar_noaanum").doubleValue() == 13664, "integer string region");
        check(read(swpc, new JSONObject().put("ar_noaanum", -9999)).get("ar_noaanum").doubleValue() == -9999, "region sentinels remain unmodified");
        check(read(cactus, new JSONObject().put("cme_radiallinvel", "679.9")).get("CME_RadialLinVel").doubleValue() == 679.9, "fractional string speed is retained");
        check(read(cactus, new JSONObject().put("cme_radiallinvel", 679)).get("CME_RadialLinVel") instanceof Double, "speed binds as a real value");
        for (Object invalid : List.of(JSONObject.NULL, "", "bad")) {
            check(read(cactus, new JSONObject().put("cme_radiallinvel", invalid)).get("CME_RadialLinVel") == null, "missing or malformed speed stays absent");
            check(read(swpc, new JSONObject().put("ar_noaanum", invalid)).get("ar_noaanum") == null, "missing or malformed region stays absent");
        }
        check(read(swpc, new JSONObject().put("ar_noaanum", "679.9")).get("ar_noaanum") == null, "fractional string is not a region identifier");
        check(read(swpc, new JSONObject().put("jhv_goesflux", "bad")).get("JHV_GOESFlux") == null, "malformed flux stays absent");
        check(read(swpc, new JSONObject()).equals(Map.of()), "absent indexed fields");
        check(read(spoca, new JSONObject().put("jhv_goesflux", 1e-5).put("ar_noaanum", 13664).put("cme_radiallinvel", 679))
                .equals(Map.of()), "supplier without indexed fields ignores numeric payload fields");
    }

    private static Map<String, Number> read(SWEKSupplier supplier, JSONObject fields) throws Exception {
        JSONObject event = new JSONObject(fields.toString()).put("kb_archivid", "test-event").put("frm_name", supplier.supplierName())
                .put("event_starttime", "2024-05-10T00:00:00").put("event_endtime", "2024-05-10T01:00:00");
        return indexHandler.parseRemotePage(new JSONObject().put("overmax", false).put("result", new JSONArray().put(event)), supplier).events().getFirst().indexedValues();
    }

    private static final HEKHandler handler = new HEKHandler();
    private static final SWEKSupplier supplier = new SWEKSupplier(new SWEKGroup("Flare", ""), "SWPC", "SWPC", new SWEK.Source("HEK", List.of(), handler, Map.of()), "test", List.of());
    private static final String BOX = "POLYGON((0 0,1 0,1 1,0 0))";

    private static SolarEvent parse(JSONObject json) {
        long start = TimeUtils.parse("2024-05-10T00:00:00");
        return handler.parseEventJSON(json, supplier, 1, start, start + 60_000, true);
    }

    private static void checkGeometry() throws Exception {

        SolarEvent swpc = parse(fixture.getJSONArray("result").getJSONObject(1));
        check(swpc.getPositionInformation().centralPoint() != null, "noposition placeholder marker is preserved");
        check(Arrays.stream(swpc.getAllEventParameters()).anyMatch(p -> p.getParameterName().equals("fl_goescls")), "metadata is retained");
        Vec3 zero = parse(new JSONObject().put("hgs_coord", "POINT(0 0)")).getPositionInformation().centralPoint();
        check(samePoint(swpc.getPositionInformation().centralPoint(), zero), "placeholder remains at its original coordinates");
        EventGeometry flare = parse(fixture.getJSONArray("result").getJSONObject(0)).getPositionInformation();
        check(flare.centralPoint() != null && flare.getBoundBox().length == 15, "saved flare point and bounding box remain usable");
        EventGeometry spoca = parse(fixture.getJSONArray("result").getJSONObject(3)).getPositionInformation();
        check(spoca.centralPoint() != null && spoca.getBoundBox().length > 15, "saved SPoCA contour remains preferred over its bounding box");

        JSONObject base = new JSONObject().put("hgs_x", 20).put("hgs_y", 10).put("hgs_bbox", BOX).put("event_title", "usable metadata");
        EventGeometry expected = parse(base).getPositionInformation();
        for (String bad : List.of("POLYGON broken", "POLYGON((0 0,bad,1 1,0 0))", "POLYGON((0 0,NaN 0,1 1,0 0))", "POLYGON((0 0,1 0,1 1))")) {
            SolarEvent event = parse(new JSONObject(base.toString()).put("hgs_boundcc", bad).put("hgs_coord", "POINT broken"));
            check(Arrays.equals(event.getPositionInformation().getBoundBox(), expected.getBoundBox()), "invalid contour falls back to box");
            check(samePoint(event.getPositionInformation().centralPoint(), expected.centralPoint()), "invalid point falls back to scalar coordinates");
            check(Arrays.stream(event.getAllEventParameters()).anyMatch(p -> p.getParameterName().equals("event_title")), "bad geometry does not discard metadata");
        }
        for (String bad : List.of("bad", "", "NaN", "Infinity")) {
            SolarEvent event = parse(new JSONObject().put("hgs_x", bad).put("hgs_y", 10).put("event_title", "usable metadata"));
            check(Arrays.stream(event.getAllEventParameters()).anyMatch(p -> p.getParameterName().equals("event_title")), "invalid scalar does not discard event");
            check(event.getPositionInformation() == null || event.getPositionInformation().centralPoint() == null, "invalid scalar is not a position");
        }
        SolarEvent badLatitude = parse(new JSONObject().put("hgs_coord", "POINT(0 91)").put("hgs_x", 0).put("hgs_y", 91));
        check(badLatitude.getPositionInformation() == null || badLatitude.getPositionInformation().centralPoint() == null, "invalid latitude is not a position");
        check(parse(new JSONObject().put("hgs_bbox", "POLYGON((0 0,1 0,1 1,-0 0))")).getPositionInformation().getBoundBox().length == 12, "signed zero closes a ring");
        check(parse(new JSONObject().put("hgs_coord", "POINT(20 10)").put("hgs_bbox", "POLYGON((-89.9 -89.9,89.9 -89.9,89.9 89.9,-89.9 89.9,-89.9 -89.9))")).getPositionInformation().getBoundBox().length == 0, "large-box guard is preserved");
    }

    private static boolean samePoint(Vec3 first, Vec3 second) {
        return first != null && second != null && first.x == second.x && first.y == second.y && first.z == second.z;
    }

    private static void checkCMEParameters() {
        HEKHandler handler = new HEKHandler();
        SWEK.Source source = new SWEK.Source("HEK", List.of(), handler, Map.of());
        for (boolean visible : List.of(false, true)) {
            SWEKSupplier supplier = new SWEKSupplier(new SWEKGroup("Coronal Mass Ejection", ""), "CACTus", "CACTus", source, "cactus", List.of(
                    new SWEK.Parameter("cme_radiallinvel", "Speed", null, visible),
                    new SWEK.Parameter("event_coord1", "Angle", null, visible),
                    new SWEK.Parameter("cme_angularwidth", "Width", null, visible)));
            for (boolean full : List.of(false, true)) {
                for (Object value : List.of(JSONObject.NULL, "", "bad", 679, 679.9, " 679.9 ", "1e3", "NaN", "Infinity", -12.5)) {
                    JSONObject json = new JSONObject().put("cme_radiallinvel", value).put("event_coord1", value).put("cme_angularwidth", value);
                    SolarEvent event = handler.parseEventJSON(json, supplier, 1, 0, 1, full);
                    SolarEvent details = handler.parseEventJSON(json, supplier, 1, 0, 1, true);
                    if (!visible && !full && event.getAllEventParameters().length != 0) throw new AssertionError("hidden CME strings retained");
                    SolarEvent.CMEParameters parameters = event.getCMEParameters();
                    check(parameters.speedKmPerSecond(), previousValue(details, "cme_radiallinvel", 500));
                    check(parameters.principalAngleDegree(), previousValue(details, "event_coord1", 0));
                    check(parameters.angularWidthDegree(), previousValue(details, "cme_angularwidth", 0));
                }
                SolarEvent missing = handler.parseEventJSON(new JSONObject(), supplier, 1, 0, 1, full);
                if (!missing.getCMEParameters().equals(SolarEvent.CMEParameters.DEFAULT)) throw new AssertionError("missing values preserve defaults");
            }
        }
    }

    // Reproduce the previous renderer's conversion to check behavior at the decoding boundary.
    private static double previousValue(SolarEvent event, String name, double fallback) {
        EventParameter parameter = Arrays.stream(event.getAllEventParameters()).filter(p -> p.getParameterName().equals(name)).findFirst().orElse(null);
        if (parameter == null) return fallback;
        try {
            return Double.parseDouble(parameter.getParameterValue());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static void check(double actual, double expected) {
        if (Double.compare(actual, expected) != 0) throw new AssertionError(actual + " != " + expected);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
