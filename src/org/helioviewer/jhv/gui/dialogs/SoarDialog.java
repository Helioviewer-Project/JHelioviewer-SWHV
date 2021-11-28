package org.helioviewer.jhv.gui.dialogs;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.gui.components.timeselector.TimeSelectorPanel;
import org.helioviewer.jhv.io.SoarClient;
import org.helioviewer.jhv.io.SoarReceiver;

import com.google.common.collect.ImmutableSortedMap;
import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
public class SoarDialog extends StandardDialog implements SoarReceiver {

    private static final int MAX_FILES = 100;
    private static final String[] Level = new String[]{/* "LL01", "LL02", "LL03", */ "L1", "L2" /*, "L3"*/};
    private static final ImmutableSortedMap<String, List<String>> Dataset = new ImmutableSortedMap.Builder<String, List<String>>(JHVGlobals.alphanumComparator).
            put("EUI FSI 174", List.of("EUI-FSI174-IMAGE")).
            put("EUI FSI 304", List.of("EUI-FSI304-IMAGE")).
            put("EUI HRI 174", List.of("EUI-HRIEUV174-IMAGE", "EUI-HRIEUVNON-IMAGE")).
            put("EUI HRI LYA", List.of("EUI-HRILYA1216-IMAGE")).
            build();

    private final TimeSelectorPanel timeSelectorPanel = new TimeSelectorPanel();
    private final JList<String> listPane = new JList<>();
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

    @Override
    public ButtonPanel createButtonPanel() {
        AbstractAction close = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };
        setDefaultCancelAction(close);

        JButton cancelBtn = new JButton(close);
        cancelBtn.setText("Cancel");

        JButton loadBtn = new JButton("Add");
        loadBtn.addActionListener(e -> {
            List<String> descriptors = listPane.getSelectedValuesList();
            int length = descriptors.size();
            if (length == 0)
                return;
            if (length > MAX_FILES) {
                Message.err("Too many files selected (" + length + ").", "Please select at most " + MAX_FILES + " files.", false);
            } else {
                SoarClient.submitLoad(descriptors);
                setVisible(false);
            }
        });

        ButtonPanel panel = new ButtonPanel();
        panel.add(loadBtn, ButtonPanel.AFFIRMATIVE_BUTTON);
        panel.add(cancelBtn, ButtonPanel.CANCEL_BUTTON);

        return panel;
    }

    @Override
    public JComponent createContentPanel() {
        JPanel dataSelector = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 0));
        JComboBox<String> datasetCombo = new JComboBox<>(Dataset.keySet().toArray(String[]::new));
        dataSelector.add(datasetCombo);
        JComboBox<String> levelCombo = new JComboBox<>(Level);
        levelCombo.setSelectedItem("L2");
        dataSelector.add(levelCombo);
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> {
            if (datasetCombo.getSelectedItem() instanceof String dataset && levelCombo.getSelectedItem() instanceof String level) {
                List<String> descriptors = Dataset.get(dataset);
                if (descriptors != null) {
                    SoarClient.submitSearch(this, descriptors, level, timeSelectorPanel.getStartTime(), timeSelectorPanel.getEndTime());
                    foundLabel.setText("Searching...");
                }
            }
        });
        dataSelector.add(searchBtn);

        JPanel foundPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 0));
        foundPanel.add(foundLabel);
        JPanel selectedPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 0));
        JLabel selectedLabel = new JLabel("0 selected", JLabel.RIGHT);
        selectedPanel.add(selectedLabel);

        listPane.addListSelectionListener(e -> selectedLabel.setText(listPane.getSelectedValuesList().size() + " selected"));
        JScrollPane scrollPane = new JScrollPane(listPane);
        scrollPane.setPreferredSize(new Dimension(350, 350));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        content.add(timeSelectorPanel);
        content.add(dataSelector);
        content.add(foundPanel);
        content.add(scrollPane);
        content.add(selectedPanel);

        content.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        return content;
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
    public void setSoarItems(List<String> items) {
        listPane.setListData(items.toArray(String[]::new));
        foundLabel.setText(items.size() + " found");
    }

}
