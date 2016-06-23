package org.helioviewer.jhv.io;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.EventDispatchQueue;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.fitsview.FITSView;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2ImageCallisto;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2View;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2ViewCallisto;
import org.helioviewer.jhv.viewmodel.view.simpleimageview.SimpleImageView;

/**
 * This class provides methods to download files from a server.
 *
 * Most of the methods only will work with the current Helioviewer server
 * because they modify links and requests that they will fit with the API.
 */
public class APIRequestManager {
    /**
     * Sends an request to the server to compute where the nearest image is
     * located on the server.
     *
     * @param server
     * @param sourceId
     *            sourceId of the requested image
     * @param startTime
     *            time if the requested image
     * @param message
     *            display error message
     * @return view of the nearest image file on the server
     * @throws IOException
     */
    private static View loadImage(String server, int sourceId, long startTime, boolean message) throws IOException {
        String fileRequest = server + "sourceId=" + Integer.toString(sourceId) + "&date=" + TimeUtils.apiDateFormat.format(startTime) + "&json=true";
        String jpipRequest = fileRequest + "&jpip=true";
        return requestData(jpipRequest, fileRequest, message);
    }

    /**
     * Sends an request to the server to compute where the image series is
     * located on the server.
     *
     * @param server
     * @param sourceId
     *            sourceId of the requested image series
     * @param startTime
     *            start time of the requested image series
     * @param endTime
     *            end time of the requested image series
     * @param cadence
     *            cadence between to images of the image series
     * @param message
     *            display error message
     * @return view of the file which represents the image series on the server
     * @throws IOException
     */
    private static View loadImageSeries(String server, int sourceId, long startTime, long endTime, int cadence, boolean message) throws IOException {
        String fileRequest = server + "sourceId=" + Integer.toString(sourceId) + "&startTime=" + TimeUtils.apiDateFormat.format(startTime) + "&endTime=" + TimeUtils.apiDateFormat.format(endTime);
        if (cadence != -100) {
            fileRequest += "&cadence=" + Integer.toString(cadence);
        }
        String jpipRequest = fileRequest + "&jpip=true&verbose=true&linked=true";
        return requestData(jpipRequest, fileRequest, message);
    }

    private static View requestData(String _jpipRequest, String fileRequest, boolean errorMessage) throws IOException {
        try {
            URL jpipRequest = new URL(_jpipRequest);
            URI downloadUri = new URI(fileRequest);
            APIResponse response = new APIResponse(new DownloadStream(jpipRequest).getInput());

            // Could we handle the answer from the server
            if (!response.hasData()) {
                Log.error("Could not understand server answer from " + jpipRequest);
                if (errorMessage) {
                    Message.err("Invalid Server reply", "The server data could not be parsed.", false);
                }
                return null;
            }
            // Just some error from the server
            String error = response.getString("error");
            if (error != null) {
                Log.error("Data query returned error: " + error);
                if (errorMessage) {
                    Message.err("Error getting the data", Message.formatMessageString(error), false);
                }
                return null;
            }

            // Try to load
            if (response.getURI() != null) {
                // The server wants to load us the data
                String message = response.getString("message");
                if (message != null && errorMessage) {
                    Message.warn("Warning", Message.formatMessageString(message));
                }
                return loadView(response.getURI(), downloadUri);
            } else {
                // We did not get a reply to load data or no reply at all
                String message = response.getString("message");
                if (message != null) {
                    Log.error("No data to load returned from " + jpipRequest);
                    Log.error("Server message: " + message);
                    if (errorMessage) {
                        Message.err("Server could not return data", Message.formatMessageString(message), false);
                    }
                } else {
                    Log.error("Did not find URI in response to " + jpipRequest);
                    if (errorMessage) {
                        Message.err("No data source response", "While quering the data source, the server did not provide an answer.", false);
                    }
                }
            }
        } catch (MalformedURLException e) {
            Log.error("APIRequestManager.requestData() > Malformed JPIP request URL: " + _jpipRequest);
        } catch (URISyntaxException e) {
            Log.error("APIRequestManager.requestData() > URI syntax exception: " + fileRequest);
        } catch (UnknownHostException e) {
            Log.debug("APIRequestManager.requestData() > Error will be thrown", e);
            throw new IOException("Unknown Host: " + e.getMessage());
        } catch (SocketTimeoutException e) {
            Log.error("Socket timeout while requesting JPIP URL", e);
            Message.err("Socket timeout", "Socket timeout while requesting JPIP URL", false);
        } catch (IOException e) {
            Log.debug("APIRequestManager.requestData() > Error will be thrown", e);
            throw new IOException("Error in the server communication: " + e.getMessage());
        }

        return null;
    }

    /**
     * Method does remote opening. If image series, file is downloaded.
     *
     * @param server
     * @param sourceId
     *            sourceId of the requested image
     * @param startTime
     *            start time of the requested image
     * @param endTime
     *            end time of the requested image
     * @param cadence
     *            cadence between two frames
     * @param message
     *            display error message
     * @return new view
     * @throws IOException
     */
    public static View requestAndOpenRemoteFile(String server, int sourceId, long startTime, long endTime, int cadence, boolean message) throws IOException {
        if (startTime == endTime) {
            if (server == null) // use default
                server = Settings.getSingletonInstance().getProperty("API.jp2images.path");
            return loadImage(server, sourceId, startTime, message);
        } else {
            if (server == null) // use default
                server = Settings.getSingletonInstance().getProperty("API.jp2series.path");
            return loadImageSeries(server, sourceId, startTime, endTime, cadence, message);
        }
    }

    /**
     * Loads a new image located at the given URI.
     *
     * Depending on the file type, a different implementation of the View is
     * chosen. If there is no implementation available for the given type, an
     * exception is thrown.
     *
     * @param uri
     *            URI representing the location of the image
     * @param downloadURI
     *            URI from which the whole file can be downloaded
     * @return View containing the image
     * @throws IOException
     */
    public static View loadView(URI uri, URI downloadURI) throws IOException {
        if (uri == null || uri.getScheme() == null) {
            throw new IOException("Invalid URI");
        }

        try {
            String loc = uri.toString().toLowerCase(Locale.ENGLISH);
            if (loc.endsWith(".fits") || loc.endsWith(".fts")) {
                return new FITSView(uri);
            } else if (loc.endsWith(".png") || loc.endsWith(".jpg") || loc.endsWith(".jpeg")) {
                 return new SimpleImageView(uri);
            } else {
                JP2Image jp2Image;
                if (loc.contains("callisto"))
                    jp2Image = new JP2ImageCallisto(uri, downloadURI);
                else
                    jp2Image = new JP2Image(uri, downloadURI);
                return EventDispatchQueue.invokeAndWait(new AllocateJP2View(jp2Image));
            }
        } catch (InterruptedException e) {
            // nothing
        } catch (Exception e) {
            Log.debug("APIRequestManager.loadView(\"" + uri + "\", \"" + downloadURI + "\") ", e);
            throw new IOException(e.getMessage());
        }
        return null;
    }

    private static class AllocateJP2View implements Callable<JP2View> {
        private final AtomicReference<JP2Image> refJP2Image = new AtomicReference<JP2Image>();

        public AllocateJP2View(JP2Image jp2Image) {
            refJP2Image.set(jp2Image);
        }

        @Override
        public JP2View call() {
            JP2View view;
            JP2Image jp2Image = refJP2Image.get();
            if (jp2Image instanceof JP2ImageCallisto) {
                view = new JP2ViewCallisto();
            } else {
                view = new JP2View();
            }
            view.setJP2Image(jp2Image);

            return view;
        }
    }

}
