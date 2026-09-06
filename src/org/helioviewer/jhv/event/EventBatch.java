package org.helioviewer.jhv.event;

import java.util.List;

public record EventBatch(long sequence, List<SolarEvent> events, List<SolarEvent.Link> associations) {}
