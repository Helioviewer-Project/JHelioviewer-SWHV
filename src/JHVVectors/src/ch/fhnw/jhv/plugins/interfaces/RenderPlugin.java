package ch.fhnw.jhv.plugins.interfaces;

import javax.media.opengl.GL;

/**
 * RenderPlugin Interface
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public interface RenderPlugin {

    /**
     * RenderPlugin Types
     * 
     * @author Robin Oster (robin.oster@students.fhnw.ch)
     * 
     */
    public enum RenderPluginType {
        SUN, PLANE, FIELDANIMATOR, CAMERACENTERPOINT, PFSSCURVES;
    }

    /**
     * Provide the GLCanvas
     * 
     * @param gl
     *            GLCanvas
     * @param currentTime
     *            current time position
     */
    void render(GL gl, float currentTime);

    /**
     * Get type
     * 
     * @return RenderPluginType
     */
    RenderPluginType getType();

    /**
     * Activate the RenderPlugin
     */
    public void activate();

    /**
     * Deactivate the RenderPlugin
     */
    public void deactivate();

    /**
     * Ask for the current status of the RenderPlugin. Is it active?
     * 
     * @return boolean is active
     */
    public boolean isActive();
}
