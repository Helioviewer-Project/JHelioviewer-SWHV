package org.helioviewer.viewmodel.view.cache;

import org.helioviewer.viewmodel.changeevent.CacheStatusChangedReason;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.CacheStatusChangedReason.CacheType;
import org.helioviewer.viewmodel.io.APIResponse;
import org.helioviewer.viewmodel.io.APIResponseDump;
import org.helioviewer.viewmodel.view.CachedMovieView;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

/**
 * Cache to parse and buffer all timestamps from all images of an image series.
 * 
 * <p>
 * To parse the time stamps, this class has its own thread.
 * 
 * @author Markus Langenberg
 * 
 */
public abstract class DateTimeCache {

    protected CachedMovieView parent;

    private ImmutableDateTime[] cache;

    protected boolean stopParsing = false;

    protected int nextDateToParse = 0;
    private Thread parsingThread = null;

    /**
     * Default constructor.
     * 
     * @param _parent
     *            parent view
     */
    public DateTimeCache(CachedMovieView _parent) {
        parent = _parent;
        cache = new ImmutableDateTime[_parent.getMaximumFrameNumber() + 1];

        parent.fireChangeEvent(new ChangeEvent(new CacheStatusChangedReason(parent, CacheType.COMPLETE, 0)));
        parent.fireChangeEvent(new ChangeEvent(new CacheStatusChangedReason(parent, CacheType.PARTIAL, 0)));
    }

    /**
     * Returns the last layer, whose meta data is already parsed.
     * 
     * @return Last layer, whose meta data is already parsed
     */
    public int getMetaStatus() {
        return nextDateToParse - 1;
    }

    /**
     * Reads date and time for the given frame number from the cache.
     * 
     * If the cache does not contain the value yet and the function was not
     * called by the thread responsible for parsing, waits until the value is
     * available. When the function is called by the thread responsible for
     * parsing, goes on parsing until the value is available. So, any case,
     * returns the correct value.
     * 
     * @param frameNumber
     *            Frame number to fetch date and time for
     * @return Date and time of the given frame
     */
    public ImmutableDateTime getDateTime(int frameNumber) {
        if (cache[frameNumber] == null) {

            do {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    break;
                }
            } while (cache[frameNumber] == null && !stopParsing);
        }

        return cache[frameNumber];
    }

    /**
     * Starts the parsing thread
     */
    public void startParsing() {
        if (parsingThread != null) {
            stopParsing();
        }

        parsingThread = new Thread(new Runnable() {
            public void run() {
                parseAll();
            }
        }, "DateTime Parser");
        parsingThread.start();
    }

    /**
     * Stops the parsing thread
     */
    public void stopParsing() {
        stopParsing = true;

        if (parsingThread != null && parsingThread.isAlive()) {
            try {
                parsingThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Parses all time stamps.
     * 
     * This function is called from the parsing thread. It test first, whether
     * the time stamps are available via an API response. If that is not the
     * cause, it waits until the meta data are loaded, so it can read the time
     * stamps from the meta data.
     */
    private void parseAll() {

        nextDateToParse = 0;

        if (parent instanceof ImageInfoView) {
            APIResponse apiResponse = APIResponseDump.getSingletonInstance().getResponse(((ImageInfoView) parent).getUri());

            if (apiResponse != null) {
                String rawFrames = apiResponse.getString("frames").replaceAll("\\[|\\]", "");

                if (rawFrames != null) {
                    String[] frames = rawFrames.split(",");

                    if (frames.length - 1 == parent.getMaximumFrameNumber()) {
                        do {
                            cache[nextDateToParse] = new ImmutableDateTime(Integer.parseInt(frames[nextDateToParse]));
                            fireChangeEventIfNecessary();
                            nextDateToParse++;

                        } while (!stopParsing && nextDateToParse <= parent.getMaximumFrameNumber());
                    }
                }
            }
        }

        while (!stopParsing && nextDateToParse <= parent.getMaximumFrameNumber()) {

            cache[nextDateToParse] = parseDateTime(nextDateToParse);

            if (nextDateToParse < cache.length && cache[nextDateToParse] == null) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                }

            } else {
                fireChangeEventIfNecessary();

                nextDateToParse++;
            }
        }
    }

    /**
     * Fires a change event into the view chain, if the cache status has
     * changed.
     */
    private void fireChangeEventIfNecessary() {
        if (nextDateToParse <= parent.getImageCacheStatus().getImageCachedCompletelyUntil()) {
            parent.fireChangeEvent(new ChangeEvent(new CacheStatusChangedReason(parent, CacheType.COMPLETE, nextDateToParse)));

        } else if (nextDateToParse <= parent.getImageCacheStatus().getImageCachedPartiallyUntil()) {
            parent.fireChangeEvent(new ChangeEvent(new CacheStatusChangedReason(parent, CacheType.PARTIAL, nextDateToParse)));
        }
    }

    /**
     * Parses the date and time of the given frame number.
     * 
     * Since depends on the format of the meta data very much, this function is
     * abstract and should be implemented by a specialized class.
     * 
     * @param frameNumber
     *            Frame number to parse date and time for
     * @return Date and time of the given frame
     */
    protected abstract ImmutableDateTime parseDateTime(int frameNumber);
}
