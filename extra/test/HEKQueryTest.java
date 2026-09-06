package org.helioviewer.jhv.plugins.swek.sources;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKGroup;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.time.TimeUtils;

public final class HEKQueryTest {

    public static void main(String[] args) throws Exception {
        HEKHandler handler = new HEKHandler();
        SWEKSupplier supplier = new SWEKSupplier(new SWEKGroup("Flare", ""), "SWPC", "NOAA SWPC",
                new SWEK.Source("HEK", List.of(), handler), "test_swpc", List.of());
        long start = TimeUtils.parse("2024-05-10T00:00:00");
        long end = TimeUtils.parse("2024-05-10T02:00:00");
        Map<String, String> query = new HashMap<>();
        for (String part : handler.createURI(supplier, start, end, 0).getRawQuery().split("&")) {
            String[] pair = part.split("=", 2);
            query.put(pair[0], URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
        }
        check(query.get("op0").equals("="), "HEK equality operator");
        check(query.get("value0").equals("SWPC"), "supplier filter");
        check(query.get("event_endtime").equals(TimeUtils.format(end)), "bounded query");
        check(query.get("page").equals("1"), "one-based first page");
        check(handler.createURI(supplier, start, end, 1).getQuery().endsWith("page=2"), "second page");

        System.out.println("HEKQueryTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }
}
