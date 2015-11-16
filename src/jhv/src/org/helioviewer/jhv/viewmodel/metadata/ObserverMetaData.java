package org.helioviewer.jhv.viewmodel.metadata;

public interface ObserverMetaData {
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
