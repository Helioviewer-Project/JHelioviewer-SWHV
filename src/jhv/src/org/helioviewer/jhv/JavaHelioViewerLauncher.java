package org.helioviewer.jhv;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * This class launches the main class JavaHelioViewer in a new process using the
 * JVM settings defined in the system property 'jvm.arguments'. It pipes the
 * output of the new JHelioviewer process to the current command line window and
 * kills the process if the launcher process terminates. This class should be
 * launchable by Java versions >= 1.2. Therefore it may contain deprecated
 * functions and it does not use type templates. For this reason the class
 * cannot use the apache logger. It is possible to pass command line arguments
 * to the launcher. The arguments are passed to the main JHelioviewer program.
 * 
 * @author Andre Dau
 * 
 */

// The following also checks the installed java version, so it must run with
// java 1.2, ie. warnings will be generated.
// So for Eclipse and development they will be suppressed and for the actual
// built all '@SuppressWarnings("all")' will
// be removed before compilation in the built script -- Helge Dietert
@SuppressWarnings("all")
public class JavaHelioViewerLauncher {

    /**
     * Listener for the Java too old dialog box in order to open the HTML link
     * and to close the window when the close or ok button is pressed
     */
    private static ListenerImpl listener;

    /** Option pane containing an error message when Java version is too old */
    private static JOptionPane errorPane;

    /**
     * Launches JavaHelioViewer.main(String[]) in a new vm with the parameters
     * specified in the config file
     * 
     * @param args
     */
    public static void main(String[] args) {
        start(JavaHelioViewer.class, args);
    }

    /**
     * Launches the main method of the given class in a new vm with the
     * parameters specified in the config file
     * 
     * @param mainClass
     *            the class which contains the main method to launch
     * @param args
     *            the command line args for the main method
     */
    public static void start(Class mainClass, String[] args) {
        // Get the specification version with the format
        // majorVersionNumber.minorVersionNumber
        String versionString = System.getProperty("java.specification.version");
        System.out.println("JavaHelioviewerLauncher > Detected Java version: " + versionString);

        // Check if version is high enough
        int firstDot = versionString.indexOf('.');
        int versionNum1 = Integer.parseInt(versionString.substring(0, firstDot));
        int secondDot = versionString.indexOf('.', firstDot + 1);
        if (secondDot < 0) {
            secondDot = versionString.length();
        }
        int versionNum2 = Integer.parseInt(versionString.substring(firstDot + 1, secondDot));

        // version too old
        if (versionNum1 < 1 || (versionNum1 == 1 && versionNum2 < 5)) {
            System.out.println("JavaHelioviewerLauncher > Java version too old to run JHelioviewer");
            JEditorPane messagePane = new JEditorPane("text/html", "Your Java version needs to be updated in order to use JHelioviewer.<br>" + "You can get the latest version at <br>" + "<a href=\"http://www.java.com/download\"> http://www.java.com/download </a><br>" + "Your Java version: " + versionString + "<br>" + "Required version: 1.5 or greater");

            messagePane.setEditable(false);
            messagePane.setOpaque(false);
            messagePane.validate();
            errorPane = new JOptionPane();
            errorPane.setMessage(messagePane);
            errorPane.setMessageType(JOptionPane.ERROR_MESSAGE);
            errorPane.setPreferredSize(new Dimension(400, 200));
            errorPane.validate();
            JDialog errorDialog = errorPane.createDialog(null, "Incompatible Java version!");
            errorDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

            listener = new ListenerImpl(errorPane);
            errorPane.addPropertyChangeListener(listener);
            messagePane.addHyperlinkListener(listener);
            errorDialog.pack();
            errorDialog.setVisible(true);
        } else {
            String argString = null;
            try {
                List commandLine = new LinkedList();

                // Path to java binary (should use the same with wich the
                // launcher was invoked)
                commandLine.add(System.getProperty("java.home") + System.getProperty("file.separator") + "bin" + System.getProperty("file.separator") + "java");

                // Initialize settings and variables
                System.out.println("JavaHelioviewerLauncher > Initialize settings and read argmuents for JVM");
                // Load settings without logging (since this class cannot use
                // the logger due to java 1.2 compatibility)
                Settings.getSingletonInstance().load(false);

                // Java class path is current class path
                String jvmArgs = "";
                File sourceLocation = null;
                try {
                    sourceLocation = new File(JavaHelioViewer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                } catch (URISyntaxException e) {
                    System.err.println("JavaHelioviewerLauncher > Error in classpath syntax");
                    e.printStackTrace();
                }
                jvmArgs += System.getProperty("java.class.path") + System.getProperty("path.separator") + sourceLocation;
                commandLine.add("-cp");
                commandLine.add(jvmArgs);
                jvmArgs = "-cp " + jvmArgs;
                String libArg = "-Djava.library.path=" + JHVDirectory.LIBS.getPath().substring(0, JHVDirectory.LIBS.getPath().length() - 1) + System.getProperty("path.separator") + System.getProperty("java.library.path");
                jvmArgs += " " + libArg;
                commandLine.add(libArg);
                String extArg = "-Djava.ext.dirs=\"\"";
                jvmArgs += " " + extArg;
                commandLine.add(extArg);

                // Mac OS
                if (System.getProperty("mrj.version") != null) {
                    commandLine.add("-Xdock:name=JHelioviewer");
                    jvmArgs += " \"" + "-Xdock:name=JHelioviewer" + "\"";
                    jvmArgs += " \"" + "-Xdock:icon=" + sourceLocation.getParentFile().getParentFile() + File.separator + "JHV_icon.icns\"";
                    commandLine.add("-Xdock:icon=" + sourceLocation.getParentFile().getParentFile() + File.separator + "JHV_icon.icns");
                }

                // Read JVM settings from properties file
                StringReader stringReader = new StringReader(Settings.getSingletonInstance().getProperty("jvm.arguments"));
                StreamTokenizer tokenizer = new StreamTokenizer(stringReader);
                tokenizer.eolIsSignificant(true);
                tokenizer.ordinaryChars(0x0023, 0x00ff);
                tokenizer.wordChars(0x0023, 0x00ff);
                while (tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
                    if (tokenizer.sval != null) {
                        commandLine.add(tokenizer.sval);
                        jvmArgs += " \"" + tokenizer.sval + "\"";
                    }
                }

                System.out.println("JavaHelioviewerLauncher > Start JHelioviewer with the following java virtual machine parameters:" + jvmArgs);

                // Pass arguments to process
                argString = "";// "--use-existing-log-time-stamp";
                for (int i = 0; i < args.length; ++i) {
                    argString += " \"" + args[i] + "\"";
                    commandLine.add(args[i]);
                }
                commandLine.add(mainClass.getName());
                System.out.println("JavaHelioviewerLauncher > Start JHelioviewer with the following command-line options:" + argString);

                String commandArray[] = new String[commandLine.size()];
                for (int i = 0; i < commandLine.size(); ++i) {
                    commandArray[i] = commandLine.get(i).toString();
                }
                final Process p = Runtime.getRuntime().exec(commandArray);
                Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                    public void run() {
                        p.destroy();
                    }
                }));
                logProcessOutput(p, "");
            } catch (IOException e) {
                System.err.println("JavaHelioviewerLauncher > Could not launch JHelioviewer");
                e.printStackTrace();
            }
        }

    }

    /**
     * Logs the output of a process and redirects it to stdout
     * 
     * @param process
     *            the process whose output should be logged
     * @param header
     *            Header which is displayed before each message
     * @throws IOException
     */
    static void logProcessOutput(final Process process, final String header) throws IOException {
        final BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
        final BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        Thread threadStdout = new Thread(new Runnable() {
            public void run() {
                try {
                    String line;
                    while ((line = stdout.readLine()) != null) {
                        System.out.println(header + line);
                    }
                } catch (IOException e) {
                    System.err.println(">> JavaHelioviewerLauncher.logProcessOutput(Process) >" + header + " Error while reading standard output");
                    e.printStackTrace();
                } finally {
                    try {
                        stdout.close();
                    } catch (IOException e) {
                        System.err.println(">> JavaHelioviewerLauncher.logProcessOutput(Process) >" + header + " Error while closing standard output stream");
                        e.printStackTrace();
                    }
                }
            }
        });
        Thread threadStderr = new Thread(new Runnable() {
            public void run() {
                try {
                    String line;
                    while ((line = stderr.readLine()) != null) {
                        System.out.println(header + "ERROR: " + line);
                    }
                } catch (IOException e) {
                    System.err.println(">> JavaHelioviewerLauncher.logProcessOutput(Process) >" + header + "Error while reading standard error");
                    e.printStackTrace();
                } finally {
                    try {
                        stderr.close();
                    } catch (IOException e) {
                        System.err.println(">> JavaHelioviewerLauncher.logProcessOutput(Process) >" + header + " Error while closing standard error stream");
                        e.printStackTrace();
                    }
                }
            }
        });

        threadStderr.start();
        threadStdout.start();
    }

}

/**
 * Class to open hyperlinks and closing the version error dialog when the close
 * or ok button is pressed.
 * 
 * @author Andre Dau
 * 
 */
// The following also checks the installed java version, so it must run with
// java 1.2, ie. warnings will be generated.
// So for Eclipse and development they will be suppressed and for the actual
// built all '@SuppressWarnings("all")' will
// be removed before compilation in the built script -- Helge Dietert
@SuppressWarnings("all")
class ListenerImpl implements HyperlinkListener, PropertyChangeListener {
    private JOptionPane errorPane;

    /**
     * Constructor
     * 
     * @param errorPane
     *            The option pane which contains the version too old error
     *            message
     */
    public ListenerImpl(JOptionPane errorPane) {
        this.errorPane = errorPane;
    }

    /**
     * {@inheritDoc}
     */
    public void hyperlinkUpdate(HyperlinkEvent event) {
        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            String url = event.getURL().toString();
            final String[] browsers = { "firefox", "opera", "konqueror", "epiphany", "seamonkey", "galeon", "kazehakase", "mozilla", "netscape" };
            String osName = System.getProperty("os.name");
            try {
                if (osName.startsWith("Mac OS")) {
                    Class fileMgr = Class.forName("com.apple.eio.FileManager");
                    Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] { String.class });
                    openURL.invoke(null, new Object[] { url });

                } else if (osName.startsWith("Windows")) {
                    Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
                } else { // assume Unix or Linux
                    boolean found = false;
                    for (int i = 0; i < browsers.length; ++i) {
                        String browser = browsers[i];
                        if (!found) {
                            Process p = Runtime.getRuntime().exec(new String[] { "which", browser });
                            JavaHelioViewerLauncher.logProcessOutput(p, ">> JavaHeliovViewerLauncher.hyperlinkUpdate(HyperliinkEvent) > which: ");
                            found = p.waitFor() == 0;
                            if (found) {
                                p = Runtime.getRuntime().exec(new String[] { browser, url });
                                JavaHelioViewerLauncher.logProcessOutput(p, ">> JavaHeliovViewerLauncher.hyperlinkUpdate(HyperliinkEvent) > browser: ");
                            }
                        }
                    }
                    if (!found) {
                        throw new Exception(Arrays.toString(browsers));
                    }
                }
            } catch (Exception e) {
                System.err.println("Could not open URL");
                e.printStackTrace();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() == errorPane) {
            Integer value = ((Integer) errorPane.getValue());
            if (value == null || value.intValue() == JOptionPane.OK_OPTION || value.intValue() == JOptionPane.CLOSED_OPTION) {
                System.exit(0);
            }
        }

    }

}
