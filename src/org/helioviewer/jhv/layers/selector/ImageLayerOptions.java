package org.helioviewer.jhv.layers.selector;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.helioviewer.jhv.gui.CompletionNotifications;
import org.helioviewer.jhv.gui.component.Buttons;
import org.helioviewer.jhv.gui.component.CircularProgressUI;
import org.helioviewer.jhv.gui.dialog.MetaDataDialog;
import org.helioviewer.jhv.io.DownloadLayer;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.filters.ChannelMixerPanel;
import org.helioviewer.jhv.layers.filters.DifferencePanel;
import org.helioviewer.jhv.layers.filters.FilterDetails;
import org.helioviewer.jhv.layers.filters.ImageFilterPanel;
import org.helioviewer.jhv.layers.filters.LUTPanel;
import org.helioviewer.jhv.layers.filters.RangeSliderFilterPanel;
import org.helioviewer.jhv.layers.filters.SliderFilterPanel;

import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideToggleButton;

@SuppressWarnings("serial")
final class ImageLayerOptions extends JPanel {

    private final FITSSettings fitsSettings;
    private final JideToggleButton fitsButton = new JideToggleButton(Buttons.fitsRight);
    private final LUTPanel lutPanel;

    private final JideToggleButton downloadButton = new JideToggleButton(Buttons.download);
    private final JProgressBar progressBar = new JProgressBar();
    private DownloadProgress downloadProgress;

    ImageLayerOptions(ImageLayer layer) {
        super(new GridBagLayout());
        fitsSettings = new FITSSettings(layer.getProcessingSettings());
        FilterDetails differencePanel = DifferencePanel.create(layer);
        FilterDetails opacityPanel = SliderFilterPanel.opacity(layer);
        FilterDetails blendPanel = SliderFilterPanel.blend(layer);
        FilterDetails channelMixerPanel = ChannelMixerPanel.create(layer);
        lutPanel = new LUTPanel(layer);
        FilterDetails levelsPanel = RangeSliderFilterPanel.levels(layer);
        FilterDetails sharpenPanel = SliderFilterPanel.sharpen(layer);
        FilterDetails imageFilterPanel = ImageFilterPanel.create(layer);

        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        addFilterRow(this, c, differencePanel);
        c.gridy++;
        addFilterRow(this, c, opacityPanel);
        c.gridy++;
        addFilterRow(this, c, blendPanel);
        c.gridy++;
        addFilterRow(this, c, sharpenPanel);
        c.gridy++;
        addFilterRow(this, c, levelsPanel);
        c.gridy++;
        addFilterRow(this, c, lutPanel);
        c.gridy++;
        addFilterRow(this, c, channelMixerPanel);
        c.gridy++;
        addFilterRow(this, c, imageFilterPanel);
        c.gridy++;

        JPanel adjustmentsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints adjustmentConstraints = new GridBagConstraints();
        adjustmentConstraints.gridy = 0;
        addFilterRow(adjustmentsPanel, adjustmentConstraints, RangeSliderFilterPanel.slit(layer));
        adjustmentConstraints.gridy++;
        addFilterRow(adjustmentsPanel, adjustmentConstraints, RangeSliderFilterPanel.mask(layer));
        adjustmentConstraints.gridy++;
        addFilterRow(adjustmentsPanel, adjustmentConstraints, SliderFilterPanel.sector(layer));
        adjustmentConstraints.gridy++;
        addFilterRow(adjustmentsPanel, adjustmentConstraints, SliderFilterPanel.sectorWidth(layer));
        adjustmentConstraints.gridy++;
        addFilterRow(adjustmentsPanel, adjustmentConstraints, SliderFilterPanel.deltaCROTA(layer));
        adjustmentConstraints.gridy++;
        addFilterRow(adjustmentsPanel, adjustmentConstraints, SliderFilterPanel.deltaCRVAL1(layer));
        adjustmentConstraints.gridy++;
        addFilterRow(adjustmentsPanel, adjustmentConstraints, SliderFilterPanel.deltaCRVAL2(layer));
        adjustmentsPanel.setVisible(false);

        JideToggleButton adjButton = new JideToggleButton(Buttons.adjustmentsRight);
        adjButton.addActionListener(e -> {
            boolean selected = adjButton.isSelected();
            adjustmentsPanel.setVisible(selected);
            adjButton.setText(selected ? Buttons.adjustmentsDown : Buttons.adjustmentsRight);
        });
        c.gridx = 1;
        c.weightx = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        add(adjButton, c);

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

                downloadProgress = new DownloadProgress();
                layer.startDownload(downloadProgress);
            } else {
                layer.cancelDownloadTask();
                if (downloadProgress != null)
                    downloadProgress.done();
            }
        });

        progressBar.setUI(new CircularProgressUI());
        progressBar.setForeground(downloadButton.getForeground());

        JideButton metaButton = new JideButton(Buttons.info);
        metaButton.setToolTipText("Show metadata of selected layer");
        metaButton.addActionListener(e -> {
            MetaDataDialog metaDialog = MetaDataDialog.getInstance();
            metaDialog.setMetaData(layer);
            metaDialog.showDialog();
        });

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(downloadButton, BorderLayout.LINE_START);
        buttonPanel.add(metaButton, BorderLayout.LINE_END);

        c.gridx = 2;
        c.anchor = GridBagConstraints.LINE_END;
        add(buttonPanel, c);

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 3;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(adjustmentsPanel, c);
        fitsButton.addActionListener(e -> {
            boolean expanded = fitsButton.isSelected();
            fitsButton.setText(expanded ? Buttons.fitsDown : Buttons.fitsRight);
            fitsSettings.setVisible(expanded);
        });
        c.gridy++;
        c.gridx = 1;
        c.gridwidth = 2;
        c.weightx = 1;
        c.weighty = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        add(fitsButton, c);
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(fitsSettings, c);
        alignColumns(this, adjustmentsPanel, fitsSettings);
        // Usually refreshed through ImageLayer activation; initialize here too in case that activation already happened before panel creation.
        refresh(layer);
    }

    private static void alignColumns(JPanel... panels) {
        int[] widths = new int[3];
        for (JPanel panel : panels) {
            GridBagLayout layout = (GridBagLayout) panel.getLayout();
            for (Component component : panel.getComponents()) {
                GridBagConstraints c = layout.getConstraints(component);
                if (c.gridwidth == 1 && (c.gridx == 0 || c.gridx == 2))
                    widths[c.gridx] = Math.max(widths[c.gridx], component.getPreferredSize().width);
            }
        }
        // Include collapsed rows, so only the center column takes remaining space.
        for (JPanel panel : panels)
            ((GridBagLayout) panel.getLayout()).columnWidths = widths;
    }

    private static void addFilterRow(JPanel panel, GridBagConstraints c, FilterDetails details) {
        c.gridwidth = 1;

        c.gridx = 0;
        c.weightx = 0;
        c.weighty = 1;
        c.anchor = GridBagConstraints.LINE_END;
        c.fill = GridBagConstraints.NONE;
        panel.add(details.getFirst(), c);

        c.gridx = 1;
        c.weightx = 1;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(details.getSecond(), c);

        c.gridx = 2;
        c.weightx = 0;
        c.anchor = GridBagConstraints.LINE_END;
        c.fill = GridBagConstraints.NONE;
        panel.add(details.getThird(), c);
    }

    void refresh(ImageLayer imageLayer) {
        downloadButton.setVisible(!imageLayer.isLocal());
        boolean hasFITS = imageLayer.frames().hasFITS();
        fitsButton.setVisible(hasFITS);
        fitsSettings.setVisible(hasFITS && fitsButton.isSelected());
        lutPanel.refresh();
    }

    private void downloadProgress(int value) {
        if (value < 0) {
            progressBar.setIndeterminate(true);
        } else {
            progressBar.setIndeterminate(false);
            progressBar.setValue(value);
        }
    }

    private void downloadDone() {
        downloadButton.remove(progressBar);
        progressBar.setIndeterminate(false);
        progressBar.setValue(0);
        downloadButton.setToolTipText("Download selected layer");
        downloadButton.setText(Buttons.download);
        downloadButton.setSelected(false);
    }

    private final class DownloadProgress implements DownloadLayer.Progress {
        @Override
        public void progress(int percent) {
            if (downloadProgress == this)
                downloadProgress(percent);
        }

        @Override
        public void success(String result) {
            CompletionNotifications.fileReady(result);
        }

        @Override
        public void done() {
            if (downloadProgress != this)
                return;
            downloadProgress = null;
            downloadDone();
        }
    }

}
