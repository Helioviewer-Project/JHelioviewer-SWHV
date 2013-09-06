package org.helioviewer.viewmodel.view.opengl;

import javax.media.opengl.GL;

import org.helioviewer.viewmodel.view.View;

/**
 * View for displaying images in OpenGL render mode.
 * 
 * <p>
 * To accelerate the rendering process, this application supports open GL. Since
 * the structure of rendering an OpenGL image differs from the rendering of Java
 * BufferedImages, a new view is necessary.
 * 
 * <p>
 * When using OpenGL, this view replaces the
 * {@link org.helioviewer.viewmodel.view.SubimageDataView}, since there OpenGL
 * does not use image data object. Instead, the topmost view calls the renderGL
 * function, so that every view can add its details to the scene or activate its
 * shaders. Also, every GLView has to call the renderGL function of its
 * successors (or children), if they are also GLViews. Otherwise, the GLView is
 * responsible for moving the incoming image data to the graphics card.
 * Therefore, the {@link GLTextureHelper} provides the function
 * {@link GLTextureHelper#renderImageDataToScreen(GL, org.helioviewer.viewmodel.region.Region, org.helioviewer.viewmodel.imagedata.ImageData)}
 * . That way, for drawing an OpenGL scene, the call traverses through the view
 * chain, calling everything needed for the final scene.
 * 
 * <p>
 * For further informations about how to use shaders, see
 * {@link org.helioviewer.viewmodel.view.opengl.shader}.
 * 
 * @author Markus Langenberg
 * 
 */
public interface GLView extends View {

    /**
     * Function to recursively traverse through the viewchain, calling OpenGL
     * rendering commands.
     * 
     * The function has to call its own commands as well as to call its OpenGL
     * successors or finish the OpenGL tree by calling
     * {@link GLTextureHelper#renderImageDataToScreen} for the incoming image
     * data object.
     * 
     * @param gl
     *            Valid reference to the current gl object
     */
    public void renderGL(GL gl);
}
