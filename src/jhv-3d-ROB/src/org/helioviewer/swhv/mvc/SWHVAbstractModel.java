package org.helioviewer.swhv.mvc;

import org.helioviewer.swhv.gui.layerpanel.SWHVLayerModelListener;
import org.helioviewer.swhv.gui.layerpanel.SWHVModelListener;

public abstract class SWHVAbstractModel implements SWHVModel{
	protected SWHVModelListener [] listenerPanel = new SWHVLayerModelListener[0];
	
	public void addListener(SWHVModelListener listener){
		//Always lock when changing the listeners array
		synchronized(this.listenerPanel){
			int len = this.listenerPanel.length;
			SWHVModelListener[] newListenerPanel = new SWHVModelListener[len + 1];
			for(int i = 0; i<len ; i++){
				newListenerPanel[i] = this.listenerPanel[i];
			}
			newListenerPanel[len] = listener;
			this.listenerPanel = newListenerPanel;
		}
	}
	
	public void removeListener(SWHVModelListener listener){
		//Always lock when changing the listeners array
		synchronized(this.listenerPanel){		
			int i=0;
			int len =  this.listenerPanel.length;
			while(i<len && listenerPanel[i]!=listener){
				i++;
			}
			if(0 <= i && i< len){
				if(len-1 >0){
					SWHVModelListener[] newListener = new SWHVModelListener[len -1];
					for(int j=0;j<i;j++){
						newListener[j] = this.listenerPanel[j];
					}
					for(int j=i;j<newListener.length;j++){
						newListener[j] = this.listenerPanel[j+1];
					}
					this.listenerPanel = newListener;
				}
				else{
					this.listenerPanel = new SWHVModelListener[0];
				}
			}
		}
	}
	
}
