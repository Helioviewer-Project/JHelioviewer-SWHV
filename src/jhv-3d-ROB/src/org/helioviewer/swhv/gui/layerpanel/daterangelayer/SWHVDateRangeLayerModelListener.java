package org.helioviewer.swhv.gui.layerpanel.daterangelayer;

import javax.swing.SwingUtilities;

import org.helioviewer.swhv.gui.layerpanel.SWHVLayerModelListener;

public interface SWHVDateRangeLayerModelListener extends SWHVLayerModelListener{
	public void beginDateChanged(SWHVDateRangeLayerModel model);
	public void endDateChanged(SWHVDateRangeLayerModel model);
	public void updateLevel(SWHVDateRangeLayerModel swhvDateRangeLayerModel);
	public void updateFolded(SWHVDateRangeLayerModel swhvDateRangeLayerModel);	
}
