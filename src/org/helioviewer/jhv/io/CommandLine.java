package org.helioviewer.jhv.io;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.Settings;

public class CommandLine {

    private static final String usageMessage = """
            The following command-line options are available:

            -load    file location
                   Load or request a supported file at program start. The option can be used multiple times.

            -request request file location
                   Load a request file and issue a request at program start. The option can be used multiple times.

            -state   state file
                   Load state file.""";

    private static final Set<String> uriSchemes = Set.of("jpip", "jpips", "http", "https", "file");

    private static String[] arguments;

    public static void setArguments(String[] args) {
        arguments = args;
        // append state if user set in GUI, command line takes precedence
        String propState = Settings.getProperty("startup.loadState");
        if (propState != null && !"false".equals(propState)) {
            arguments = Arrays.copyOf(args, args.length + 2);
            arguments[args.length] = "-state";
            arguments[args.length + 1] = Path.of(propState).toUri().toString();
        }
    }

    public static void load() {
        // -load
        for (URI uri : getURIOptionValues("-load")) {
            Load.image.get(uri);
        }
    }

    // after DataSources is loaded
    public static void loadRequest() {
        // -request: works only for default server
        for (URI uri : getURIOptionValues("-request")) {
            Load.request.get(uri);
        }
        // -state
        for (URI uri : getURIOptionValues("-state")) {
            Load.state.get(uri);
            break;
        }
    }

    private static List<URI> getURIOptionValues(String param) {
        List<String> opts = getOptionValues(param);
        List<URI> uris = new ArrayList<>();
        for (String opt : opts) {
            try {
                URI uri = new URI(opt);
                String scheme = uri.getScheme();
                if (scheme != null)
                    scheme = scheme.toLowerCase();

                if (uriSchemes.contains(scheme)) {
                    uris.add(uri);
                } else {
                    Path path = Path.of(opt);
                    if (Files.isReadable(path)) {
                        uris.add(path.toUri()); // toUri() is correct
                    } else
                        Log.warn("File not found: " + opt);
                }
            } catch (Exception e) {
                Log.warn(e);
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
        List<String> values = new ArrayList<>();
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
