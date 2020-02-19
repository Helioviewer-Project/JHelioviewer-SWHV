package org.helioviewer.jhv.gui.components;

import java.awt.Font;

import org.helioviewer.jhv.gui.UIGlobals;

public class Buttons {

    private static String uc2ent(MaterialDesign... uc) {
        StringBuilder out = new StringBuilder();
        for (MaterialDesign c : uc)
            out.append(c.code);
        return out.toString();
    }

    public static Font getMaterialFont(float size) {
        return UIGlobals.uiFontMDI.deriveFont(size);
    }

    public static final String close = String.valueOf(MaterialDesign.MDI_CLOSE.code);
    public static final String play = String.valueOf(MaterialDesign.MDI_PLAY.code);
    public static final String pause = String.valueOf(MaterialDesign.MDI_PAUSE.code);
    public static final String backward = String.valueOf(MaterialDesign.MDI_STEP_BACKWARD.code);
    public static final String forward = String.valueOf(MaterialDesign.MDI_STEP_FORWARD.code);
    public static final String record = String.valueOf(MaterialDesign.MDI_RECORD.code);
    public static final String check = String.valueOf(MaterialDesign.MDI_CHECK.code);

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
            uc2ent(MaterialDesign.MDI_SYNC) + "</font>&nbsp;Sync";

    public static final String lock = "<html>&nbsp;<font face='Material Design Icons' size=4>" +
            uc2ent(MaterialDesign.MDI_LOCK) + "</font>&nbsp;";
    public static final String unlock = "<html>&nbsp;<font face='Material Design Icons' size=4>" +
            uc2ent(MaterialDesign.MDI_LOCK_OPEN) + "</font>&nbsp;";

    public static final String info = button(MaterialDesign.MDI_INFORMATION_VARIANT);
    public static final String download = button(MaterialDesign.MDI_DOWNLOAD);

    public static final String invert = button(MaterialDesign.MDI_INVERT_COLORS);
    public static final String corona = button(MaterialDesign.MDI_WHITE_BALANCE_SUNNY);

    public static final String calendar = button(MaterialDesign.MDI_CALENDAR);
    public static final String skipBack = button(MaterialDesign.MDI_SKIP_BACKWARD);
    public static final String skipFore = button(MaterialDesign.MDI_SKIP_FORWARD);

    private static String button(MaterialDesign uc) {
        return "<html><span style='font-size:12px'>&nbsp;<font face='Material Design Icons'>" + uc2ent(uc) + "</font>&nbsp;";
    }

    // toolbar

    private static String toolBar(MaterialDesign... uc) {
        return "<html><center><font face='Material Design Icons' size=5>" + uc2ent(uc) + "</font>";
    }

    public static final String annotate = toolBar(MaterialDesign.MDI_SHAPE_POLYGON_PLUS);
    public static final String axis = toolBar(MaterialDesign.MDI_BACKUP_RESTORE);
    public static final String cutOut = toolBar(MaterialDesign.MDI_VECTOR_CIRCLE_VARIANT);
    public static final String diffRotation = toolBar(MaterialDesign.MDI_CHART_GANTT);
    public static final String multiview = toolBar(MaterialDesign.MDI_BORDER_ALL);
    public static final String offDisk = toolBar(MaterialDesign.MDI_WEATHER_SUNNY);
    public static final String pan = toolBar(MaterialDesign.MDI_CURSOR_MOVE);
    public static final String projection = toolBar(MaterialDesign.MDI_CUBE_OUTLINE);
    public static final String resetCamera = toolBar(MaterialDesign.MDI_CROSSHAIRS);
    public static final String rotate = toolBar(MaterialDesign.MDI_ROTATE_3D);
    public static final String samp = toolBar(MaterialDesign.MDI_SHARE_VARIANT);
    public static final String track = toolBar(MaterialDesign.MDI_CROSSHAIRS_GPS);
    public static final String zoomFit = toolBar(MaterialDesign.MDI_CROP_LANDSCAPE);
    public static final String zoomIn = toolBar(MaterialDesign.MDI_MAGNIFY_PLUS);
    public static final String zoomOne = toolBar(MaterialDesign.MDI_PLUS_ONE);
    public static final String zoomOut = toolBar(MaterialDesign.MDI_MAGNIFY_MINUS);

}
