package org.helioviewer.jhv.io;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.log.Log;

public class CommandLine {

    private static String[] arguments;
    private static String usageMessage;

    public static void setArguments(String[] args) {
        arguments = args;
        usageMessage = "The following command-line options are available: \n\n" +
                "-load    file location\n" + "       Load or request a supported file at program start. The option can be used multiple times.\n\n" +
                "-request request file location\n" + "       Load a request file and issue a request at program start. The option can be used multiple times.\n\n" +
                "-state   state file\n" + "       Load state file.";
    }

    public static void load() {
        // -load
        for (URI uri : getURIOptionValues("-load")) {
            Load.image.get(uri);
        }
        // -state
        for (URI uri : getURIOptionValues("-state")) {
            Load.state.get(uri);
            break;
        }
    }

    // after DataSources is loaded
    public static void loadRequest() {
        // -request: works only for default server
        for (URI uri : getURIOptionValues("-request")) {
            Load.request.get(uri);
        }
    }

    private static List<URI> getURIOptionValues(String param) {
        List<String> opts = getOptionValues(param);
        List<String> schemes = List.of("jpip", "http", "https", "file");
        ArrayList<URI> uris = new ArrayList<>();
        for (String opt : opts) {
            try {
                URI uri = new URI(opt);
                if (schemes.contains(uri.getScheme())) {
                    uris.add(uri);
                } else {
                    File f = new File(opt).getAbsoluteFile();
                    if (f.canRead()) {
                        uris.add(f.toURI());
                    } else
                        Log.error("File not found: " + opt);
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return uris;
    }

    /**
     * Method that looks for options in the command line.
     *
     * @param param name of the option.
     * @return the values associated to the option.
     */
    private static List<String> getOptionValues(String param) {
        ArrayList<String> values = new ArrayList<>();
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
