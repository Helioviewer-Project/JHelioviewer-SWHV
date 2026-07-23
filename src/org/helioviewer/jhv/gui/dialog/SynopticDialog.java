package org.helioviewer.jhv.gui.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
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
import org.helioviewer.jhv.gui.component.MoviePanel;
import org.helioviewer.jhv.gui.time.TimeSelectorPanel;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.thread.Task;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
public class SynopticDialog extends StandardDialog {
    private static final String server = "https://idoc-ssa-prod.ias.u-psud.fr";
    private static final String hmiQuery = "/api/s005a/map/hmi";
    private static final String aiaQuery = "/api/s050a/map/";
    private static final Dimension resultSize = new Dimension(500, 350);

    private final TimeSelectorPanel timeSelectorPanel = new TimeSelectorPanel();
    private final JRadioButton hmiButton = new JRadioButton("HMI", true);
    private final JRadioButton aiaButton = new JRadioButton("AIA");
    private final JComboBox<String> hmiCombo = new JComboBox<>(new String[]{"Magnetogram", "Continuum"});
    private final JComboBox<String> aiaCombo = new JComboBox<>(new String[]{"94", "131", "171", "193", "211", "304", "335", "1600", "1700"});
    private final JRadioButton forecastButton = new JRadioButton("forecast", true);
    private final JRadioButton nowcastButton = new JRadioButton("nowcast");
    private final JRadioButton temporaryButton = new JRadioButton("temporary");
    private final JRadioButton archivedButton = new JRadioButton("archived");
    private final JButton searchButton = new JButton("Search");
    private final JButton addButton = new JButton("Add");
    private final JList<URI> listPane = new JList<>();
    private final JLabel foundLabel = new JLabel("0 found", JLabel.RIGHT);
    private final JLabel selectedLabel = new JLabel("0 selected", JLabel.RIGHT);
    private boolean searching;

    private static SynopticDialog instance;

    public static SynopticDialog getInstance() {
        return instance == null ? instance = new SynopticDialog(MainFrame.get()) : instance;
    }

    private SynopticDialog(JFrame mainFrame) {
        super(mainFrame, true);
        setResizable(false);
        setTitle("New Synoptic Layer");
    }

    @Override
    public ButtonPanel createButtonPanel() {
        addButton.addActionListener(e -> {
            List<URI> selected = listPane.getSelectedValuesList();
            if (selected.isEmpty())
                return;

            Commands.loadImage(selected);
            setVisible(false);
        });

        AbstractAction close = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };
        setDefaultCancelAction(close);

        JButton closeButton = new JButton(close);
        closeButton.setText("Cancel");

        ButtonPanel panel = new ButtonPanel();
        panel.add(addButton, ButtonPanel.AFFIRMATIVE_BUTTON);
        panel.add(closeButton, ButtonPanel.CANCEL_BUTTON);
        return panel;
    }

    @Override
    public JComponent createContentPanel() {
        ButtonGroup sourceGroup = new ButtonGroup();
        sourceGroup.add(hmiButton);
        sourceGroup.add(aiaButton);
        ButtonGroup mapTypeGroup = new ButtonGroup();
        mapTypeGroup.add(forecastButton);
        mapTypeGroup.add(nowcastButton);
        mapTypeGroup.add(temporaryButton);
        mapTypeGroup.add(archivedButton);

        hmiCombo.setSelectedItem("Magnetogram");
        aiaCombo.setSelectedItem("171");
        updateSourceControls();

        JPanel queryPanel = new JPanel(new GridBagLayout());
        queryPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        GridBagConstraints gc = new GridBagConstraints();
        gc.weighty = 1;
        gc.fill = GridBagConstraints.BOTH;

        gc.weightx = 0;
        gc.gridx = 0;
        gc.gridy = 0;
        queryPanel.add(hmiButton, gc);
        gc.gridy = 1;
        queryPanel.add(aiaButton, gc);

        gc.weightx = 0;
        gc.gridx = 1;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.WEST;
        queryPanel.add(hmiCombo, gc);
        gc.gridy = 1;
        queryPanel.add(aiaCombo, gc);

        JPanel mapTypePanel = new JPanel();
        mapTypePanel.setLayout(new BoxLayout(mapTypePanel, BoxLayout.LINE_AXIS));
        mapTypePanel.add(forecastButton);
        mapTypePanel.add(nowcastButton);
        mapTypePanel.add(temporaryButton);
        mapTypePanel.add(archivedButton);
        mapTypePanel.add(Box.createHorizontalGlue());
        mapTypePanel.add(searchButton);

        searchButton.addActionListener(e -> {
            searching = true;
            foundLabel.setText("Searching...");
            selectedLabel.setText("0 selected");
            listPane.setListData(new URI[0]);
            updateButtonState();
            Task.submit(server, new SearchSynoptic(server, hmiButton.isSelected() ? hmiQuery : aiaQuery, buildQuery()), this::onSearchSuccess,
                    (logContext, t) -> onSearchFailure(t));
        });
        gc.gridx = 0;
        gc.gridy = 2;
        gc.gridwidth = 2;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.BOTH;
        queryPanel.add(mapTypePanel, gc);

        JPanel foundPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 0));
        foundPanel.add(foundLabel);

        JPanel selectedPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 0));
        selectedPanel.add(selectedLabel);

        listPane.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                updateButtonState();
        });
        listPane.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof URI uri) {
                    String path = uri.getPath();
                    int slash = path.lastIndexOf('/');
                    label.setText(slash >= 0 ? path.substring(slash + 1) : path);
                    label.setToolTipText(uri.toString());
                }
                return label;
            }
        });
        com.jidesoft.swing.SearchableUtils.installSearchable(listPane);
        JScrollPane scrollPane = new JScrollPane(listPane);
        scrollPane.setPreferredSize(resultSize);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        content.add(timeSelectorPanel);
        content.add(queryPanel);
        content.add(foundPanel);
        content.add(scrollPane);
        content.add(selectedPanel);
        content.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        hmiButton.addActionListener(e -> updateSourceControls());
        aiaButton.addActionListener(e -> updateSourceControls());
        forecastButton.addActionListener(e -> updateButtonState());
        nowcastButton.addActionListener(e -> updateButtonState());
        temporaryButton.addActionListener(e -> updateButtonState());
        archivedButton.addActionListener(e -> updateButtonState());
        updateButtonState();
        return content;
    }

    @Nullable
    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    public void showDialog() {
        // Preset the main window's time range so it does not have to be retyped.
        timeSelectorPanel.setTime(MoviePanel.getInstance().getStartTime(), MoviePanel.getInstance().getEndTime());
        pack();
        setLocationRelativeTo(MainFrame.get());
        setVisible(true);
    }

    private JSONObject buildQuery() {
        JSONObject jo = new JSONObject();
        jo.put("start_date", TimeUtils.formatDate(timeSelectorPanel.getStartTime()));
        jo.put("end_date", TimeUtils.formatDate(timeSelectorPanel.getEndTime()));
        String channel = hmiButton.isSelected()
                ? ("Magnetogram".equals(hmiCombo.getSelectedItem()) ? "hmi.m_720s" : "hmi.ic_720s")
                : (String) aiaCombo.getSelectedItem();
        jo.put("channels", new JSONArray().put(channel));
        JSONArray mapTypes = new JSONArray();
        if (forecastButton.isSelected())
            mapTypes.put("forecast");
        else if (nowcastButton.isSelected())
            mapTypes.put("nowcast");
        else if (temporaryButton.isSelected())
            mapTypes.put("temporary");
        else if (archivedButton.isSelected())
            mapTypes.put("archived");
        jo.put("map_type", mapTypes);
        jo.put("latest", true);
        return jo;
    }

    private void onSearchSuccess(List<URI> result) {
        searching = false;
        listPane.setListData(result.toArray(URI[]::new));
        foundLabel.setText(result.size() + " found");
        updateButtonState();
    }

    private void onSearchFailure(Throwable ignored) {
        searching = false;
        foundLabel.setText("0 found");
        updateButtonState();
    }

    private void updateButtonState() {
        searchButton.setEnabled(!searching);
        int count = listPane.getSelectedIndices().length;
        selectedLabel.setText(count + " selected");
        addButton.setEnabled(count > 0 && !searching);
    }

    private void updateSourceControls() {
        boolean hmi = hmiButton.isSelected();
        hmiCombo.setEnabled(hmi);
        aiaCombo.setEnabled(!hmi);
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
