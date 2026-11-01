package org.helioviewer.jhv.ui;

import java.util.List;
import java.util.function.Consumer;

import org.helioviewer.jhv.app.Message;
import org.helioviewer.jhv.base.NaturalSort;
import org.helioviewer.jhv.io.SoarClient;

import com.google.common.collect.ImmutableSortedMap;

// The SOAR archive: a search by time or by SOOP, and the loading of the selected files.
public final class SoarScreen extends DialogScreen {

    interface Archive {
        void soops(Consumer<List<String>> reply);

        void search(Consumer<List<SoarClient.DataItem>> reply, List<String> descriptors, String level, long start, long end);

        void search(Consumer<List<SoarClient.DataItem>> reply, List<String> descriptors, String level, String soop);

        void load(List<SoarClient.DataItem> items);
    }

    private static final Archive ARCHIVE = new Archive() {
        @Override
        public void soops(Consumer<List<String>> reply) {
            SoarClient.submitGetSoops(reply);
        }

        @Override
        public void search(Consumer<List<SoarClient.DataItem>> reply, List<String> descriptors, String level, long start, long end) {
            SoarClient.submitSearchTime(reply, descriptors, level, start, end);
        }

        @Override
        public void search(Consumer<List<SoarClient.DataItem>> reply, List<String> descriptors, String level, String soop) {
            SoarClient.submitSearchSoop(reply, descriptors, level, soop);
        }

        @Override
        public void load(List<SoarClient.DataItem> items) {
            SoarClient.submitLoad(items);
        }
    };

    private static final double MAX_SIZE = 2;
    private static final String[] Level = {/* "LL01", "LL02", "LL03",*/ "L1", "L2", "L3"};
    private static final ImmutableSortedMap<String, List<String>> Dataset = new ImmutableSortedMap.Builder<String, List<String>>(NaturalSort.comparator).
            put("EUI FSI 174", List.of("eui-fsi174-image", "eui-fsi174-image-occulter", "eui-fsi174-image-short")).
            put("EUI FSI 304", List.of("eui-fsi304-image", "eui-fsi304-image-occulter", "eui-fsi304-image-short")).
            put("EUI HRI 174", List.of("eui-hrieuv174-image", "eui-hrieuvnon-image")).
            put("EUI HRI LYA", List.of("eui-hrilya1216-image")).
            put("PHI FDT", List.of("phi-fdt-blos", "phi-fdt-icnt")).
            put("PHI HRT", List.of("phi-hrt-bazi", "phi-hrt-binc", "phi-hrt-blos", "phi-hrt-bmag", "phi-hrt-icnt", /* "phi-hrt-stokes",*/ "phi-hrt-vlos")).
            put("Metis", List.of("metis-uv-image", "metis-vl-image", "metis-vl-pb", "metis-vl-pol-angle", "metis-vl-stokes", "metis-vl-tb")).
            put("SoloHI", List.of("solohi-11t", "solohi-12t", "solohi-1ft",
                    "solohi-21s", "solohi-21t", "solohi-22t", "solohi-23t", "solohi-24t", "solohi-2ft",
                    "solohi-31t", "solohi-32t", "solohi-3fg", "solohi-3ft",
                    "solohi-41t", "solohi-42t", "solohi-4fg", "solohi-4ft")).
            put("MAG RTN", List.of("mag-rtn-normal", "mag-rtn-normal-1-minute"/*, "mag-rtn-burst"*/)).
            // put("MAG SRF", List.of("mag-srf-normal" /*, "mag-srf-burst" */)).
            // put("MAG VSO", List.of("mag-vso-normal", "mag-vso-normal-1-minute"/*, "mag-vso-burst"*/)).
                    put("SWA PAS", List.of("swa-pas-grnd-mom")).
            build();
    private static final String[] DATASETS = Dataset.keySet().toArray(String[]::new);

    private static final int ADD = TimeSelection.NEXT_ID;
    private static final int CANCEL = ADD + 1;
    private static final int SEARCH = ADD + 2;
    private static final int QUERY = ADD + 3;
    private static final int SOOP = ADD + 4;
    private static final int DATASET = ADD + 5;
    private static final int LEVEL = ADD + 6;

    private final Panel buffer = new Panel();
    private final Archive archive;
    private final TimeSelection time = new TimeSelection(this::clearResults);
    private final ResultList<SoarClient.DataItem> results = new ResultList<>(SoarClient.DataItem::toString, item -> "");
    private final Requests searches = new Requests();
    private final Requests listings = new Requests();
    private List<String> soops = List.of();
    private boolean selectionChanged;
    private boolean soopMode;
    private int soop;
    private int dataset;
    private int level = 1;

    public SoarScreen() {
        this(ARCHIVE);
    }

    SoarScreen(Archive _archive) {
        archive = _archive;
    }

    @Override
    public String title() {
        return "New SOAR Layer";
    }

    @Override
    public void show() {
        if (soopMode && soops.isEmpty())
            requestSoops();
    }

    private void requestSoops() {
        archive.soops(listings.latest(list -> {
            soops = List.copyOf(list);
            soop = 0;
            changed();
        }));
    }

    @Override
    public void close() {
        searches.cancel();
        listings.cancel();
        if (results.isSearching())
            results.clear();
        super.close();
    }

    private void clearResults() {
        searches.cancel();
        results.clear();
    }

    private static double totalSize(List<SoarClient.DataItem> items) {
        return items.stream().mapToLong(SoarClient.DataItem::size).sum() / (1024. * 1024. * 1024.);
    }

    @Override
    public Panel build() {
        Panel p = buffer.reset().column().margin(3, 3, 3, 3)
                .grid(2).stretch(1)
                .row().height(2).radio(QUERY, "Time", 0, soopMode ? 1 : 0).rowEnd();
        time.build(p, !soopMode);
        p.radio(QUERY, "SOOP", 1, soopMode ? 1 : 0)
                .combo(SOOP, soops.toArray(String[]::new), soops.isEmpty() ? -1 : soop).enabled(soopMode)
                .gridEnd()
                .row().right().gap(5).margin(0, 5, 0, 5)
                .combo(DATASET, DATASETS, dataset).combo(LEVEL, Level, level).button(SEARCH, "Search").rowEnd();
        List<SoarClient.DataItem> items = results.selection();
        results.build(p, selectionChanged ? String.format("%d selected (%.1fGiB)", items.size(), totalSize(items)) : "0 selected");
        return p.columnEnd().row().right().gap(5).button(CANCEL, "Cancel").button(ADD, "Add").rowEnd();
    }

    private void search() {
        clearResults();
        List<String> descriptors = Dataset.get(DATASETS[dataset]);
        Consumer<List<SoarClient.DataItem>> reply = searches.latest(list -> {
            results.show(list, list.size() + " found");
            changed();
        });
        if (!soopMode) {
            archive.search(reply, descriptors, Level[level], time.start(), time.end());
            results.searching();
        } else if (!soops.isEmpty()) {
            archive.search(reply, descriptors, Level[level], soops.get(soop));
            results.searching();
        }
    }

    private void add() {
        List<SoarClient.DataItem> items = results.selection();
        if (items.isEmpty())
            return;
        double size = totalSize(items);
        if (size > MAX_SIZE) {
            Message.err("SOAR error", String.format("Too much data selected for download: %.1fGiB.%nPlease reduce the selection to less than %.1fGiB.", size, MAX_SIZE));
        } else {
            archive.load(items);
            close();
        }
    }

    @Override
    public void command(int owner, int item, Panel.Command command) {
        if (results.command(item, command)) {
            selectionChanged = true;
            changed();
            return;
        }
        if (time.command(item)) {
            changed();
            return;
        }
        switch (item) {
            case ADD -> add();
            case CANCEL -> close();
            case SEARCH -> search();
            default -> throw new IllegalArgumentException("SOAR item " + item);
        }
        changed();
    }

    @Override
    public void choice(int owner, int item, int value) {
        if (!time.choice(item, value)) {
            switch (item) {
                case QUERY -> {
                    if (soopMode == (value == 1))
                        return;
                    soopMode = value == 1;
                    if (soopMode && soops.isEmpty())
                        requestSoops();
                    clearResults();
                }
                case SOOP -> {
                    soop = Panel.choiceIndex(value, soops.size());
                    clearResults();
                }
                case DATASET -> {
                    dataset = Panel.choiceIndex(value, DATASETS.length);
                    clearResults();
                }
                case LEVEL -> {
                    level = Panel.choiceIndex(value, Level.length);
                    clearResults();
                }
                default -> throw new IllegalArgumentException("SOAR item " + item);
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

}
