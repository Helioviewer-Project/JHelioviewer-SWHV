package org.helioviewer.jhv.timelines.gui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.band.Band;
import org.helioviewer.jhv.timelines.band.BandType;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;

@SuppressWarnings("serial")
public final class TimelineDialog extends StandardDialog implements Interfaces.ShowableDialog {

    private final JComboBox<String> comboGroup = new JComboBox<>();
    private final JList<BandType> listBand = new JList<>();
    private final AbstractAction load = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            for (BandType bandType : listBand.getSelectedValuesList()) {
                Timelines.getLayers().add(Band.createFromType(bandType));
            }
            setVisible(false);
        }
    };

    public TimelineDialog() {
        super(JHVFrame.getFrame(), "New Layer", true);
        setResizable(false);
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
        setDefaultAction(load);
        setInitFocusedComponent(listBand);

        JButton cancelBtn = new JButton(close);
        cancelBtn.setText("Cancel");
        JButton okBtn = new JButton(load);
        okBtn.setText("Add");

        ButtonPanel panel = new ButtonPanel();
        panel.add(okBtn, ButtonPanel.AFFIRMATIVE_BUTTON);
        panel.add(cancelBtn, ButtonPanel.CANCEL_BUTTON);

        return panel;
    }

    @Override
    public JComponent createContentPanel() {
        comboGroup.addActionListener(e -> updateGroupValues());
        listBand.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    load.actionPerformed(null);
                }
            }
        });
        com.jidesoft.swing.SearchableUtils.installSearchable(listBand);
        JScrollPane scrollPane = new JScrollPane(listBand);
        scrollPane.setPreferredSize(new Dimension(350, 350));

        JPanel groupPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
        groupPanel.add(new JLabel("Group", JLabel.RIGHT));
        groupPanel.add(comboGroup);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        content.add(groupPanel);
        content.add(scrollPane);
        return content;
    }

    @Nullable
    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    @Override
    public void showDialog() {
        pack();
        setLocationRelativeTo(JHVFrame.getFrame());
        setVisible(true);
    }

    public void setupDatasets() {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(BandType.getGroups());
        if (model.getSize() > 0) {
            comboGroup.setModel(model);
            comboGroup.setSelectedIndex(0);
            updateGroupValues();
        }
    }

    private void updateGroupValues() {
        if (comboGroup.getSelectedItem() instanceof String group) {
            listBand.setListData(BandType.getBandTypes(group).toArray(BandType[]::new));
            listBand.setSelectedIndex(0);
        }
    }

}
