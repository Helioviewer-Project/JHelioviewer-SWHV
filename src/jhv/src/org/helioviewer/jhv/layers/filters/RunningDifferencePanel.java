package org.helioviewer.jhv.layers.filters;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.dialogs.MetaDataDialog;
import org.helioviewer.jhv.layers.ImageLayerOptions;
import org.helioviewer.jhv.io.DownloadViewTask;
import org.helioviewer.jhv.opengl.GLImage.DifferenceMode;

@SuppressWarnings("serial")
public class RunningDifferencePanel {

    private enum DifferenceModeChoice {
        None("No difference images", DifferenceMode.None),
        Running("Running difference", DifferenceMode.RunningRotation),
        Base("Base difference", DifferenceMode.BaseRotation);

        private final String str;
        private final DifferenceMode mode;

        DifferenceModeChoice(String s, DifferenceMode m) {
            str = s;
            mode = m;
        }

        @Override
        public String toString() {
            return str;
        }

        public DifferenceMode getDiffMode() {
            return mode;
        }
    }

    private final JPanel topPanel = new JPanel(new GridBagLayout());

    public RunningDifferencePanel() {
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
        metaButton.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        metaButton.setText(null);
        metaButton.setBorderPainted(false);
        metaButton.setFocusPainted(false);
        metaButton.setContentAreaFilled(false);

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
        downloadButton.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 2));
        downloadButton.setText(null);
        downloadButton.setBorderPainted(false);
        downloadButton.setFocusPainted(false);
        downloadButton.setContentAreaFilled(false);

        JComboBox<DifferenceModeChoice> comboBox = new JComboBox<>(DifferenceModeChoice.values());
        comboBox.addActionListener(e -> {
            ((ImageLayerOptions) getComponent().getParent()).getGLImage().setDifferenceMode(((DifferenceModeChoice) comboBox.getSelectedItem()).getDiffMode());
            Displayer.display();
        });

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.weighty = 1;
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
    }

    public Component getComponent() {
        return topPanel;
    }

}
