package org.helioviewer.viewmodelplugin.overlay;

/**
 * This class bundles all needed information of a visual control component for
 * an overlay.
 * 
 * @author Stephan Pagel
 */
public class OverlayControlComponent {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private OverlayPanel panel;
    private String title;

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    /**
     * Default constructor.
     * 
     * @param panel
     *            Panel which includes all control components of the overlay.
     * @param title
     *            User friendly title which will be displayed with the control
     *            component.
     */
    public OverlayControlComponent(OverlayPanel panel, String title) {
        this.panel = panel;
        this.title = title;
    }

    /**
     * Returns the control component of the overlay.
     * 
     * @return the control component of the overlay.
     */
    public OverlayPanel getOverlayPanel() {
        return panel;
    }

    /**
     * Returns the title for the control component.
     * 
     * @return the title for the control component.
     */
    public String getTitle() {
        return title;
    }
}
