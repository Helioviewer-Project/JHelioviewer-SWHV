package org.helioviewer.jhv.ui;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.thread.Task;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

// Proba-3 ASPIICS products of an orbit.
public final class AspiicsScreen extends DialogScreen {

    interface Archive {
        void orbits(Consumer<List<Orbit>> reply);

        void load(APIRequest request);
    }

    private static final Archive ARCHIVE = new Archive() {
        @Override
        public void orbits(Consumer<List<Orbit>> reply) {
            Task.submitBackground("ASPIICS orbits", new LoadOrbits(), reply, "Failed to load ASPIICS orbits");
        }

        @Override
        public void load(APIRequest request) {
            ImageLayer.create().load(request);
        }
    };

    private static final String ORBITS_URL = "https://p3sc.oma.be/api/l3_orbit_time_ranges";
    // ROB Proba-3 ASPIICS datasets
    private static final int[] SOURCE_IDS = {506, 508, 509, 510};
    private static final String[] PRODUCTS = {"Total brightness", "Polarized brightness", "He I D3", "Fe XIV"};

    private static final int ADD = 1;
    private static final int CLOSE = 2;
    private static final int ORBIT = 3;
    private static final int PRODUCT = 10;

    private final Panel buffer = new Panel();
    private final Archive archive;
    private final Requests requests = new Requests();
    private final boolean[] products = {true, false, false, false};
    private List<Orbit> orbits = List.of();
    private int orbit;

    public AspiicsScreen() {
        this(ARCHIVE);
    }

    AspiicsScreen(Archive _archive) {
        archive = _archive;
    }

    @Override
    public String title() {
        return "New ASPIICS Layer";
    }

    @Override
    public void show() {
        if (orbits.isEmpty())
            archive.orbits(requests.latest(list -> {
                orbits = List.copyOf(list);
                orbit = 0;
                changed();
            }));
    }

    @Override
    public void close() {
        requests.cancel();
        super.close();
    }

    @Override
    public Panel build() {
        String[] names = new String[orbits.size()];
        String[] tips = new String[orbits.size()];
        for (int i = 0; i < names.length; i++) {
            Orbit item = orbits.get(i);
            names[i] = item.toString();
            tips[i] = TimeUtils.format(item.start) + " - " + TimeUtils.format(item.end);
        }
        Panel p = buffer.reset().column().margin(3, 8, 3, 8)
                .row().gap(5).margin(0, 5, 0, 5).label("Orbits").combo(ORBIT, names, orbits.isEmpty() ? -1 : orbit).widthText("9999").choiceTips(tips).rowEnd();
        for (int i = 0; i < products.length; i++)
            p.check(PRODUCT + i, PRODUCTS[i], products[i]);
        return p.columnEnd().row().right().gap(5).button(CLOSE, "Close").button(ADD, "Add").rowEnd();
    }

    private void add() {
        if (orbits.isEmpty())
            return;
        Orbit selected = orbits.get(orbit);
        for (int i = 0; i < SOURCE_IDS.length; i++) {
            if (products[i])
                archive.load(new APIRequest("ROB", SOURCE_IDS[i], selected.start, selected.end, APIRequest.CADENCE_ALL));
        }
        close();
    }

    @Override
    public void command(int owner, int item, Panel.Command command) {
        switch (item) {
            case ADD -> add();
            case CLOSE -> close();
            default -> throw new IllegalArgumentException("ASPIICS item " + item);
        }
    }

    @Override
    public void choice(int owner, int item, int value) {
        if (item != ORBIT)
            throw new IllegalArgumentException("ASPIICS item " + item);
        orbit = Panel.choiceIndex(value, orbits.size());
        changed();
    }

    @Override
    public void checked(int owner, int item, boolean value) {
        products[item - PRODUCT] = value;
        changed();
    }

    record Orbit(int orbitId, long start, long end) {
        @Override
        public String toString() {
            return Integer.toString(orbitId);
        }
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
            orbits.sort(Comparator.comparingLong(Orbit::start).reversed());
            return orbits;
        }
    }

}
