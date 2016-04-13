package org.helioviewer.jhv.plugins.eveplugin.draw;

public interface TimingListener {

    public void availableIntervalChanged();

    public void selectedIntervalChanged(boolean keepFullValueRange);

}
