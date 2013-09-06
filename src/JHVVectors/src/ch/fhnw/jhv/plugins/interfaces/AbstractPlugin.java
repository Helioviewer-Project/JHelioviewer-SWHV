/**
 * 
 */
package ch.fhnw.jhv.plugins.interfaces;

/**
 * Abstact Plugin class provides several methods for the plugins
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class AbstractPlugin {

    /**
     * Is the plugin currently active
     */
    protected boolean active = false;

    /**
     * Activate
     */
    public void activate() {
        active = true;
    }

    /**
     * Deactivate
     */
    public void deactivate() {
        active = false;
    }

    /**
     * Is the plugin active
     * 
     * @return boolean active
     */
    public boolean isActive() {
        return this.active;
    }

    /**
     * Empty Disable implementation
     * 
     * @param enabled
     *            is it enabled or not
     */
    public void setEnabled(boolean enabled) {
        // empty default implementation
    }
}
