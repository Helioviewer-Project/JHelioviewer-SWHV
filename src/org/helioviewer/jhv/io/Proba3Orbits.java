package org.helioviewer.jhv.io;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.thread.Task;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

// Orbit time ranges from the Proba-3 Science Center.
public final class Proba3Orbits {

    private static final String ORBITS_URL = "https://p3sc.oma.be/api/l3_orbit_time_ranges";

    public record Orbit(int id, long start, long end) {
    }

    public static Future<List<Orbit>> submitLoad(@Nonnull Consumer<List<Orbit>> onResult) {
        return Task.submitBackground("Proba-3 orbits", new LoadOrbits(), onResult, "Failed to load Proba-3 orbits");
    }

    private record LoadOrbits() implements Callable<List<Orbit>> {
        @Override
        public List<Orbit> call() throws Exception {
            JSONArray array = JSONUtils.getArray(new URI(ORBITS_URL));
            List<Orbit> orbits = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                orbits.add(new Orbit(item.getInt("orbit_id"), TimeUtils.parse(item.getString("aa_start_time")), TimeUtils.parse(item.getString("aa_end_time"))));
            }
            return orbits;
        }
    }

}
