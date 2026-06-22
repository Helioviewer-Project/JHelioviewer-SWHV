package org.helioviewer.jhv.layers.selector;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.helioviewer.jhv.app.Message;
import org.helioviewer.jhv.gui.CompletionNotifications;
import org.helioviewer.jhv.gui.component.Buttons;
import org.helioviewer.jhv.gui.component.CircularProgressUI;
import org.helioviewer.jhv.gui.dialog.MetaDataDialog;
import org.helioviewer.jhv.io.DownloadLayer;
import org.helioviewer.jhv.io.PunchClient;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.filters.ChannelMixerPanel;
import org.helioviewer.jhv.layers.filters.DifferencePanel;
import org.helioviewer.jhv.layers.filters.FilterDetails;
import org.helioviewer.jhv.layers.filters.ImageFilterPanel;
import org.helioviewer.jhv.layers.filters.InnerMaskPanel;
import org.helioviewer.jhv.layers.filters.LUTPanel;
import org.helioviewer.jhv.layers.filters.LevelsPanel;
import org.helioviewer.jhv.layers.filters.SliderFilterPanel;
import org.helioviewer.jhv.layers.filters.SlitPanel;

import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideToggleButton;

@SuppressWarnings("serial")
final class ImageLayerOptions extends JPanel {

    private final LUTPanel lutPanel;
    private final SlitPanel slitPanel;
    // private final SectorPanel sectorPanel;
    private final InnerMaskPanel innerMaskPanel;
    private final SliderFilterPanel.DeltaCROTA deltaCROTAPanel;
    private final SliderFilterPanel.DeltaCRVAL1 deltaCRVAL1Panel;
    private final SliderFilterPanel.DeltaCRVAL2 deltaCRVAL2Panel;

    private final JideToggleButton downloadButton = new JideToggleButton(Buttons.download);
    private final JProgressBar progressBar = new JProgressBar();
    private DownloadProgress downloadProgress;

    ImageLayerOptions(ImageLayer layer) {
        DifferencePanel differencePanel = new DifferencePanel(layer);
        FilterDetails opacityPanel = new SliderFilterPanel.Opacity(layer);
        FilterDetails blendPanel = new SliderFilterPanel.Blend(layer);
        FilterDetails channelMixerPanel = new ChannelMixerPanel(layer);
        lutPanel = new LUTPanel(layer);
        FilterDetails levelsPanel = new LevelsPanel(layer);
        FilterDetails sharpenPanel = new SliderFilterPanel.Sharpen(layer);
        FilterDetails imageFilterPanel = new ImageFilterPanel(layer);

        slitPanel = new SlitPanel(layer);
        innerMaskPanel = new InnerMaskPanel(layer);
        // sectorPanel = new SectorPanel(layer);
        deltaCROTAPanel = new SliderFilterPanel.DeltaCROTA(layer);
        deltaCRVAL1Panel = new SliderFilterPanel.DeltaCRVAL1(layer);
        deltaCRVAL2Panel = new SliderFilterPanel.DeltaCRVAL2(layer);

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;

        c.gridy = 0;
        addToGridBag(c, differencePanel);
        c.gridy++;
        addToGridBag(c, opacityPanel);
        c.gridy++;
        addToGridBag(c, blendPanel);
        c.gridy++;
        addToGridBag(c, sharpenPanel);
        c.gridy++;
        addToGridBag(c, levelsPanel);
        c.gridy++;
        addToGridBag(c, lutPanel);
        c.gridy++;
        addToGridBag(c, channelMixerPanel);
        c.gridy++;
        addToGridBag(c, imageFilterPanel);
        c.gridy++;

        JideToggleButton adjButton = new JideToggleButton(Buttons.adjustmentsRight);
        adjButton.addActionListener(e -> {
            boolean selected = adjButton.isSelected();
            setAdjustmentsVisibility(selected);
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

        MetaDataDialog metaDialog = new MetaDataDialog();
        JideButton metaButton = new JideButton(Buttons.info);
        metaButton.setToolTipText("Show metadata of selected layer");
        metaButton.addActionListener(e -> {
            metaDialog.setMetaData(layer);
            metaDialog.showDialog();
        });

        // Only PUNCH layers carry a remembered query; the button stays hidden otherwise
        JideButton refreshButton = new JideButton(Buttons.refresh);
        refreshButton.setToolTipText("Check the PUNCH archive for new frames in this layer's time range");
        refreshButton.setVisible(PunchClient.hasRememberedQuery(layer));
        JProgressBar refreshSpinner = new JProgressBar();
        refreshSpinner.setUI(new CircularProgressUI());
        refreshSpinner.setIndeterminate(true);
        refreshSpinner.setVisible(false);
        refreshSpinner.setPreferredSize(new Dimension(20, 20));
        refreshButton.addActionListener(e -> {
            refreshButton.setEnabled(false);
            refreshButton.setText(null);
            refreshButton.add(refreshSpinner);
            refreshSpinner.setVisible(true);
            PunchClient.submitRefresh(layer, result -> {
                refreshSpinner.setVisible(false);
                refreshButton.remove(refreshSpinner);
                refreshButton.setText(Buttons.refresh);
                refreshButton.setEnabled(true);
                Message.warn("PUNCH refresh", result.newCount() == 0
                        ? "No new frames in the archive for this layer."
                        : String.format("Loaded %d new frame%s as a new layer.", result.newCount(), result.newCount() == 1 ? "" : "s"));
            });
        });

        JPanel rightCluster = new JPanel(new BorderLayout());
        rightCluster.add(refreshButton, BorderLayout.LINE_START);
        rightCluster.add(metaButton, BorderLayout.LINE_END);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(downloadButton, BorderLayout.LINE_START);
        buttonPanel.add(rightCluster, BorderLayout.LINE_END);

        c.gridx = 2;
        c.anchor = GridBagConstraints.LINE_END;
        add(buttonPanel, c);

        setAdjustmentsVisibility(false);
        c.gridy++;
        addToGridBag(c, slitPanel);
        c.gridy++;
        addToGridBag(c, innerMaskPanel);
        // c.gridy++;
        // addToGridBag(c, sectorPanel);
        c.gridy++;
        addToGridBag(c, deltaCROTAPanel);
        c.gridy++;
        addToGridBag(c, deltaCRVAL1Panel);
        c.gridy++;
        addToGridBag(c, deltaCRVAL2Panel);
        // Usually refreshed through ImageLayer activation; initialize here too in case that activation already happened before panel creation.
        refresh(layer);
    }

    private void addToGridBag(GridBagConstraints c, FilterDetails details) {
        c.gridwidth = 1;

        c.gridx = 0;
        c.weightx = 0;
        c.weighty = 1;
        c.anchor = GridBagConstraints.LINE_END;
        c.fill = GridBagConstraints.NONE;
        add(details.getFirst(), c);

        c.gridx = 1;
        c.weightx = 1;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(details.getSecond(), c);

        c.gridx = 2;
        c.weightx = 0;
        c.anchor = GridBagConstraints.LINE_END;
        c.fill = GridBagConstraints.NONE;
        add(details.getThird(), c);
    }

    private void setAdjustmentsVisibility(boolean visibility) {
        slitPanel.setVisible(visibility);
        innerMaskPanel.setVisible(visibility);
        // sectorPanel.setVisible(visibility);
        deltaCROTAPanel.setVisible(visibility);
        deltaCRVAL1Panel.setVisible(visibility);
        deltaCRVAL2Panel.setVisible(visibility);
    }

    public void refresh(Layer layer) {
        ImageLayer imageLayer = (ImageLayer) layer;
        downloadButton.setVisible(!imageLayer.isLocal());
        lutPanel.setLUT(imageLayer.getView().getDefaultLUT());
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
