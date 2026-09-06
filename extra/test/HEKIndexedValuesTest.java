package org.helioviewer.jhv.plugins.swek.sources;

import java.util.List;

import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKCatalog;
import org.helioviewer.jhv.event.SWEKGroup;
import org.helioviewer.jhv.event.SWEKHandler;
import org.helioviewer.jhv.event.SWEKSupplier;

import org.json.JSONArray;
import org.json.JSONObject;

public final class HEKIndexedValuesTest {

    private static final HEKHandler handler = new HEKHandler();

    public static void main(String[] args) throws Exception {
        SWEK.Source source = new SWEK.Source("HEK", List.of(), handler);
        SWEKGroup flare = new SWEKGroup("Flare", ""), activeRegion = new SWEKGroup("Active Region", "");
        SWEKSupplier swpc = new SWEKSupplier(flare, "SWPC", "SWPC", source, "test_swpc", List.of(
                new SWEK.Parameter("JHV_GOESFlux", "Flux", new SWEK.ParameterFilter("", 0, 1, 0, 1, "", "REAL"), false)));
        SWEKSupplier cactus = new SWEKSupplier(new SWEKGroup("Coronal Mass Ejection", ""), "CACTus (Computer Aided CME Tracking)", "CACTus", source, "test_cactus", List.of(
                new SWEK.Parameter("CME_RadialLinVel", "Speed", new SWEK.ParameterFilter("", 0, 3000, 1000, 100, "", "INTEGER"), false)));
        SWEKSupplier spoca = new SWEKSupplier(new SWEKGroup("Coronal Hole", ""), "SPoCA", "SPoCA", source, "test_spoca", List.of());
        for (SWEKSupplier supplier : List.of(swpc, cactus, spoca)) SWEKCatalog.add(supplier);
        SWEKCatalog.setRelatedEvents(List.of(new SWEK.RelatedEvents(activeRegion, flare, List.of(new SWEK.RelatedOn("ar_noaanum", "ar_noaanum", "INTEGER")))));

        SWEKHandler.IndexedValues values = read(swpc, new JSONObject().put("fl_goescls", "M1.0").put("ar_noaanum", 13664).put("cme_radiallinvel", 679));
        check(Math.abs(values.goesFlux() - 1e-5) < 1e-12 && values.noaaRegion() == 13664, "GOES conversion and NOAA identity");
        check(values.cmeSpeed() == null, "unconfigured speed is not indexed for flares");
        check(values.get("JHV_GOESFlux") instanceof Double && values.get("ar_noaanum") instanceof Integer, "database binding types");
        check(read(cactus, new JSONObject().put("cme_radiallinvel", 679.9)).cmeSpeed() == 679, "JSON numeric speed retains integer conversion");
        check(read(cactus, new JSONObject().put("cme_radiallinvel", "680")).cmeSpeed() == 680, "integer string speed");
        check(read(swpc, new JSONObject().put("ar_noaanum", "13664")).noaaRegion() == 13664, "integer string region");
        check(read(swpc, new JSONObject().put("ar_noaanum", -9999)).noaaRegion() == -9999, "region sentinels remain unmodified");
        for (Object invalid : List.of(JSONObject.NULL, "", "bad", "679.9")) {
            check(read(cactus, new JSONObject().put("cme_radiallinvel", invalid)).cmeSpeed() == null, "missing or malformed speed stays absent");
            check(read(swpc, new JSONObject().put("ar_noaanum", invalid)).noaaRegion() == null, "missing or malformed region stays absent");
        }
        check(read(swpc, new JSONObject().put("jhv_goesflux", "bad")).goesFlux() == null, "malformed flux stays absent");
        check(read(swpc, new JSONObject()).equals(new SWEKHandler.IndexedValues(null, null, null)), "absent indexed fields");
        check(read(spoca, new JSONObject().put("jhv_goesflux", 1e-5).put("ar_noaanum", 13664).put("cme_radiallinvel", 679))
                .equals(new SWEKHandler.IndexedValues(null, null, null)), "supplier without indexed fields ignores numeric payload fields");
        System.out.println("HEKIndexedValuesTest passed");
    }

    private static SWEKHandler.IndexedValues read(SWEKSupplier supplier, JSONObject fields) throws Exception {
        JSONObject event = new JSONObject(fields.toString()).put("kb_archivid", "test-event").put("frm_name", supplier.supplierName())
                .put("event_starttime", "2024-05-10T00:00:00").put("event_endtime", "2024-05-10T01:00:00");
        return handler.parseRemotePage(new JSONObject().put("overmax", false).put("result", new JSONArray().put(event)), supplier).events().getFirst().indexedValues();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
