package org.helioviewer.jhv.io;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.helioviewer.base.DownloadStream;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.viewmodel.io.APIResponse;
import org.helioviewer.viewmodel.io.APIResponseDump;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

/**
 * This class provides methods to download files from a server.
 *
 * Most of the methods only will work with the current Helioviewer server
 * because they modify links and requests that they will fit with the API.
 *
 * @author Stephan Pagel
 * @author Andre Dau
 * @author Helge Dietert
 */
public class APIRequestManager {
    /**
     * Returns the date of the latest image available from the server
     *
     * @param observatory
     *            observatory of the requested image.
     * @param instrument
     *            instrument of the requested image.
     * @param detector
     *            detector of the requested image.
     * @param measurement
     *            measurement of the requested image.
     * @param message
     *            display error message
     * @return time stamp of the latest available image on the server
     * @throws IOException
     * @throws MalformedURLException
     */
    public static Date getLatestImageDate(String observatory, String instrument, String detector, String measurement, boolean message) {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Date date = new Date();
        boolean readDate = false;
        AbstractView view = null;

        try {
            view = loadImage(false, observatory, instrument, detector, measurement, formatter.format(date), message);
            if (view != null) {
                MetaData metaData = view.getMetaData();
                if (metaData instanceof HelioviewerMetaData) {
                    HelioviewerMetaData helioviewerMetaData = (HelioviewerMetaData) metaData;
                    date = helioviewerMetaData.getDateTime().getTime();
                    readDate = true;
                } else {
                    Log.error(">> APIRequestManager.getLatestImageDate() > Could not find Helioviewer meta data in latest image. Use current date as initial end date.", new Exception());
                }
                if (view instanceof JHVJP2View) {
                    ((JHVJP2View) view).abolish();
                }
            } else {
                Log.error(">> APIRequestManager.getLatestImageDate() > Could not load latest image. Use current date as initial end date.", new Exception());
            }
        } catch (MalformedURLException e) {
            Log.error(">> APIRequestManager.getLatestImageDate() > Malformed jpip request url. Use current date as initial end date.", e);
        } catch (IOException e) {
            Log.error(">> APIRequestManager.getLatestImageDate() > Error while opening stream. Use current date as initial end date.", e);
        }

        if (readDate) {
            return date;
        } else {
            return new Date(System.currentTimeMillis() - 48 * 60 * 60 * 1000);
        }
    }

    /**
     * Sends an request to the server to compute where the nearest image is
     * located on the server. The address of the file will be returned.
     *
     * @param addToViewChain
     *            specifies whether the generated View should be added to the
     *            view chain of the main image
     * @param observatory
     *            observatory of the requested image.
     * @param instrument
     *            instrument of the requested image.
     * @param detector
     *            detector of the requested image.
     * @param measurement
     *            measurement of the requested image.
     * @param startTime
     *            time if the requested image.
     * @param message
     *            display error message.
     * @return view of the nearest image file on the server.
     * @throws MalformedURLException
     * @throws IOException
     */
    public static AbstractView loadImage(boolean addToViewChain, String observatory, String instrument, String detector, String measurement, String startTime, boolean message) throws MalformedURLException, IOException {
        String fileRequest = Settings.getSingletonInstance().getProperty("API.jp2images.path") + "?action=getJP2Image&observatory=" + observatory + "&instrument=" + instrument + "&detector=" + detector + "&measurement=" + measurement + "&date=" + startTime + "&json=true";
        String jpipRequest = fileRequest + "&jpip=true";

        // get URL from server where file with image series is located
        try {
            return requestData(addToViewChain, new URL(jpipRequest), new URI(fileRequest), message);
        } catch (IOException e) {
            if (e instanceof UnknownHostException) {
                Log.debug(">> APIRequestManager.loadImageSeries(boolean,String,String,String,String,String,String,String)  > Error will be throw", e);
                throw new IOException("Unknown Host: " + e.getMessage());
            } else {
                Log.debug(">> APIRequestManager.loadImageSeries(boolean,String,String,String,String,String,String,String)  > Error will be throw", e);
                throw new IOException("Error in the server communication:" + e.getMessage());
            }
        } catch (URISyntaxException e) {
            Log.error("Error creating jpip request", e);
        }
        return null;
    }

    /**
     * Sends an request to the server to compute where the image series is
     * located on the server. The address of the file will be returned.
     *
     * @param addToViewChain
     *            specifies whether the generated View should be added to the
     *            view chain of the main image
     * @param observatory
     *            observatory of the requested image series.
     * @param instrument
     *            instrument of the requested image series.
     * @param detector
     *            detector of the requested image series.
     * @param measurement
     *            measurement of the requested image series.
     * @param startTime
     *            start time of the requested image series.
     * @param endTime
     *            end time of the requested image series.
     * @param cadence
     *            cadence between to images of the image series.
     * @param message
     *            display error message.
     * @return view of the file which represents the image series on the server.
     * @throws MalformedURLException
     * @throws IOException
     */
    private static AbstractView loadImageSeries(boolean addToViewChain, String observatory, String instrument, String detector, String measurement, String startTime, String endTime, String cadence, boolean message) throws MalformedURLException, IOException {
        String fileRequest = Settings.getSingletonInstance().getProperty("API.jp2series.path") + "?action=getJPX&observatory=" + observatory + "&instrument=" + instrument + "&detector=" + detector + "&measurement=" + measurement + "&startTime=" + startTime + "&endTime=" + endTime;

        if (cadence != null) {
            fileRequest += "&cadence=" + cadence;
        }

        String jpipRequest = fileRequest + "&jpip=true&verbose=true&linked=true";

        Log.debug(">> APIRequestManager.loadImageSeries(boolean,String,String,String,String,String,String,String) > jpip request url: " + jpipRequest);
        Log.debug(">> APIRequestManager.loadImageSeries(boolean,String,String,String,String,String,String,String) > http request url: " + fileRequest);

        // get URL from server where file with image series is located
        try {
            return requestData(addToViewChain, new URL(jpipRequest), new URI(fileRequest), message);
        } catch (IOException e) {
            if (e instanceof UnknownHostException) {
                Log.debug(">> APIRequestManager.loadImageSeries(boolean,String,String,String,String,String,String,String)  > Error will be throw", e);
                throw new IOException("Unknown Host: " + e.getMessage());
            } else {
                Log.debug(">> APIRequestManager.loadImageSeries(boolean,String,String,String,String,String,String,String)  > Error will be throw", e);
                throw new IOException("Error in the server communication:" + e.getMessage());
            }
        } catch (URISyntaxException e) {
            Log.error("Error creating jpip request", e);
        }

        return null;
    }

    /**
     * Sends an request to the server to compute where the image series is
     * located on the server together with meta information like timestamps for
     * the frames.
     * <p>
     * After processing the request it will if the server gives a sufficient
     * reply, i.e. "uri" is set it will try to load the result with
     * {@link #newLoad(URI, URI, boolean)}. It will display and log any further
     * message from the server.
     * <p>
     * Returns the corresponding View for the file.
     *
     * @param addToViewChain
     *            specifies whether the generated View should be added to the
     *            view chain of the main image
     * @param jpipRequest
     *            The http request url which is sent to the server
     * @param downloadUri
     *            the http uri from which the whole file can be downloaded
     * @param errorMessage
     *            display error message
     * @return The View corresponding to the file whose location was returned by
     *         the server
     */
    public static AbstractView requestData(boolean addToViewChain, URL jpipRequest, URI downloadUri, boolean errorMessage) throws IOException {
        try {
            DownloadStream ds = new DownloadStream(jpipRequest, JHVGlobals.getStdConnectTimeout(), JHVGlobals.getStdReadTimeout());
            APIResponse response = new APIResponse(new BufferedReader(new InputStreamReader(ds.getInput())));

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
                if (message != null && !message.equalsIgnoreCase("null") && errorMessage) {
                    Message.warn("Warning", Message.formatMessageString(message));
                }
                APIResponseDump.getSingletonInstance().putResponse(response);
                return newLoad(response.getURI(), downloadUri, addToViewChain);
            } else {
                // We did not get a reply to load data or no reply at all
                String message = response.getString("message");
                if (message != null && !message.equalsIgnoreCase("null")) {
                    Log.error("No data to load returned from " + jpipRequest);
                    Log.error("Server message: " + message);
                    if (errorMessage) {
                        Message.err("Server could not return data", Message.formatMessageString(message), false);
                    }
                } else {
                    Log.error("Did not find uri in reponse to " + jpipRequest);
                    if (errorMessage) {
                        Message.err("No data source response", "While quering the data source, the server did not provide an answer.", false);
                    }
                }
            }
        } catch (SocketTimeoutException e) {
            Log.error("Socket timeout while requesting jpip url", e);
            Message.err("Socket timeout", "Socket timeout while requesting jpip url", false);
        }
        return null;
    }

    /**
     * Loads the image or image series from the given URI, creates a new image
     * info view and adds it as a new layer to the view chain of the main image.
     *
     * @param uri
     *            specifies the location of the file.
     * @param addToViewChain
     *            specifies whether the generated View should be added to the
     *            view chain of the main image
     * @return associated image info view of the given image or image series
     *         file.
     * @throws IOException
     */
    public static AbstractView newLoad(URI uri, boolean addToViewChain) throws IOException {
        if (uri == null) {
            return null;
        }

        // Load new view and assign it to view chain of Main Image

        AbstractView view = ViewHelper.loadView(uri);

        if (addToViewChain) {
            addToViewchain(view);
        }
        return view;
    }

    /**
     * Loads the image or image series from the given URI, creates a new image
     * info view and adds it as a new layer to the view chain of the main image.
     *
     * @param uri
     *            specifies the location of the file.
     * @param downloadURI
     *            the http uri from which the whole file can be downloaded
     * @param addToViewChain
     *            specifies whether the generated View should be added to the
     *            view chain of the main image
     * @return associated image info view of the given image or image series
     *         file.
     * @throws IOException
     */
    public static AbstractView newLoad(URI uri, URI downloadURI, boolean addToViewChain) throws IOException {
        if (uri == null) {
            return null;
        }

        // Load new view and assign it to view chain of Main Image

        AbstractView view = ViewHelper.loadView(uri, downloadURI);

        if (addToViewChain) {
            addToViewchain(view);
        }

        return view;
    }

    private static void addToViewchain(AbstractView view) {
        while (view.getSubimageData() == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        EventQueue.invokeLater(new Runnable() {
            private AbstractView theView;

            @Override
            public void run() {
                Displayer.getLayersModel().addLayer(theView);
            }

            public Runnable init(AbstractView theView) {
                this.theView = theView;
                return this;
            }
        }.init(view));
    }

    /**
     * Method does remote opening. If image series, file is downloaded. If
     * single frame, file is opened via JPIP on delphi.nascom.nasa.gov:8090.
     *
     * @param addToViewChain
     *            specifies whether the generated View should be added to the
     *            view chain of the main image
     * @param cadence
     *            cadence between two frames (null for single images).
     * @param startTime
     *            start time of the requested image
     * @param endTime
     *            end time of the requested image (empty for single images).
     * @param observatory
     *            observatory of the requested image
     * @param instrument
     *            instrument of the requested image
     * @param detector
     *            detector of the requested image.
     * @param measurement
     *            measurement of the requested image.
     * @param message
     *            display error message
     * @return new view
     * @throws IOException
     */
    public static AbstractView requestAndOpenRemoteFile(boolean addToViewChain, String cadence, String startTime, String endTime, String observatory, String instrument, String detector, String measurement, boolean message) throws IOException {
        if (endTime.equals("")) {
            return loadImage(addToViewChain, observatory, instrument, detector, measurement, startTime, message);
        } else {
            return loadImageSeries(addToViewChain, observatory, instrument, detector, measurement, startTime, endTime, cadence, message);
        }
    }

}
