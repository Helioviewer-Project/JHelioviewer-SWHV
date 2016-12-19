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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.gui.dialogs.MetaDataDialog;
import org.helioviewer.jhv.layers.ImageLayerOptions;
import org.helioviewer.jhv.io.DownloadViewTask;

@SuppressWarnings("serial")
public class RunningDifferencePanel implements ChangeListener {

    private static final String[] combolist = { "No difference images", "Running difference", "Base difference" };

    private final JPanel diffPanel = new JPanel();
    private final JSpinner truncateSpinner;
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

        JLabel truncateLabel = new JLabel("Contrast boost", JLabel.RIGHT);

        truncateSpinner = new JSpinner();
        truncateSpinner.setModel(new SpinnerNumberModel(Float.valueOf(0.8f), Float.valueOf(0), Float.valueOf(0.99f), Float.valueOf(0.01f)));
        truncateSpinner.addChangeListener(this);
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(truncateSpinner, "0%");
        truncateSpinner.setEditor(editor);
        editor.getTextField().setColumns(3);
        editor.getTextField().setHorizontalAlignment(JTextField.CENTER);
        WheelSupport.installMouseWheelSupport(truncateSpinner);

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
        gc.gridx = 1;
        radPanel.add(truncateLabel, gc);
        gc.gridx = 2;
        radPanel.add(truncateSpinner, gc);

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

    @Override
    public void stateChanged(ChangeEvent e) {
        float value = ((SpinnerNumberModel) truncateSpinner.getModel()).getNumber().floatValue();
        ((ImageLayerOptions) getComponent().getParent()).getGLImage().setTruncation(1 - value);
        Displayer.display();
    }

    public Component getComponent() {
        return diffPanel;
    }

}
