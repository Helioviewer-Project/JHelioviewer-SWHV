package org.helioviewer.jhv.gui.dialogs;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.components.timeselector.TimeSelectorPanel;
import org.helioviewer.jhv.io.LoadSynoptic;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
public class SynopticDialog extends StandardDialog {
    private static final String server = "https://idoc-ssa-prod.ias.u-psud.fr";
    private static final String hmiQuery = "/api/s005a/map/hmi";
    private static final String aiaQuery = "/api/s005a/map/";

    private final TimeSelectorPanel timeSelectorPanel = new TimeSelectorPanel();
    private final JRadioButton hmiButton = new JRadioButton("HMI", true);
    private final JRadioButton aiaButton = new JRadioButton("AIA");
    private final JComboBox<String> hmiCombo = new JComboBox<>(new String[]{"Magnetogram", "Continuum"});
    private final JComboBox<String> aiaCombo = new JComboBox<>(new String[]{"94", "131", "171", "193", "211", "304", "335", "1600", "1700"});

    private static SynopticDialog instance;

    public static SynopticDialog getInstance() {
        return instance == null ? instance = new SynopticDialog(JHVFrame.getFrame()) : instance;
    }

    private SynopticDialog(JFrame mainFrame) {
        super(mainFrame, true);
        setResizable(false);
        setTitle("New Synoptic Layer");
    }

    @Override
    public ButtonPanel createButtonPanel() {
        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> {
            JSONObject jo = new JSONObject();
            jo.put("start_date", TimeUtils.formatDate(timeSelectorPanel.getStartTime()));
            jo.put("end_date", TimeUtils.formatDate(timeSelectorPanel.getEndTime()));
            String channel = hmiButton.isSelected()
                    ? ("Magnetogram".equals(hmiCombo.getSelectedItem()) ? "hmi.m_720s" : "hmi.ic_720s")
                    : (String) aiaCombo.getSelectedItem();
            jo.put("channels", new JSONArray().put(channel));
            jo.put("map_type", new JSONArray().put("forecast").put("nowcast").put("temporary").put("archived"));
            jo.put("latest", true);
            LoadSynoptic.submit(server, hmiButton.isSelected() ? hmiQuery : aiaQuery, jo);
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

        hmiCombo.setSelectedItem("Magnetogram");
        aiaCombo.setSelectedItem("171");

        JPanel hmiPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
        hmiPanel.add(hmiButton);
        hmiPanel.add(hmiCombo);

        JPanel aiaPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
        aiaPanel.add(aiaButton);
        aiaPanel.add(aiaCombo);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        content.add(timeSelectorPanel);
        content.add(hmiPanel);
        content.add(aiaPanel);
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

}
