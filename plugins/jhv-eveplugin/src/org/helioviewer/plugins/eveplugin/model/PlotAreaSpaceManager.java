package org.helioviewer.plugins.eveplugin.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlotAreaSpaceManager {
	private static PlotAreaSpaceManager instance;
	private Map<String, PlotAreaSpace> spaces;
	private List<PlotAreaSpaceListener> listensToAllPlotAreas;
	
	private PlotAreaSpaceManager (){
		this.spaces = new HashMap<String, PlotAreaSpace>();
		this.listensToAllPlotAreas = new ArrayList<PlotAreaSpaceListener>();
	}
	
	public static PlotAreaSpaceManager getInstance(){
		if (instance == null){
			instance = new PlotAreaSpaceManager();
		}
		return instance;
	}
	
	public PlotAreaSpace getPlotAreaSpace(String plotIdentifier){
		if (!spaces.containsKey(plotIdentifier)) {
			PlotAreaSpace newSpace = new PlotAreaSpace();
			for(PlotAreaSpaceListener l : listensToAllPlotAreas){
				newSpace.addPlotAreaSpaceListener(l);
			}
			spaces.put(plotIdentifier,newSpace);
		}
		return spaces.get(plotIdentifier);
	}
	
	public void addPlotAreaSpaceListenerToAllSpaces(PlotAreaSpaceListener listener){
		listensToAllPlotAreas.add(listener);
		for(PlotAreaSpace pas : spaces.values()){
			pas.addPlotAreaSpaceListener(listener);
		}
	}
	
	public Collection<PlotAreaSpace> getAllPlotAreaSpaces(){
		return spaces.values();
	}
}
