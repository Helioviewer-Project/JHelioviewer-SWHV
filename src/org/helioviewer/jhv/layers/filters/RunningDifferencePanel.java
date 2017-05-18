package org.helioviewer.jhv.layers.filters;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.dialogs.MetaDataDialog;
import org.helioviewer.jhv.io.DownloadViewTask;
import org.helioviewer.jhv.layers.ImageLayerOptions;
import org.helioviewer.jhv.opengl.GLImage;

import com.jidesoft.swing.JideButton;

@SuppressWarnings("serial")
public class RunningDifferencePanel {

    private enum DifferenceModeChoice {
        None("No difference images", GLImage.DifferenceMode.None),
        Running("Running difference", GLImage.DifferenceMode.Running),
        Base("Base difference", GLImage.DifferenceMode.Base);

        private final String str;
        final GLImage.DifferenceMode mode;

        DifferenceModeChoice(String s, GLImage.DifferenceMode m) {
            str = s;
            mode = m;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    private final JPanel topPanel = new JPanel(new GridBagLayout());

    public RunningDifferencePanel(ImageLayerOptions parent) {
        JideButton metaButton = new JideButton(Buttons.info);
        metaButton.setToolTipText("Show metadata of selected layer");
        metaButton.addActionListener(e -> {
            MetaDataDialog dialog = new MetaDataDialog(parent.getView());
            dialog.showDialog();
        });

        JideButton downloadButton = new JideButton(Buttons.download);
        downloadButton.setToolTipText("Download selected layer");
        downloadButton.addActionListener(e -> {
            DownloadViewTask downloadTask = new DownloadViewTask(parent.getView());
            JHVGlobals.getExecutorService().execute(downloadTask);
        });

        JComboBox<DifferenceModeChoice> comboBox = new JComboBox<>(DifferenceModeChoice.values());
        comboBox.setSelectedItem(DifferenceModeChoice.valueOf(parent.getGLImage().getDifferenceMode().toString()));
        comboBox.addActionListener(e -> {
            parent.getGLImage().setDifferenceMode(((DifferenceModeChoice) comboBox.getSelectedItem()).mode);
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
