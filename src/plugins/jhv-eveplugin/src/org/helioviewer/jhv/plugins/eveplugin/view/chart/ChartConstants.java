package org.helioviewer.jhv.plugins.eveplugin.view.chart;

import java.awt.Color;
import java.awt.Font;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import org.helioviewer.jhv.gui.UIGlobals;

/**
 * @author Stephan Pagel
 * */
public class ChartConstants {

    private static final int GRAPH_LEFT_SPACE = 50;
    private static final int GRAPH_RIGHT_SPACE = 10;
    private static final int GRAPH_TOP_SPACE = 20;
    private static final int GRAPH_BOTTOM_SPACE = 22;
    private static final int TWO_AXIS_GRAPH_RIGHT = 30;

    private static final int MIN_VERTICAL_TICK_SPACE = 20;

    private static final int INTERVAL_SELECTION_HEIGHT = 30;
    private static final int RANGE_SELECTION_WIDTH = 15;

    public static final Color AVAILABLE_INTERVAL_BACKGROUND_COLOR = new Color(224, 224, 224);
    public static final Color SELECTED_INTERVAL_BACKGROUND_COLOR = Color.WHITE;
    public static final Color BORDER_COLOR = new Color(182, 190, 206);
    public static final Color GRASP_POINT_COLOR = new Color(0, 43, 109);

    public static final Color TICK_LINE_COLOR = Color.LIGHT_GRAY;
    public static final Color UNSELECTED_AREA_COLOR = new Color(200, 200, 200);
    public static final Color LABEL_TEXT_COLOR = Color.BLACK;

    public static final Color MOVIE_FRAME_COLOR = Color.BLACK;
    public static final Color MOVIE_INTERVAL_COLOR = Color.LIGHT_GRAY;

    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###,##0.00");

    public static final SimpleDateFormat FULL_DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd\nHH:mm:ss");
    public static final SimpleDateFormat FULL_DATE_TIME_FORMAT_NO_SEC = new SimpleDateFormat("yyyy-MM-dd\nHH:mm");
    public static final SimpleDateFormat FULL_DATE_TIME_FORMAT_REVERSE = new SimpleDateFormat("HH:mm:ss\nyyyy-MM-dd");

    public static final SimpleDateFormat HOUR_TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    public static final SimpleDateFormat HOUR_TIME_FORMAT_NO_SEC = new SimpleDateFormat("HH:mm");

    public static final SimpleDateFormat DAY_MONTH_YEAR_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat MONTH_YEAR_TIME_FORMAT = new SimpleDateFormat("MMM yyyy");
    public static final SimpleDateFormat YEAR_ONLY_TIME_FORMAT = new SimpleDateFormat("yyyy");

    public static int getGraphLeftSpace() {
        return GRAPH_LEFT_SPACE;
    }

    public static int getGraphRightSpace() {
        return GRAPH_RIGHT_SPACE;
    }

    public static int getGraphTopSpace() {
        return GRAPH_TOP_SPACE;
    }

    public static int getGraphBottomSpace() {
        return GRAPH_BOTTOM_SPACE;
    }

    public static int getTwoAxisGraphRight() {
        return TWO_AXIS_GRAPH_RIGHT;
    }

    public static int getMinVerticalTickSpace() {
        return MIN_VERTICAL_TICK_SPACE;
    }

    public static int getIntervalSelectionHeight() {
        return INTERVAL_SELECTION_HEIGHT;
    }

    public static int getRangeSelectionWidth() {
        return RANGE_SELECTION_WIDTH;
    }

    public static Font font = UIGlobals.UIFontSmall;

    public static final String absentText = "No band / diode / line selected";

}
