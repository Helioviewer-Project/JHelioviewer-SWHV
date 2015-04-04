package org.helioviewer.viewmodel.view;

/**
 * View to add additional overlays to the image.
 *
 * <p>
 * This view provides the capability to add overlays such as markers or other
 * graphical figures enhancing the image. To keep the overlays customizable, is
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
public interface OverlayView extends ViewListener {

}
