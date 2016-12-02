package org.helioviewer.jhv.io;

import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.viewmodel.view.View;

public class DownloadViewTask extends JHVWorker<Void, Void> {

    private static final int BUFSIZ = 65536;

    private final JWindow dialog;
    private final JProgressBar progressBar;

    private final URI uri;
    private final URI downloadURI;
    private final ImageLayer layer;

    public DownloadViewTask(View view) {
        uri = view.getURI();
        downloadURI = view.getDownloadURI();
        layer = view.getImageLayer();

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
        URI srcURI;
        if (downloadURI.getScheme().equalsIgnoreCase("jpip")) {
            try {
                srcURI = new URI(downloadURI.toString().replaceFirst("jpip://", "http://").replaceFirst(":" + downloadURI.getPort(), "/jp2"));
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return null;
            }
        } else
            srcURI = downloadURI;

        File dstFile = new File(JHVDirectory.REMOTEFILES.getPath() + uri.getPath().substring(Math.max(0, uri.getPath().lastIndexOf('/')))).getAbsoluteFile();
        if (srcURI.equals(dstFile.toURI())) // avoid self-destruction
            return null;

        URL srcURL;
        try {
            srcURL = srcURI.toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }

        boolean failed = false;
        DownloadStream ds = new DownloadStream(srcURL);
        try (InputStream in = new BufferedInputStream(ds.getInput(), BUFSIZ)) {
            int contentLength = ds.getContentLength();
            if (contentLength > 0) {
                EventQueue.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setMaximum(contentLength);
                });
            }

            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(dstFile), BUFSIZ)) {
                byte[] buffer = new byte[BUFSIZ];
                int numTotalRead = 0, numCurrentRead;
                while (!Thread.interrupted() && (numCurrentRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, numCurrentRead);
                    numTotalRead += numCurrentRead;

                    if (contentLength > 0) {
                        int finalTotalRead = numTotalRead;
                        EventQueue.invokeLater(() -> progressBar.setValue(finalTotalRead));
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
                else { // reload JPX from disk
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
