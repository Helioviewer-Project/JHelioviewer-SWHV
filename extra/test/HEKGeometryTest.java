package org.helioviewer.jhv.plugins.swek.sources;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.event.JHVEvent;
import org.helioviewer.jhv.event.JHVPositionInformation;
import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKGroup;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONObject;

public final class HEKGeometryTest {

    private static final HEKHandler handler = new HEKHandler();
    private static final SWEKSupplier supplier = new SWEKSupplier(new SWEKGroup("Flare", ""), "SWPC", "SWPC", new SWEK.Source("HEK", List.of(), handler), "test", List.of());
    private static final String BOX = "POLYGON((0 0,1 0,1 1,0 0))";

    private static JHVEvent parse(JSONObject json) {
        long start = TimeUtils.parse("2024-05-10T00:00:00");
        return handler.parseEventJSON(json, supplier, 1, start, start + 60_000, true);
    }

    public static void main(String[] args) throws Exception {
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();

        JSONObject fixture = new JSONObject(Files.readString(Path.of("extra/test/data/hek-events.json")));
        JHVEvent swpc = parse(fixture.getJSONArray("result").getJSONObject(1));
        check(swpc.getPositionInformation().centralPoint() != null, "noposition placeholder marker is preserved");
        check(swpc.getParameter("fl_goescls") != null, "metadata is retained");
        Vec3 zero = parse(new JSONObject().put("hgs_coord", "POINT(0 0)")).getPositionInformation().centralPoint();
        check(samePoint(swpc.getPositionInformation().centralPoint(), zero), "placeholder remains at its original coordinates");
        JHVPositionInformation flare = parse(fixture.getJSONArray("result").getJSONObject(0)).getPositionInformation();
        check(flare.centralPoint() != null && flare.getBoundBox().length == 15, "saved flare point and bounding box remain usable");
        JHVPositionInformation spoca = parse(fixture.getJSONArray("result").getJSONObject(3)).getPositionInformation();
        check(spoca.centralPoint() != null && spoca.getBoundBox().length > 15, "saved SPoCA contour remains preferred over its bounding box");

        JSONObject base = new JSONObject().put("hgs_x", 20).put("hgs_y", 10).put("hgs_bbox", BOX).put("event_title", "usable metadata");
        JHVPositionInformation expected = parse(base).getPositionInformation();
        for (String bad : List.of("POLYGON broken", "POLYGON((0 0,bad,1 1,0 0))", "POLYGON((0 0,NaN 0,1 1,0 0))", "POLYGON((0 0,1 0,1 1))")) {
            JHVEvent event = parse(new JSONObject(base.toString()).put("hgs_boundcc", bad).put("hgs_coord", "POINT broken"));
            check(Arrays.equals(event.getPositionInformation().getBoundBox(), expected.getBoundBox()), "invalid contour falls back to box");
            check(samePoint(event.getPositionInformation().centralPoint(), expected.centralPoint()), "invalid point falls back to scalar coordinates");
            check(event.getParameter("event_title") != null, "bad geometry does not discard metadata");
        }
        for (String bad : List.of("bad", "", "NaN", "Infinity")) {
            JHVEvent event = parse(new JSONObject().put("hgs_x", bad).put("hgs_y", 10).put("event_title", "usable metadata"));
            check(event.getParameter("event_title") != null, "invalid scalar does not discard event");
            check(event.getPositionInformation() == null || event.getPositionInformation().centralPoint() == null, "invalid scalar is not a position");
        }
        JHVEvent badLatitude = parse(new JSONObject().put("hgs_coord", "POINT(0 91)").put("hgs_x", 0).put("hgs_y", 91));
        check(badLatitude.getPositionInformation() == null || badLatitude.getPositionInformation().centralPoint() == null, "invalid latitude is not a position");
        check(parse(new JSONObject().put("hgs_bbox", "POLYGON((0 0,1 0,1 1,-0 0))")).getPositionInformation().getBoundBox().length == 12, "signed zero closes a ring");
        check(parse(new JSONObject().put("hgs_coord", "POINT(20 10)").put("hgs_bbox", "POLYGON((-89.9 -89.9,89.9 -89.9,89.9 89.9,-89.9 89.9,-89.9 -89.9))")).getPositionInformation().getBoundBox().length == 0, "large-box guard is preserved");
        System.out.println("HEKGeometryTest passed");
    }

    private static boolean samePoint(Vec3 first, Vec3 second) {
        return first != null && second != null && first.x == second.x && first.y == second.y && first.z == second.z;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
