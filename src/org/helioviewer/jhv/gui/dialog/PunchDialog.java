package org.helioviewer.jhv.gui.dialog;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.app.Message;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.component.HTMLPane;
import org.helioviewer.jhv.gui.time.TimeSelectorPanel;
import org.helioviewer.jhv.io.PunchClient;
import org.helioviewer.jhv.time.TimeUtils;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
public class PunchDialog extends StandardDialog implements PunchClient.ReceiverItems, PunchClient.ReceiverProducts, PunchClient.ReceiverCoverage {

    // Generous cap with a confirmation prompt past CONFIRM_FILES; multi-URI image
    // loads can resolve files concurrently, so very large selections need a hard limit
    private static final int MAX_FILES = 5000;
    private static final int CONFIRM_FILES = 200;
    private static final Dimension resultSize = new Dimension(500, 350);
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

    private final JComboBox<String> levelCombo = new JComboBox<>(Level);
    private final JComboBox<String> productCombo = new JComboBox<>();
    private final JComboBox<Cadence> cadenceCombo = new JComboBox<>(Cadences);
    private final TimeSelectorPanel timeSelectorPanel = new TimeSelectorPanel();
    private final JList<PunchClient.DataItem> listPane = new JList<>();
    private final JLabel foundLabel = new JLabel("0 found", JLabel.RIGHT);
    private final HTMLPane coverageLabel = new HTMLPane();
    private final JLabel selectedLabel = new JLabel("0 selected", JLabel.RIGHT);

    private boolean productsDownloaded;
    private boolean rangeUserChanged; // true once the user has explicitly set a range
    private boolean updatingProducts;

    private static PunchDialog instance;

    public static PunchDialog getInstance() {
        return instance == null ? instance = new PunchDialog(MainFrame.get()) : instance;
    }

    private PunchDialog(JFrame mainFrame) {
        super(mainFrame, true);
        setResizable(false);
        setTitle("New PUNCH Layer");
    }

    @Override
    public ButtonPanel createButtonPanel() {
        AbstractAction close = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };
        setDefaultCancelAction(close);

        JButton loadButton = getLoadButton();
        JButton cancelButton = new JButton(close);
        cancelButton.setText("Cancel");

        ButtonPanel panel = new ButtonPanel();
        panel.add(loadButton, ButtonPanel.AFFIRMATIVE_BUTTON);
        panel.add(cancelButton, ButtonPanel.CANCEL_BUTTON);

        return panel;
    }

    private JButton getLoadButton() {
        JButton loadButton = new JButton("Add");
        loadButton.addActionListener(e -> {
            List<PunchClient.DataItem> items = listPane.getSelectedValuesList();
            if (items.isEmpty())
                return;

            if (items.size() > MAX_FILES) {
                Message.err("PUNCH error", String.format("Too many files selected for download: %d.%nPlease reduce the selection to less than %d files or choose a sparser cadence.", items.size(), MAX_FILES));
                return;
            }
            if (items.size() > CONFIRM_FILES) {
                int choice = JOptionPane.showConfirmDialog(this,
                        String.format("This will download %d native-resolution FITS files (typically a few MB each).%nSeveral files may be fetched in parallel.%nProceed?", items.size()),
                        "Large PUNCH download", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                if (choice != JOptionPane.OK_OPTION)
                    return;
            }
            if (levelCombo.getSelectedItem() instanceof String level && productCombo.getSelectedItem() instanceof String product &&
                    cadenceCombo.getSelectedItem() instanceof Cadence cadence) {
                PunchClient.submitLoad(items, level, product, timeSelectorPanel.getStartTime(), timeSelectorPanel.getEndTime(), cadence.milli);
                setVisible(false);
            }
        });
        return loadButton;
    }

    @Override
    public JComponent createContentPanel() {
        JPanel dataSelector = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 0));
        dataSelector.add(new JLabel("Level", JLabel.RIGHT));
        dataSelector.add(levelCombo);
        dataSelector.add(productCombo);
        dataSelector.add(cadenceCombo);
        JButton searchButton = getSearchButton();
        dataSelector.add(searchButton);

        levelCombo.addActionListener(e -> {
            if (levelCombo.getSelectedItem() instanceof String level) {
                clearResults();
                coverageLabel.setText("Checking products...");
                updatingProducts = true;
                try {
                    productCombo.setModel(new DefaultComboBoxModel<>());
                } finally {
                    updatingProducts = false;
                }
                productsDownloaded = false;
                PunchClient.submitGetProducts(this, level);
            }
        });
        productCombo.addActionListener(e -> {
            if (!updatingProducts && productCombo.getSelectedItem() instanceof String product) {
                clearResults();
                productCombo.setToolTipText(ProductInfo.getOrDefault(product, "PUNCH data product"));
                coverageLabel.setText("Checking archive coverage...");
                if (levelCombo.getSelectedItem() instanceof String level)
                    PunchClient.submitGetCoverage(this, level, product);
            }
        });

        JPanel foundPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 0));
        foundPanel.add(foundLabel);
        JPanel selectedPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 0));
        selectedPanel.add(selectedLabel);

        listPane.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                selectedLabel.setText(listPane.getSelectedValuesList().size() + " selected");
        });
        com.jidesoft.swing.SearchableUtils.installSearchable(listPane);
        JScrollPane scrollPane = new JScrollPane(listPane);
        scrollPane.setPreferredSize(resultSize);

        // Any explicit edit to the time fields counts as "user-set", so we stop
        // overwriting on the next coverage probe
        timeSelectorPanel.addListener((start, end) -> {
            rangeUserChanged = true;
            clearResults();
        });

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        content.add(timeSelectorPanel);
        content.add(dataSelector);
        JPanel coveragePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
        coveragePanel.add(coverageLabel);
        content.add(coveragePanel);
        content.add(foundPanel);
        content.add(scrollPane);
        content.add(selectedPanel);

        content.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        return content;
    }

    private JButton getSearchButton() {
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> {
            if (levelCombo.getSelectedItem() instanceof String level && productCombo.getSelectedItem() instanceof String product &&
                    cadenceCombo.getSelectedItem() instanceof Cadence cadence) {
                clearResults();
                PunchClient.submitSearchTime(this, level, product, timeSelectorPanel.getStartTime(), timeSelectorPanel.getEndTime(), cadence.milli);
                foundLabel.setText("Searching...");
            }
        });
        return searchButton;
    }

    private void clearResults() {
        listPane.setListData(new PunchClient.DataItem[0]);
        foundLabel.setText("0 found");
        selectedLabel.setText("0 selected");
    }

    @Nullable
    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    public void showDialog() {
        if (!productsDownloaded && levelCombo.getSelectedItem() instanceof String level)
            PunchClient.submitGetProducts(this, level);

        pack();
        setLocationRelativeTo(MainFrame.get());
        setVisible(true);
    }

    @Override
    public void setPunchResponseItems(List<PunchClient.DataItem> list) {
        listPane.setListData(list.toArray(PunchClient.DataItem[]::new));
        foundLabel.setText(list.isEmpty()
                ? "0 found — archive may not cover this period; see umbra.nascom.nasa.gov/punch"
                : list.size() + " found");
    }

    @Override
    public void setPunchResponseProducts(List<String> list) {
        updatingProducts = true;
        try {
            productCombo.setModel(new DefaultComboBoxModel<>(list.toArray(String[]::new)));
            productsDownloaded = true; // mark loaded only after successful callback
            if (list.contains("CAM"))
                productCombo.setSelectedItem("CAM");
            else if (!list.isEmpty())
                productCombo.setSelectedIndex(0);
        } finally {
            updatingProducts = false;
        }
        if (productCombo.getSelectedItem() instanceof String product) {
            productCombo.setToolTipText(ProductInfo.getOrDefault(product, "PUNCH data product"));
            coverageLabel.setText("Checking archive coverage...");
            if (levelCombo.getSelectedItem() instanceof String level)
                PunchClient.submitGetCoverage(this, level, product);
        } else {
            coverageLabel.setText("Archive: no products for this level");
        }
    }

    @Override
    public void setPunchResponseCoverage(long latestDayMilli) {
        if (latestDayMilli == 0) {
            coverageLabel.setText("Archive: no data for this product");
            return;
        }
        coverageLabel.setText("Archive: latest day available is " + TimeUtils.formatDate(latestDayMilli));
        if (!rangeUserChanged) {
            // Default to the latest archived day (00:00 -> 24:00 UTC). The user can edit
            // and the next probe will not overwrite their choice.
            timeSelectorPanel.setTime(latestDayMilli, latestDayMilli + TimeUtils.DAY_IN_MILLIS - 1);
            rangeUserChanged = false; // setTime fires the listener and flips this — undo
        }
    }

}
