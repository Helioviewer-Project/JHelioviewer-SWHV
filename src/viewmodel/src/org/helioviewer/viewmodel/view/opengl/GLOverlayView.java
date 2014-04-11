package org.helioviewer.viewmodel.view.opengl;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.media.opengl.GL;

import org.helioviewer.base.logging.Log;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.RegionChangedReason;
import org.helioviewer.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.viewmodel.renderer.physical.GLPhysicalRenderGraphics;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderer;
import org.helioviewer.viewmodel.view.LayeredView;
import org.helioviewer.viewmodel.view.OverlayView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
/**
 * Implementation of OverlayView for rendering in OpenGL mode.
 * 
 * <p>
 * This class provides the capability to draw overlays in OpenGL. Therefore it
 * manages a {@link PhysicalRenderer}, which is passed to the registered
 * renderer.
 * 
 * <p>
 * For further information about the role of the OverlayView within the view
 * chain, see {@link org.helioviewer.viewmodel.view.OverlayView}.
 * 
 * @author Markus Langenberg
 */
public class GLOverlayView extends AbstractGLView implements OverlayView{

	private LayeredView layeredView;
	private CopyOnWriteArrayList<OverlayPluginContainer> overlays = new CopyOnWriteArrayList<OverlayPluginContainer>();
	
	/**
	 * {@inheritDoc}
	 */
	protected void setViewSpecificImplementation(View newView,
			ChangeEvent changeEvent) {
		layeredView = ViewHelper.getViewAdapter(view, LayeredView.class);
	}

	/**
	 * {@inheritDoc}
	 */
	public void renderGL(GL gl, boolean nextView) {
		// just for 2d
		renderChild(gl);
		
		if (nextView) {

			GLPhysicalRenderGraphics glRenderGraphics = new GLPhysicalRenderGraphics(gl, view);
			Iterator<OverlayPluginContainer> iterator = this.overlays.iterator();
			while(iterator.hasNext()){
				OverlayPluginContainer overlay = iterator.next();
				if (overlay.getRenderer() != null && (layeredView == null || layeredView.getNumLayers() > 0)){ 
					overlay.getRenderer().render(glRenderGraphics);
				}
			}
		}
	}

	public void postRender3D(GL gl) {
		GLPhysicalRenderGraphics glRenderGraphics = new GLPhysicalRenderGraphics(gl, view);
		Iterator<OverlayPluginContainer> iterator = this.overlays.iterator();
		
		while(iterator.hasNext()){
			OverlayPluginContainer overlay = iterator.next();
			if (overlay.getRenderer3d() != null && (layeredView == null || layeredView.getNumLayers() > 0) && overlay.getPostRender()){
				overlay.getRenderer3d().render(glRenderGraphics);
			}
		}
	}

	public void preRender3D(GL gl) {
		GLPhysicalRenderGraphics glRenderGraphics = new GLPhysicalRenderGraphics(gl, view);
		Iterator<OverlayPluginContainer> iterator = this.overlays.iterator();
		
		while(iterator.hasNext()){
			OverlayPluginContainer overlay = iterator.next();
			if (overlay.getRenderer3d() != null && (layeredView == null || layeredView.getNumLayers() > 0) && !overlay.getPostRender()){
				overlay.getRenderer3d().render(glRenderGraphics);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void viewChanged(View sender, ChangeEvent aEvent) {
		//Log.debug("viewChange: sender : " + sender);
		if (aEvent.reasonOccurred(RegionChangedReason.class)){
				
			GLLayeredView layeredView = sender.getAdapter(GLLayeredView.class);
			Iterator<OverlayPluginContainer> iterator = this.overlays.iterator();
			Log.debug("sender : " + sender);
			//Region region = sender.getAdapter(RegionView.class);
			while(iterator.hasNext()){
				OverlayPluginContainer overlay = iterator.next();
				if (overlay.getRenderer3d() != null){
					overlay.getRenderer3d().viewChanged(sender);
				}
			}
		}
		if (aEvent.reasonOccurred(ViewChainChangedReason.class)) {
			layeredView = ViewHelper.getViewAdapter(view, LayeredView.class);
		}

		super.viewChanged(sender, aEvent);
	}



	@Override
	public void addOverlay(OverlayPluginContainer overlayPluginContainer) {
		this.overlays.add(overlayPluginContainer);
	}

	@Override
	public void removeOverlay(int index) {
		// TODO Auto-generated method stub
		
	}

	@Override
	// Just implemented for exist plugin, for new one, pls don't use this function
	public void setRenderer(PhysicalRenderer renderer) {
		// TODO Auto-generated method stub
		OverlayPluginContainer overlayPluginContainer = new OverlayPluginContainer();
		overlayPluginContainer.setRenderer(renderer);
		this.overlays.add(overlayPluginContainer);
	}
	
	public View getView(){
		return view;
	}

	@Override
	public void setOverlays(CopyOnWriteArrayList<OverlayPluginContainer> overlays) {
        this.overlays = overlays;		
	}

	@Override
	public CopyOnWriteArrayList<OverlayPluginContainer> getOverlays() {
		return overlays;
	}
	
}
