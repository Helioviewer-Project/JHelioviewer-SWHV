package org.helioviewer.jhv;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.components.StatusPanel;
import org.helioviewer.jhv.gui.components.StatusPanel.StatusTextListener;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.jhv.opengl.GLInitPanel;

/**
 * Represents the splash screen which will be displayed when program is
 * starting.
 * 
 * The splash screen manages a progress bar and a label, representing the
 * current state of starting JHV. It is connected to
 * {@link org.helioviewer.jhv.gui.components.StatusPanel}, so every call to
 * {@link org.helioviewer.jhv.gui.components.StatusPanel#setStatusInfoText(String)}
 * results in updating the splash screen to. This behavior is useful for
 * plugins.
 * 
 * @author Stephan Pagel
 */
public class JHVSplashScreen extends JFrame implements StatusTextListener {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    private static final Dimension splashScreenSize = new Dimension(400, 215);

    private static JHVSplashScreen instance = new JHVSplashScreen();

    private SplashImagePanel imagePanel = new SplashImagePanel();
    private JProgressBar progressBar = new JProgressBar(0, 100);

    private int steps = 1;
    private int currentStep = 1;

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    /**
     * Default constructor.
     * */
    private JHVSplashScreen() {

        // initialize the frame itself
        initFrame();

        // initialize the visual components
        initVisualComponents();

        // show the splash screen
        setVisible(true);
    }

    /**
     * Method returns the sole instance of this class.
     * 
     * @return the only instance of this class.
     * */
    public static JHVSplashScreen getSingletonInstance() {
        return instance;
    }

    /**
     * Initializes the dialog controller itself.
     * */
    private void initFrame() {
        setTitle("ESA JHelioviewer");
        setSize(splashScreenSize);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setFocusable(false);
        setResizable(false);
        setUndecorated(true);
    }

    /**
     * Initializes all visual components on the controller.
     * */
    private void initVisualComponents() {
        progressBar.setValue(0);
        progressBar.setPreferredSize(new Dimension(progressBar.getWidth(), 15));
        imagePanel.setText("");

        add(imagePanel, BorderLayout.CENTER);
        add(progressBar, BorderLayout.SOUTH);
    }

    /**
     * Adds a OpenGL component to the form to get information about the OpenGL
     * version on the machine and creates the main view chain
     * */
    public void initializeViewchain() {
        imagePanel.setText("Starting OpenGL...");
        nextStep();
        StatusPanel.addStatusTextListener(this);
        if (GLInfo.glIsUsable()) {
            try {
                add(new GLInitPanel(), BorderLayout.NORTH);
            } catch (Throwable t) {
                Log.error("Could not load OpenGL", t);
                GLInfo.glUnusable();
                GLInitPanel.startViewChainThread();
            }
        } else {
            GLInitPanel.startViewChainThread();
        }

        if (!GLInfo.glIsUsable()) {
            Message.err("Could not initialize OpenGL", "OpenGL could not be initialized properly during startup. JHelioviewer will start in Software Mode. For detailed information please read the log output. ", false);
        }
        validate();
    }

    /**
     * Sets the number of main progress steps. The lowest allowed value is 1.
     * 
     * @param steps
     *            number of steps.
     */
    public void setProgressSteps(int steps) {

        if (steps >= 1) {
            this.steps = steps;
            progressBar.setMaximum(steps * 100);
        }
    }

    /**
     * Sets the current main progress step. Future changes to the progress bar
     * value will be made inside the range of this step. The lowest allowed
     * value is 1 and the highest value is the number of main progress steps.
     * 
     * @param step
     *            current main progress step.
     */
    public void setCurrentStep(int step) {

        if (step >= 1 && step <= steps) {
            this.currentStep = step - 1;

            progressBar.setValue(currentStep * 100);
        }
    }

    /**
     * Returns the current main progress step.
     * 
     * @return current main progress step.
     */
    public int getCurrentStep() {
        return currentStep + 1;
    }

    /**
     * Increments the current main progress step by one.
     */
    public void nextStep() {

        if (currentStep + 1 < steps) {
            currentStep++;

            progressBar.setValue(currentStep * 100);
        }
    }

    /**
     * Sets the value of the progress bar which is displayed on the splash
     * screen. The value must be between 0 and 100 otherwise it will be ignored.
     * 
     * @param value
     *            new value for the progress bar.
     * */
    public void setProgressValue(int value) {

        if (value >= 0 && value <= 100)
            progressBar.setValue(currentStep * 100 + value);
    }

    /**
     * Sets the text which gives information about what actually happens. The
     * text will be displayed above the progress bar. If the passed value is
     * null nothing will happen.
     * 
     * @param text
     *            new text which shall be displayed.
     * */
    public void setProgressText(String text) {

        if (text != null) {
            imagePanel.setText(text);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void statusTextChanged(String newStatusText) {
        if (newStatusText.length() > 0) {
            setProgressText(newStatusText);
            setProgressSteps(steps + 1);
            nextStep();
        }
    }

    /**
     * Returns a progress bar object. The values which will be set to this
     * progress bar will be mapped to the progress bar which is displayed on the
     * splash screen.
     * 
     * @return progress bar object.
     * */
    public JProgressBar getProgressBar() {

        JProgressBar progressBar = new JProgressBar();

        progressBar.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                int value = ((JProgressBar) e.getSource()).getValue();
                int max = ((JProgressBar) e.getSource()).getMaximum();

                setProgressValue((int) ((float) value / (float) max * 100.0f));
            }
        });

        return progressBar;
    }

    /**
     * Returns the label which displays the current information text.
     * 
     * @return label instance which is displayed in the splash screen.
     * */
    public JLabel getInfoLabel() {
        return imagePanel.getLabel();
    }

    /**
     * The panel acts as container which displays the splash screen image and
     * position the label which displays the current status information.
     * 
     * @author Stephan Pagel
     * */
    private class SplashImagePanel extends JPanel {

        // ////////////////////////////////////////////////////////////
        // Definitions
        // ////////////////////////////////////////////////////////////

        private static final long serialVersionUID = 1L;

        private BufferedImage image = IconBank.getImage(JHVIcon.SPLASH);
        private JLabel label = new JLabel("");

        // ////////////////////////////////////////////////////////////
        // Methods
        // ////////////////////////////////////////////////////////////

        /**
         * Default constructor.
         * */
        public SplashImagePanel() {

            // set basic layout
            setLayout(null);

            // set size of panel
            if (image != null) {
                setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
                setSize(image.getWidth(), image.getHeight());
            } else {
                setPreferredSize(new Dimension(400, 200));
                setSize(400, 200);
            }

            // set label for displaying status information
            label.setOpaque(false);
            label.setBounds(2, this.getHeight() - 20, 396, 20);
            label.setForeground(Color.WHITE);
            add(label);
        }

        /**
         * Sets the information text of the current status.
         * 
         * @param text
         *            text which shall be displayed.
         */
        public void setText(String text) {
            label.setText(text);
        }

        /**
         * Returns the instance of the label which displays the current status
         * information.
         * 
         * @return label object which displays the current status information.
         * */
        public JLabel getLabel() {
            return label;
        }

        /**
         * Draws the splash screen image on the panel. If the image is not
         * available nothing will happen.
         * 
         * @param g
         *            Graphics object where image shall be drawn.
         */
        protected void paintComponent(Graphics g) {

            if (image != null)
                g.drawImage(image, 0, 0, null);
        }
    }
}
