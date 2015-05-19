package org.helioviewer.jhv.plugins.eveplugin.lines.data;

/**
 * 
 * @author Stephan Pagel
 * */
public interface BandControllerListener {

    public void bandAdded(final Band band);

    public void bandRemoved(final Band band);

    public void bandUpdated(final Band band);

    public void bandGroupChanged();
}
