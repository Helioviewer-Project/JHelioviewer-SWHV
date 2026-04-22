package org.helioviewer.jhv.io;

import java.awt.EventQueue;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.base.Regex;

public final class TransferLoad {

    private TransferLoad() {}

    public static void transferFileList(List<?> objects) {
        List<URI> imageUris = new ArrayList<>(objects.size());
        List<URI> jsonUris = new ArrayList<>(objects.size());
        List<URI> cdfUris = new ArrayList<>(objects.size());
        for (Object o : objects) {
            if (o instanceof File file)
                classifyFile(file, imageUris, jsonUris, cdfUris);
        }
        loadData(imageUris, jsonUris, cdfUris);
    }

    public static void transferStringArray(String loc) {
        String[] words = Regex.MultiCommaSpace.split(loc);

        List<URI> imageUris = new ArrayList<>(words.length);
        List<URI> jsonUris = new ArrayList<>(words.length);
        List<URI> cdfUris = new ArrayList<>(words.length);
        for (String word : words) {
            try {
                URI uri = new URI(word); // attempt to check if it's a URI
                if (uri.getScheme() == null) // maybe on filesystem
                    classifyFile(new File(word), imageUris, jsonUris, cdfUris);
                else
                    classifyUri(uri, imageUris, jsonUris, cdfUris);
            } catch (Exception e) {
                Log.warn("Not found: " + word, e);
            }
        }
        loadData(imageUris, jsonUris, cdfUris);
    }

    private static void classifyUri(URI uri, List<URI> imageUris, List<URI> jsonUris, List<URI> cdfUris) {
        String loc = uri.toString().toLowerCase();
        if (loc.endsWith(".json"))
            jsonUris.add(uri);
        else if (loc.endsWith(".cdf"))
            cdfUris.add(uri);
        else
            imageUris.add(uri);
    }

    private static void classifyFile(File file, List<URI> imageUris, List<URI> jsonUris, List<URI> cdfUris) {
        if (file.isFile() && file.canRead()) {
            classifyUri(file.toURI(), imageUris, jsonUris, cdfUris);
        } else if (file.isDirectory()) {
            try {
                FileUtils.listDir(file.toPath()).forEach(uri -> classifyUri(uri, imageUris, jsonUris, cdfUris));
            } catch (Exception e) {
                Log.warn("Error reading directory: " + file, e);
            }
        }
    }

    private static void loadData(List<URI> imageUris, List<URI> jsonUris, List<URI> cdfUris) {
        EventQueue.invokeLater(() -> {
            Load.cdf(cdfUris);
            Load.image(imageUris);
            Load.sunJSON(jsonUris);
        });
    }

}
