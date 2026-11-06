package org.helioviewer.jhv.timelines.draw;

import java.text.DecimalFormat;

import org.helioviewer.jhv.math.MathUtils;

public class DrawConstants {

    public static final int GRAPH_LEFT_SPACE = 50;
    public static final int GRAPH_RIGHT_SPACE = 10;
    public static final int GRAPH_TOP_SPACE = 20;
    public static final int GRAPH_BOTTOM_SPACE = 8;
    public static final int GRAPH_BOTTOM_AXIS_SPACE = 18;
    public static final int RIGHT_AXIS_WIDTH = 30;

    public static final int INTERVAL_SELECTION_HEIGHT = 20;
    public static final int RANGE_SELECTION_WIDTH = 15;

    public static final int EVENT_OFFSET = 3;

    public static final DecimalFormat valueFormatter = MathUtils.numberFormatter("0", 4);

}
