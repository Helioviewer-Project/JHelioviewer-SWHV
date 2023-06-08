package org.helioviewer.jhv.gui.dialogs;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
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
import com.google.common.collect.ImmutableSortedSet;
import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
public class SoarDialog extends StandardDialog implements SoarClient.Receiver {

    private static final double MAX_SIZE = 2;
    private static final String[] Level = new String[]{/* "LL01", "LL02", "LL03",*/ "L1", "L2", "L3"};
    private static final ImmutableSortedMap<String, List<String>> Dataset = new ImmutableSortedMap.Builder<String, List<String>>(JHVGlobals.alphanumComparator).
            put("EUI FSI 174", List.of("EUI-FSI174-IMAGE", "eui-fsi174-image")).
            put("EUI FSI 304", List.of("EUI-FSI304-IMAGE", "eui-fsi304-image")).
            put("EUI HRI 174", List.of("EUI-HRIEUV174-IMAGE", "EUI-HRIEUVNON-IMAGE", "eui-hrieuv174-image", "eui-hrieuvnon-image")).
            put("EUI HRI LYA", List.of("EUI-HRILYA1216-IMAGE", "eui-hrilya1216-image")).
            put("MAG RTN", List.of("MAG-RTN-NORMAL", "MAG-RTN-NORMAL-1-MINUTE"/*, "MAG-RTN-BURST"*/)).
            // put("MAG SRF", List.of("MAG-SRF-NORMAL" /*, "MAG-SRF-BURST" */)).
            // put("MAG VSO", List.of("MAG-VSO-NORMAL", "MAG-VSO-NORMAL-1-MINUTE"/*, "MAG-VSO-BURST"*/)).
                    put("SWA PAS", List.of("SWA-PAS-GRND-MOM")).
            build();

    // curl "http://soar.esac.esa.int/soar-sl-tap/tap/sync?REQUEST=doQuery&LANG=ADQL&FORMAT=csv&QUERY=SELECT+DISTINCT+soop_name+FROM+soar.soop+ORDER+BY+soop_name"
    private static final ImmutableSortedSet<String> SOOPs = new ImmutableSortedSet.Builder<String>(JHVGlobals.alphanumComparator).
            add("CC_OFFPOI_ALIGNMENT").
            add("CC_OFFPOI_FLATFIELD_FULL").
            add("CC_OFFPOI_FLATFIELD_HRI").
            add("CC_OFFPOI_OOF").
            add("CC_OFFPOI_STAR").
            add("CC_OFFPOI_STRAYLIGHT").
            add("CC_ROLLS_RS").
            add("COORD_CALIBRATION").
            add("I_DEFAULT").
            add("L_BOTH_HRES_HCAD_Major-Flare").
            add("L_BOTH_HRES_LCAD_CH-Boundary-Expansion").
            add("L_BOTH_LRES_MCAD_Pole-to-Pole").
            add("L_BOTH_MRES_MCAD_Farside-Connection").
            add("L_BOTH_MRES_MCAD_Flare-SEPs").
            add("L_FULL_HRES_HCAD_Coronal-Dynamics").
            add("L_FULL_HRES_HCAD_Eruption-Watch").
            add("L_FULL_HRES_LCAD_MagnFieldConfig").
            add("L_FULL_HRES_MCAD_Coronal-He-Abundance").
            add("L_FULL_LRES_MCAD_Coronal-Synoptic").
            add("L_FULL_LRES_MCAD_Probe-Quadrature").
            add("L_FULL_MRES_MCAD_CME-SEPs").
            add("L_IS_SoloHI_STIX").
            add("L_IS_STIX").
            add("L_SMALL_HRES_HCAD_Fast-Wind").
            add("L_SMALL_HRES_HCAD_Slow-Wind-Connection").
            add("L_SMALL_MRES_MCAD_Ballistic-Connection").
            add("L_SMALL_MRES_MCAD_Composition-Mosaic").
            add("L_SMALL_MRES_MCAD_Connection-Mosaic").
            add("L_SMALL_MRES_MCAD_Earth-Quadrature").
            add("L_TEMPORARY").
            add("R_BOTH_HRES_HCAD_Filaments").
            add("R_BOTH_HRES_HCAD_Nanoflares").
            add("R_BOTH_HRES_MCAD_Bright-Points").
            add("R_FULL_HRES_HCAD_Density-Fluctuations").
            add("R_FULL_LRES_HCAD_Full-Disk-Helioseismology").
            add("R_FULL_LRES_LCAD_Out-of-RSW-synoptics").
            add("R_FULL_LRES_LCAD_Transition-Corona").
            add("R_SMALL_HRES_HCAD_AR-Dynamics").
            add("R_SMALL_HRES_HCAD_Atmospheric-Dynamics-Structure").
            add("R_SMALL_HRES_HCAD_Ephemeral").
            add("R_SMALL_HRES_HCAD_Granulation-Tracking").
            add("R_SMALL_HRES_HCAD_Local-Area-Helioseismology").
            add("R_SMALL_HRES_HCAD_PDF-Mosaic").
            add("R_SMALL_HRES_HCAD_RS-burst").
            add("R_SMALL_HRES_HCAD_Wave-Stereoscopy").
            add("R_SMALL_HRES_LCAD_Composition-vs-Height").
            add("R_SMALL_HRES_LCAD_Fine-Scale-Structure").
            add("R_SMALL_HRES_MCAD_AR-Heating").
            add("R_SMALL_HRES_MCAD_Full-Disk-Mosaic").
            add("R_SMALL_HRES_MCAD_Polar-Observations").
            add("R_SMALL_MRES_HCAD_Sunspot-Oscillations").
            add("R_SMALL_MRES_MCAD_AR-Long-Term").
            build();

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

        JButton loadBtn = getLoadBtn();
        JButton cancelBtn = new JButton(close);
        cancelBtn.setText("Cancel");

        ButtonPanel panel = new ButtonPanel();
        panel.add(loadBtn, ButtonPanel.AFFIRMATIVE_BUTTON);
        panel.add(cancelBtn, ButtonPanel.CANCEL_BUTTON);

        return panel;
    }

    private JButton getLoadBtn() {
        JButton loadBtn = new JButton("Add");
        loadBtn.addActionListener(e -> {
            List<SoarClient.DataItem> items = listPane.getSelectedValuesList();
            if (items.isEmpty())
                return;

            double size = getTotalSize(items);
            if (size > MAX_SIZE) {
                Message.err("SOAR error", String.format("Too much data selected for download: %.1fGiB.\nPlease reduce the selection to less than %.1fGiB.", size, MAX_SIZE));
            } else {
                SoarClient.submitLoad(items);
                setVisible(false);
            }
        });
        return loadBtn;
    }

    @Override
    public JComponent createContentPanel() {
        JComboBox<String> soopCombo = new JComboBox<>(SOOPs.toArray(String[]::new));
        soopCombo.setEnabled(false);

        JRadioButton timeQuery = new JRadioButton("Time");
        timeQuery.setSelected(true);
        JRadioButton soopQuery = new JRadioButton("SOOP");
        soopQuery.setSelected(false);

        ButtonGroup queryGroup = new ButtonGroup();
        queryGroup.add(timeQuery);
        queryGroup.add(soopQuery);

        soopQuery.addItemListener(e -> {
                boolean select = soopQuery.isSelected();
                soopCombo.setEnabled(select);
                ComponentUtils.setEnabled(timeSelectorPanel, !select);
        });

        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 1;
        gc.weighty = 1;
        gc.fill = GridBagConstraints.BOTH;

        JPanel queryPanel = new JPanel(new GridBagLayout());
        gc.gridx = 0;
        gc.gridy = 0;
        queryPanel.add(timeQuery, gc);
        gc.gridx = 1;
        gc.gridy = 0;
        queryPanel.add(timeSelectorPanel, gc);
        gc.gridx = 0;
        gc.gridy = 1;
        queryPanel.add(soopQuery, gc);
        gc.gridx = 1;
        gc.gridy = 1;
        queryPanel.add(soopCombo, gc);

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
        dataSelector.add(searchBtn);

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
    public void setSoarResponse(List<SoarClient.DataItem> items) {
        listPane.setListData(items.toArray(SoarClient.DataItem[]::new));
        foundLabel.setText(items.size() + " found");
    }

}
