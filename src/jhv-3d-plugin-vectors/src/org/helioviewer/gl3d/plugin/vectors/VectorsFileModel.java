package org.helioviewer.gl3d.plugin.vectors;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import nom.tam.fits.FitsException;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.plugin.vectors.data.InconsistentVectorfieldSizeException;
import org.helioviewer.gl3d.plugin.vectors.data.ObservationDateMissingException;
import org.helioviewer.gl3d.plugin.vectors.data.VectorField;
import org.helioviewer.gl3d.plugin.vectors.data.VectorImporter;
import org.helioviewer.gl3d.plugin.vectors.data.filter.LengthVectorFieldFilter;
import org.helioviewer.gl3d.scenegraph.GL3DGroup;
import org.helioviewer.gl3d.scenegraph.GL3DNode;

/**
 * Internal representation of a file based vector field.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class VectorsFileModel {
    private String filename;

    private File modelFile;

    private GL3DGroup root;

    private boolean loaded = false;

    public VectorsFileModel(File file) {
        this.modelFile = file;
        this.filename = modelFile.getAbsolutePath();
    }

    public boolean load() {
        if (!this.isLoaded()) {
            List<String> files = new ArrayList<String>();
            files.add(this.filename);
            try {
                VectorField vectorfield = VectorImporter.loadFITSExport(files);

                this.root = new GL3DGroup(this.filename);

                this.filterVectorField(vectorfield);

                this.createVectorfield(vectorfield, this.root);

                this.loaded = true;
                Log.debug("VectorFileModel: Loaded Vector Model from " + this.filename);
                return true;
            } catch (FitsException e) {
                Log.error(e);
            } catch (ObservationDateMissingException e) {
                Log.error(e);
            } catch (InconsistentVectorfieldSizeException e) {
                Log.error(e);
            }
        }
        return false;
    }

    private void filterVectorField(VectorField vectorField) {
        LengthVectorFieldFilter lengthFilter = new LengthVectorFieldFilter();
        lengthFilter.filter(vectorField);
    }

    private void createVectorfield(VectorField vectorField, GL3DGroup root) {
        root.addNode(new GL3DVectorfield(vectorField));
    }

    public GL3DNode getRoot() {
        return this.root;
    }

    public boolean isLoaded() {
        return loaded;
    }
}
