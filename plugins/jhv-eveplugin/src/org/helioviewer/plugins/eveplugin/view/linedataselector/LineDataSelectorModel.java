package org.helioviewer.plugins.eveplugin.view.linedataselector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LineDataSelectorModel {
	private List<LineDataSelectorModelListener> listeners;
	private Map<String, List<LineDataSelectorElement>> elementMap;
	
	private static LineDataSelectorModel instance; 

	
	private LineDataSelectorModel() {
		listeners = Collections.synchronizedList(new ArrayList<LineDataSelectorModelListener>());
		elementMap = new HashMap<String, List<LineDataSelectorElement>>();
	}

	public static LineDataSelectorModel getSingletonInstance(){
		if(instance == null){
			instance = new LineDataSelectorModel();
		}
		return instance;
	}
	
	public void addLineDataSelectorModelListener(LineDataSelectorModelListener listener){
		listeners.add(listener);
	}
	
	public void removeLineDataSelectorModelListener(LineDataSelectorModelListener listener){
		listeners.remove(listener);
	}
	
	public void downloadStarted(LineDataSelectorElement element){
		fireDownloadStarted(element);
	}
	
	public void downloadFinished(LineDataSelectorElement element){
		fireDownloadFinished(element);
	}

	public int getNumberOfAvailableLineData(String identifier){
		synchronized(elementMap){
			List<LineDataSelectorElement> tempList = elementMap.get(identifier);		
			return tempList == null?0:tempList.size();
		}
	}
	
	public void addLineData(LineDataSelectorElement element){
		synchronized(elementMap){
			if (this.elementMap.containsKey(element.getPlotIdentifier())){
				this.elementMap.get(element.getPlotIdentifier()).add(element);
			}else{
				ArrayList<LineDataSelectorElement> tempList = new ArrayList<LineDataSelectorElement>();
				tempList.add(element);
				this.elementMap.put(element.getPlotIdentifier(), tempList);
			}		
			fireLineDataSelectorElementAdded(element);
		}
	}
	
	public List<LineDataSelectorElement> getAllLineDataSelectorElements(String identifier){
		synchronized(elementMap){
			return this.elementMap.get(identifier);
		}
	}

	public void removeLineData(LineDataSelectorElement element){
		synchronized(elementMap){
			this.elementMap.remove(element);
			fireLineDataSelectorElementRemoved(element);
		}
	}
	
	public void lineDataElementUpdated(LineDataSelectorElement element){
		synchronized(elementMap){
			fireLineDataSelectorElementUpdated(element);
		}
	}

	public void lineDataGroupChanged(){
		
	}
	
	public boolean atLeastOneDownloading(String identifier){
		synchronized(elementMap){
			for(LineDataSelectorElement el : elementMap.get(identifier)){
				if(el.isDownloading()){
					return true;
				}
			}
			return false;
		}
	}
	
	private synchronized void fireLineDataSelectorElementRemoved(
			LineDataSelectorElement element) {
		for(LineDataSelectorModelListener listener: listeners){
			listener.lineDataRemoved(element);
		}
		
	}
	
	private synchronized void fireLineDataSelectorElementAdded(
			LineDataSelectorElement element) {
		for(LineDataSelectorModelListener listener: listeners){
			listener.lineDataAdded(element);
		}
	}
	
	private synchronized void fireDownloadStarted(LineDataSelectorElement element) {
		for(LineDataSelectorModelListener listener: listeners){
			listener.downloadStartded(element);
		}
	}
	
	private synchronized void fireDownloadFinished(LineDataSelectorElement element) {
		for(LineDataSelectorModelListener listener: listeners){
			listener.downloadFinished(element);
		}
	}
	
	private synchronized void fireLineDataSelectorElementUpdated(
			LineDataSelectorElement element) {
		for(LineDataSelectorModelListener listener: listeners){
			listener.lineDataUpdated(element);
		}
	}
}
