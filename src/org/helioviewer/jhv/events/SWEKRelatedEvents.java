package org.helioviewer.jhv.events;

import java.util.List;

public record SWEKRelatedEvents(SWEKGroup group, SWEKGroup relatedWith, List<SWEKRelatedOn> relatedOnList) {
}
