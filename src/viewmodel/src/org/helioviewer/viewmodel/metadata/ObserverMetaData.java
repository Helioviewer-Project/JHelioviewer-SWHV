package org.helioviewer.viewmodel.metadata;

import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

/**
 * Meta data providing informations about the instrument used to take the
 * picture.
 * 
 * <p>
 * This interface is used for space related pictures, providing informations
 * about instrument, detector and measurements as well as the time, when the
 * picture was taken.
 * 
 * @author Ludwig Schmidt
 * 
 */
public interface ObserverMetaData {

    /**
     * Returns date and time when the picture was taken.
     * 
     * @return Date and time when the picture was taken.
     */
    public ImmutableDateTime getDateTime();

    /**
     * Updates the observation time by reparsing the information from the meta
     * data container.
     * 
     * <p>
     * This might be necessary for image series: An image series contains
     * multiple images from different times, but there is only one visible at a
     * moment. If the visible image changes, the meta data has to be updated,
     * too.
     */
    public void updateDateTime();

    /**
     * Updates the observation time by resetting it to the given time.
     * 
     * <p>
     * This might be necessary for image series: An image series contains
     * multiple images from different times, but there is only one visible at a
     * moment. If the visible image changes, the meta data has to be updated,
     * too.
     * 
     * @param newDateTime
     *            New date and time to save
     */
    public void updateDateTime(ImmutableDateTime newDateTime);

    /**
     * Returns the observatory used to take the picture.
     * 
     * @return Observatory used to take the picture.
     */
    public String getObservatory();

    /**
     * Returns the instrument used to take the picture.
     * 
     * @return Instrument used to take the picture.
     */
    public String getInstrument();

    /**
     * Returns the detector used to take the picture.
     * 
     * @return Detector used to take the picture.
     */
    public String getDetector();

    /**
     * Returns the measurement used to take the picture.
     * 
     * Usually, Represents a wavelength.
     * 
     * @return Measurement used to take the picture.
     */
    public String getMeasurement();

    /**
     * Returns the full name of the instrument.
     * 
     * @return Full name of the instrument
     */
    public String getFullName();
}
