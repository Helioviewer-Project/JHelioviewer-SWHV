package org.helioviewer.viewmodel.view;

import java.net.URI;
import java.util.Date;

import org.helioviewer.base.math.Interval;

/**
 * View to manage an image data source.
 * 
 * <p>
 * This view represents undermost view within the view chain. It should not be
 * possible to use another view as an input for this view.
 * 
 * <p>
 * Usually, it is expected to have at least one {@link RegionView}, one
 * {@link ViewportView} an one {@link MetaDataView} per path within the view
 * chain, so it might be a good idea to always implement them together with the
 * ImageInfoView, since every path starts at an ImageInfoView.
 * 
 * @author Ludwig Schmidt
 */
public interface ImageInfoView extends View {

    /**
     * Returns the URI representing the location of the image.
     * 
     * @return URI representing the location of the image.
     */
    public URI getUri();

    /**
     * Returns the name the image.
     * 
     * This might be the filename, but it can be something else extracted from
     * the meta data.
     * 
     * @return Name of the image
     */
    public String getName();

    /**
     * Returns, whether the image is a remote image (e.g. jpip).
     * 
     * @return true, if the image is accessed remotely, false otherwise
     */
    public boolean isRemote();

    /**
     * Returns the download uri the image.
     * 
     * This is the uri from which the whole file can be downloaded and stored
     * locally
     * 
     * @return download uri
     */
    public URI getDownloadURI();
    public Interval<Date> getDateRange();
    public void setDateRange(Interval<Date> range);  
}
