package org.helioviewer.plugins.eveplugin.controller;

import java.awt.Color;

import org.helioviewer.plugins.eveplugin.settings.BandType;


/**
 * 
 * @author Stephan Pagel
 * */
public class Band {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////
    
    private final BandType bandType;
    
    private boolean isVisible = true;
    private Color graphColor = Color.BLACK;
    
    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////
    
    public Band(final BandType bandType) {
        this.bandType = bandType;
    }
    
    public final BandType getBandType() {
        return bandType;
    }
    
    public final String getTitle() {
        return bandType.getLabel();
    }
    
    public boolean isVisible() {
        return isVisible;
    }
    
    public void setVisible(final boolean visible) {
        isVisible = visible;
    }
    
    public void setGraphColor(final Color color) {
        graphColor = color;
    }
    
    public final Color getGraphColor() {
        return graphColor;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        
        if (!(obj instanceof Band))
            return false;
        
        return bandType.equals(((Band)obj).bandType);
    }
    
    @Override
    public int hashCode() {
        return bandType.hashCode();
    }
}
