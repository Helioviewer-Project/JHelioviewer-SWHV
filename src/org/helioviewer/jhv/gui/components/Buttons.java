package org.helioviewer.jhv.gui.components;

import java.awt.Font;

import org.helioviewer.jhv.gui.UIGlobals;

public class Buttons {

    public static Font getMaterialFont(float size) {
        return UIGlobals.uiFontMDI.deriveFont(size);
    }

    public static final String close = MaterialDesign.CLOSE.toString();
    public static final String play = MaterialDesign.PLAY.toString();
    public static final String pause = MaterialDesign.PAUSE.toString();
    public static final String backward = MaterialDesign.STEP_BACKWARD.toString();
    public static final String forward = MaterialDesign.STEP_FORWARD.toString();
    public static final String record = MaterialDesign.RECORD.toString();
    public static final String check = MaterialDesign.CHECK.toString();

    public static final String chevronRight = "<html><font face='Material Design Icons' size=4>" +
            MaterialDesign.CHEVRON_RIGHT + "</font>&nbsp;";
    public static final String chevronDown = "<html><font face='Material Design Icons' size=4>" +
            MaterialDesign.CHEVRON_DOWN + "</font>&nbsp;";

    public static final String optionsRight = "<html>Options<font face='Material Design Icons' size=4>" +
            MaterialDesign.CHEVRON_RIGHT;
    public static final String optionsDown = "<html>Options<font face='Material Design Icons' size=4>" +
            MaterialDesign.CHEVRON_DOWN;

    public static final String adjustmentsRight = "<html>Adjustments<font face='Material Design Icons' size=4>" +
            MaterialDesign.CHEVRON_RIGHT;
    public static final String adjustmentsDown = "<html>Adjustments<font face='Material Design Icons' size=4>" +
            MaterialDesign.CHEVRON_DOWN;

    public static final String newLayer = "<html><font face='Material Design Icons' size=4>" +
            MaterialDesign.PLUS_CIRCLE + "</font>&nbsp;New Layer";
    public static final String syncLayers = "<html>&nbsp;<font face='Material Design Icons' size=4>" +
            MaterialDesign.SYNC + "</font>&nbsp;Sync";

    public static final String lock = "<html>&nbsp;<font face='Material Design Icons' size=4>" +
            MaterialDesign.LOCK + "</font>&nbsp;";
    public static final String unlock = "<html>&nbsp;<font face='Material Design Icons' size=4>" +
            MaterialDesign.LOCK_OPEN + "</font>&nbsp;";

    public static final String sync = button(MaterialDesign.SYNC);
    public static final String info = button(MaterialDesign.INFORMATION_VARIANT);
    public static final String download = button(MaterialDesign.DOWNLOAD);
    public static final String mgn = button(MaterialDesign.IMAGE_FILTER_HDR);

    public static final String invert = button(MaterialDesign.INVERT_COLORS);
    public static final String corona = button(MaterialDesign.WHITE_BALANCE_SUNNY);

    public static final String calendar = button(MaterialDesign.CALENDAR);
    public static final String skipBack = button(MaterialDesign.SKIP_BACKWARD);
    public static final String skipFore = button(MaterialDesign.SKIP_FORWARD);

    private static String button(MaterialDesign uc) {
        return "<html><span style='font-size:12px'>&nbsp;<font face='Material Design Icons'>" + uc + "</font>&nbsp;";
    }

    // toolbar

    private static String toolBar(MaterialDesign uc) {
        return "<html><center><font face='Material Design Icons' size=5>" + uc + "</font>";
    }

    public static final String annotate = toolBar(MaterialDesign.SHAPE_POLYGON_PLUS);
    public static final String axis = toolBar(MaterialDesign.BACKUP_RESTORE);
    public static final String cutOut = toolBar(MaterialDesign.VECTOR_CIRCLE_VARIANT);
    public static final String diffRotation = toolBar(MaterialDesign.CHART_GANTT);
    public static final String multiview = toolBar(MaterialDesign.BORDER_ALL);
    public static final String offDisk = toolBar(MaterialDesign.WEATHER_SUNNY);
    public static final String pan = toolBar(MaterialDesign.CURSOR_MOVE);
    public static final String projection = toolBar(MaterialDesign.CUBE_OUTLINE);
    public static final String refresh = toolBar(MaterialDesign.REFRESH);
    public static final String resetCamera = toolBar(MaterialDesign.IMAGE_FILTER_CENTER_FOCUS);
    public static final String resetCameraAxis = toolBar(MaterialDesign.DEBUG_STEP_OUT);
    public static final String rotate = toolBar(MaterialDesign.ROTATE_3D);
    public static final String rotate90 = toolBar(MaterialDesign.ROTATE_90);
    public static final String samp = toolBar(MaterialDesign.SHARE_VARIANT);
    public static final String track = toolBar(MaterialDesign.CROSSHAIRS_GPS);
    public static final String zoomFit = toolBar(MaterialDesign.CROP_LANDSCAPE);
    public static final String zoomIn = toolBar(MaterialDesign.MAGNIFY_PLUS);
    public static final String zoomOne = toolBar(MaterialDesign.PLUS_ONE);
    public static final String zoomOut = toolBar(MaterialDesign.MAGNIFY_MINUS);

}
