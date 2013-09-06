package org.helioviewer.viewmodel.view;

/**
 * View to scale the image to the final viewport size.
 * 
 * <p>
 * Usually, the view chain keeps the image in its native resolution as long as
 * possible, to avoid side effects. If this behavior is not desired, a
 * ScaleToViewportImageSizeView can be used.
 * 
 * <p>
 * For further information about hot to scale images, see {@link ScalingView}
 * 
 * @author Ludwig Schmidt
 * 
 */
public interface ScaleToViewportImageSizeView extends ScalingView, ModifiableInnerViewView {

}
