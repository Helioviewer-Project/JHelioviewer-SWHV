package org.helioviewer.plugins.eveplugin.radio.data;

public interface RadioImageCacheListener {
	public abstract void imageRemoved(Long ID);
	public abstract void imageAdded(Long ID);
}
