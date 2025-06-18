package org.helioviewer.jhv.layers;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.components.base.CircularProgressUI;
import org.helioviewer.jhv.gui.dialogs.MetaDataDialog;
import org.helioviewer.jhv.layers.filters.*;

import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideToggleButton;

@SuppressWarnings("serial")
class ImageLayerOptions extends JPanel {

    private final LUTPanel lutPanel;
    private final SlitPanel slitPanel;
    // private final SectorPanel sectorPanel;
    private final InnerMaskPanel innerMaskPanel;
    private final DeltaCROTAPanel deltaCROTAPanel;
    private final DeltaCRVAL1Panel deltaCRVAL1Panel;
    private final DeltaCRVAL2Panel deltaCRVAL2Panel;

    private final JideToggleButton downloadButton = new JideToggleButton(Buttons.download);
    private final JProgressBar progressBar = new JProgressBar();

    ImageLayerOptions(ImageLayer layer) {
        DifferencePanel differencePanel = new DifferencePanel(layer);
        FilterDetails opacityPanel = new OpacityPanel(layer);
        FilterDetails blendPanel = new BlendPanel(layer);
        FilterDetails channelMixerPanel = new ChannelMixerPanel(layer);
        lutPanel = new LUTPanel(layer);
        FilterDetails levelsPanel = new LevelsPanel(layer);
        FilterDetails sharpenPanel = new SharpenPanel(layer);
        FilterDetails imageFilterPanel = new ImageFilterPanel(layer);

        slitPanel = new SlitPanel(layer);
        innerMaskPanel = new InnerMaskPanel(layer);
        // sectorPanel = new SectorPanel(layer);
        deltaCROTAPanel = new DeltaCROTAPanel(layer);
        deltaCRVAL1Panel = new DeltaCRVAL1Panel(layer);
        deltaCRVAL2Panel = new DeltaCRVAL2Panel(layer);

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

                layer.startDownload();
            } else
                layer.stopDownload();
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

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(downloadButton, BorderLayout.LINE_START);
        buttonPanel.add(metaButton, BorderLayout.LINE_END);

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
        c.weightx = 0;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(details.getSecond(), c);

        c.gridx = 2;
        c.weightx = 1;
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

    void setLUT(LUT lut) {
        lutPanel.setLUT(lut);
    }

    void downloadProgress(int value) {
        if (value < 0)
            progressBar.setIndeterminate(true);
        else
            progressBar.setValue(value);
    }

    void downloadDone() {
        downloadButton.remove(progressBar);
        downloadButton.setToolTipText("Download selected layer");
        downloadButton.setText(Buttons.download);
        downloadButton.setSelected(false);
    }

    void downloadVisible(boolean visible) {
        downloadButton.setVisible(visible);
    }

}
