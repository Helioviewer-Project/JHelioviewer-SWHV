package org.helioviewer.jhv.io;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

public class CommandLineProcessor {

    private static String[] arguments;
    private static String usageMessage;

    /**
     * Sets the arguments (text from command line) to this class.
     * 
     * @param args
     *            command line arguments.
     */
    public static void setArguments(String[] args) {
        arguments = args;
        usageMessage = "The following command-line options are available: \n\n" +
        "-jpx   JPX_REQUEST_URL\n" + "       Allows users to pass a JPX request URL for a JPX movie which will be opened upon program start. The option can be used multiple times." + "\n\n" +
        "-jpip  JPIP_URL\n" + "       Allows users to pass a JPIP URL of a JP2 or JPX image to be opened upon program start. The option can be used multiple times.";
    }

    /**
     * Returns the values of the -jpx option.
     * 
     * @return list of values of the -jpx option as URLs
     */
    public static List<URI> getJPXOptionValues() {
        List<String> jpxURLs = getOptionValues("jpx");
        LinkedList<URI> uris = new LinkedList<>();
        for (String jpxURL : jpxURLs) {
            if (!jpxURL.isEmpty()) {
                try {
                    URI uri = new URI(jpxURL);
                    if (uri.getScheme() == null) {
                        File f = new File(jpxURL).getAbsoluteFile();
                        if (f.canRead()) {
                            uris.add(f.toURI());
                        }
                    } else
                        uris.add(uri);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        return uris;
    }

    /**
     * Returns the values of the -jpip option
     * 
     * @return list of values of the -jpip option as URIs
     */
    public static List<URI> getJPIPOptionValues() {
        List<String> jpipURIs = getOptionValues("jpip");
        LinkedList<URI> uris = new LinkedList<>();
        for (String jpipURI : jpipURIs) {
            if (!jpipURI.isEmpty()) {
                try {
                    URI uri = new URI(jpipURI);
                    if ("jpip".equals(uri.getScheme()))
                        uris.add(uri);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        return uris;
    }

    /**
     * Checks whether a specific option is set
     * 
     * @param option
     *            option which to check for
     * @return true if the option was given as a command line argument, false
     *         else
     */
    public static boolean isOptionSet(String option) {
        for (String arg : arguments) {
            if (arg.equals(option)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method that looks for options in the command line.
     * 
     * @param param
     *            name of the option.
     * @return the values associated to the option.
     * */
    public static List<String> getOptionValues(String param) {
        param = '-' + param;
        LinkedList<String> values = new LinkedList<>();
        if (arguments != null) {
            for (int i = 0; i < arguments.length; i++) {
                if (param.equals(arguments[i]) && arguments.length > i + 1) {
                    values.add(arguments[i + 1]);
                }
            }
        }
        return values;
    }

    public static String getUsageMessage() {
        return usageMessage;
    }

}
