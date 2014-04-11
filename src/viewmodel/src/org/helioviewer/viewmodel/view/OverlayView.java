package org.helioviewer.viewmodel.view;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderer;
import org.helioviewer.viewmodel.view.opengl.OverlayPluginContainer;

/**
 * View to add additional overlays to the image.
 * 
 * <p>
 * This view provides the capability to add overlays such as markers or other
 * graphical figures enhancing the image. To keep the overlays customizable, a
 * {@link org.helioviewer.viewmodel.renderer.physical.PhysicalRenderer} is
 * required. The renderer will be called on every image data change within the
 * view chain, to update the overlays.
 * 
 * <p>
 * Note that the coordinate system used be the renderer is the same used for
 * within this point in the view chain (e.g. for solar images, the base unit is
 * kilometers).
 * 
 * @author Markus Langenberg
 * 
 */
public interface OverlayView extends ModifiableInnerViewView, ViewListener{

	public void addOverlay(OverlayPluginContainer overlayPluginContainer);
	public CopyOnWriteArrayList<OverlayPluginContainer> getOverlays();
	public void setOverlays(CopyOnWriteArrayList<OverlayPluginContainer> overlays);	
	public void removeOverlay(int index);
	/**
	 * Sets the renderer to draw overlays.
	 * 
	 * @param renderer
	 *            New renderer to draw overlays.
	 * @see #getRenderer
	 */
	public void setRenderer(PhysicalRenderer renderer);

	/**
	 * Returns the currently used renderer.
	 * 
	 * If there is currently no renderer present, return null.
	 * 
	 * @return renderer currently in use, null if there is none.
	 * @see #setRenderer
	 */


}
