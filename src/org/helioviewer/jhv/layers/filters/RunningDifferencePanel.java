package org.helioviewer.jhv.layers.filters;

import java.awt.Component;
import java.util.Objects;

import javax.swing.JComboBox;
import javax.swing.JLabel;
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
public class RunningDifferencePanel implements FilterDetails {

    private enum DifferenceModeChoice {
        None("None", GLImage.DifferenceMode.None),
        Running("Running", GLImage.DifferenceMode.Running),
        Base("Base", GLImage.DifferenceMode.Base);

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

    private final JComboBox<DifferenceModeChoice> comboBox;
    private final JPanel buttonPanel;

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

        comboBox = new JComboBox<>(DifferenceModeChoice.values());
        comboBox.setSelectedItem(DifferenceModeChoice.valueOf(parent.getGLImage().getDifferenceMode().toString()));
        comboBox.addActionListener(e -> {
            parent.getGLImage().setDifferenceMode(((DifferenceModeChoice) Objects.requireNonNull(comboBox.getSelectedItem())).mode);
            Displayer.display();
        });

        buttonPanel = new JPanel();
        buttonPanel.add(metaButton);
        buttonPanel.add(downloadButton);
    }

    @Override
    public Component getTitle() {
        return new JLabel("Difference", JLabel.RIGHT);
    }

    @Override
    public Component getComponent() {
        return comboBox;
    }

    @Override
    public Component getLabel() {
        return buttonPanel;
    }

}
