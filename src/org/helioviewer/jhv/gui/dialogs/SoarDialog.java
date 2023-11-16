package org.helioviewer.jhv.gui.dialogs;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.gui.components.timeselector.TimeSelectorPanel;
import org.helioviewer.jhv.io.SoarClient;

import com.google.common.collect.ImmutableSortedMap;
import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
public class SoarDialog extends StandardDialog implements SoarClient.ReceiverItems, SoarClient.ReceiverSoops {

    private static final double MAX_SIZE = 2;
    private static final String[] Level = new String[]{/* "LL01", "LL02", "LL03",*/ "L1", "L2", "L3"};
    private static final ImmutableSortedMap<String, List<String>> Dataset = new ImmutableSortedMap.Builder<String, List<String>>(JHVGlobals.alphanumComparator).
            put("EUI FSI 174", List.of("eui-fsi174-image")).
            put("EUI FSI 304", List.of("eui-fsi304-image")).
            put("EUI HRI 174", List.of("eui-hrieuv174-image", "eui-hrieuvnon-image")).
            put("EUI HRI LYA", List.of("eui-hrilya1216-image")).
            put("PHI FDT", List.of("phi-fdt-blos", "phi-fdt-icnt")).
            put("PHI HRT", List.of("phi-hrt-bazi", "phi-hrt-binc", "phi-hrt-blos", "phi-hrt-bmag", "phi-hrt-icnt", /* "phi-hrt-stokes",*/ "phi-hrt-vlos")).
            put("MAG RTN", List.of("mag-rtn-normal", "mag-rtn-normal-1-minute"/*, "mag-rtn-burst"*/)).
            // put("MAG SRF", List.of("mag-srf-normal" /*, "mag-srf-burst" */)).
            // put("MAG VSO", List.of("mag-vso-normal", "mag-vso-normal-1-minute"/*, "mag-vso-burst"*/)).
                    put("SWA PAS", List.of("swa-pas-grnd-mom")).
            build();

    private boolean soopsDownloaded;
    private final JComboBox<String> soopCombo = new JComboBox<>();
    private final TimeSelectorPanel timeSelectorPanel = new TimeSelectorPanel();
    private final JList<SoarClient.DataItem> listPane = new JList<>();
    private final JLabel foundLabel = new JLabel("0 found", JLabel.RIGHT);

    private static SoarDialog instance;

    public static SoarDialog getInstance() {
        return instance == null ? instance = new SoarDialog(JHVFrame.getFrame()) : instance;
    }

    private SoarDialog(JFrame mainFrame) {
        super(mainFrame, true);
        setResizable(false);
        setTitle("New SOAR Layer");
    }

    private static double getTotalSize(List<SoarClient.DataItem> items) {
        return items.stream().mapToLong(SoarClient.DataItem::size).sum() / (1024. * 1024. * 1024.);
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
            List<SoarClient.DataItem> items = listPane.getSelectedValuesList();
            if (items.isEmpty())
                return;

            double size = getTotalSize(items);
            if (size > MAX_SIZE) {
                Message.err("SOAR error", String.format("Too much data selected for download: %.1fGiB.%nPlease reduce the selection to less than %.1fGiB.", size, MAX_SIZE));
            } else {
                SoarClient.submitLoad(items);
                setVisible(false);
            }
        });
        return loadButton;
    }

    @Override
    public JComponent createContentPanel() {
        JRadioButton timeQuery = new JRadioButton("Time");
        timeQuery.setSelected(true);
        JRadioButton soopQuery = new JRadioButton("SOOP");
        soopQuery.setSelected(false);
        soopCombo.setEnabled(false);

        ButtonGroup queryGroup = new ButtonGroup();
        queryGroup.add(timeQuery);
        queryGroup.add(soopQuery);

        soopQuery.addItemListener(e -> {
            boolean selected = soopQuery.isSelected();
            if (selected && !soopsDownloaded) {
                soopsDownloaded = true;
                SoarClient.submitGetSoops(this);
            }
            soopCombo.setEnabled(selected);
            ComponentUtils.setEnabled(timeSelectorPanel, !selected);
        });

        JPanel queryPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.weighty = 1;
        gc.fill = GridBagConstraints.BOTH;

        gc.weightx = 0;
        gc.gridx = 0;
        gc.gridy = 0;
        queryPanel.add(timeQuery, gc);
        gc.gridy = 1;
        queryPanel.add(soopQuery, gc);

        gc.weightx = 1;
        gc.gridx = 1;
        gc.gridy = 0;
        queryPanel.add(timeSelectorPanel, gc);
        gc.gridy = 1;
        queryPanel.add(soopCombo, gc);

        JPanel dataSelector = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 0));
        JComboBox<String> datasetCombo = new JComboBox<>(Dataset.keySet().toArray(String[]::new));
        dataSelector.add(datasetCombo);
        JComboBox<String> levelCombo = new JComboBox<>(Level);
        levelCombo.setSelectedItem("L2");
        dataSelector.add(levelCombo);
        JButton searchButton = getSearchButton(datasetCombo, levelCombo, timeQuery);
        dataSelector.add(searchButton);

        JPanel foundPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 0));
        foundPanel.add(foundLabel);
        JPanel selectedPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 0));
        JLabel selectedLabel = new JLabel("0 selected", JLabel.RIGHT);
        selectedPanel.add(selectedLabel);

        listPane.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                List<SoarClient.DataItem> items = listPane.getSelectedValuesList();
                selectedLabel.setText(items.size() + " selected " + String.format("(%.1fGiB)", getTotalSize(items)));
            }
        });
        JScrollPane scrollPane = new JScrollPane(listPane);
        scrollPane.setPreferredSize(new Dimension(350, 350));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        content.add(queryPanel);
        content.add(dataSelector);
        content.add(foundPanel);
        content.add(scrollPane);
        content.add(selectedPanel);

        content.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        return content;
    }

    private JButton getSearchButton(JComboBox<String> datasetCombo, JComboBox<String> levelCombo, JRadioButton timeQuery) {
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> {
            if (datasetCombo.getSelectedItem() instanceof String dataset && levelCombo.getSelectedItem() instanceof String level) {
                List<String> descriptors = Dataset.get(dataset);
                if (descriptors != null) {
                    if (timeQuery.isSelected()) {
                        SoarClient.submitSearchTime(this, descriptors, level, timeSelectorPanel.getStartTime(), timeSelectorPanel.getEndTime());
                        foundLabel.setText("Searching...");
                    } else if (soopCombo.getSelectedItem() instanceof String soop) {
                        SoarClient.submitSearchSoop(this, descriptors, level, soop);
                        foundLabel.setText("Searching...");
                    }
                }
            }
        });
        return searchButton;
    }

    @Nullable
    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    public void showDialog() {
        pack();
        setLocationRelativeTo(JHVFrame.getFrame());
        setVisible(true);
    }

    @Override
    public void setSoarResponseItems(List<SoarClient.DataItem> list) {
        listPane.setListData(list.toArray(SoarClient.DataItem[]::new));
        foundLabel.setText(list.size() + " found");
    }

    @Override
    public void setSoarResponseSoops(List<String> list) {
        soopCombo.setModel(new DefaultComboBoxModel<>(list.toArray(String[]::new)));
        soopCombo.setSelectedIndex(0);
    }

}
