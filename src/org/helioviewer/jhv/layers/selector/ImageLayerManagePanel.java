package org.helioviewer.jhv.layers.selector;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
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
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.view.View;

import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideToggleButton;

// Download, metadata, and PUNCH-refresh controls for the selected image layer.
// Shown in the "Manage" wrapper of the Layers section.
@SuppressWarnings("serial")
final class ImageLayerManagePanel extends JPanel {

    private final ImageLayer layer;
    private final JLabel readout = new JLabel();
    private long lastReadoutSig = Long.MIN_VALUE; // memoize: skip rebuild when nothing shown changed
    private final JideToggleButton downloadButton = new JideToggleButton(Buttons.download);
    private final JProgressBar progressBar = new JProgressBar();
    private DownloadProgress downloadProgress;

    ImageLayerManagePanel(ImageLayer layer) {
        this.layer = layer;
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        JPanel buttonRow = new JPanel(new BorderLayout());

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

        // Icons sit inline with the readout on one row, not on a line of their own.
        JPanel icons = new JPanel(new FlowLayout(FlowLayout.TRAILING, 2, 0));
        icons.add(downloadButton);
        // NOTE (upstream port): the per-layer PUNCH archive-refresh button is intentionally omitted
        // here — it depends on the PUNCH-refresh feature (PR #329), which is a separate open PR.
        // Re-add `icons.add(makeRefreshButton())` (guarded on PunchClient.hasRememberedQuery(layer))
        // in this spot once #329 merges.
        icons.add(metaButton);

        readout.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0)); // let the readout breathe
        buttonRow.add(readout, BorderLayout.CENTER);
        buttonRow.add(icons, BorderLayout.LINE_END);

        add(buttonRow);

        // Usually refreshed through ImageLayer activation; initialize here too in case that activation already happened before panel creation.
        refresh(layer);
        updateReadout();
    }


    void refresh(Layer layer) {
        ImageLayer imageLayer = (ImageLayer) layer;
        downloadButton.setVisible(!imageLayer.isLocal());
    }

    // Force a recompute even if the signature is unchanged — used when the layer's
    // view may have been swapped (layerUpdated) so a same-count/different-range layer refreshes.
    void forceReadoutRefresh() {
        lastReadoutSig = Long.MIN_VALUE;
        updateReadout();
    }

    void updateReadout() {
        View view = layer.getView();
        int max = view.getMaximumFrameNumber();
        int total = max + 1;
        boolean downloading = layer.isDownloading();
        int done = downloading ? view.getCompleteFrameCount() : total;

        // timeUpdated fires per displayed frame; rebuild only when something shown actually
        // changed. While downloading, `done` climbs so the signature advances each new frame;
        // during plain playback total/done are stable so we skip the O(n log n) median sort.
        long sig = ((long) total << 21) ^ ((long) done << 1) ^ (downloading ? 1 : 0);
        if (sig == lastReadoutSig)
            return;
        lastReadoutSig = sig;

        long start = view.getFirstTime().milli;
        long end = view.getLastTime().milli;
        String cadence = total > 1 ? formatSeconds(medianSpacingSec(view, max)) : "—";
        String frames = downloading
                ? (max == 0 ? "0/0 frames" : done + "/" + total + " frames") // scope not yet known
                : total + (total == 1 ? " frame" : " frames");
        String duration = TimeUtils.formatDurationSig(end - start);
        readout.setText(String.format("<html>%s – %s<br>cadence %s · %s · %s total</html>",
                TimeUtils.format(start), TimeUtils.format(end), cadence, frames, duration));
    }

    private static long medianSpacingSec(View view, int max) {
        long[] gaps = new long[max];
        long prev = view.getFrameTime(0).milli;
        for (int i = 1; i <= max; i++) {
            long t = view.getFrameTime(i).milli;
            gaps[i - 1] = (t - prev) / 1000;
            prev = t;
        }
        Arrays.sort(gaps);
        return gaps[gaps.length / 2];
    }

    private static String formatSeconds(long sec) {
        if (sec >= 86400) return (sec / 86400) + " d";
        if (sec >= 3600) return (sec / 3600) + " h";
        if (sec >= 60) return (sec / 60) + " min";
        return sec + " s";
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
