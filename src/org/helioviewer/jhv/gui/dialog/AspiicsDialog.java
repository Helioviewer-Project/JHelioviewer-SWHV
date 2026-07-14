package org.helioviewer.jhv.gui.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.thread.Task;

import org.json.JSONArray;
import org.json.JSONObject;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;
import com.jidesoft.swing.SearchableUtils;

@SuppressWarnings("serial")
public class AspiicsDialog extends StandardDialog {

    private static final String ORBITS_URL = "https://p3sc.oma.be/api/l3_orbit_time_ranges";
    private static final String FITS_URL = "https://p3sc.oma.be/api/L3?select=datalocation&orbit_id=eq.%d&active=is.true";
    private static final String JP2_URL = "https://p3sc.oma.be/api/L3_jpeg2000?select=datalocation&orbit_id=eq.%d&active=is.true";
    private static final String DATA_URL = "https://p3sc.oma.be/datarepfiles/";
    private static final Dimension RESULT_SIZE = new Dimension(500, 350);
    // An L3 FITS frame is a 2048x2048 float image, about 17 MB, and the P3SC archive is not fast;
    // a whole orbit is easily several GB, so warn before a large download.
    private static final int CONFIRM_FILES = 50;
    private static final double FITS_MB = 16.8;
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{8}T\\d{6})");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private record Cadence(String label, long milli) {
        @Override
        public String toString() {
            return label;
        }
    }

    private static final Cadence[] CADENCES = {
            new Cadence("all frames", 0),
            new Cadence("every minute", 60_000L),
            new Cadence("every 5 minutes", 5 * 60_000L),
            new Cadence("every 10 minutes", 10 * 60_000L),
            new Cadence("every 30 minutes", 30 * 60_000L),
            new Cadence("every hour", 3600_000L)};

    private final JComboBox<Orbit> orbitCombo = new JComboBox<>();
    private final JRadioButton fitsButton = new JRadioButton("FITS", true);
    private final JRadioButton jp2Button = new JRadioButton("JP2");
    private final JRadioButton[] productButtons = {
            new JRadioButton("bt", true),
            new JRadioButton("fe"),
            new JRadioButton("he"),
            new JRadioButton("pb"),
            new JRadioButton("pa")
    };
    private final JComboBox<Cadence> cadenceCombo = new JComboBox<>(CADENCES);
    private final JButton searchButton = new JButton("Search");
    private final JButton addButton = new JButton("Add");
    private final JList<String> listPane = new JList<>();
    private final JLabel foundLabel = new JLabel("0 found", JLabel.RIGHT);
    private List<String> products = List.of();
    private boolean loadingOrbits;
    private boolean searching;

    private static AspiicsDialog instance;

    public static AspiicsDialog getInstance() {
        return instance == null ? instance = new AspiicsDialog(MainFrame.get()) : instance;
    }

    private AspiicsDialog(JFrame mainFrame) {
        super(mainFrame, true);
        setResizable(false);
        setTitle("New ASPIICS Layer");
    }

    @Override
    public ButtonPanel createButtonPanel() {
        addButton.addActionListener(e -> {
            List<String> selected = listPane.getSelectedValuesList();
            if (selected.isEmpty() || !confirmDownload(selected.size()))
                return;
            Commands.loadImage(selected.stream().map(AspiicsDialog::uri).toList());
            setVisible(false);
        });

        AbstractAction close = new AbstractAction("Close") {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };
        setDefaultCancelAction(close);

        JButton closeButton = new JButton(close);

        ButtonPanel panel = new ButtonPanel();
        panel.add(addButton, ButtonPanel.AFFIRMATIVE_BUTTON);
        panel.add(closeButton, ButtonPanel.CANCEL_BUTTON);
        return panel;
    }

    @Override
    public JComponent createContentPanel() {
        addToGroup(fitsButton, jp2Button);
        addToGroup(productButtons);
        orbitCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Orbit(int ignored, String start, String end))
                    label.setToolTipText(start + " - " + end);
                return label;
            }
        });

        JPanel queryPanel = new JPanel(new GridBagLayout());
        queryPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0;
        queryPanel.add(new JLabel("Orbit"), gc);
        gc.gridx = 1;
        queryPanel.add(orbitCombo, gc);
        gc.gridx = 2;
        queryPanel.add(fitsButton, gc);
        gc.gridx = 3;
        queryPanel.add(jp2Button, gc);
        gc.gridx = 4;
        queryPanel.add(searchButton, gc);

        gc.gridx = 0;
        gc.gridy = 1;
        queryPanel.add(new JLabel("Product"), gc);
        JPanel productPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        for (JRadioButton button : productButtons)
            productPanel.add(button);
        gc.gridx = 1;
        gc.gridwidth = 4;
        queryPanel.add(productPanel, gc);
        gc.gridwidth = 1;

        gc.gridx = 0;
        gc.gridy = 2;
        queryPanel.add(new JLabel("Cadence"), gc);
        gc.gridx = 1;
        cadenceCombo.setToolTipText("Thin the frames to at most one per interval; the archive is slow and the frames are large");
        queryPanel.add(cadenceCombo, gc);

        listPane.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof String datalocation)
                    label.setText(fileName(datalocation));
                return label;
            }
        });
        SearchableUtils.installSearchable(listPane);
        listPane.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                updateButtonState();
        });

        JPanel foundPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 0));
        foundPanel.add(foundLabel);

        JScrollPane scrollPane = new JScrollPane(listPane);
        scrollPane.setPreferredSize(RESULT_SIZE);

        searchButton.addActionListener(e -> search());
        fitsButton.addActionListener(e -> clearProducts());
        jp2Button.addActionListener(e -> clearProducts());
        for (JRadioButton button : productButtons)
            button.addActionListener(e -> updateProductList());
        orbitCombo.addActionListener(e -> clearProducts());
        cadenceCombo.addActionListener(e -> updateProductList());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        content.add(queryPanel);
        content.add(foundPanel);
        content.add(scrollPane);
        content.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        updateButtonState();
        return content;
    }

    @Nullable
    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    public void showDialog() {
        if (orbitCombo.getItemCount() == 0 && !loadingOrbits)
            loadOrbits();
        pack();
        setLocationRelativeTo(MainFrame.get());
        setVisible(true);
    }

    private void loadOrbits() {
        loadingOrbits = true;
        foundLabel.setText("Loading orbits...");
        updateButtonState();
        Task.submit("ASPIICS orbits", new LoadOrbits(), this::onLoadOrbitsSuccess, (logContext, t) -> onLoadOrbitsFailure());
    }

    private void onLoadOrbitsSuccess(List<Orbit> orbits) {
        loadingOrbits = false;
        orbitCombo.setModel(new DefaultComboBoxModel<>(orbits.toArray(Orbit[]::new)));
        foundLabel.setText("0 found");
        updateButtonState();
    }

    private void onLoadOrbitsFailure() {
        loadingOrbits = false;
        foundLabel.setText("Failed to load orbits");
        updateButtonState();
    }

    private void search() {
        Orbit orbit = (Orbit) orbitCombo.getSelectedItem();
        if (orbit == null)
            return;

        searching = true;
        clearProducts("Searching...");
        boolean jp2 = jp2Button.isSelected();
        Task.submit("ASPIICS search", new SearchProducts(orbit.orbitId(), jp2), this::onSearchSuccess, (logContext, t) -> onSearchFailure());
    }

    private void onSearchSuccess(List<String> result) {
        searching = false;
        products = result;
        updateProductList();
    }

    private void onSearchFailure() {
        searching = false;
        clearProducts();
    }

    private void updateButtonState() {
        boolean enabled = !loadingOrbits && !searching;
        orbitCombo.setEnabled(enabled);
        fitsButton.setEnabled(enabled);
        jp2Button.setEnabled(enabled);
        setEnabled(productButtons, enabled);
        cadenceCombo.setEnabled(enabled);
        searchButton.setEnabled(enabled && orbitCombo.getSelectedItem() != null);
        addButton.setEnabled(enabled && listPane.getSelectedIndex() >= 0);
    }

    private void updateProductList() {
        String type = selectedProductType();
        List<String> matching = products.stream()
                .filter(datalocation -> type.equals(productType(datalocation)))
                .sorted(Comparator.comparingLong(AspiicsDialog::timeOf))
                .toList();
        List<String> shown = thin(matching, selectedCadence());
        listPane.setListData(shown.toArray(String[]::new));
        foundLabel.setText(shown.size() < matching.size()
                ? shown.size() + " found (thinned from " + matching.size() + ')'
                : shown.size() + " found");
        updateButtonState();
    }

    // Keep at most one frame per cadence interval. ASPIICS runs at a high native cadence, and the
    // frames are large, so loading every one of them is rarely what you want for a movie.
    private static List<String> thin(List<String> sorted, long cadence) {
        if (cadence <= 0)
            return sorted;
        List<String> out = new ArrayList<>();
        long last = Long.MIN_VALUE;
        for (String datalocation : sorted) {
            long t = timeOf(datalocation);
            if (t < 0 || last == Long.MIN_VALUE || t - last >= cadence) {
                out.add(datalocation);
                if (t >= 0)
                    last = t;
            }
        }
        return out;
    }

    private long selectedCadence() {
        return cadenceCombo.getSelectedItem() instanceof Cadence cadence ? cadence.milli() : 0;
    }

    private boolean confirmDownload(int count) {
        if (count <= CONFIRM_FILES)
            return true;
        String size = jp2Button.isSelected() ? "" : String.format(" (roughly %.1f GB)", count * FITS_MB / 1024);
        return JOptionPane.showConfirmDialog(this,
                String.format("This will download %d frames%s.%nThe P3SC archive is not fast; a sparser cadence will cut this down.%nProceed?", count, size),
                "Large ASPIICS download", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
    }

    // Observation time from the file name: aspiics_<product>_l3_<YYYYMMDDThhmmss>_...
    private static long timeOf(String datalocation) {
        Matcher m = TIME_PATTERN.matcher(fileName(datalocation));
        return m.find() ? LocalDateTime.parse(m.group(1), FILE_TIME).toInstant(ZoneOffset.UTC).toEpochMilli() : -1;
    }

    private void clearProducts() {
        clearProducts("0 found");
    }

    private void clearProducts(String status) {
        products = List.of();
        listPane.setListData(new String[0]);
        foundLabel.setText(status);
        updateButtonState();
    }

    private String selectedProductType() {
        for (JRadioButton button : productButtons) {
            if (button.isSelected())
                return button.getText();
        }
        return productButtons[0].getText();
    }

    private static void addToGroup(JRadioButton... buttons) {
        ButtonGroup group = new ButtonGroup();
        for (JRadioButton button : buttons)
            group.add(button);
    }

    private static void setEnabled(JRadioButton[] buttons, boolean enabled) {
        for (JRadioButton button : buttons)
            button.setEnabled(enabled);
    }

    private static String productType(String datalocation) {
        String name = fileName(datalocation);
        int start = "aspiics_".length();
        int end = name.indexOf("_l3_", start);
        return end > start ? name.substring(start, end) : null;
    }

    private static URI uri(String datalocation) {
        return URI.create(DATA_URL + datalocation);
    }

    private static String fileName(String datalocation) {
        int slash = datalocation.lastIndexOf('/');
        return slash >= 0 ? datalocation.substring(slash + 1) : datalocation;
    }

    private record Orbit(int orbitId, String start, String end) {
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
                JSONObject item = array.optJSONObject(i);
                if (item != null && item.has("orbit_id"))
                    orbits.add(new Orbit(item.getInt("orbit_id"), item.optString("aa_start_time"), item.optString("aa_end_time")));
            }
            orbits.sort(Comparator.comparingInt(Orbit::orbitId));
            return orbits;
        }
    }

    private record SearchProducts(int orbitId, boolean jp2) implements Callable<List<String>> {
        @Override
        public List<String> call() throws Exception {
            String url = String.format(jp2 ? JP2_URL : FITS_URL, orbitId);
            JSONArray array = JSONUtils.getArray(new URI(url));
            List<String> products = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item != null) {
                    String datalocation = item.optString("datalocation");
                    if (productType(datalocation) != null)
                        products.add(datalocation);
                }
            }
            return products;
        }

    }

}
