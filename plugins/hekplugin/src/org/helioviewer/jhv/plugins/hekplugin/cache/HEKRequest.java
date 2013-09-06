package org.helioviewer.jhv.plugins.hekplugin.cache;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.helioviewer.base.math.Interval;

public abstract class HEKRequest {

    /**
     * The path for which this request retrieves data
     */
    protected HEKPath path;

    /**
     * Interval for which this request retrieves data
     */
    protected Interval<Date> interval;

    /**
     * Instance of an InputStream in order to be able to cancel downloads
     */
    protected InputStream inputStream;

    /**
     * Flag showing if this request should be canceled
     */
    protected boolean cancel = false;

    /**
     * Sets the cancel flag to true and closes the currently used InputStream
     */
    public void cancel() {

        cancel = true;

        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                // ignore
            }
        }

        // we are not loading anymore
        this.finishRequest();
    }

    /**
     * Method to be called when the request is finished
     */
    protected abstract void finishRequest();

}
