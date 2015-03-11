package org.helioviewer.viewmodel.metadata;

import java.io.IOException;

import org.helioviewer.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

/**
 * Factory for creating meta data out of a meta data container.
 *
 * <p>
 * This factory ensures, that the correct type of meta data is generated.
 * Currently, it supports {@link HelioviewerMetaData}, its extension
 * {@link HelioviewerOcculterMetaData} and {@link PixelBasedMetaData} as a
 * fallback solution.
 *
 * @author Ludwig Schmidt
 *
 */
public class MetaDataConstructor {

    /**
     * Returns an implementation of MetaData.
     *
     * The function tries to search which implementation matches the image
     * contents best.
     *
     * @param mdc
     *            Meta data container serving as a base for the construction
     * @return Implementation of MetaData
     */
    public static MetaData getMetaData(MetaDataContainer mdc) {

        // Try occulter meta data
        HelioviewerOcculterMetaData occulterMetaData = new HelioviewerOcculterMetaData(mdc);

        // If the inner radius is 0, then there wasn't any
        // supported meta data available
        if (occulterMetaData.getInnerPhysicalOcculterRadius() != 0.0)
            return occulterMetaData;

        HelioviewerPositionedMetaData hvPosMetaData = new HelioviewerPositionedMetaData(mdc);
        if (hvPosMetaData.isHEEQProvided() || hvPosMetaData.isHEEProvided() || hvPosMetaData.isCarringtonProvided() || hvPosMetaData.isStonyhurstProvided()) {
            return hvPosMetaData;
        }

        // Try helioviewer meta data
        HelioviewerMetaData hvMetaData = new HelioviewerMetaData(mdc);

        // If the sun radius is -1, there wasn't any
        // supported meta data available
        if (hvMetaData.getSunPixelRadius() == -1) {
            return new PixelBasedMetaData(mdc.getPixelWidth(), mdc.getPixelHeight());
        } else {
            return hvMetaData;
        }
    }

    /**
     * {@inheritDoc} This class implements this function for helioviewer images.
     */
    public static ImmutableDateTime parseDateTime(JP2Image source, int frameNumber, boolean isSWAP, boolean isLASCO) {
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
                return new ImmutableDateTime(0, 0, 0, 0, 0, 0);
            }
            return null;
        }
    }

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
