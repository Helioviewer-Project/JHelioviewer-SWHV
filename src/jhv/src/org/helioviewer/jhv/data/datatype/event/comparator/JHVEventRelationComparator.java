package org.helioviewer.jhv.data.datatype.event.comparator;

import java.util.Comparator;

import org.helioviewer.jhv.data.datatype.event.JHVEventRelation;

public class JHVEventRelationComparator implements Comparator<JHVEventRelation> {

    @Override
    public int compare(JHVEventRelation o1, JHVEventRelation o2) {
        return o1.getTheEvent().getStartDate().compareTo(o2.getTheEvent().getStartDate());
    }

}
