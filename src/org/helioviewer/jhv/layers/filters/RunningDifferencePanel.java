package org.helioviewer.jhv.layers.filters;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.components.base.CircularProgressUI;
import org.helioviewer.jhv.gui.dialogs.MetaDataDialog;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.opengl.GLImage;

import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideToggleButton;

@SuppressWarnings("serial")
public class RunningDifferencePanel implements FilterDetails {

    private final JideToggleButton downloadButton = new JideToggleButton(Buttons.download);
    private final JProgressBar progressBar = new JProgressBar();
    private final JPanel modePanel = new JPanel(new GridLayout(1, 3));
    private final JPanel buttonPanel = new JPanel();

    public RunningDifferencePanel(ImageLayer layer) {
        ButtonGroup modeGroup = new ButtonGroup();
        for (GLImage.DifferenceMode mode : GLImage.DifferenceMode.values()) {
            JRadioButton item = new JRadioButton(mode.toString());
            if (mode == layer.getGLImage().getDifferenceMode())
                item.setSelected(true);
            item.addActionListener(e -> {
                layer.getGLImage().setDifferenceMode(mode);
                Displayer.display();
            });
            modeGroup.add(item);
            modePanel.add(item);
        }

        JideButton metaButton = new JideButton(Buttons.info);
        metaButton.setToolTipText("Show metadata of selected layer");
        metaButton.addActionListener(e -> {
            MetaDataDialog dialog = new MetaDataDialog(layer);
            dialog.showDialog();
        });

        downloadButton.setToolTipText("Download selected layer");
        downloadButton.addActionListener(e -> {
            if (downloadButton.isSelected()) {
                Insets margin = downloadButton.getMargin();
                Dimension size = downloadButton.getSize(null);
                progressBar.setPreferredSize(new Dimension(size.width - margin.left - margin.right, size.height - margin.top - margin.bottom));

                downloadButton.setText(null);
                downloadButton.add(progressBar);
                downloadButton.setToolTipText("Stop download");

                layer.startDownloadView();
            } else
                layer.stopDownloadView();
        });

        progressBar.setUI(new CircularProgressUI());
        progressBar.setForeground(downloadButton.getForeground());

        buttonPanel.add(metaButton);
        buttonPanel.add(downloadButton);
    }

    @Override
    public Component getTitle() {
        return new JLabel("Difference", JLabel.RIGHT);
    }

    @Override
    public Component getComponent() {
        return modePanel;
    }

    @Override
    public Component getLabel() {
        return buttonPanel;
    }

    public void setValue(int value) {
        if (value < 0)
            progressBar.setIndeterminate(true);
        else
            progressBar.setValue(value);
    }

    public void done() {
        downloadButton.remove(progressBar);
        downloadButton.setToolTipText("Download selected layer");
        downloadButton.setText(Buttons.download);
        downloadButton.setSelected(false);
    }

    public void downloadVisible(boolean visible) {
        downloadButton.setVisible(visible);
    }

}
