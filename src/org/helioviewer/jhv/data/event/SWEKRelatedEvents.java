package org.helioviewer.jhv.data.event;

import java.util.List;

public class SWEKRelatedEvents {

    private final SWEKGroup group;
    private final SWEKGroup relatedWith;
    private final List<SWEKRelatedOn> relatedOnList;

    public SWEKRelatedEvents(SWEKGroup _group, SWEKGroup _relatedWith, List<SWEKRelatedOn> _relatedOnList) {
        group = _group;
        relatedWith = _relatedWith;
        relatedOnList = _relatedOnList;
    }

    public SWEKGroup getGroup() {
        return group;
    }

    public SWEKGroup getRelatedWith() {
        return relatedWith;
    }

    public List<SWEKRelatedOn> getRelatedOnList() {
        return relatedOnList;
    }

}
