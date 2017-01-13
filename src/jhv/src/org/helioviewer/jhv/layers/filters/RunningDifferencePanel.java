package org.helioviewer.jhv.layers.filters;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.dialogs.MetaDataDialog;
import org.helioviewer.jhv.layers.ImageLayerOptions;
import org.helioviewer.jhv.io.DownloadViewTask;

@SuppressWarnings("serial")
public class RunningDifferencePanel {

    private static final String[] combolist = { "No difference images", "Running difference", "Base difference" };

    private final JPanel diffPanel = new JPanel();
    private final JCheckBox diffRot;
    private final JPanel radPanel;

    public RunningDifferencePanel() {
        JButton downloadButton = new JButton(new AbstractAction() {
            {
                putValue(SHORT_DESCRIPTION, "Download selected layer");
                putValue(SMALL_ICON, IconBank.getIcon(JHVIcon.DOWNLOAD));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                DownloadViewTask downloadTask = new DownloadViewTask(((ImageLayerOptions) getComponent().getParent()).getView());
                JHVGlobals.getExecutorService().execute(downloadTask);
            }
        });
        downloadButton.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        downloadButton.setText(null);
        downloadButton.setBorderPainted(false);
        downloadButton.setFocusPainted(false);
        downloadButton.setContentAreaFilled(false);

        JButton metaButton = new JButton(new AbstractAction() {
            {
                putValue(SHORT_DESCRIPTION, "Show metadata of selected layer");
                putValue(SMALL_ICON, IconBank.getIcon(JHVIcon.INFO));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                MetaDataDialog dialog = new MetaDataDialog(((ImageLayerOptions) getComponent().getParent()).getView());
                dialog.showDialog();
            }
        });
        metaButton.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        metaButton.setText(null);
        metaButton.setBorderPainted(false);
        metaButton.setFocusPainted(false);
        metaButton.setContentAreaFilled(false);

        diffPanel.setLayout(new BoxLayout(diffPanel, BoxLayout.PAGE_AXIS));

        JComboBox<String> comboBox = new JComboBox<>(combolist);
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        radPanel = new JPanel(new FlowLayout());
        radPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        radPanel.setVisible(false);

        //
        comboBox.setSelectedItem(0);
        diffRot = new JCheckBox("Rotation correction");
        diffRot.setSelected(true);

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.weighty = 1;

        c.gridx = 0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 0;
        c.gridx = 0;
        topPanel.add(comboBox, c);
        c.gridx = 1;
        c.weightx = 0;
        topPanel.add(metaButton, c);
        c.gridx = 2;
        topPanel.add(downloadButton, c);
        diffPanel.add(topPanel);
        c.gridy = 1;
        c.gridx = 0;
        c.gridwidth = 3;

        comboBox.addActionListener(e -> {
            if (comboBox.getSelectedItem().equals(combolist[0])) {
                setDifferenceModetoJP2View(false, false, false);
            } else if (comboBox.getSelectedItem().equals(combolist[1])) {
                setDifferenceModetoJP2View(true, true, false);
            } else if (comboBox.getSelectedItem().equals(combolist[2])) {
                setDifferenceModetoJP2View(true, true, true);
            }
            Displayer.display();
            topPanel.revalidate();
            topPanel.repaint();
        });
        diffRot.addItemListener(e -> {
            if (comboBox.getSelectedItem().equals(combolist[2])) {
                ((ImageLayerOptions) getComponent().getParent()).getGLImage().setBaseDifferenceNoRot(!diffRot.isSelected());
            } else {
                ((ImageLayerOptions) getComponent().getParent()).getGLImage().setRunDiffNoRot(!diffRot.isSelected());
            }
            Displayer.display();
        });

        radPanel.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 1;
        gc.gridx = 0;
        gc.gridwidth = 1;
        gc.anchor = GridBagConstraints.CENTER;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridy = 0;
        gc.gridx = 0;
        radPanel.add(diffRot, gc);

        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridwidth = 3;
        topPanel.add(radPanel, c);
    }

    private void setDifferenceModetoJP2View(boolean showExtraPanel, boolean differenceMode, boolean baseDifferenceMode) {
        if (showExtraPanel) {
            radPanel.setVisible(true);
            ((ImageLayerOptions) getComponent().getParent()).getGLImage().setRunDiffNoRot(!diffRot.isSelected());
        } else {
            radPanel.setVisible(false);
        }

        ((ImageLayerOptions) getComponent().getParent()).getGLImage().setDifferenceMode(differenceMode);
        ((ImageLayerOptions) getComponent().getParent()).getGLImage().setBaseDifferenceMode(baseDifferenceMode);
    }

    public Component getComponent() {
        return diffPanel;
    }

}
