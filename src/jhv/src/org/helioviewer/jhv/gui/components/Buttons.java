package org.helioviewer.jhv.gui.components;

import java.awt.Font;

import org.apache.commons.lang3.StringEscapeUtils;
import org.helioviewer.jhv.gui.UIGlobals;

public class Buttons {

    private static String uc2ent(MaterialDesign uc) {
        return StringEscapeUtils.escapeHtml4(String.valueOf(uc.code));
    }

    public static Font getMaterialFont(float size) {
        return UIGlobals.UIFontMDI.deriveFont(size);
    }

    public static final String close = String.valueOf(MaterialDesign.MDI_CLOSE.code);
    public static final String play = String.valueOf(MaterialDesign.MDI_PLAY.code);
    public static final String pause = String.valueOf(MaterialDesign.MDI_PAUSE.code);
    public static final String backward = String.valueOf(MaterialDesign.MDI_STEP_BACKWARD.code);
    public static final String forward = String.valueOf(MaterialDesign.MDI_STEP_FORWARD.code);
    public static final String record = String.valueOf(MaterialDesign.MDI_RECORD.code);

    public static final String chevronRight = "<html><font face='Material Design Icons' size=4>" +
        uc2ent(MaterialDesign.MDI_CHEVRON_RIGHT) + "</font>&nbsp;";
    public static final String chevronDown = "<html><font face='Material Design Icons' size=4>" +
        uc2ent(MaterialDesign.MDI_CHEVRON_DOWN) + "</font>&nbsp;";

    public static final String optionsRight = "<html>Options<font face='Material Design Icons' size=4>" +
        uc2ent(MaterialDesign.MDI_CHEVRON_RIGHT);
    public static final String optionsDown = "<html>Options<font face='Material Design Icons' size=4>" +
        uc2ent(MaterialDesign.MDI_CHEVRON_DOWN);

    public static final String newLayer = "<html><font face='Material Design Icons' size=4>" +
        uc2ent(MaterialDesign.MDI_PLUS_CIRCLE) + "</font>&nbsp;New Layer";
    public static final String syncLayers = "<html>&nbsp;<font face='Material Design Icons' size=4>" +
        uc2ent(MaterialDesign.MDI_SYNC) + "</font>&nbsp;";

    public static final String lock = "<html>&nbsp;<font face='Material Design Icons' size=4>" +
        uc2ent(MaterialDesign.MDI_LOCK) + "</font>&nbsp;";
    public static final String unlock = "<html>&nbsp;<font face='Material Design Icons' size=4>" +
        uc2ent(MaterialDesign.MDI_LOCK_OPEN) + "</font>&nbsp;";

    public static final String info = "<html>&nbsp;<font face='Material Design Icons' size=4>" +
        uc2ent(MaterialDesign.MDI_INFORMATION_VARIANT) + "</font>&nbsp;";
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

    public static final String cutOut = "<html><center><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_VECTOR_CIRCLE_VARIANT) + "</font>";
    public static final String projection = "<html><center><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_CUBE_OUTLINE) + "</font>";
    public static final String offDisk = "<html><center><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_WEATHER_SUNNY) + "</font>";
    public static final String track = "<html><center><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_CROSSHAIRS_GPS) + "</font>";
    public static final String annotate = "<html><center><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_SHAPE_POLYGON_PLUS) + "</font>";
    public static final String rotate = "<html><center><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_ROTATE_3D) + "</font>";
    public static final String pan = "<html><center><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_CURSOR_MOVE) + "</font>";
    public static final String resetCamera = "<html><center><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_CROSSHAIRS) + "</font>";
    public static final String zoomOne = "<html><center><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_PLUS_ONE) + "</font>";
    public static final String zoomFit = "<html><center><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_CROP_LANDSCAPE) + "</font>";
    public static final String zoomOut = "<html><center><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_MAGNIFY_MINUS) + "</font>";
    public static final String zoomIn = "<html><center><font face='Material Design Icons' size=5>" +
        uc2ent(MaterialDesign.MDI_MAGNIFY_PLUS) + "</font>";

}
