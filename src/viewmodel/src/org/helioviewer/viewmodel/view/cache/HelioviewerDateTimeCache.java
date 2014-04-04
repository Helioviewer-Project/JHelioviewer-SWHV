package org.helioviewer.viewmodel.view.cache;

import java.io.IOException;

import org.helioviewer.viewmodel.metadata.MultiFrameMetaDataContainer;
import org.helioviewer.viewmodel.view.CachedMovieView;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

/**
 * Implementation of JP2DateTimeCache for helioviewer images.
 * 
 * <p>
 * Currently, supports the observatory SOHO with its instruments EIT, LASCO and
 * MDI, as well as some instruments on board of the observatory STEREO.
 * 
 * @author Markus Langenberg
 * 
 */
public class HelioviewerDateTimeCache extends DateTimeCache {

    private boolean isLASCO;
    private boolean checkedForLasco = false;
    private boolean isSWAP;
    private boolean checkedForSwap = false;

    private MultiFrameMetaDataContainer source;

    /**
     * Default constructor.
     * 
     * @param _parent
     *            the cached movie view
     * @param _source
     *            the multi frame meta data container
     */
    public HelioviewerDateTimeCache(CachedMovieView _parent, MultiFrameMetaDataContainer _source) {
        super(_parent);

        source = _source;

        checkForLasco();
        checkForSwap();
    }

    private void checkForSwap() {
        if (!checkedForSwap) {
            String instrument = source.get("INSTRUME");
            isSWAP = ((instrument != null && instrument.contains("SWAP"))||(instrument != null && instrument.contains("CALLISTO")));
            checkedForSwap = true;
        }
    }

    /**
     * Checks, whether the given image was taken by the instrument LASCO. This
     * is necessary, since date and time within the LASCO meta data are given in
     * different format.
     */
    private void checkForLasco() {
        if (!checkedForLasco) {
            String instrument = source.get("INSTRUME");
            isLASCO = (instrument != null && instrument.trim().equalsIgnoreCase("LASCO"));
            checkedForLasco = true;
        }
    }

    /**
     * {@inheritDoc} This class implements this function for helioviewer images.
     */
    protected ImmutableDateTime parseDateTime(int frameNumber) {
        if (parent.getImageCacheStatus().getImageStatus(frameNumber) == null)
            return null;

        checkForLasco();
        checkForSwap();

        try {
            String observedDate;
            if (isSWAP) {
                observedDate = source.get("DATE-OBS", frameNumber);
            } else {
                observedDate = source.get("DATE_OBS", frameNumber);
            }
            if (isLASCO) {
                observedDate += "T" + source.get("TIME_OBS", frameNumber);
            }
            return parseDateTime(observedDate);

        } catch (IOException e) {
            if (e.getMessage() == "No XML data present") {
                nextDateToParse = parent.getMaximumFrameNumber() + 1;
                stopParsing = true;
                return new ImmutableDateTime(0, 0, 0, 0, 0, 0);
            }
            return null;
        }
    }

    /**
     * Parses date and time given in one string into one object.
     * 
     * <p>
     * The String should have the following format:
     * 
     * <br>
     * yyyy-mm-ddThh:mm:ss <br>
     * or <br>
     * yyyy/mm/ddThh:mm:ss
     * 
     * The string may also contain the milliseconds after a period, but they
     * will not be included int the final result
     * 
     * @param dateTime
     *            Date and time given in one string
     * @return object representing date and time
     */
    public static ImmutableDateTime parseDateTime(String dateTime) {

        int year = 0, month = 0, day = 0, hour = 0, minute = 0, second = 0;

        if (dateTime != null) {
            try {
                String[] firstDivide = dateTime.split("T");
                String[] secondDivide1 = firstDivide[0].split("[-/]");
                String[] secondDivide2 = firstDivide[1].split(":");
                String[] thirdDivide = secondDivide2[2].split("\\.");
                year = Integer.valueOf(secondDivide1[0]);
                month = Integer.valueOf(secondDivide1[1]);
                day = Integer.valueOf(secondDivide1[2]);
                hour = Integer.valueOf(secondDivide2[0]);
                minute = Integer.valueOf(secondDivide2[1]);
                second = Integer.valueOf(thirdDivide[0]);
            } catch (Exception e) {
                year = 0;
                month = 0;
                day = 0;
                hour = 0;
                minute = 0;
                second = 0;
            }
        }

        return new ImmutableDateTime(year, month != 0 ? month - 1 : 0, day, hour, minute, second);
    }
}
