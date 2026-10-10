package org.helioviewer.jhv.io;

import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.time.TimeUtils;

// The request settings' normalization, notifications and sampling rules.
public final class ImageRequestSettingsTest {

    public static void main(String[] arguments) {
        long start = 1_800_000_000_000L, end = start + 60 * TimeUtils.MINUTE_IN_MILLIS;
        ImageRequestSettings settings = new ImageRequestSettings(start, end);
        List<String> events = new ArrayList<>();
        settings.addListener(new ImageRequestSettings.Listener() {
            @Override public void intervalChanged(long s, long e) { events.add("interval " + s + " " + e); }
            @Override public void samplingChanged(ImageRequestSettings.Sampling sampling) { events.add("sampling " + sampling); }
        });

        settings.setInterval(start + 500, end + 999);
        if (settings.start() != start || settings.end() != end || !events.isEmpty())
            throw new AssertionError("Sub-second changes were not floored away without notice: " + events);
        settings.setInterval(start + 1500, end + 1999);
        if (settings.start() != start + 1000 || settings.end() != end + 1000 || !events.equals(List.of("interval " + (start + 1000) + " " + (end + 1000))))
            throw new AssertionError("Whole-second change not applied with one notification: " + events);
        events.clear();
        settings.setInterval(end + 1000, start);
        if (settings.start() != end + 1000 || settings.end() != end + 1000 || events.size() != 1)
            throw new AssertionError("End before start not clamped to the start: " + settings.end() + " " + events);
        events.clear();
        long outside = TimeUtils.MINIMAL_TIME.milli - TimeUtils.DAY_IN_MILLIS;
        settings.setInterval(outside, end + 2000);
        if (settings.start() != end + 1000 || settings.end() != end + 2000 || events.size() != 1)
            throw new AssertionError("A start outside the range was not kept while the end moved: " + events);
        events.clear();
        settings.setInterval(settings.start(), settings.end());
        if (!events.isEmpty())
            throw new AssertionError("Unchanged interval notified");
        System.out.println("PASS: interval floored to seconds, kept outside the range, end clamped to start, one notification per change");

        settings.setInterval(start, end);
        events.clear();
        if (!(settings.sampling() instanceof ImageRequestSettings.TimeStep step) || step.seconds() != APIRequest.CADENCE_DEFAULT
                || settings.cadence() != APIRequest.CADENCE_DEFAULT || settings.singleFrame() || settings.requestEnd() != end)
            throw new AssertionError("Default sampling is not the default time step");
        settings.setSampling(new ImageRequestSettings.TimeStep(APIRequest.CADENCE_DEFAULT));
        if (!events.isEmpty())
            throw new AssertionError("Unchanged sampling notified");
        settings.setSampling(new ImageRequestSettings.All());
        if (settings.cadence() != APIRequest.CADENCE_ALL || events.size() != 1)
            throw new AssertionError("Every-frame sampling: " + settings.cadence() + " " + events);
        settings.setSampling(new ImageRequestSettings.FrameCount(4));
        if (settings.cadence() != 900 || settings.singleFrame() || settings.requestEnd() != end)
            throw new AssertionError("Four frames over an hour: " + settings.cadence());
        settings.setSampling(new ImageRequestSettings.FrameCount(7));
        if (settings.cadence() != 514) // 3600 / 7 = 514.3
            throw new AssertionError("Seven frames over an hour did not round: " + settings.cadence());
        settings.setSampling(new ImageRequestSettings.FrameCount(1000));
        settings.setInterval(start, start + 1000);
        if (settings.cadence() != 1)
            throw new AssertionError("Cadence below one second: " + settings.cadence());
        settings.setInterval(start, end);
        settings.setSampling(new ImageRequestSettings.FrameCount(1));
        if (!settings.singleFrame() || settings.requestEnd() != start)
            throw new AssertionError("One frame is not a single image at the start");
        APIRequest single = settings.request("ROB", 10);
        if (single.startTime() != start || single.endTime() != start || single.cadence() != 3600)
            throw new AssertionError("Single-image request: " + single);
        settings.setSampling(new ImageRequestSettings.TimeStep(60));
        APIRequest movie = settings.request("ROB", 10);
        if (movie.startTime() != start || movie.endTime() != end || movie.cadence() != 60 || movie.sourceId() != 10)
            throw new AssertionError("Movie request: " + movie);
        try {
            settings.setSampling(new ImageRequestSettings.TimeStep(0));
            throw new AssertionError("Zero time step accepted");
        } catch (IllegalArgumentException expected) {
        }
        System.out.println("PASS: time step, every frame and frame count cadences, single image at the start, requests");

        long now = end + TimeUtils.DAY_IN_MILLIS;
        if (!movie.endingAt(now).equals(new APIRequest("ROB", 10, now - (end - start), now, 60))
                || !single.endingAt(now).equals(new APIRequest("ROB", 10, now, now, 3600)))
            throw new AssertionError("Latest interval changed duration, sampling or source");
        APIRequest expanded = new APIRequest("ROB", 10, start, start + TimeUtils.MINUTE_IN_MILLIS, APIRequest.CADENCE_ALL);
        APIRequest latest = expanded.endingAt(now);
        if (latest.startTime() != now - (expanded.endTime() - expanded.startTime()) || latest.endTime() != now
                || latest.cadence() != APIRequest.CADENCE_ALL || !latest.endingAt(now).equals(latest))
            throw new AssertionError("Latest interval expanded the normalized span again: " + latest);
        System.out.println("PASS: latest intervals preserve movie duration, single images and sampling");
    }

    private ImageRequestSettingsTest() {}
}
