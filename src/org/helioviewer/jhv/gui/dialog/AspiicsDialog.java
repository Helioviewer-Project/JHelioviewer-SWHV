package org.helioviewer.jhv.gui.dialog;

import java.awt.Component;
import java.awt.FlowLayout;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.thread.Task;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
public class AspiicsDialog extends StandardDialog {

    private static final String ORBITS_URL = "https://p3sc.oma.be/api/l3_orbit_time_ranges";
    // ROB Proba-3 ASPIICS datasets
    private static final int[] SOURCE_IDS = {506, 508, 509, 510};

    private final JComboBox<Orbit> orbitCombo = new JComboBox<>();
    private final JCheckBox[] productBoxes = {
            new JCheckBox("Total brightness", true),
            new JCheckBox("Polarized brightness"),
            new JCheckBox("He I D3"),
            new JCheckBox("Fe XIV")
    };

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
        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> {
            if (!(orbitCombo.getSelectedItem() instanceof Orbit orbit))
                return;
            for (int i = 0; i < SOURCE_IDS.length; i++) {
                if (productBoxes[i].isSelected())
                    ImageLayer.create().load(new APIRequest("ROB", SOURCE_IDS[i], orbit.start(), orbit.end(), APIRequest.CADENCE_ALL));
            }
            setVisible(false);
        });

        AbstractAction close = ComponentUtils.hideAction(this);
        setDefaultCancelAction(close);

        JButton closeButton = new JButton(close);
        closeButton.setText("Close");

        ButtonPanel panel = new ButtonPanel();
        panel.add(addButton, ButtonPanel.AFFIRMATIVE_BUTTON);
        panel.add(closeButton, ButtonPanel.CANCEL_BUTTON);
        return panel;
    }

    @Override
    public JComponent createContentPanel() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        orbitCombo.setPrototypeDisplayValue(new Orbit(9999, 0, 0));
        orbitCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Orbit(int ignored, long start, long end))
                    label.setToolTipText(TimeUtils.format(start) + " - " + TimeUtils.format(end));
                return label;
            }
        });
        JPanel orbitPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
        orbitPanel.setAlignmentX(LEFT_ALIGNMENT);
        orbitPanel.add(new JLabel("Orbits"));
        orbitPanel.add(orbitCombo);
        content.add(orbitPanel);
        for (JCheckBox box : productBoxes)
            content.add(box);
        return content;
    }

    @Nullable
    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    public void showDialog() {
        if (orbitCombo.getItemCount() == 0)
            Task.submitBackground("ASPIICS orbits", new LoadOrbits(), this::setOrbits, "Failed to load ASPIICS orbits");
        pack();
        setLocationRelativeTo(MainFrame.get());
        setVisible(true);
    }

    private void setOrbits(List<Orbit> orbits) {
        orbitCombo.setModel(new DefaultComboBoxModel<>(orbits.toArray(Orbit[]::new)));
        pack();
    }

    private record Orbit(int orbitId, long start, long end) {
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
                JSONObject item = array.getJSONObject(i);
                orbits.add(new Orbit(item.getInt("orbit_id"), TimeUtils.parse(item.getString("aa_start_time")), TimeUtils.parse(item.getString("aa_end_time"))));
            }
            orbits.sort(Comparator.comparingLong(Orbit::start).reversed());
            return orbits;
        }
    }

}
