package org.helioviewer.jhv.ui;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.io.Load;
import org.helioviewer.jhv.thread.Task;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

// Synoptic maps of the MEDOC archive: a search by instrument, channel and map type, and the loading of the selected maps.
public final class SynopticScreen extends DialogScreen {

    interface Archive {
        void search(String path, JSONObject query, Consumer<List<URI>> reply, Runnable failure);

        void load(List<URI> maps);
    }

    private static final String SERVER = "https://idoc-ssa-prod.ias.u-psud.fr";

    private static final Archive ARCHIVE = new Archive() {
        @Override
        public void search(String path, JSONObject query, Consumer<List<URI>> reply, Runnable failure) {
            Task.submitBackground(SERVER, new SearchSynoptic(SERVER, path, query), reply, (logContext, t) -> failure.run());
        }

        @Override
        public void load(List<URI> maps) {
            Load.image(maps);
        }
    };

    private static final String HMI_QUERY = "/api/s005a/map/hmi";
    private static final String AIA_QUERY = "/api/s050a/map/";
    private static final String[] INSTRUMENTS = {"HMI", "AIA"};
    private static final String[] HMI_PRODUCTS = {"Magnetogram", "Continuum"};
    private static final String[] AIA_CHANNELS = {"94", "131", "171", "193", "211", "304", "335", "1600", "1700"};
    private static final String[] MAP_TYPES = {"forecast", "nowcast", "temporary", "archived"};

    private static final int ADD = TimeSelection.NEXT_ID;
    private static final int CANCEL = ADD + 1;
    private static final int SEARCH = ADD + 2;
    private static final int INSTRUMENT = ADD + 3;
    private static final int HMI = ADD + 4;
    private static final int AIA = ADD + 5;
    private static final int MAP_TYPE = ADD + 6;

    private final Panel buffer = new Panel();
    private final Archive archive;
    // Nothing here depends on the interval until the next search.
    private final TimeSelection time = new TimeSelection(() -> {});
    private final ResultList<URI> results = new ResultList<>(SynopticScreen::fileName, URI::toString);
    private final Requests searches = new Requests();
    private int instrument;
    private int hmi;
    private int aia = 2;
    private int mapType;

    public SynopticScreen() {
        this(ARCHIVE);
    }

    SynopticScreen(Archive _archive) {
        archive = _archive;
    }

    private static String fileName(URI uri) {
        String path = uri.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    @Override
    public String title() {
        return "New Synoptic Layer";
    }

    @Override
    public void close() {
        searches.cancel();
        if (results.isSearching())
            results.clear();
        super.close();
    }

    @Override
    public Panel build() {
        boolean searching = results.isSearching();
        int selected = results.selectedCount();
        Panel p = buffer.reset().column().margin(3, 3, 3, 3);
        time.build(p, true);
        p.grid(2).margin(0, 5, 0, 0)
                .radio(INSTRUMENT, INSTRUMENTS[0], 0, instrument).combo(HMI, HMI_PRODUCTS, hmi).enabled(instrument == 0)
                .radio(INSTRUMENT, INSTRUMENTS[1], 1, instrument).combo(AIA, AIA_CHANNELS, aia).enabled(instrument == 1)
                .row().cells(2).radio(MAP_TYPE, MAP_TYPES, mapType)
                .row().right().button(SEARCH, "Search").enabled(!searching).rowEnd()
                .rowEnd().gridEnd();
        results.build(p, selected + " selected");
        return p.columnEnd().row().right().gap(5).button(CANCEL, "Cancel").button(ADD, "Add").enabled(selected > 0 && !searching).rowEnd();
    }

    JSONObject query() {
        return new JSONObject().put("start_date", TimeUtils.formatDate(time.start())).put("end_date", TimeUtils.formatDate(time.end()))
                .put("channels", new JSONArray().put(instrument == 0 ? (hmi == 0 ? "hmi.m_720s" : "hmi.ic_720s") : AIA_CHANNELS[aia]))
                .put("map_type", new JSONArray().put(MAP_TYPES[mapType])).put("latest", true);
    }

    private void search() {
        results.clear();
        results.searching();
        Consumer<List<URI>> reply = searches.latest(list -> {
            results.show(list, list.size() + " found");
            changed();
        });
        // A failure is an empty result.
        archive.search(instrument == 0 ? HMI_QUERY : AIA_QUERY, query(), reply, () -> reply.accept(List.of()));
    }

    @Override
    public void command(int owner, int item, Panel.Command command) {
        if (results.command(item, command) || time.command(item)) {
            changed();
            return;
        }
        switch (item) {
            case ADD -> {
                List<URI> maps = results.selection();
                if (!maps.isEmpty()) {
                    archive.load(maps);
                    close();
                }
            }
            case CANCEL -> close();
            case SEARCH -> search();
            default -> throw new IllegalArgumentException("Synoptic item " + item);
        }
        changed();
    }

    @Override
    public void choice(int owner, int item, int value) {
        if (!time.choice(item, value)) {
            switch (item) {
                case INSTRUMENT -> instrument = Panel.choiceIndex(value, INSTRUMENTS.length);
                case HMI -> hmi = Panel.choiceIndex(value, HMI_PRODUCTS.length);
                case AIA -> aia = Panel.choiceIndex(value, AIA_CHANNELS.length);
                case MAP_TYPE -> mapType = Panel.choiceIndex(value, MAP_TYPES.length);
                default -> throw new IllegalArgumentException("Synoptic item " + item);
            }
        }
        changed();
    }

    @Override
    public void number(int owner, int item, double value) {
        if (time.number(item, value))
            changed();
    }

    @Override
    public void setText(int owner, int item, String text) {
        if (time.setText(item, text))
            changed();
    }

    private record SearchSynoptic(String baseUri, String queryPath, JSONObject jo) implements Callable<List<URI>> {
        @Override
        public List<URI> call() throws Exception {
            URI server = new URI(baseUri);
            JSONArray result = JSONUtils.post(server.resolve(queryPath), jo);
            return extractFitsFiles(server, result);
        }
    }

    private static List<URI> extractFitsFiles(URI server, JSONArray array) {
        List<URI> fitsFiles = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null)
                continue;
            JSONObject resources = item.optJSONObject("_resources");
            if (resources == null)
                continue;
            String fitsFile = resources.optString("fits_file", null);
            if (fitsFile != null)
                fitsFiles.add(server.resolve(fitsFile));
        }
        return fitsFiles;
    }

}
