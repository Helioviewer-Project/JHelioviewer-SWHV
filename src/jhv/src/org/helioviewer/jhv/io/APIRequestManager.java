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

public class APIRequestManager {

    public static final int CADENCE_ANY = -100;

    public static class APIRequest {

        public final String server;
        public final int sourceId;
        public final long startTime;
        public final long endTime;
        public final int cadence;

        public APIRequest(String server, int sourceId, long startTime, long endTime, int cadence) {
            this.server = server;
            this.sourceId = sourceId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.cadence = cadence;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof APIRequest) {
                APIRequest r = (APIRequest) o;
                return server.equals(r.server) && sourceId == r.sourceId && startTime == r.startTime && endTime == r.endTime && cadence == r.cadence;
            }
            return false;
        }

        @Override
        public int hashCode() {
            assert false : "hashCode not designed";
            return 42;
        }

        String[] getRequests() {
            String fileRequest, jpipRequest;
            if (startTime == endTime) {
                fileRequest = DataSources.getServerSetting(server, "API.jp2images.path") + "sourceId=" + Integer.toString(sourceId) +
                                                           "&date=" + TimeUtils.apiDateFormat.format(startTime) + "&json=true";
                jpipRequest = fileRequest + "&jpip=true";
            } else {
                fileRequest = DataSources.getServerSetting(server, "API.jp2series.path") + "sourceId=" + Integer.toString(sourceId) +
                                                           "&startTime=" + TimeUtils.apiDateFormat.format(startTime) + "&endTime=" + TimeUtils.apiDateFormat.format(endTime);
                if (cadence != CADENCE_ANY) {
                    fileRequest += "&cadence=" + Integer.toString(cadence);
                }
                jpipRequest = fileRequest + "&jpip=true&verbose=true&linked=true";
            }
            return new String[] { jpipRequest, fileRequest };
        }

    }

    public static View requestAndOpenRemoteFile(String server, int sourceId, long startTime, long endTime, int cadence, boolean errorMessage) throws IOException {
        APIRequest apiRequest = new APIRequest(server, sourceId, startTime, endTime, cadence);
        String[] reqStrings = apiRequest.getRequests();
        String reqJPIP = reqStrings[0], reqDownload = reqStrings[1];

        try {
            URL jpipRequest = new URL(reqJPIP);
            URI downloadUri = new URI(reqDownload);
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
                return loadView(response.getURI(), downloadUri, apiRequest);
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
            Log.error("APIRequestManager.requestData() > Malformed JPIP request URL: " + reqJPIP);
        } catch (URISyntaxException e) {
            Log.error("APIRequestManager.requestData() > URI syntax exception: " + reqDownload);
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

    public static View loadView(URI uri, URI downloadURI, APIRequest apiRequest) throws IOException {
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

                JP2View view = EventDispatchQueue.invokeAndWait(new AllocateJP2View(jp2Image));
                view.setAPIRequest(apiRequest);
                return view;
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
