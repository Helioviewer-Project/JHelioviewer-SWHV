package org.helioviewer.viewmodel.metadata;

import java.util.ArrayList;

import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JP2Image;

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

    public static ArrayList<MetaData> getMetaDataList(JP2Image mdc, JHVJP2View jp2v) {
        ArrayList<MetaData> metaDataList = new ArrayList<MetaData>();
        int numberOfLayers = mdc.getNumberFrames();
        synchronized (jp2v.imageViewParams) {
            for (int i = 0; i <= numberOfLayers; i++) {
                MetaData md = getMetaData(mdc);
                metaDataList.add(md);

                //jpxv.setCurrentFrameNumber(i, null, false);
                jp2v.imageViewParams.compositionLayer = i;
            }

            jp2v.getImageViewParams().compositionLayer = 0;
        }
        return metaDataList;
    }
}
