package org.helioviewer.jhv.event;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.event.sources.HEKHandler;
import org.helioviewer.jhv.io.JSONUtils;

import org.json.JSONObject;

// A database-backed session with a flare, a linked flare and related regions, as the information window reads them.
public final class EventInformationFixture {

    public record Open(SWEKSession session, RelatedEvents related, SolarEvent event) {}

    public static final long START = 1_700_000_000_000L;
    public static final long END = START + 3_600_000L;
    public static final String LONG_VALUE = "Long description ".repeat(30).trim();
    public static final String REFERENCE_URL = "https://example.com/flares/reference?id=42";
    public static final int REGION = 13664;

    public static Open open() throws Exception {
        SWEK.Source source = new SWEK.Source("HEK", List.of(), new HEKHandler(), Map.of("ar_noaanum", SWEK.NumericType.INTEGER));
        SWEKGroup flares = new SWEKGroup("Solar Flare", "Flare");
        SWEKGroup regions = new SWEKGroup("Active Region", "ActiveRegion");
        List<SWEK.Parameter> parameters = List.of(
                new SWEK.Parameter("fl_goescls", "GOES Class", null, true),
                new SWEK.Parameter("ar_noaanum", "NOAA Region", null, true),
                new SWEK.Parameter("event_description", "Description", null, true),
                new SWEK.Parameter("event_peakflux", "Peak Flux", null, true),
                new SWEK.Parameter("frm_url", "Method URL", null, true));
        SWEKSupplier flare = new SWEKSupplier(flares, "SWPC", "SWPC", source, "test_flare", parameters);
        SWEKSupplier region = new SWEKSupplier(regions, "NOAA", "NOAA", source, "test_region", parameters);
        SWEK.Relation relation = new SWEK.Relation(flares, regions, List.of(new SWEK.RelatedOn("ar_noaanum", "ar_noaanum")));
        SWEKSession session = new SWEKSession(new SWEKCatalog(0, List.of(flare, region), List.of(relation)));
        Field field = SWEKSession.class.getDeclaredField("database");
        field.setAccessible(true);
        EventDatabase database = (EventDatabase) field.get(session);

        SWEKHandler.RemoteEvent first = event("flare-1", START, new JSONObject().put("fl_goescls", "M2.4").put("ar_noaanum", REGION)
                .put("event_description", LONG_VALUE).put("event_peakflux", "1234.5678").put("frm_url", REFERENCE_URL).put("obs_instrument", "GOES"));
        SWEKHandler.RemoteEvent second = event("flare-2", START + 60_000, new JSONObject().put("fl_goescls", "C1.1"));
        check(database.storeRemotePage(new SWEKHandler.RemotePage(false, List.of(first, second), List.of(new SolarEvent.LinkRef("flare-1", "flare-2"))), flare), "store flares");
        SWEKHandler.RemoteEvent firstRegion = event("region-1", START - 86_400_000, new JSONObject().put("ar_noaanum", REGION));
        SWEKHandler.RemoteEvent secondRegion = event("region-2", START - 172_800_000, new JSONObject().put("ar_noaanum", REGION));
        check(database.storeRemotePage(new SWEKHandler.RemotePage(false, List.of(firstRegion, secondRegion), List.of()), region), "store regions");

        // The published event carries only the visible parameters and no others, as the cache does.
        session.replaceEvents(database.loadEvents(START - 1, END + 1, flare, List.of()));
        session.replaceEvents(database.loadEvents(START - 86_400_001, START - 86_399_000, region, List.of()));
        RelatedEvents related = session.getEvents(START, START).getFirst();
        for (SolarEvent event : related.getEvents())
            if (event.start == START)
                return new Open(session, related, event);
        throw new AssertionError("flare-1 is not published");
    }

    private static SWEKHandler.RemoteEvent event(String uid, long start, JSONObject json) throws Exception {
        return new SWEKHandler.RemoteEvent(JSONUtils.compressJSON(json.put("event_title", uid)), start, start + 3_600_000L, 100, uid,
                json.has("ar_noaanum") ? Map.of("ar_noaanum", json.getInt("ar_noaanum")) : Map.of());
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private EventInformationFixture() {}
}
