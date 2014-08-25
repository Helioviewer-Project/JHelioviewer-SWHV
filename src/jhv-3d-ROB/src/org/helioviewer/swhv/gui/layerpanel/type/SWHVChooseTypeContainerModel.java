package org.helioviewer.swhv.gui.layerpanel.type;

import org.helioviewer.swhv.mvc.SWHVAbstractModel;

public class SWHVChooseTypeContainerModel extends SWHVAbstractModel{
	
	private static SWHVChooseTypeContainerModel singletonInstance = new SWHVChooseTypeContainerModel();
	private SWHVChooseTypeModel [] registeredModels = new SWHVChooseTypeModel[0];
	private SWHVChooseTypeContainerController controller; 
	
	private SWHVChooseTypeContainerModel(){}
	
	public SWHVChooseTypeModel[] getRegisteredModels() {
		return registeredModels;
	}

	private void setRegisteredModels(SWHVChooseTypeModel[] models) {
		this.registeredModels = models;
	}

	public void registerType(SWHVChooseTypeModel model){
		synchronized(registeredModels){	
			if( this.getRegisteredModels().length != 0 ){
				int len = this.getRegisteredModels().length;
				SWHVChooseTypeModel[] newModels = new SWHVChooseTypeModel[len + 1];
				for(int i = 0; i<len ; i++){
					newModels[i] = this.getRegisteredModels()[i];
				}
				newModels[len] = model;
				this.setRegisteredModels(newModels);
			}
			else{
				this.setRegisteredModels(new SWHVChooseTypeModel[1]);
				this.getRegisteredModels()[0] = model;
			}
			fireTypeRegistered();
		}
		
	}
	
	public void fireTypeRegistered(){
		synchronized(this.listenerPanel){
			for(int i=0; i<listenerPanel.length; i++){
				SWHVChooseTypeContainerModelListener listener = (SWHVChooseTypeContainerModelListener)listenerPanel[i];
				listener.typeRegistered(this);
			}
		}			
	}

	public static SWHVChooseTypeContainerModel getSingletonInstance() {
		return singletonInstance;
	}

	public void setController(SWHVChooseTypeContainerController controller) {
		this.controller = controller;
	}

}
