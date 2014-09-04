package org.helioviewer.plugins.eveplugin.lines.data;

/**
 * 
 * @author Stephan Pagel
 * */
public interface BandControllerListener {

    public void bandAdded(final Band band, final String identifier);

    public void bandRemoved(final Band band, final String identifier);

    public void bandUpdated(final Band band, final String identifer);

    public void bandGroupChanged(final String identifer);
}
