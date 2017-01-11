package org.helioviewer.jhv.viewmodel.view.jp2view;

import java.io.IOException;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.viewmodel.view.jp2view.cache.JP2ImageCacheStatus;
import org.helioviewer.jhv.viewmodel.view.jp2view.concurrency.BooleanSignal;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPConstants;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPQuery;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPResponse;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPSocket;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_Kdu_cache;

class J2KReader implements Runnable {

    // The thread that this object runs on
    private final Thread myThread;

    // A boolean flag used for stopping the thread
    private volatile boolean isAbolished;

    // A reference to the JP2Image this object is owned by
    private final JP2Image parentImageRef;

    // A reference to the JP2View this object is owned by
    private final JP2View parentViewRef;

    // The a reference to the cache object used by the run method
    private final JHV_Kdu_cache cacheRef;

    private final JP2ImageCacheStatus cacheStatusRef;

    private JPIPSocket socket;

    private final BooleanSignal readerSignal = new BooleanSignal(false);

    private final int num_layers;

    J2KReader(JP2View _imageViewRef, JP2Image _jp2ImageRef) {
        parentViewRef = _imageViewRef;
        parentImageRef = _jp2ImageRef;

        num_layers = parentImageRef.getMaximumFrameNumber() + 1;
        cacheRef = parentImageRef.getReaderCache();
        cacheStatusRef = parentImageRef.getStatusCache();
        socket = parentImageRef.getSocket();

        myThread = new Thread(this, "Reader " + parentImageRef.getName());
        myThread.setDaemon(true);
        myThread.start();
    }

    // runs in abolish thread
    void abolish() {
        if (isAbolished)
            return;
        isAbolished = true;

        while (myThread.isAlive()) {
            try {
                if (socket != null)
                    socket.close(); // try to unblock i/o
                myThread.interrupt();
                myThread.join(100);
            } catch (Exception e) { // avoid exit from loop
                e.printStackTrace();
            }
        }
    }

    void signalReader(JP2ImageParameter params) {
        readerSignal.signal(params);
    }

    private static String createQuery(String fSiz, int iniLayer, int endLayer) {
        return JPIPQuery.create(JPIPConstants.MAX_REQUEST_LEN, "context", "jpxl<" + iniLayer + '-' + endLayer + '>', "fsiz", fSiz + ",closest", "rsiz", fSiz, "roff", "0,0");
    }

    private String[] createMultiQuery(String fSiz) {
        int num_steps = num_layers / JPIPConstants.MAX_REQ_LAYERS;
        if ((num_layers % JPIPConstants.MAX_REQ_LAYERS) != 0)
            num_steps++;

        String[] stepQuerys = new String[num_steps];
        int lpf = -1, lpi = 0, max_layers = num_layers - 1;
        for (int i = 0; i < num_steps; i++) {
            lpf += JPIPConstants.MAX_REQ_LAYERS;
            if (lpf > max_layers)
                lpf = max_layers;

            stepQuerys[i] = createQuery(fSiz, lpi, lpf);

            lpi = lpf + 1;
            if (lpi > max_layers)
                lpi = 0;
        }
        return stepQuerys;
    }

    @Override
    public void run() {
        while (!isAbolished) {
            JP2ImageParameter params;
            // wait for signal
            try {
                parentImageRef.setDownloading(false);
                params = readerSignal.waitForSignal();
                parentImageRef.setDownloading(true);
            } catch (InterruptedException e) {
                continue;
            }

            try {
                if (socket.isClosed()) {
                    // System.out.println(">>> reconnect");
                    socket = new JPIPSocket(parentImageRef.getURI(), cacheRef);
                }

                int frame = params.frame;
                int level = params.resolution.level;

                // choose cache strategy
                boolean singleFrame = false;
                if (num_layers <= 1 /* one frame */ || params.priority) {
                    singleFrame = true;
                }

                // build query based on strategy
                int current_step;
                String[] stepQuerys;
                String fSiz = params.resolution.width + "," + params.resolution.height;
                if (singleFrame) {
                    stepQuerys = new String[] { createQuery(fSiz, frame, frame) };
                    current_step = 0;
                } else {
                    stepQuerys = createMultiQuery(fSiz);

                    int partial = cacheStatusRef.getImageCachedPartiallyUntil();
                    if (partial < num_layers - 1)
                        current_step = partial / JPIPConstants.MAX_REQ_LAYERS;
                    else
                        current_step = frame / JPIPConstants.MAX_REQ_LAYERS;
                }

                // send queries until everything is complete or caching is interrupted
                int complete_steps = 0;
                boolean stopReading = false;
                while (!stopReading && complete_steps < stepQuerys.length) {
                    if (current_step >= stepQuerys.length)
                        current_step = 0;

                    // if query is already complete, go to next step
                    if (stepQuerys[current_step] == null) {
                        current_step++;
                        continue;
                    }

                    // receive and add data to cache
                    JPIPResponse res = socket.send(stepQuerys[current_step], cacheRef);
                    // react if query complete
                    if (res.isResponseComplete()) {
                        // mark query as complete
                        complete_steps++;
                        stepQuerys[current_step] = null;

                        // tell the cache status
                        if (singleFrame) {
                            cacheStatusRef.setFrameLevelComplete(frame, level);
                            parentViewRef.signalRenderFromReader(params); // refresh current image
                        } else {
                            for (int j = current_step * JPIPConstants.MAX_REQ_LAYERS; j < Math.min((current_step + 1) * JPIPConstants.MAX_REQ_LAYERS, num_layers); j++) {
                                cacheStatusRef.setFrameLevelComplete(j, level);
                            }
                        }
                    } else {
                        // tell the cache status
                        if (singleFrame) {
                            cacheStatusRef.setFrameLevelPartial(frame);
                        } else {
                            for (int j = current_step * JPIPConstants.MAX_REQ_LAYERS; j < Math.min((current_step + 1) * JPIPConstants.MAX_REQ_LAYERS, num_layers); j++) {
                                cacheStatusRef.setFrameLevelPartial(j);
                            }
                        }
                    }

                    MoviePanel.cacheStatusChanged();

                    // select next query based on strategy
                    if (!singleFrame)
                        current_step++;

                    // check whether caching has to be interrupted
                    if (readerSignal.isSignaled() || Thread.interrupted()) {
                        stopReading = true;
                    }
                }
                // suicide if fully done
                if (cacheStatusRef.isLevelComplete(0)) {
                    parentImageRef.setDownloading(false);
                    try {
                        socket.close();
                    } catch (IOException ignore) {
                    }
                    return;
                }

                // if single frame & not interrupted & incomplete -> signal again to go on reading
                if (singleFrame && !stopReading && !cacheStatusRef.isLevelComplete(level)) {
                    params.priority = false;
                    readerSignal.signal(params);
                }
             } catch (IOException e) {
                // e.printStackTrace();
                try {
                    socket.close();
                } catch (IOException ioe) {
                    Log.error("J2KReader.run() > Error closing socket", ioe);
                }

                if (retries++ < 13)
                    readerSignal.signal(params); // signal to retry
                else
                    Log.error("Retry limit reached: " + parentImageRef.getURI()); // something may be terribly wrong
            }
        }
    }

    private int retries = 0;

}
