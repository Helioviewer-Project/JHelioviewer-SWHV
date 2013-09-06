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
import java.net.URLConnection;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.JWindow;

import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.JHVDirectory;
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
     * Downloads a file from a given HTTP address to the download directory of
     * JHV.
     * 
     * @param sourceURI
     *            address of file which have to be downloaded.
     * @param downloadIfAlreadyExists
     *            set this flag to download a file when a file in the download
     *            directory with the same name already exists. The file will not
     *            be overridden but an increased number will be added to the
     *            file name.
     * @return URI to the downloaded file or null if download fails.
     */
    public URI downloadFromHTTP(URI sourceURI, boolean downloadIfAlreadyExists) {

        // check if sourceURI is an http address
        if (sourceURI == null)
            return null;

        String scheme = sourceURI.getScheme();

        if (scheme == null)
            return null;

        if (!scheme.equalsIgnoreCase("http")) {
            return null;
        }

        try {
            sourceURI = new URI(sourceURI.toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        String name = sourceURI.getPath().substring(sourceURI.getPath().lastIndexOf('/') + 1);
        String outFileName = JHVDirectory.REMOTEFILES.getPath() + sourceURI.getPath().substring(sourceURI.getPath().lastIndexOf('/') + 1);

        progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
        StandAloneDialog dialog = new StandAloneDialog("Downloading " + name);
        dialog.setVisible(true);

        // if local file name doesn't exist, download file
        try {
            if (!downloadFile(sourceURI, new File(outFileName))) {
                if (dialog.wasInterrupted == false) {
                    Message.err("Download", "Unable to download from http", false);
                }
                // if the file was not loaded successfully
            } else {
            }
        } catch (IOException e) {
            dialog.setVisible(false);
            e.printStackTrace();
        } finally {
            dialog.setVisible(false);
            dialog = null;
        }
        // return destination of file
        return new File(outFileName).toURI();
    }

    //
    // /**
    // * Downloads a file from a given JPIP address to the download directory of
    // * JHV. This method only works for the Helioviewer server because of the
    // * source URI modifications which have to be done in this method.
    // *
    // * @param sourceURI
    // * address of file which have to be downloaded.
    // * @param downloadIfAlreadyExists
    // * set this flag to download a file when a file in the download
    // * directory with the same name already exists. The file will not
    // * be overridden but an increased number will be added to the
    // * file name.
    // * @return URI to the downloaded file or null if download fails.
    // */
    // public static URI downloadFromJPIP(URI sourceURI, boolean
    // downloadIfAlreadyExists) {
    //
    // URI sourceURI = null;
    //
    // // set up source url and destination
    // try {
    // sourceURI = new URI(sourceURI.toString().replaceFirst("jpip://",
    // "http://").replaceFirst(":" + sourceURI.getPort(), "/jp2"));
    // } catch (MalformedURIException e) {
    // e.printStackTrace();
    // }
    // String outFileName = JHVDirectory.REMOTEFILES.getPath() +
    // sourceURI.getPath().substring(sourceURI.getPath().lastIndexOf('/')+1);
    //
    // // change output file name if file already exists locally and it should
    // be downloaded again
    // if (downloadIfAlreadyExists && new File(outFileName).exists()) {
    // String suffix = outFileName.substring(outFileName.lastIndexOf('.'));
    // outFileName = outFileName.substring(0, outFileName.lastIndexOf('.'));
    //
    // int i = 1;
    // while (new File(outFileName + "(" + i + ")" + suffix).exists()) {
    // i++;
    // }
    //
    // outFileName = outFileName + "(" + i + ")" + suffix;
    // }
    //
    // // if local file name doesn't exist, download file
    // if (!(new File(outFileName).exists())) {
    //
    // if (!downloadFile(sourceURI, outFileName)) {
    // JHVGlobals.err("Download", "Unable to download from jpip", false);
    // }
    // }
    //
    // return new File(outFileName).toURI();
    // }

    public File getDefaultDownloadLocation(URI source) {
        if (source == null) {
            return null;
        }

        return new File(JHVDirectory.REMOTEFILES.getPath() + source.getPath().substring(Math.max(0, source.getPath().lastIndexOf("/"))));
    }

    /**
     * Gets the file from the source and writes it to the destination file.
     * 
     * @param source
     *            specifies the location of the file which has to be downloaded.
     * @param dest
     *            location where data of the file has to be stored.
     * @return True, if download was successful, false otherwise.
     * @throws IOException
     */
    public boolean get(URI source, File dest) throws IOException {
        progressBar = null;
        return downloadFile(source, dest);
    }

    /**
     * Gets the file from the source and writes it to the destination file. The
     * methods provides an own dialog, which displays the current download
     * progress.
     * 
     * @param source
     *            specifies the location of the file which has to be downloaded.
     * @param dest
     *            location where data of the file has to be stored.
     * @param title
     *            title which should be displayed in the header of the progress
     *            dialog.
     * @return True, if download was successful, false otherwise.
     * @throws IOException
     */
    public boolean get(URI source, File dest, String title) throws IOException {

        // create own dialog where to display the progress
        progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
        // progressBar.setPreferredSize(new Dimension(200, 20));

        StandAloneDialog dialog = new StandAloneDialog(title);
        dialog.setVisible(true);

        // download the file
        boolean result = downloadFile(source, dest);

        dialog.dispose();

        return result;
    }

    public boolean get(URI source, File dest, JProgressBar progressBar) throws IOException {

        // set up progress bar and progress label
        this.progressBar = progressBar;
        progressBar.setName("Downloading '" + source.getPath().substring(source.getPath().lastIndexOf("/") + 1) + "'...");

        // download the file
        return downloadFile(source, dest);
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

        final URI finalSource;
        final File finalDest;
        if (source == null || dest == null)
            return false;

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

        if (dest.isDirectory()) {
            finalDest = new File(dest, finalSource.getPath().substring(Math.max(0, finalSource.getPath().lastIndexOf("/"))));
        } else {
            finalDest = dest;
        }
        finalDest.createNewFile();

        downloadThread = new Thread(new Runnable() {
            public void run() {

                URLConnection conn = null;
                FileOutputStream out = null;
                InputStream in = null;

                try {
                    conn = finalSource.toURL().openConnection();
                    in = conn.getInputStream();
                    out = new FileOutputStream(finalDest);

                    if (progressBar != null) {
                        progressBar.setMaximum(conn.getContentLength());
                    }

                    byte[] buffer = new byte[1024];
                    int numCurrentRead;
                    int numTotalRead = 0;

                    while (!Thread.interrupted() && (numCurrentRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, numCurrentRead);

                        if (progressBar != null) {
                            numTotalRead += numCurrentRead;
                            progressBar.setValue(numTotalRead);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        in.close();
                    } catch (Exception e) {
                    }
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

        boolean result = true;
        if (progressBar != null) {
            result = (progressBar.getValue() >= progressBar.getMaximum());
        }
        return result;
    }

    /**
     * Dialog displaying the current download status.
     * 
     * The download can be interrupted using the provided button.
     */
    private class StandAloneDialog extends JWindow implements ActionListener {

        private static final long serialVersionUID = 1L;
        private boolean wasInterrupted;

        /**
         * Default constructor
         * 
         * @param title
         *            Text to show on top of the progress bar
         */
        public StandAloneDialog(String title) {
            super(ImageViewerGui.getMainFrame());
            setLocationRelativeTo(ImageViewerGui.getMainFrame());

            setLayout(new FlowLayout());

            progressBar.setString(title);
            progressBar.setStringPainted(true);
            add(progressBar);

            JButton cmdCancel = new JButton("Cancel");
            cmdCancel.setBorder(BorderFactory.createEtchedBorder());
            cmdCancel.addActionListener(this);
            add(cmdCancel);

            setSize(getPreferredSize());
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent e) {
            if (downloadThread != null && downloadThread.isAlive()) {
                downloadThread.interrupt();
                wasInterrupted = true;
            }
        }
    }
}
