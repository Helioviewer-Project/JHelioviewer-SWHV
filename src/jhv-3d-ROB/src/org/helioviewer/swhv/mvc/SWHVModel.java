package org.helioviewer.swhv.mvc;

import org.helioviewer.swhv.gui.layerpanel.SWHVLayerModelListener;
import org.helioviewer.swhv.gui.layerpanel.SWHVModelListener;

public interface SWHVModel {
	public void addListener(SWHVModelListener listener);
	public void removeListener(SWHVModelListener listener);
}
