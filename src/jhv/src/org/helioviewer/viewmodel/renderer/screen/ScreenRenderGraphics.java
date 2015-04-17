package org.helioviewer.viewmodel.renderer.screen;

import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.viewmodel.renderer.RenderGraphics;

/**
 * Interface for a renderer with basic graphical drawing functions.
 * 
 * <p>
 * An implementation of this interface will provided for every
 * {@link ScreenRenderer}, so it can draw whatever needed.
 * 
 * <p>
 * All coordinates shall be given as screen coordinates, using pixels as unit.
 * The reference point of each object is its upper left corner.
 * 
 * @author Stephan Pagel
 * @author Markus Langenberg
 * 
 */
public interface ScreenRenderGraphics extends RenderGraphics<Integer, Vector2dInt> {

}
