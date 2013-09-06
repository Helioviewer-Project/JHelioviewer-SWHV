package org.helioviewer.viewmodel.renderer.physical;

import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.viewmodel.renderer.RenderGraphics;

/**
 * Interface for a renderer with basic graphical drawing functions.
 * 
 * <p>
 * An implementation of this interface will provided for every
 * {@link PhysicalRenderer}, so it can draw whatever needed.
 * 
 * <p>
 * All coordinates shall be given as physical coordinates, using kilometers as
 * unit. The reference point of each object is its center.
 * 
 * @author Stephan Pagel
 * @author Markus Langenberg
 * */
public interface PhysicalRenderGraphics extends RenderGraphics<Double, Vector2dDouble> {

}
