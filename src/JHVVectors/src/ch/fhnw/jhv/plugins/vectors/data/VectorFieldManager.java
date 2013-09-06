package ch.fhnw.jhv.plugins.vectors.data;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector2f;

import nom.tam.fits.FitsException;

import ch.fhnw.jhv.plugins.PluginManager;
import ch.fhnw.jhv.plugins.interfaces.ControlPlugin.ControlPluginType;
import ch.fhnw.jhv.plugins.vectors.control.InformationControlPlugin;
import ch.fhnw.jhv.plugins.vectors.data.filter.VectorFieldFilter;
import ch.fhnw.jhv.plugins.vectors.data.filter.VectorFieldFilterAverage;
import ch.fhnw.jhv.plugins.vectors.data.filter.VectorFieldFilterLength;
import ch.fhnw.jhv.plugins.vectors.data.filter.VectorFieldFilterSelection;
import ch.fhnw.jhv.plugins.vectors.data.importer.InconsistentVectorfieldSizeException;
import ch.fhnw.jhv.plugins.vectors.data.importer.ObservationDateMissingException;
import ch.fhnw.jhv.plugins.vectors.data.importer.VectorImporter;

/**
 * VectorFieldManager (SINGLETON)
 * 
 * Is responsible for all access on the vector fields. It also contains several
 * filters which are bound together in a filter chain. The filter are
 * manipulating the vector field.
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * @author David Hostettler (davidhostettler@gmail.com)
 * 
 */
public class VectorFieldManager {

    /**
     * Original vector field
     */
    private VectorField originalField;

    /**
     * Manipulated vector field
     */
    private VectorField manipulatedField;

    /**
     * Should use original field for manipulations
     */
    private boolean shouldUseOriginal = true;

    /**
     * Generates the textures for the 2D-Plane view
     */
    private TextureGenerator textureGenerator;

    /**
     * Listeners
     */
    private ArrayList<Listener> listeners = new ArrayList<VectorFieldManager.Listener>();

    /**
     * FILTERS
     */
    private VectorFieldFilter vectorFieldFilterAverage = new VectorFieldFilterAverage();
    private VectorFieldFilter vectorFieldFilterSelection = new VectorFieldFilterSelection();
    private VectorFieldFilter vectorFieldFilterLength = new VectorFieldFilterLength();

    /**
     * Amount of all vectors in the vector field
     */
    private int amountOfVectors = 0;

    /**
     * Amount of displayed vectors
     */
    private int amountOfDisplayedVectors = 0;

    /**
     * Min value of the shortest vector
     */
    private float minValVectorLength;

    /**
     * Max value of the longest vector
     */
    private float maxValVectorLength;

    /**
     * Holder of the only existing instance
     * 
     * @author Robin Oster (robin.oster@students.fhnw.ch)
     * 
     */
    private static class Holder {
        private static final VectorFieldManager INSTANCE = new VectorFieldManager();
    }

    /**
     * Get an instance of the VectorFieldManager
     * 
     * @return VectorFieldManager
     */
    public static VectorFieldManager getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Listener Interface
     * 
     * Used for getting updates about changes in the VectorFieldManager
     * 
     * @author Robin Oster (robin.oster@students.fhnw.ch)
     * 
     */
    public interface Listener {

        /**
         * Field loaded
         * 
         * @param vectorField
         */
        public void vectorFieldLoaded(VectorField vectorField);

        /**
         * Field adjusted
         * 
         * @param vectorField
         */
        public void vectorFieldAdjusted();
    }

    /**
     * Private constructor, for singleton
     */
    private VectorFieldManager() {

        // Define Filter chain
        vectorFieldFilterAverage.setNextFilter(vectorFieldFilterSelection);
        vectorFieldFilterSelection.setNextFilter(vectorFieldFilterLength);

        textureGenerator = new TextureGenerator();
    }

    /**
     * Determine the stats values of a given vector field and a specified time
     * value.
     * 
     * @param list
     *            list of VectorData
     */
    private void determineStats(ArrayList<VectorData> list) {

        float max = Float.MIN_VALUE;
        float min = Float.MAX_VALUE;

        for (VectorData vector : list) {
            if (vector != null) {
                if (max < vector.length)
                    max = vector.length;

                if (min > vector.length)
                    min = vector.length;
            }
        }

        minValVectorLength = min;
        maxValVectorLength = max;

        int counter = 0;
        for (VectorData vector : list) {
            if (vector != null)
                counter++;
        }

        amountOfDisplayedVectors = counter;
        amountOfVectors = (int) (this.originalField.sizePixel.x * this.originalField.sizePixel.y);
    }

    /**
     * Method to provide loading the vectorfield with 51 dimension exported from
     * IDL into a textfield.
     */
    public void loadIDLExample() {

        // A new vector field has been loaded. Calculate again the perfect
        // averaging factor.
        ((VectorFieldFilterAverage) vectorFieldFilterAverage).setCalculateAverageFactor(true);

        // LOAD FITS
        Vector2f sizeArcsec = new Vector2f(100, 410);
        Vector2f posArcsec = new Vector2f(170f, 430f);

        VectorField f = VectorImporter.loadIDLExport("vectordata.txt", 30, 256, 51);

        f.sizeArcsec = sizeArcsec;
        f.posArcsec = posArcsec;

        this.setOriginalField(f);
        this.setShouldUseOriginal(true);

        // set the original vectorfield on the TextureGenerator.
        // Use it later for generation of the texture
        textureGenerator.setVectorField(this.originalField);

        // notify all listeners
        for (Listener listener : listeners) {
            listener.vectorFieldLoaded(this.originalField);
        }
    }

    /**
     * Method to provide loading the vectorfield with 51 dimension exported from
     * IDL into a textfield.
     * 
     * @throws InconsistentVectorfieldSizeException
     * @throws ObservationDateMissingException
     */
    public void loadMediumExample() throws ObservationDateMissingException, InconsistentVectorfieldSizeException {

        // A new vector field has been loaded. Calculate again the perfect
        // averaging factor.
        ((VectorFieldFilterAverage) vectorFieldFilterAverage).setCalculateAverageFactor(true);

        ArrayList<String> paths = new ArrayList<String>();
        paths.add(VectorFieldManager.class.getResource("/fits/2011_03_04_H0600_x.fits").toString());

        VectorField f = null;

        try {
            f = VectorImporter.loadFITSExport(paths);
        } catch (FitsException e) {
            e.printStackTrace();
            return;
        }

        int oldWidth = (int) f.sizePixel.x;
        int oldHeight = (int) f.sizePixel.y;

        VectorData[] vectorsCopy = f.vectors[0];

        int multiply = 2;
        int height = 512 * multiply;
        int width = 512 * multiply;
        int size = width * height;

        f.vectors[0] = new VectorData[size];

        int z = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int originalIndex = (y % oldHeight) * oldHeight + x % oldWidth;
                f.vectors[0][z] = new VectorData();
                f.vectors[0][z].azimuth = vectorsCopy[originalIndex].azimuth;
                f.vectors[0][z].inclination = vectorsCopy[originalIndex].inclination;
                f.vectors[0][z].length = vectorsCopy[originalIndex].length;
                f.vectors[0][z].x = x + 0.5f;
                f.vectors[0][z].y = y + 0.5f;
                z++;
            }
        }

        f.sizeArcsec = new Vector2f(4000, 4000);
        f.posArcsec = new Vector2f(-1000, 1000);
        f.sizePixel = new Vector2f(width, height);

        this.setOriginalField(f);
        // this.setShouldUseOriginal(true);

        // set the original vectorfield on the TextureGenerator.
        // Use it later for generation of the texture
        textureGenerator.setVectorField(f);

        // notify all listeners
        for (Listener listener : listeners) {
            listener.vectorFieldLoaded(f);
        }
    }

    /**
     * Method to provide loading the vectorfield with 51 dimension exported from
     * IDL into a textfield.
     * 
     * @throws InconsistentVectorfieldSizeException
     * @throws ObservationDateMissingException
     */
    public void loadHugeExample() throws ObservationDateMissingException, InconsistentVectorfieldSizeException {

        // A new vector field has been loaded. Calculate again the perfect
        // averaging factor.
        ((VectorFieldFilterAverage) vectorFieldFilterAverage).setCalculateAverageFactor(true);

        ArrayList<String> paths = new ArrayList<String>();
        paths.add(VectorFieldManager.class.getResource("/fits/2011_03_04_H0600_x.fits").toString());

        VectorField f = null;
        try {
            f = VectorImporter.loadFITSExport(paths);
        } catch (FitsException e) {
            e.printStackTrace();
        }

        int oldWidth = (int) f.sizePixel.x;
        int oldHeight = (int) f.sizePixel.y;

        VectorData[] vectorsCopy = f.vectors[0];

        int multiply = 8;
        int height = 512 * multiply;
        int width = 512 * multiply;
        int size = width * height;

        f.vectors[0] = new VectorData[size];

        int z = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int originalIndex = (y % oldHeight) * oldHeight + x % oldWidth;
                f.vectors[0][z] = new VectorData();
                f.vectors[0][z].azimuth = vectorsCopy[originalIndex].azimuth;
                f.vectors[0][z].inclination = vectorsCopy[originalIndex].inclination;
                f.vectors[0][z].length = vectorsCopy[originalIndex].length;
                f.vectors[0][z].x = x + 0.5f;
                f.vectors[0][z].y = y + 0.5f;
                z++;
            }
        }

        f.sizeArcsec = new Vector2f(2000, 2000);
        f.posArcsec = new Vector2f(-1000, 1000);
        f.sizePixel = new Vector2f(width, height);

        this.setOriginalField(f);
        // this.setShouldUseOriginal(true);

        // set the original vectorfield on the TextureGenerator.
        // Use it later for generation of the texture
        textureGenerator.setVectorField(f);

        // notify all listeners
        for (Listener listener : listeners) {
            listener.vectorFieldLoaded(f);
        }
    }

    /**
     * This method loads a serie of FITS files. Every FITS file represents one
     * time dimension. The Observation Date is read from the fits files and used
     * to sort the time dimensions in the correct order.
     * 
     * @param paths
     *            A List of paths of the fits files
     * @param sizeArcSec
     *            Size of Vectorfield in Arcseconds
     * @param posArcSec
     *            Position of Vectorfield in Arcseconds
     * @throws ObservationDateMissingException
     *             If an Observation Date is not specified in FITS-Header the
     *             file can't be loaded, since it's impossible to know to which
     *             time dimension it belongs
     * @throws InconsistentVectorfieldSizeException
     *             If one of the Vectofields sizes distinguishes from the
     *             other's size it is not possible to display them as different
     *             time dimensions
     */
    public void loadVectorField(List<String> paths, Vector2f sizeArcSec, Vector2f posArcSec) throws ObservationDateMissingException, InconsistentVectorfieldSizeException, FitsException {

        // A new vector field has been loaded. Calculate again the perfect
        // averaging factor.
        ((VectorFieldFilterAverage) vectorFieldFilterAverage).setCalculateAverageFactor(true);

        VectorField f = VectorImporter.loadFITSExport(paths);

        f.sizeArcsec = sizeArcSec;
        f.posArcsec = posArcSec;

        this.setOriginalField(f);
        this.setShouldUseOriginal(true);

        textureGenerator.setVectorField(this.originalField);

        // notify all listeners
        for (Listener listener : listeners) {
            listener.vectorFieldLoaded(this.originalField);
        }
    }

    /**
     * Return the adapted vectorfield stored in an ArrayList of VectorData
     * Objects
     * 
     * @param index
     *            time dimension
     * 
     * @return ArrayList<VectorData>
     */
    public ArrayList<VectorData> getAdaptedVectorField(int index) {

        // FIXME check if time dimension exists!!!
        ArrayList<VectorData> list = new ArrayList<VectorData>();
        vectorFieldFilterAverage.filterVectorField(this.originalField, index, list);

        // determine the min and max value to this vector field
        // on the given index
        determineStats(list);

        // Send notification to the Information plugin
        InformationControlPlugin informationControlPlugin = (InformationControlPlugin) PluginManager.getInstance().getControlPluginByType(ControlPluginType.INFORMATION);
        informationControlPlugin.receiveInformations(minValVectorLength * 1 / getLengthScaleValue(), maxValVectorLength * 1 / getLengthScaleValue(), amountOfVectors, amountOfDisplayedVectors);

        return list;
    }

    /**
     * Notify all listeners that something has been adjusted
     */
    private void notifyListeners() {
        for (Listener listener : listeners) {
            listener.vectorFieldAdjusted();
        }
    }

    /**
     * Add a listener
     * 
     * @param listener
     *            Listener
     */
    public void addListener(Listener listener) {
        if (listeners.contains(listener)) {
            // TODO exception or ignore??
            System.out.println("Already contains that listener!");
        } else {
            listeners.add(listener);
        }
    }

    /**
     * Get a texture by time
     * 
     * @param time
     *            time dimension
     * 
     * @return
     */
    public BufferedImage getTextureByTime(int time) {
        if (textureGenerator != null) {
            return textureGenerator.receiveTexture(time);
        }

        return null;
    }

    /**
     * @param filterThresholdValue
     *            the filterThresholdValue to set
     */
    public void setFilterThresholdValue(double filterThresholdValue) {

        VectorFieldFilterSelection vffs = (VectorFieldFilterSelection) this.vectorFieldFilterSelection;
        vffs.setFilterThresholdValue(filterThresholdValue);

        notifyListeners();
    }

    /**
     * @param lengthScaleValue
     *            the lengthScaleValue to set
     */
    public void setLengthScaleValue(float lengthScaleValue) {
        VectorFieldFilterLength vffl = (VectorFieldFilterLength) this.vectorFieldFilterLength;

        vffl.setLengthScaleValue(lengthScaleValue);

        notifyListeners();
    }

    /**
     * @param averageFactor
     *            the averageFactor to set
     */
    public void setAverageFactor(int averageFactor) {

        VectorFieldFilterAverage vectorFieldAverage = (VectorFieldFilterAverage) vectorFieldFilterAverage;
        vectorFieldAverage.setAverageFactor(averageFactor);

        shouldUseOriginal = true;

        notifyListeners();
    }

    /**
     * @return the manipulatedField
     */
    public VectorField getManipulatedField() {
        return manipulatedField;
    }

    /**
     * @param originalField
     *            the originalField to set
     */
    public void setOriginalField(VectorField originalField) {
        this.originalField = originalField;
    }

    /**
     * Retrieve the original vector field
     * 
     * @return
     */
    public VectorField getOriginalField() {
        return this.originalField;
    }

    /**
     * Get ShouldUseOriginal
     * 
     * @return boolean ShouldUseOriginal
     */
    public boolean getShouldUseOriginal() {
        return this.shouldUseOriginal;
    }

    public void setShouldUseOriginal(boolean shouldUseOriginal) {
        this.shouldUseOriginal = shouldUseOriginal;
    }

    /**
     * Get lengthScalValue
     * 
     * @return double lengthScaleValue
     */
    public float getLengthScaleValue() {
        return ((VectorFieldFilterLength) vectorFieldFilterLength).getLengthScaleValue();
    }

    /**
     * Get filterThresholdValue
     * 
     * @return int filterThresholdValue
     */
    public int getFilterThresholdValue() {
        return (int) ((VectorFieldFilterSelection) vectorFieldFilterSelection).getFilterThresholdValue();
    }

    /**
     * Get average factor
     * 
     * @return int averageFactor
     */
    public int getAverageFactor() {
        return ((VectorFieldFilterAverage) vectorFieldFilterAverage).getAverageFactor();
    }
}
