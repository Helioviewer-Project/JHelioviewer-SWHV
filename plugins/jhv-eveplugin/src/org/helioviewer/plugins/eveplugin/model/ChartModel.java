package org.helioviewer.plugins.eveplugin.model;

import java.util.ArrayList;
import java.util.List;



public class ChartModel{
	
	private static ChartModel instance;
	private List<ChartModelListener> listeners;
	
	
	private ChartModel() {
		this.listeners = new ArrayList<ChartModelListener>();
	}
	
	public static ChartModel getSingletonInstance(){
		if(instance == null){
			instance = new ChartModel();
		}
		
		return instance;
	}
	
	public void addChartModelListener(ChartModelListener listener){
		this.listeners.add(listener);
	}
	
	public void removeChartModelListener(ChartModelListener listener){
		this.listeners.remove(listener);
	}
	
	public void chartRedrawRequest(){
		for (ChartModelListener l : listeners){
			l.chartRedrawRequested();
		}
	}
	
	
}
