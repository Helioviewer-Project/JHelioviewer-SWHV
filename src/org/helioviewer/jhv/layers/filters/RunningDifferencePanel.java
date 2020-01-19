package org.helioviewer.jhv.layers.filters;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;

import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.components.base.CircularProgressUI;
import org.helioviewer.jhv.gui.components.base.JHVButton;
import org.helioviewer.jhv.gui.components.base.JHVToggleButton;
import org.helioviewer.jhv.gui.dialogs.MetaDataDialog;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.opengl.GLImage;

public class RunningDifferencePanel implements FilterDetails {

    private final JHVToggleButton downloadButton = new JHVToggleButton(Buttons.download);
    private final JProgressBar progressBar = new JProgressBar();
    private final JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
    private final JPanel buttonPanel = new JPanel();

    public RunningDifferencePanel(ImageLayer layer) {
        ButtonGroup modeGroup = new ButtonGroup();
        for (GLImage.DifferenceMode mode : GLImage.DifferenceMode.values()) {
            JRadioButton item = new JRadioButton(mode.toString());
            if (mode == layer.getGLImage().getDifferenceMode())
                item.setSelected(true);
            item.addActionListener(e -> {
                layer.getGLImage().setDifferenceMode(mode);
                MovieDisplay.display();
            });
            modeGroup.add(item);
            modePanel.add(item);
        }

        MetaDataDialog metaDialog = new MetaDataDialog();
        JHVButton metaButton = new JHVButton(Buttons.info);
        metaButton.setToolTipText("Show metadata of selected layer");
        metaButton.addActionListener(e -> {
            metaDialog.setMetaData(layer);
            metaDialog.showDialog();
        });

        downloadButton.setToolTipText("Download selected layer");
        downloadButton.addActionListener(e -> {
            if (downloadButton.isSelected()) {
                Insets margin = downloadButton.getMargin();
                if (margin == null) // satisfy coverity
                    margin = new Insets(0, 0, 0, 0);
                Dimension size = downloadButton.getSize(null);
                progressBar.setPreferredSize(new Dimension(size.width - margin.left - margin.right, size.height - margin.top - margin.bottom));

                downloadButton.setText(null);
                downloadButton.add(progressBar);
                downloadButton.setToolTipText("Stop download");

                layer.startDownload();
            } else
                layer.stopDownload();
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
