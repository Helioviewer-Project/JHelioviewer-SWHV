package org.helioviewer.gl3d.plugin.vectors.data;

@SuppressWarnings("serial")
public class ObservationDateMissingException extends Exception {

    public ObservationDateMissingException(String filename) {
        // TODO specify adaquate message!

        super(filename + " file is missing an Obersvation Date. Add the 'DATE-OBS' Property to its FITS-Header.");
    }

}
