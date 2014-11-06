package org.helioviewer.jhv.plugins.swek.sources.comesep;

import java.awt.Color;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.swing.ImageIcon;

import org.helioviewer.jhv.data.datatype.event.JHVCoordinateSystem;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.datatype.event.JHVEventRelationship;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.JHVPositionInformation;

public class ComesepEvent implements JHVEvent {

    private Date startDate;
    private Date endDate;
    private ImageIcon icon;
    private String eventName;
    private String eventDisplayName;
    private String description;
    private List<JHVEventParameter> allParameters;
    private List<JHVEventParameter> allVisibleParameters;
    private List<JHVEventParameter> allVisibleNotNullParameter;
    private List<JHVEventParameter> allVisibleNullParameters;
    private List<JHVEventParameter> allNonVisibleParameters;
    private List<JHVEventParameter> allNonVisibleNotNullParameters;
    private List<JHVEventParameter> allNonVisibleNullParameters;
    private JHVEventType eventType;

    @Override
    public Date getStartDate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getEndDate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDisplayName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ImageIcon getIcon() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getShortDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<JHVEventParameter> getAllEventParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<JHVEventParameter> getVisibleEventParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<JHVEventParameter> getVisibleNotNullEventParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<JHVEventParameter> getVisibleNullEventParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<JHVEventParameter> getNonVisibleEventParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<JHVEventParameter> getNonVisibleNotNullEventParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<JHVEventParameter> getNonVisibleNullEventParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JHVEventType getJHVEventType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getUniqueID() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HashMap<JHVCoordinateSystem, JHVPositionInformation> getPositioningInformation() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Color getColor() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JHVEventRelationship getEventRelationShip() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void merge(JHVEvent event) {
        // TODO Auto-generated method stub

    }

}
