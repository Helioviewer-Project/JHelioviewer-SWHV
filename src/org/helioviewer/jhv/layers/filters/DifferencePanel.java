package org.helioviewer.jhv.layers.filters;

import java.awt.BorderLayout;
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
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.opengl.GLImage;

import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideToggleButton;

public class DifferencePanel implements FilterDetails {

    private final JideToggleButton downloadButton = new JideToggleButton(Buttons.download);
    private final JProgressBar progressBar = new JProgressBar();
    private final JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
    private final JPanel buttonPanel = new JPanel(new BorderLayout());
    private final JLabel title = new JLabel(" Difference ", JLabel.RIGHT);

    public DifferencePanel(ImageLayer layer) {
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

        JideButton syncButton = new JideButton(Buttons.sync);
        syncButton.setToolTipText("Synchronize time intervals of other layers");
        syncButton.addActionListener(e -> MoviePanel.getInstance().syncLayersSpan(layer.getStartTime(), layer.getEndTime()));

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

        buttonPanel.add(syncButton, BorderLayout.LINE_START);
        buttonPanel.add(downloadButton, BorderLayout.LINE_END);
    }

    @Override
    public Component getFirst() {
        return title;
    }

    @Override
    public Component getSecond() {
        return modePanel;
    }

    @Override
    public Component getThird() {
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
