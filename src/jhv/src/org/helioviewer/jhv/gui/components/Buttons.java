package org.helioviewer.jhv.gui.components;

import org.apache.commons.lang3.StringEscapeUtils;

public class Buttons {

    public static final String optionsDown = "<html>Options&nbsp;<font face='Material Design Icons' size=4>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_CHEVRON_DOWN.getCode()));
    public static final String optionsUp = "<html>Options&nbsp;<font face='Material Design Icons' size=4>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_CHEVRON_UP.getCode()));

    public static final String play = "<html><body style='width: 16px'><center><font face='Material Design Icons' size=5>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_PLAY.getCode()));
    public static final String pause = "<html><body style='width: 16px'><center><font face='Material Design Icons' size=5>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_PAUSE.getCode()));
    public static final String prev = "<html><body style='width: 16px'><center><font face='Material Design Icons' size=5>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_CHEVRON_LEFT.getCode()));
    public static final String next = "<html><body style='width: 16px'><center><font face='Material Design Icons' size=5>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_CHEVRON_RIGHT.getCode()));
    public static final String record = "<html><body style='width: 16px'><center><font face='Material Design Icons' color=#800000 size=5>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_RECORD.getCode()));

    public static final String newLayer = "<html><font face='Material Design Icons' size=4>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_PLUS_CIRCLE.getCode())) + "</font>&nbsp;New Layer";
    public static final String syncLayers = "<html>&nbsp;<font face='Material Design Icons' size=4>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_SYNC.getCode())) + "</font>&nbsp;";

    public static final String lock = "<html>&nbsp;<font face='Material Design Icons' size=4>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_LOCK.getCode())) + "</font>&nbsp;";
    public static final String unlock = "<html>&nbsp;<font face='Material Design Icons' size=4>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_LOCK_OPEN.getCode())) + "</font>&nbsp;";

    public static final String info = "<html>&nbsp;<font face='Material Design Icons' size=4>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_INFORMATION.getCode())) + "</font>&nbsp;";
    public static final String download = "<html>&nbsp;<font face='Material Design Icons' size=4>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_DOWNLOAD.getCode())) + "</font>&nbsp;";

    public static final String invert = "<html>&nbsp;<font face='Material Design Icons' size=4>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_INVERT_COLORS.getCode())) + "</font>&nbsp;";
    public static final String corona = "<html>&nbsp;<font face='Material Design Icons' size=4>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_WHITE_BALANCE_SUNNY.getCode())) + "</font>&nbsp;";

    public static final String calendar = "<html>&nbsp;<font face='Material Design Icons' size=4>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_CALENDAR.getCode())) + "</font>&nbsp;";

    public static final String plugOn = "<html>&nbsp;<font face='Material Design Icons' size=5>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_POWER_PLUG.getCode())) + "</font>&nbsp;";
    public static final String plugOff = "<html>&nbsp;<font face='Material Design Icons' size=5>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_POWER_PLUG_OFF.getCode())) + "</font>&nbsp;";

    // toolbar

    public static final String cutOut = "<html><font face='Material Design Icons' size=5>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_VECTOR_CIRCLE_VARIANT.getCode()));
    public static final String projection = "<html><font face='Material Design Icons' size=5>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_CUBE_OUTLINE.getCode()));
    public static final String offDisk = "<html><font face='Material Design Icons' size=5>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_WEATHER_SUNNY.getCode()));
    public static final String track = "<html><font face='Material Design Icons' size=5>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_CROSSHAIRS_GPS.getCode()));
    public static final String annotate = "<html><font face='Material Design Icons' size=5>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_SHAPE_POLYGON_PLUS.getCode()));
    public static final String rotate = "<html><font face='Material Design Icons' size=5>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_ROTATE_3D.getCode()));
    public static final String pan = "<html><font face='Material Design Icons' size=5>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_CURSOR_MOVE.getCode()));
    public static final String resetCamera = "<html><font face='Material Design Icons' size=5>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_CROSSHAIRS.getCode()));
    public static final String zoomOne = "<html><font face='Material Design Icons' size=5>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_PLUS_ONE.getCode()));
    public static final String zoomFit = "<html><font face='Material Design Icons' size=5>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_CROP_LANDSCAPE.getCode()));
    public static final String zoomOut = "<html><font face='Material Design Icons' size=5>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_MAGNIFY_MINUS.getCode()));
    public static final String zoomIn = "<html><font face='Material Design Icons' size=5>" +
        StringEscapeUtils.escapeHtml4(String.valueOf(MaterialDesign.MDI_MAGNIFY_PLUS.getCode()));

}
