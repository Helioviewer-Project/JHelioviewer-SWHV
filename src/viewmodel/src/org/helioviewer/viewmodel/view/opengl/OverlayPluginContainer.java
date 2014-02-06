package org.helioviewer.viewmodel.view.opengl;

import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderer;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderer3d;

public class OverlayPluginContainer {
	
	private PhysicalRenderer renderer = null;
	private PhysicalRenderer3d renderer3d = null;
	private boolean postRender = true;
	
	public PhysicalRenderer getRenderer() {
		return renderer;
	}
	public void setRenderer(PhysicalRenderer renderer) {
		this.renderer = renderer;
	}
	public PhysicalRenderer3d getRenderer3d() {
		return renderer3d;
	}
	public void setRenderer3d(PhysicalRenderer3d renderer3d) {
		this.renderer3d = renderer3d;
	}
	
	public void setPostRender(boolean postRender){
		this.postRender = postRender;
	}
	
	public boolean getPostRender(){
		return this.postRender;
	}
}
