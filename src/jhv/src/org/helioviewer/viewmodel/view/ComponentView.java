package org.helioviewer.viewmodel.view;

import java.awt.Component;
import java.io.File;
import java.io.IOException;

/**
 * View to draw the final image to a Component, thus should be the topmost view
 * of the view chain.
 *
 * <p>
 * A ComponentView is responsible for rendering the actual image to a Component.
 * The Component can an AWT component as well as a Swing component. If possible,
 * it is recommended to use a Swing component, because it interacts better with
 * the rest of the graphical user interface.
 *
 * <p>
 * It is not recommended to use a ComponentView as input for another view, since
 * its purpose is to display the result of all operations, that have taken place
 * within the view chain. But still, it is possible to use it in the middle of
 * the view chain, to display intermediate results, if desired.
 *
 * @author Stephan Pagel
 * @author Markus Langenberg
 *
 */
public interface ComponentView {

    /**
     * Deactivate the Component View can be used to clean up the component view
     * when this view chain, i.e. this Component View is not in use anymore
     *
     * @Author Simon Spoerri
     */
    public void deactivate();

    /**
     * Activate is called before the component view will be the active component
     * view displayed on the GUI.
     *
     * @Author Simon Spoerri
     */
    public void activate();

    /**
     * Sets the component where the image will be displayed.
     *
     * To see the result of the view chain, this component has to be build in
     * the graphical user interface somewhere. Apart from that, it is
     * recommended to connect some listeners to the component, if input feedback
     * is required.
     *
     */
    public void setComponent(Component component);

    /**
     * Saves the current screen content to the given file in the given format.
     *
     * @param imageFormat
     *            Desired output format
     * @param outputFile
     *            Desired output destination
     * @return
     * @throws IOException
     *             is thrown, if the given output file is not valid
     */
    public boolean saveScreenshot(String imageFormat, File outputFile);

}
