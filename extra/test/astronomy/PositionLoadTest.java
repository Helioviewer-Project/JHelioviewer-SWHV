package org.helioviewer.jhv.astronomy;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.helioviewer.jhv.time.TimeUtils;

public class PositionLoadTest {

    public static void main(String[] arguments) throws Exception {
        long start = TimeUtils.parse("2026-08-12T15:25:05.554");
        for (long span : new long[]{0, 1, 59999, 60000, 60001, 120000, 14515000})
            request(start, span, 60);

        long full = 49999 * 60000L;
        request(start, full - 1, 60);
        request(start, full, 60);
        request(start, full + 1, 61);
        request(start, 3_059_999_000L, 62);

        PositionLoad.LoadPosition position = load(start, start + 14515000, false);
        Map<String, String> parameters = parameters(position.uri());
        check(parameters.get("utc_end").equals("2026-08-12T19:27:05.554"), "screenshot range lacks its final bracket");

        URI state = load(start, start + 1, true).uri();
        check(state.getPath().equals("/state"), "state request used the position endpoint");
        check(parameters(state).get("utc_end").equals(TimeUtils.format(start + 60000)), "state request lacks endpoint coverage");

        System.out.println("PASS: position/state request coverage, fractional intervals, default cadence and strict point cap");
    }

    private static void request(long start, long span, long expectedStep) throws Exception {
        PositionLoad.LoadPosition load = load(start, start + span, false);
        Map<String, String> parameters = parameters(load.uri());
        check(parameters.get("deltat").equals(Long.toString(expectedStep)), "wire cadence differs from local cadence");
        check(parameters.get("utc").equals(TimeUtils.format(start)), "request lost fractional start time");

        long sampleEnd = TimeUtils.parse(parameters.get("utc_end"));
        long stepMillis = expectedStep * 1000;
        check(sampleEnd >= start + span, "request stops before the requested end");
        check(sampleEnd - (start + span) < stepMillis, "request extends by an unnecessary step");
        check((sampleEnd - start) % stepMillis == 0, "request's last sample is off grid");
        long count = (sampleEnd - start) / stepMillis + 1;
        check(count <= 50000, "request exceeds the point cap: " + count);
        check(span != 0 || count == 1, "single time requested more than one sample");
    }

    private static PositionLoad.LoadPosition load(long start, long end, boolean state) {
        return new PositionLoad.LoadPosition(SpaceObject.SUN, SpaceObject.get("Earth"), Frame.J2000, "LT+S", state, start, end);
    }

    private static Map<String, String> parameters(URI uri) {
        Map<String, String> parameters = new HashMap<>();
        for (String parameter : uri.getRawQuery().split("&")) {
            if (parameter.isEmpty())
                continue;
            int equals = parameter.indexOf('=');
            parameters.put(parameter.substring(0, equals), URLDecoder.decode(parameter.substring(equals + 1), StandardCharsets.UTF_8));
        }
        return parameters;
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

}
