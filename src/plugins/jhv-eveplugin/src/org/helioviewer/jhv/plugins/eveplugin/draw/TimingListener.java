package org.helioviewer.jhv.plugins.eveplugin.draw;

/**
 *
 * @author Stephan Pagel
 * */
public interface TimingListener {

    public void availableIntervalChanged();

    public void selectedIntervalChanged(boolean keepFullValueRange);
}
