package org.helioviewer.jhv.gui.dialog;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import org.helioviewer.jhv.base.NaturalSort;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.io.SoarClient;

import com.google.common.collect.ImmutableSortedMap;

@SuppressWarnings("serial")
public final class SoarDialog extends CatalogDialog<SoarClient.DataItem> {

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
    private static final String BY_TIME = "Time";
    private static final String BY_SOOP = "SOOP";

    private final JComboBox<String> datasetCombo = new JComboBox<>(Dataset.keySet().toArray(String[]::new));
    private final JComboBox<String> levelCombo = new JComboBox<>(Level);
    private final JComboBox<String> searchByCombo = new JComboBox<>(new String[]{BY_TIME, BY_SOOP});
    private final JComboBox<String> soopCombo = new JComboBox<>();
    private boolean soopsDownloaded;

    private static SoarDialog instance;

    public static SoarDialog getInstance() {
        return instance == null ? instance = new SoarDialog() : instance;
    }

    private SoarDialog() {
        super("New SOAR Layer");
    }

    private static double getTotalSize(List<SoarClient.DataItem> items) {
        return items.stream().mapToLong(SoarClient.DataItem::size).sum() / (1024. * 1024. * 1024.);
    }

    private boolean bySoop() {
        return BY_SOOP.equals(searchByCombo.getSelectedItem());
    }

    @Override
    protected void addQueryRows() {
        levelCombo.setSelectedItem("L2");
        soopCombo.setEnabled(false);

        datasetCombo.addActionListener(e -> scheduleSearch());
        levelCombo.addActionListener(e -> scheduleSearch());
        soopCombo.addActionListener(e -> scheduleSearch());
        searchByCombo.addActionListener(e -> {
            boolean bySoop = bySoop();
            if (bySoop && !soopsDownloaded)
                SoarClient.submitGetSoops(this::setSoops);
            soopCombo.setEnabled(bySoop);
            ComponentUtils.setEnabled(timeSelectorPanel, !bySoop);
            scheduleSearch();
        });

        addRow("Dataset", datasetCombo);
        addRow("Level", levelCombo);
        addRow("Search by", searchByCombo);
        addRow("SOOP", soopCombo);
    }

    @Override
    protected boolean startSearch(Consumer<List<SoarClient.DataItem>> onResult, Consumer<Throwable> onFailure) {
        if (!(datasetCombo.getSelectedItem() instanceof String dataset && levelCombo.getSelectedItem() instanceof String level))
            return false;
        List<String> descriptors = Dataset.get(dataset);
        if (!bySoop()) {
            SoarClient.submitSearchTime(onResult, onFailure, descriptors, level, timeSelectorPanel.getStartTime(), timeSelectorPanel.getEndTime());
            return true;
        }
        if (soopCombo.getSelectedItem() instanceof String soop) {
            SoarClient.submitSearchSoop(onResult, onFailure, descriptors, level, soop);
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    protected String refuse(List<SoarClient.DataItem> selected) {
        double size = getTotalSize(selected);
        return size > MAX_SIZE ? String.format("Select at most %.1fGiB (%.1fGiB selected)", MAX_SIZE, size) : null;
    }

    @Override
    protected String describe(List<SoarClient.DataItem> selected) {
        return String.format(" (%.1fGiB)", getTotalSize(selected));
    }

    @Override
    protected boolean load(List<SoarClient.DataItem> items) {
        SoarClient.submitLoad(items);
        return true;
    }

    @Override
    public void showDialog() {
        scheduleSearch();
        super.showDialog();
    }

    private void setSoops(List<String> list) {
        soopCombo.setModel(new DefaultComboBoxModel<>(list.toArray(String[]::new)));
        soopsDownloaded = true; // mark loaded only after successful callback
        if (!list.isEmpty())
            soopCombo.setSelectedIndex(0);
    }

}
