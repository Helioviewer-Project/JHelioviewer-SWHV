package org.helioviewer.jhv.io;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractList;
import java.util.LinkedList;

/**
 * A simple class to process command line arguments for JHelioviewer
 * specification.
 * 
 * @author Alen Alexanderian
 * @author Stephan Pagel
 * @author Andre Dau
 */
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
        setUsageMessage("The following command-line options are available: \n\n" +
        "-jpx   JPX_REQUEST_URL\n" + "       Allows users to pass a jpx request url for a jpx movie which will be opened upon program start. The option can be used multiple times." + "\n\n" +
        "       Example:\n" +
        "       -jpx \"http://helioviewer.nascom.nasa.gov/api/index.php?action=getJPX&observatory=SOHO&instrument=MDI&detector=MDI&measurement=magnetogram&startTime=2003-10-05T00:00:00Z&endTime=2003-10-20T00:00:00Z&cadence=3600&linked=true&jpip=true&frames=true\"" + "\n\n\n" +
        "-jpip  JPIP_URL\n" + "       Allows users to pass a jpip url of a JP2 or JPX image to be opened upon program start. The option can be used multiple times." +
        "\n\nExample:\n" +
        "       -jpip \"jpip://delphi.nascom.nasa.gov:8090/test/images/JP2_v20090917/2003_10_05__00_00_10_653__SOHO_EIT_EIT_195.jp2\"" + "\n\n\n" +
        "-download  URI_TO_FILE \n" + "       Allows the users to pass the location of JP2 or JPX image, which will be \n" +
        "       downloaded to a default location and opened when the program starts. This is specially useful \n" +
        "       for the case of large jpx files which will be very slow to play remotely." + "\n\n" +
        "       Example:\n" +
        "       -download \"http://delphi.nascom.nasa.gov/jp2/test/images/JP2_v20090917/2003_10_05__00_00_10_653__SOHO_EIT_EIT_195.jp2\"");
    }

    /**
     * Returns the values of the -jpx option.
     * 
     * @return list of values of the -jpx option as URLs
     */
    public static AbstractList<URI> getJPXOptionValues() {
        AbstractList<String> jpxURLs = getOptionValues("jpx");
        LinkedList<URI> result = new LinkedList<URI>();

        for (String jpxURL : jpxURLs) {
            if (!jpxURL.equals("")) {
                try {
                    result.add(new URI(jpxURL));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * Returns the values of the -jpip option
     * 
     * @return list of values of the -jpip option as URIs
     */
    public static AbstractList<URI> getJPIPOptionValues() {
        AbstractList<String> jpipURIs = getOptionValues("jpip");
        LinkedList<URI> uris = new LinkedList<URI>();

        for (String jpipURI : jpipURIs) {
            if (!jpipURI.equals("")) {
                try {
                    uris.add(new URI(jpipURI));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        return uris;
    }

    /**
     * Returns the values of the -download option.
     * 
     * @return list of value of the -download option as URIs
     */
    public static AbstractList<URI> getDownloadOptionValues() {
        AbstractList<String> addresses = getOptionValues("download");
        LinkedList<URI> uris = new LinkedList<URI>();
        for (String address : addresses) {
            if (!address.equals("")) {
                try {
                    uris.add(new URI(address));
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
    public static AbstractList<String> getOptionValues(String param) {
        param = "-" + param;
        LinkedList<String> values = new LinkedList<String>();

        if (arguments != null) {
            for (int i = 0; i < arguments.length; i++) {
                if (param.equals(arguments[i])) {
                    if (arguments.length > (i + 1)) {
                        values.add(arguments[i + 1]);
                    }
                }
            }
        }
        return values;
    }

    /**
     * Returns the command line usage message.
     * 
     * @return command line usage message.
     */
    public static String getUsageMessage() {
        return usageMessage;
    }

    /**
     * Sets the command line usage message.
     * 
     * @param string
     *            command line usage message.
     */
    public static void setUsageMessage(String string) {
        usageMessage = string;
    }

}
