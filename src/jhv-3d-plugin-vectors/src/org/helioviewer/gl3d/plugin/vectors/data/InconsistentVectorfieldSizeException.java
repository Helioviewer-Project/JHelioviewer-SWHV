package org.helioviewer.gl3d.plugin.vectors.data;

@SuppressWarnings("serial")
public class InconsistentVectorfieldSizeException extends Exception {

    public InconsistentVectorfieldSizeException(String path) {
        // TODO bessere Fehlermeldung!!
        super("The size of the images in the file " + path + " distinguishes from the size in the other files");
    }

}
