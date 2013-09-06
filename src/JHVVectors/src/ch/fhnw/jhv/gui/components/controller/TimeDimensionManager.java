package ch.fhnw.jhv.gui.components.controller;

/**
 * This class manages the time dependent data. The purpose of this class is
 * providing an Interface for plugins that can register data to be displayed.
 * 
 * Currently a plugin can call addTimedimensions to register as a listener and
 * specify the count of dimensions it has to render.
 * 
 * 
 * @author David Hostettler (davidhostettler@gmail.com)
 * 
 *         09.08.2011
 */
public class TimeDimensionManager {

    /**
     * Holder of Singleton Instance
     * 
     * @author David Hostettler (davidhostettler@gmail.com)
     * 
     *         09.08.2011
     */
    private static class Holder {
        private static final TimeDimensionManager INSTANCE = new TimeDimensionManager();
    }

    /**
     * Get an instance of the TimeDependetDataManager
     * 
     * @return VectorFieldManager
     */
    public static TimeDimensionManager getInstance() {
        return Holder.INSTANCE;
    }

    private AnimatorController animator;
    private AnimatorController.Listener currentListener;

    private TimeDimensionManager() {

    }

    public void setAnimatorController(AnimatorController animatorController) {
        animator = animatorController;
    }

    /**
     * Plugins call this method to specify the amount of dimensions they can
     * register. By registring with this method, the application can initialize
     * the MoviePanel.
     * 
     * @param listener
     *            Listener will receive events like play and pause from the
     *            moviepanel
     * @param timeDimensions
     *            count of timedimensions to be rendered
     */
    public void addTimedimensions(AnimatorController.Listener listener, int timeDimensions) {
        if (animator != null) {
            if (currentListener != null) {
                animator.removeListener(currentListener);
            }
            animator.addListener(listener);
            animator.setTimeDimensionCount(timeDimensions);
            this.currentListener = listener;
        }
    }

    /**
     * resets the movie panel
     */
    public void unloadLayers() {
        if (currentListener != null) {
            animator.removeListener(currentListener);
        }

        animator.setTimeDimensionCount(0);
    }
}
