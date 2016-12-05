package org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip;

import java.util.LinkedList;

/**
 * A response to a JPIPRequest, encapsulates the JPIPDataSegments
 *
 * @author caplins
 *
 */
public class JPIPResponse {

    /** The status... could be EOR_WINDOW_DONE or EOR_IMAGE_DONE */
    private long status = -1;

    /** A list of the data segments. */
    private final LinkedList<JPIPDataSegment> jpipDataList = new LinkedList<>();

    private final String cnew;

    public JPIPResponse(String _cnew) {
        cnew = _cnew;
    }

    public String getCNew() {
        return cnew;
    }

    /**
     * Adds the data segment to this object.
     *
     * @param data
     */
    public void addJpipDataSegment(JPIPDataSegment data) {
        if (data.isEOR) {
            status = data.binID;
        }
        jpipDataList.add(data);
    }

    /**
     * Removes a data segment from this object.
     *
     * @return The removed data segment, null if the list was empty
     */
    public JPIPDataSegment removeJpipDataSegment() {
        if (jpipDataList.isEmpty())
            return null;
        return jpipDataList.remove();
    }

    /**
     * Tells if the response completes the last request.
     *
     * @return True, if the response is complete
     */
    public boolean isResponseComplete() {
        return status == JPIPConstants.EOR_WINDOW_DONE || status == JPIPConstants.EOR_IMAGE_DONE;
    }

}
