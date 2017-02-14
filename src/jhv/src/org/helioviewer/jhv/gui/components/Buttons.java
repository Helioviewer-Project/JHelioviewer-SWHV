package org.helioviewer.jhv.gui.components;

import org.apache.commons.lang3.StringEscapeUtils;

public class Buttons {

    private static String uc2ent(MaterialDesign uc) {
        return StringEscapeUtils.escapeHtml4(String.valueOf(uc.getCode()));
    }

    public static final String chevronRight = "<html><font face='Material Design Icons' size=4>" +
        uc2ent(MaterialDesign.MDI_CHEVRON_RIGHT) + "</font>&nbsp;";
    public static final String chevronDown = "<html><font face='Material Design Icons' size=4>" +
        uc2ent(MaterialDesign.MDI_CHEVRON_DOWN) + "</font>&nbsp;";

    public static final String optionsRight = "<html>Options<font face='Material Design Icons' size=4>" +
        uc2ent(MaterialDesign.MDI_CHEVRON_RIGHT);
    public static final String optionsDown = "<html>Options<font face='Material Design Icons' size=4>" +
        uc2ent(MaterialDesign.MDI_CHEVRON_DOWN);

    public static final String play = "<html><body style='width: 16px'><center><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_PLAY);
    public static final String pause = "<html><body style='width: 16px'><center><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_PAUSE);
    public static final String prev = "<html><body style='width: 16px'><center><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_CHEVRON_LEFT);
    public static final String next = "<html><body style='width: 16px'><center><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_CHEVRON_RIGHT);
    public static final String record = "<html><body style='width: 16px'><center><font face='Material Design Icons' color=#800000 size=5>" +
        uc2ent(MaterialDesign.MDI_RECORD);

    public static final String newLayer = "<html><font face='Material Design Icons' size=4>" +
        uc2ent(MaterialDesign.MDI_PLUS_CIRCLE) + "</font>&nbsp;New Layer";
    public static final String syncLayers = "<html>&nbsp;<font face='Material Design Icons' size=4>" +
        uc2ent(MaterialDesign.MDI_SYNC) + "</font>&nbsp;";

    public static final String lock = "<html>&nbsp;<font face='Material Design Icons' size=4>" +
        uc2ent(MaterialDesign.MDI_LOCK) + "</font>&nbsp;";
    public static final String unlock = "<html>&nbsp;<font face='Material Design Icons' size=4>" +
        uc2ent(MaterialDesign.MDI_LOCK_OPEN) + "</font>&nbsp;";

    public static final String info = "<html>&nbsp;<font face='Material Design Icons' size=4>" +
        uc2ent(MaterialDesign.MDI_INFORMATION) + "</font>&nbsp;";
    public static final String download = "<html>&nbsp;<font face='Material Design Icons' size=4>" +
        uc2ent(MaterialDesign.MDI_DOWNLOAD) + "</font>&nbsp;";

    public static final String invert = "<html>&nbsp;<font face='Material Design Icons' size=4>" +
        uc2ent(MaterialDesign.MDI_INVERT_COLORS) + "</font>&nbsp;";
    public static final String corona = "<html>&nbsp;<font face='Material Design Icons' size=4>" +
        uc2ent(MaterialDesign.MDI_WHITE_BALANCE_SUNNY) + "</font>&nbsp;";

    public static final String calendar = "<html>&nbsp;<font face='Material Design Icons' size=4>" +
        uc2ent(MaterialDesign.MDI_CALENDAR) + "</font>&nbsp;";

    public static final String plugOn = "<html>&nbsp;<font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_POWER_PLUG) + "</font>&nbsp;";
    public static final String plugOff = "<html>&nbsp;<font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_POWER_PLUG_OFF) + "</font>&nbsp;";

    // toolbar

    public static final String cutOut = "<html><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_VECTOR_CIRCLE_VARIANT);
    public static final String projection = "<html><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_CUBE_OUTLINE);
    public static final String offDisk = "<html><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_WEATHER_SUNNY);
    public static final String track = "<html><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_CROSSHAIRS_GPS);
    public static final String annotate = "<html><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_SHAPE_POLYGON_PLUS);
    public static final String rotate = "<html><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_ROTATE_3D);
    public static final String pan = "<html><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_CURSOR_MOVE);
    public static final String resetCamera = "<html><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_CROSSHAIRS);
    public static final String zoomOne = "<html><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_PLUS_ONE);
    public static final String zoomFit = "<html><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_CROP_LANDSCAPE);
    public static final String zoomOut = "<html><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_MAGNIFY_MINUS);
    public static final String zoomIn = "<html><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_MAGNIFY_PLUS);

}
