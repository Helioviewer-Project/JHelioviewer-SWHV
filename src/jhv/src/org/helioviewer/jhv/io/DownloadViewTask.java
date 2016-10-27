package org.helioviewer.jhv.io;

import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

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
        cancelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel(true);
            }
        });
        dialog.add(cancelBtn);

        dialog.pack();
        dialog.setLocationRelativeTo(ImageViewerGui.getMainFrame());
        dialog.setVisible(true);

        setThreadName("MAIN--DownloadView");
    }

    @Override
    protected Void backgroundWork() {
        File dstFile = new File(JHVDirectory.REMOTEFILES.getPath() + uri.getPath().substring(Math.max(0, uri.getPath().lastIndexOf('/')))).getAbsoluteFile();

        URI srcURI;
        if (downloadURI.getScheme().equalsIgnoreCase("jpip")) {
            // String httpPath = Settings.getSingletonInstance().getProperty("default.httpRemote.path"); - not good if server changed
            try {
                srcURI = new URI(downloadURI.toString().replaceFirst("jpip://", "http://").replaceFirst(":" + downloadURI.getPort(), "/jp2"));
                // srcURI = new URI(httpPath + downloadURI.getPath().substring(Math.max(0, downloadURI.getPath().lastIndexOf('/'))));
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return null;
            }
        } else
            srcURI = downloadURI;

        if (srcURI.equals(dstFile.toURI())) // avoid self-destruction
            return null;

        boolean failed = false;
        FileOutputStream out = null;
        try {
            DownloadStream ds = new DownloadStream(srcURI.toURL());
            InputStream in = ds.getInput();
            out = new FileOutputStream(dstFile);

            final int contentLength = ds.getContentLength();
            if (contentLength > 0) {
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setIndeterminate(false);
                        progressBar.setMaximum(contentLength);
                    }
                });
            }

            byte[] buffer = new byte[BUFSIZ];
            int numTotalRead = 0, numCurrentRead;

            while (!Thread.interrupted() && (numCurrentRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numCurrentRead);
                numTotalRead += numCurrentRead;

                if (contentLength > 0) {
                    final int finalTotalRead = numTotalRead;
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setValue(finalTotalRead);
                        }
                    });
                }
            }

            if (!isCancelled()) { // reload JPX from disk
                LoadURITask uriTask = new LoadURITask(layer, dstFile.toURI());
                JHVGlobals.getExecutorService().execute(uriTask);
            }
        } catch (Exception e) {
            failed = true;
            e.printStackTrace();
        } finally {
            try {
                if (out != null)
                    out.close();
                if (failed || isCancelled())
                    dstFile.delete();
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
