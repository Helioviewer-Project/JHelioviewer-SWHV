/**
 * 
 */
package org.helioviewer.plugins.eveplugin.radio.model;

import java.util.HashMap;
import java.util.Map;

import org.helioviewer.plugins.eveplugin.model.PlotAreaSpaceManager;


/**
 * Manages the YValueModels for the different plot-identifiers.
 * 
 * @author Bram.Bourgoignie@oma.be
 *
 */
public class YValueModelManager {
    /** The collection of YValueModels */
    private Map<String, YValueModel> yValueModels;
    
    /** The singleton instance */
    private static YValueModelManager instance;
    
    /** Instance of plot area manager */
    private PlotAreaSpaceManager pam;
    
    /**
     * private constructor
     */
    private YValueModelManager(){
        yValueModels = new HashMap<String, YValueModel>();
        this.pam = PlotAreaSpaceManager.getInstance();
    }
    
    
    /**
     * Access to the singleton instance.
     * 
     * @return  Gives the singleton object of the YValueModelManager.
     */
    public static YValueModelManager getInstance() {
        if(instance == null){
            instance = new YValueModelManager();
        }
        return instance;
    }
    
    /**
     * Gives the y-value model for the given plot identifier.
     * 
     * @param plotIdentifier    The plot identifier for which the model was requested
     * @return  The y-value model corresponding with the plot identifier
     */
    public YValueModel getYValueModel(String plotIdentifier) {
        if (yValueModels.containsKey(plotIdentifier)) {
            return yValueModels.get(plotIdentifier);
        } else {
            YValueModel newModel = new YValueModel(plotIdentifier);
            pam.getPlotAreaSpace(plotIdentifier).addPlotAreaSpaceListener(newModel);
            yValueModels.put(plotIdentifier,newModel);
            return newModel;
        }
    }
}
