package org.helioviewer.viewmodel.view.jp2view.io.jpip;

import java.io.IOException;
import java.util.LinkedList;

import org.helioviewer.viewmodel.view.jp2view.io.http.HTTPResponse;

/**
 * A response to a JPIPRequest. Encapsulates both the HTTPResponse headers and
 * the JPIPDataSegments.
 * 
 * @author caplins
 * 
 */
public class JPIPResponse extends HTTPResponse {

    /** The status... could be EOR_WINDOW_DONE or EOR_IMAGE_DONE */
    private long status;

    /** A list of the data segments. */
    private LinkedList<JPIPDataSegment> jpipDataList;

    /**
     * Used to form responses.
     * 
     * @param res
     * @throws IOException
     */
    public JPIPResponse(HTTPResponse res) throws IOException {
        super(res.getCode(), res.getReason());

        for (String key : res.getHeaders())
            this.setHeader(key, res.getHeader(key));

        status = -1;
        jpipDataList = new LinkedList<JPIPDataSegment>();
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
        return (jpipDataList.isEmpty() ? null : jpipDataList.remove());
    }

    /**
     * Determines the response size.
     * 
     * @return Response size
     */
    public long getResponseSize() {
        long size = 0;
        for (int i = 0; i < jpipDataList.size(); i++)
            size += jpipDataList.get(i).length;
        return size;
    }

    /**
     * Tells if the response completes the last request.
     * 
     * @return True, if the response is complete
     */
    public boolean isResponseComplete() {
        return (status == JPIPConstants.EOR_WINDOW_DONE || status == JPIPConstants.EOR_IMAGE_DONE);
    }

};
