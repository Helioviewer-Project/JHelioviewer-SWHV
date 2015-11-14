package org.helioviewer.jhv.io;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JOptionPane;

import org.helioviewer.jhv.viewmodel.view.jp2view.JP2View;

public class DownloadView {

    public static void downloadLayer(JP2View view) {
        Thread downloadThread = new Thread(new Runnable() {
            private JP2View theView;

            @Override
            public void run() {
                downloadFromJPIP(theView);
            }

            public Runnable init(JP2View theView) {
                this.theView = theView;
                return this;
            }
        }.init(view), "DownloadFromJPIPThread");
        downloadThread.start();
    }

    private static void downloadFromJPIP(JP2View v) {
        FileDownloader fileDownloader = new FileDownloader();
        URI downloadUri = v.getDownloadURI();
        URI uri = v.getUri();

        // the http server to download the file from is unknown
        if (downloadUri.equals(uri) && !downloadUri.toString().contains("delphi.nascom.nasa.gov")) {
            String inputValue = JOptionPane.showInputDialog("To download this file, please specify a concurrent HTTP server address to the JPIP server: ", uri);
            if (inputValue != null) {
                try {
                    downloadUri = new URI(inputValue);
                } catch (URISyntaxException e) {
                }
            }
        }

        File downloadDestination = fileDownloader.getDefaultDownloadLocation(uri);
        try {
            if (!fileDownloader.get(downloadUri, downloadDestination, "Downloading " + v.getName())) {
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

}
