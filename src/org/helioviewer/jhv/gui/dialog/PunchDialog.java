package org.helioviewer.jhv.gui.dialog;

import java.awt.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JOptionPane;

import org.helioviewer.jhv.io.PunchClient;
import org.helioviewer.jhv.time.TimeUtils;

@SuppressWarnings("serial")
public final class PunchDialog extends CatalogDialog<PunchClient.DataItem> {

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
            new Cadence("native", 0),
            new Cadence("every 10 minutes", 10 * TimeUtils.MINUTE_IN_MILLIS),
            new Cadence("every 30 minutes", 30 * TimeUtils.MINUTE_IN_MILLIS),
            new Cadence("every hour", 60 * TimeUtils.MINUTE_IN_MILLIS),
            new Cadence("every 3 hours", 3 * 60 * TimeUtils.MINUTE_IN_MILLIS),
            new Cadence("every 6 hours", 6 * 60 * TimeUtils.MINUTE_IN_MILLIS),
            new Cadence("every day", TimeUtils.DAY_IN_MILLIS)};

    private final JComboBox<String> levelCombo = new JComboBox<>(Level);
    private final JComboBox<String> productCombo = new JComboBox<>();
    private final JComboBox<Cadence> cadenceCombo = new JComboBox<>(Cadences);

    private int productsRequest;
    private int coverageRequest;
    private boolean productsDownloaded;
    private boolean rangeUserChanged; // true once the user has explicitly set a range
    private long latestDay;

    private static PunchDialog instance;

    public static PunchDialog getInstance() {
        return instance == null ? instance = new PunchDialog() : instance;
    }

    private PunchDialog() {
        super("New PUNCH Layer");
    }

    @Override
    protected void addQueryRows() {
        productCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof String product)
                    setText(product + ", " + ProductInfo.getOrDefault(product, "PUNCH data product"));
                return this;
            }
        });
        levelCombo.addActionListener(e -> loadProducts());
        productCombo.addActionListener(e -> loadCoverage());
        cadenceCombo.addActionListener(e -> scheduleSearch());

        addRow("Level", levelCombo);
        addRow("Product", productCombo);
        addRow("Cadence", cadenceCombo);
    }

    @Override
    protected boolean startSearch(Consumer<List<PunchClient.DataItem>> onResult, Consumer<Throwable> onFailure) {
        if (levelCombo.getSelectedItem() instanceof String level && productCombo.getSelectedItem() instanceof String product &&
                cadenceCombo.getSelectedItem() instanceof Cadence cadence) {
            PunchClient.submitSearchTime(onResult, onFailure, level, product,
                    timeSelectorPanel.getStartTime(), timeSelectorPanel.getEndTime(), cadence.milli);
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    protected String refuse(List<PunchClient.DataItem> selected) {
        return selected.size() > MAX_FILES ? "Select at most " + MAX_FILES + " files, or choose a sparser cadence" : null;
    }

    @Override
    protected boolean load(List<PunchClient.DataItem> items) {
        if (items.size() > CONFIRM_FILES && JOptionPane.showConfirmDialog(this,
                String.format("This will download %d native-resolution FITS files (typically a few MB each).%nSeveral files may be fetched in parallel.%nProceed?", items.size()),
                "Large PUNCH download", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION)
            return false;
        PunchClient.submitLoad(items);
        return true;
    }

    @Override
    protected String nothingFound() {
        return latestDay == 0 ? "Nothing found" : "Nothing found; the latest day available is " + TimeUtils.formatDate(latestDay);
    }

    // Any explicit edit to the time fields counts as user-set, so the next coverage probe keeps it.
    @Override
    protected void timeChanged() {
        rangeUserChanged = true;
        super.timeChanged();
    }

    @Override
    public void showDialog() {
        if (!productsDownloaded)
            loadProducts();
        super.showDialog();
    }

    private void loadProducts() {
        coverageRequest++;
        int request = ++productsRequest;
        productsDownloaded = false;
        latestDay = 0;
        productCombo.setModel(new DefaultComboBoxModel<>());
        clearResults();
        setStatus("Checking products...");
        if (levelCombo.getSelectedItem() instanceof String level)
            PunchClient.submitGetProducts(list -> setProducts(request, list), level);
    }

    private void setProducts(int request, List<String> list) {
        if (request != productsRequest)
            return;
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(list.toArray(String[]::new));
        if (list.contains("CAM"))
            model.setSelectedItem("CAM");
        productCombo.setModel(model);
        productsDownloaded = true; // mark loaded only after successful callback
        loadCoverage();
    }

    private void loadCoverage() {
        int request = ++coverageRequest;
        latestDay = 0;
        if (productCombo.getSelectedItem() instanceof String product && levelCombo.getSelectedItem() instanceof String level) {
            if (rangeUserChanged)
                scheduleSearch();
            else {
                clearResults();
                setStatus("Checking archive coverage...");
            }
            PunchClient.submitGetCoverage(latest -> setCoverage(request, latest), level, product);
        } else {
            clearResults();
            setStatus("No products at this level");
        }
    }

    private void setCoverage(int request, long latestDayMilli) {
        if (request != coverageRequest)
            return;
        latestDay = latestDayMilli;
        if (latestDayMilli == 0)
            setStatus("No data for this product");
        else if (!rangeUserChanged) {
            // Default to the latest archived day; setTime counts as a user edit, so undo that.
            timeSelectorPanel.setTime(latestDayMilli, latestDayMilli + TimeUtils.DAY_IN_MILLIS - 1);
            rangeUserChanged = false;
        }
    }

}
