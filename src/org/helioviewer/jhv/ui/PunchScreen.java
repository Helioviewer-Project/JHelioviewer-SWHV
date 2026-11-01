package org.helioviewer.jhv.ui;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import org.helioviewer.jhv.app.Message;
import org.helioviewer.jhv.io.PunchClient;
import org.helioviewer.jhv.time.TimeUtils;

// The PUNCH archive: the products of a level, their coverage, a search by time and cadence, and the loading of the selected files.
public final class PunchScreen extends DialogScreen {

    interface Archive {
        void products(Consumer<List<String>> reply, String level);

        void coverage(LongConsumer reply, String level, String product);

        void search(Consumer<List<PunchClient.DataItem>> reply, String level, String product, long start, long end, long cadence);

        void load(List<PunchClient.DataItem> items);
    }

    private static final Archive ARCHIVE = new Archive() {
        @Override
        public void products(Consumer<List<String>> reply, String level) {
            PunchClient.submitGetProducts(reply, level);
        }

        @Override
        public void coverage(LongConsumer reply, String level, String product) {
            PunchClient.submitGetCoverage(reply, level, product);
        }

        @Override
        public void search(Consumer<List<PunchClient.DataItem>> reply, String level, String product, long start, long end, long cadence) {
            PunchClient.submitSearchTime(reply, level, product, start, end, cadence);
        }

        @Override
        public void load(List<PunchClient.DataItem> items) {
            PunchClient.submitLoad(items);
        }
    };

    // Generous cap with a confirmation prompt past CONFIRM_FILES; multi-URI image
    // loads can resolve files concurrently, so very large selections need a hard limit
    private static final int MAX_FILES = 5000;
    private static final int CONFIRM_FILES = 200;
    private static final String[] Level = {"3", "Q", "2", "1", "0"};
    private static final Map<String, String> ProductInfo = Map.ofEntries(
            Map.entry("PTM", "polarized trefoil mosaic (high cadence)"),
            Map.entry("CTM", "clear trefoil mosaic (high cadence)"),
            Map.entry("PAM", "polarized average / low-noise mosaic"),
            Map.entry("CAM", "clear average / low-noise mosaic"),
            Map.entry("PNN", "polarized NFI image"),
            Map.entry("CNN", "clear NFI image"),
            Map.entry("PFM", "polarized F-corona mosaic"),
            Map.entry("CFM", "clear F-corona mosaic"),
            Map.entry("PIM", "polarized instrumental mosaic"),
            Map.entry("CIM", "clear instrumental mosaic"),
            Map.entry("PSM", "polarized starfield mosaic"),
            Map.entry("CSM", "clear starfield mosaic"),
            Map.entry("CQM", "quickPUNCH clear mosaic"));

    private record Cadence(String label, long milli) {
        @Override
        public String toString() {
            return label;
        }
    }

    private static final Cadence[] Cadences = {
            new Cadence("native cadence", 0),
            new Cadence("every 10 minutes", 10 * TimeUtils.MINUTE_IN_MILLIS),
            new Cadence("every 30 minutes", 30 * TimeUtils.MINUTE_IN_MILLIS),
            new Cadence("every hour", 60 * TimeUtils.MINUTE_IN_MILLIS),
            new Cadence("every 3 hours", 3 * 60 * TimeUtils.MINUTE_IN_MILLIS),
            new Cadence("every 6 hours", 6 * 60 * TimeUtils.MINUTE_IN_MILLIS),
            new Cadence("every day", TimeUtils.DAY_IN_MILLIS)};
    private static final String[] CADENCE_LABELS = Panel.labels(Cadences);

    private static final int ADD = TimeSelection.NEXT_ID;
    private static final int CANCEL = ADD + 1;
    private static final int SEARCH = ADD + 2;
    private static final int LEVEL = ADD + 3;
    private static final int PRODUCT = ADD + 4;
    private static final int CADENCE = ADD + 5;

    private final Panel buffer = new Panel();
    private final Archive archive;
    private final TimeSelection time = new TimeSelection(() -> {
        rangeUserChanged = true;
        clearResults();
    });
    private final ResultList<PunchClient.DataItem> results = new ResultList<>(PunchClient.DataItem::toString, item -> "");
    private final Requests searches = new Requests();
    private final Requests listings = new Requests();
    private final Requests coverages = new Requests();
    private List<String> products = List.of();
    private String coverage = "";
    private int level;
    private int product;
    private int cadence;
    private boolean productsDownloaded;
    private boolean coverageKnown;
    private boolean rangeUserChanged; // true once the user has explicitly set a range

    public PunchScreen() {
        this(ARCHIVE);
    }

    PunchScreen(Archive _archive) {
        archive = _archive;
    }

    @Override
    public String title() {
        return "New PUNCH Layer";
    }

    @Override
    public void show() {
        if (!productsDownloaded)
            loadProducts();
        else if (!coverageKnown)
            loadCoverage();
    }

    @Override
    public void close() {
        searches.cancel();
        listings.cancel();
        coverages.cancel();
        if (results.isSearching())
            results.clear();
        super.close();
    }

    private void clearResults() {
        searches.cancel();
        results.clear();
    }

    private void loadProducts() {
        clearResults();
        coverages.cancel();
        productsDownloaded = false;
        coverage = "Checking products...";
        products = List.of();
        archive.products(listings.latest(this::setProducts), Level[level]);
    }

    private void setProducts(List<String> list) {
        products = List.copyOf(list);
        product = Math.max(0, products.indexOf("CAM"));
        productsDownloaded = true; // mark loaded only after successful callback
        loadCoverage();
        changed();
    }

    private void loadCoverage() {
        clearResults();
        coverageKnown = false;
        if (products.isEmpty()) {
            coverages.cancel();
            coverage = "Archive: no products for this level";
            coverageKnown = true;
        } else {
            coverage = "Checking archive coverage...";
            Consumer<Long> reply = coverages.latest(this::setCoverage);
            archive.coverage(reply::accept, Level[level], products.get(product));
        }
    }

    private void setCoverage(long latestDayMilli) {
        coverageKnown = true;
        if (latestDayMilli == 0)
            coverage = "Archive: no data for this product";
        else {
            coverage = "Archive: latest day available is " + TimeUtils.formatDate(latestDayMilli);
            if (!rangeUserChanged) {
                // Default to the latest archived day (00:00 -> 24:00 UTC). The user can edit
                // and the next probe will not overwrite their choice.
                time.set(latestDayMilli, latestDayMilli + TimeUtils.DAY_IN_MILLIS - 1);
                rangeUserChanged = false; // set runs the listener, which flips this; undo
            }
        }
        changed();
    }

    @Override
    public Panel build() {
        Panel p = buffer.reset().column().margin(3, 3, 3, 3);
        time.build(p, true);
        p.row().right().gap(5).margin(0, 5, 0, 5).label("Level").combo(LEVEL, Level, level)
                .combo(PRODUCT, products.toArray(String[]::new), products.isEmpty() ? -1 : product)
                .tip(products.isEmpty() ? "" : ProductInfo.getOrDefault(products.get(product), "PUNCH data product"))
                .combo(CADENCE, CADENCE_LABELS, cadence).button(SEARCH, "Search").rowEnd()
                .row().margin(2, 11, 2, 5).label(coverage).rowEnd();
        results.build(p, results.selectedCount() + " selected");
        return p.columnEnd().row().right().gap(5).button(CANCEL, "Cancel").button(ADD, "Add").rowEnd();
    }

    private void search() {
        if (products.isEmpty())
            return;
        clearResults();
        archive.search(searches.latest(list -> {
            results.show(list, list.isEmpty() ? "0 found — archive may not cover this period; see umbra.nascom.nasa.gov/punch" : list.size() + " found");
            changed();
        }), Level[level], products.get(product), time.start(), time.end(), Cadences[cadence].milli);
        results.searching();
    }

    private void load(List<PunchClient.DataItem> items) {
        archive.load(items);
        close();
    }

    private void add() {
        List<PunchClient.DataItem> items = results.selection();
        if (items.isEmpty())
            return;
        if (items.size() > MAX_FILES) {
            Message.err("PUNCH error", String.format("Too many files selected for download: %d.%nPlease reduce the selection to less than %d files or choose a sparser cadence.", items.size(), MAX_FILES));
        } else if (items.size() > CONFIRM_FILES) {
            Message.confirm("Large PUNCH download", String.format("This will download %d native-resolution FITS files (typically a few MB each).%nSeveral files may be fetched in parallel.%nProceed?", items.size()),
                    () -> load(items));
        } else {
            load(items);
        }
    }

    @Override
    public void command(int owner, int item, Panel.Command command) {
        if (results.command(item, command) || time.command(item)) {
            changed();
            return;
        }
        switch (item) {
            case ADD -> add();
            case CANCEL -> close();
            case SEARCH -> search();
            default -> throw new IllegalArgumentException("PUNCH item " + item);
        }
        changed();
    }

    @Override
    public void choice(int owner, int item, int value) {
        if (!time.choice(item, value)) {
            switch (item) {
                case LEVEL -> {
                    level = Panel.choiceIndex(value, Level.length);
                    loadProducts();
                }
                case PRODUCT -> {
                    product = Panel.choiceIndex(value, products.size());
                    loadCoverage();
                }
                case CADENCE -> {
                    cadence = Panel.choiceIndex(value, Cadences.length);
                    clearResults();
                }
                default -> throw new IllegalArgumentException("PUNCH item " + item);
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
