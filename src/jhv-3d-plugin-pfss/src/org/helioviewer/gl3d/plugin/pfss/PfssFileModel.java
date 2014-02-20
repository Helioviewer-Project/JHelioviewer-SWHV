package org.helioviewer.gl3d.plugin.pfss;

import java.io.File;
import java.io.IOException;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.plugin.pfss.data.IncorrectPfssFileException;
import org.helioviewer.gl3d.plugin.pfss.data.PfssDimension;
import org.helioviewer.gl3d.plugin.pfss.data.PfssReader;
import org.helioviewer.gl3d.scenegraph.GL3DGroup;
import org.helioviewer.gl3d.scenegraph.GL3DNode;

/**
 * Plugin Internal representation of a file based PFSS Model
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class PfssFileModel {
    private String filename;

    private File modelFile;

    private GL3DGroup root;

    private boolean loaded = false;

    public PfssFileModel(File file) {
        this.modelFile = file;
        this.filename = modelFile.getAbsolutePath();
    }

    public boolean load() {
        if (!this.isLoaded()) {

            // PfssImporter importer = new PfssImporter();
            //
            // try {
            // PfssDimension dim = importer.readPfssExport(this.filename);
            // this.root = new GL3DPfssModel(dim);
            //
            // this.loaded = true;
            // Log.debug("Loaded Pfss Model from "+this.filename);
            // return true;
            // } catch (IncorrectPfssFileException e) {
            // e.printStackTrace();
            // } catch (IOException e) {
            // e.printStackTrace();
            // }
            PfssReader importer = new PfssReader();

            try {
                PfssDimension dim = importer.readFile(this.filename);
                this.root = new GL3DPfssModel(dim);

                this.loaded = true;
                Log.debug("Loaded Pfss Model from " + this.filename);
                return true;
            } catch (IncorrectPfssFileException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public GL3DNode getRoot() {
        return this.root;
    }

    public boolean isLoaded() {
        return loaded;
    }
}
