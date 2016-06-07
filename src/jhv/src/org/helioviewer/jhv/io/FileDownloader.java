package org.helioviewer.jhv.io;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.gui.ImageViewerGui;

/**
 * Class for downloading files from the internet.
 * 
 * This class provides the capability to download files from the internet and
 * give a feedback via a progress bar.
 * 
 * @author Stephan Pagel
 * @author Markus Langenberg
 */
public class FileDownloader {

    private Thread downloadThread;
    private JProgressBar progressBar;

    /**
     * Gets the file from the source and writes it to the destination file. The
     * methods provides an own dialog, which displays the current download
     * progress.
     * 
     * @param source
     *            specifies the location of the file which has to be downloaded.
     * @param title
     *            title which should be displayed in the header of the progress
     *            dialog.
     * @return True, if download was successful, false otherwise.
     * @throws IOException
     */
    boolean get(URI source, String title) throws IOException {
        // create own dialog where to display the progress
        progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
        // progressBar.setPreferredSize(new Dimension(200, 20));

        StandAloneDialog dialog = new StandAloneDialog(title);
        dialog.setVisible(true);

        // download the file
        File dest = new File(JHVDirectory.REMOTEFILES.getPath() + source.getPath().substring(Math.max(0, source.getPath().lastIndexOf('/'))));
        boolean result = downloadFile(source, dest);
        dialog.dispose();

        return result;
    }

    /**
     * Gets the file from the source and writes it to the _dest file.
     * 
     * @param source
     *            specifies the location of the file which has to be downloaded.
     * @param dest
     *            location where data of the file has to be stored.
     * @return True, if download was successful, false otherwise.
     * @throws IOException
     * @throws URISyntaxException
     * */
    private boolean downloadFile(URI source, File dest) throws IOException {
        if (source == null || dest == null)
            return false;

        final URI finalSource;
        if (source.getScheme().equalsIgnoreCase("jpip")) {
            try {
                finalSource = new URI(source.toString().replaceFirst("jpip://", "http://").replaceFirst(":" + source.getPort(), "/jp2"));
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            finalSource = source;
        }

        if (!(finalSource.getScheme().equalsIgnoreCase("http") || finalSource.getScheme().equalsIgnoreCase("ftp"))) {
            return false;
        }

        final File finalDest;
        if (dest.isDirectory()) {
            finalDest = new File(dest, finalSource.getPath().substring(Math.max(0, finalSource.getPath().lastIndexOf('/'))));
        } else {
            finalDest = dest;
        }
        finalDest.createNewFile();

        int contentLength = -100;

        downloadThread = new Thread(new Runnable() {
            public void run() {
                FileOutputStream out = null;
                try {
                    DownloadStream ds = new DownloadStream(finalSource.toURL());
                    InputStream in = ds.getInput();
                    out = new FileOutputStream(finalDest);

                    int contentLength = ds.getContentLength();
                    if (contentLength < 0)
                        progressBar.setIndeterminate(true);
                    else
                        progressBar.setMaximum(contentLength);

                    byte[] buffer = new byte[1024];
                    int numCurrentRead;
                    int numTotalRead = 0;

                    while (!Thread.interrupted() && (numCurrentRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, numCurrentRead);

                        numTotalRead += numCurrentRead;
                        progressBar.setValue(numTotalRead);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        out.close();
                    } catch (Exception e) {
                    }
                }
            }
        }, "DownloadFile");

        downloadThread.start();
        while (downloadThread.isAlive()) {
            try {
                downloadThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return contentLength == -1 /* cannot determine */ || progressBar.getValue() >= progressBar.getMaximum();
    }

    /**
     * Dialog displaying the current download status.
     * 
     * The download can be interrupted using the provided button.
     */
    @SuppressWarnings("serial")
    private class StandAloneDialog extends JWindow implements ActionListener {

        private boolean wasInterrupted;

        /**
         * @param title
         *            Text to show next to the progress bar
         */
        public StandAloneDialog(String title) {
            super(ImageViewerGui.getMainFrame());
            setLocationRelativeTo(ImageViewerGui.getMainFrame());

            setLayout(new FlowLayout());

            add(new JLabel(title));
            add(progressBar);

            JButton cmdCancel = new JButton("Cancel");
            cmdCancel.setBorder(BorderFactory.createEtchedBorder());
            cmdCancel.addActionListener(this);
            add(cmdCancel);

            setSize(getPreferredSize());
        }

        public void actionPerformed(ActionEvent e) {
            if (downloadThread != null && downloadThread.isAlive()) {
                downloadThread.interrupt();
                wasInterrupted = true;
            }
        }

    }

}
