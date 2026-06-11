package org.helioviewer.jhv.gui.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
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
    private static final String FITS_URL = "https://p3sc.oma.be/api/L3?select=version,name,orbit_id&orbit_id=eq.%d&active=is.true";
    private static final String JP2_URL = "https://p3sc.oma.be/api/L3_jpeg2000?select=version,name,orbit_id&orbit_id=eq.%d&active=is.true";
    private static final String FITS_DATA_URL = "https://p3sc.oma.be/datarepfiles/L3/%s/%s";
    private static final String JP2_DATA_URL = "https://p3sc.oma.be/datarepfiles/L3_jpeg2000/%s/%06d/%s";
    private static final Dimension RESULT_SIZE = new Dimension(500, 350);

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
    private final JButton searchButton = new JButton("Search");
    private final JButton addButton = new JButton("Add");
    private final JList<Product> listPane = new JList<>();
    private final JLabel foundLabel = new JLabel("0 found", JLabel.RIGHT);
    private List<Product> products = List.of();
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
            List<Product> selected = listPane.getSelectedValuesList();
            if (selected.isEmpty())
                return;

            Commands.loadImage(selected.stream().map(Product::uri).toList());
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
                if (value instanceof Orbit(int orbitId, String start, String end)) {
                    label.setText(Integer.toString(orbitId));
                    label.setToolTipText(start + " - " + end);
                }
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

        JPanel foundPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 0));
        foundPanel.add(foundLabel);

        listPane.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Product product) {
                    label.setText(product.name());
                    label.setToolTipText(product.version() + " orbit " + product.orbitId());
                }
                return label;
            }
        });
        SearchableUtils.installSearchable(listPane);
        listPane.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                updateButtonState();
        });

        JScrollPane scrollPane = new JScrollPane(listPane);
        scrollPane.setPreferredSize(RESULT_SIZE);

        searchButton.addActionListener(e -> search());
        fitsButton.addActionListener(e -> clearProducts());
        jp2Button.addActionListener(e -> clearProducts());
        for (JRadioButton button : productButtons)
            button.addActionListener(e -> updateProductList());
        orbitCombo.addActionListener(e -> clearProducts());

        JPanel content = new JPanel();
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.PAGE_AXIS));
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
        clearProducts();
        foundLabel.setText("Searching...");
        boolean jp2 = jp2Button.isSelected();
        String url = String.format(jp2 ? JP2_URL : FITS_URL, orbit.orbitId());
        Task.submit("ASPIICS search", new SearchProducts(url, jp2), this::onSearchSuccess, (logContext, t) -> onSearchFailure());
    }

    private void onSearchSuccess(List<Product> result) {
        searching = false;
        products = result;
        updateProductList();
    }

    private void onSearchFailure() {
        searching = false;
        clearProducts();
    }

    private void updateButtonState() {
        boolean enabled = !busy();
        orbitCombo.setEnabled(enabled);
        fitsButton.setEnabled(enabled);
        jp2Button.setEnabled(enabled);
        setEnabled(productButtons, enabled);
        searchButton.setEnabled(enabled && orbitCombo.getSelectedItem() != null);
        addButton.setEnabled(enabled && listPane.getSelectedIndex() >= 0);
    }

    private boolean busy() {
        return loadingOrbits || searching;
    }

    private void updateProductList() {
        String type = selectedProductType();
        Product[] filtered = products.stream()
                .filter(product -> product.type().equals(type))
                .toArray(Product[]::new);
        listPane.setListData(filtered);
        foundLabel.setText(filtered.length + " found");
        updateButtonState();
    }

    private void clearProducts() {
        products = List.of();
        listPane.setListData(new Product[0]);
        foundLabel.setText("0 found");
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

    private record Orbit(int orbitId, String start, String end) {}

    private record Product(String version, String name, int orbitId, String type, boolean jp2) {
        private URI uri() {
            return jp2
                    ? URI.create(String.format(JP2_DATA_URL, version, orbitId, name))
                    : URI.create(String.format(FITS_DATA_URL, version, name));
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

    private record SearchProducts(String url, boolean jp2) implements Callable<List<Product>> {
        @Override
        public List<Product> call() throws Exception {
            JSONArray array = JSONUtils.getArray(new URI(url));
            List<Product> products = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item != null) {
                    String name = item.optString("name");
                    String type = productType(name);
                    if (type != null)
                        products.add(new Product(item.optString("version"), name, item.optInt("orbit_id"), type, jp2));
                }
            }
            return products;
        }

        private static String productType(String name) {
            int start = "aspiics_".length();
            int end = name.indexOf("_l3_", start);
            return end > start ? name.substring(start, end) : null;
        }
    }

}
