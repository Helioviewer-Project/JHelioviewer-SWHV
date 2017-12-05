package org.helioviewer.jhv.io;

import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.io.File;
import java.net.URI;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.view.View;

import okio.Okio;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;

public class DownloadViewTask extends JHVWorker<Void, Void> {

    private static final int BUFSIZ = 1024 * 1024;

    private final JWindow dialog;
    private final JProgressBar progressBar;

    private final URI uri;
    private final URI downloadURI;
    private final ImageLayer layer;

    public DownloadViewTask(ImageLayer _layer, View view) {
        layer = _layer;
        uri = view.getURI();

        APIRequest req = view.getAPIRequest();
        downloadURI = req == null ? uri : req.fileRequest;

        dialog = new JWindow(ImageViewerGui.getMainFrame());
        dialog.setLayout(new FlowLayout());
        dialog.add(new JLabel("Downloading " + view.getName()));

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        dialog.add(progressBar);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setBorder(BorderFactory.createEtchedBorder());
        cancelBtn.addActionListener(e -> cancel(true));
        dialog.add(cancelBtn);

        dialog.pack();
        dialog.setLocationRelativeTo(ImageViewerGui.getMainFrame());
        dialog.setVisible(true);

        setThreadName("MAIN--DownloadView");
    }

    @Override
    protected Void backgroundWork() {
        File dstFile = new File(JHVDirectory.REMOTEFILES.getPath() + uri.getPath().substring(Math.max(0, uri.getPath().lastIndexOf('/')))).getAbsoluteFile();
        if (downloadURI.equals(dstFile.toURI())) // avoid self-destruction
            return null;

        boolean failed = false;
        try (NetClient nc = NetClient.of(downloadURI.toString())) {
            int contentLength = (int) nc.getContentLength();
            if (contentLength > 0)
                EventQueue.invokeLater(() -> progressBar.setIndeterminate(false));

            BufferedSource source = nc.getSource();
            try (BufferedSink sink = Okio.buffer(Okio.sink(dstFile))) {
                long bytesRead, totalRead = 0;
                Buffer sinkBuffer = sink.buffer();
                while ((bytesRead = source.read(sinkBuffer, BUFSIZ)) != -1) {
                    totalRead += bytesRead;
                    if (contentLength > 0) {
                        int percent = (int) ((totalRead * 100.) / contentLength);
                        EventQueue.invokeLater(() -> progressBar.setValue(percent));
                    }
                }
            }
        } catch (Exception e) {
            failed = true;
            e.printStackTrace();
        } finally {
            try {
                if (failed || isCancelled())
                    dstFile.delete();
                else { // reload from disk
                    LoadURITask uriTask = new LoadURITask(layer, dstFile.toURI());
                    JHVGlobals.getExecutorService().execute(uriTask);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void done() {
        dialog.dispose();
    }

}
