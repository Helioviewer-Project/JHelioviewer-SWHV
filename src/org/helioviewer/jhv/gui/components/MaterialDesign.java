package org.helioviewer.jhv.gui.components;

public enum MaterialDesign {

    // https://github.com/aalmiray/ikonli
    MDI_BACKUP_RESTORE("mdi-backup-restore", '\uf06f'),
    MDI_BORDER_ALL("mdi-border-all", '\uf0c7'),
    // MDI_BRIGHTNESS_AUTO("mdi-brightness-auto", '\uf0e1'),
    MDI_CALENDAR("mdi-calendar", '\uf0ed'),
    MDI_CHART_GANTT("mdi-chart-gantt", '\uf66c'),
    MDI_CHECK("mdi-check", '\uf12c'),
    MDI_CHEVRON_DOWN("mdi-chevron-down", '\uf140'),
    MDI_CHEVRON_RIGHT("mdi-chevron-right", '\uf142'),
    MDI_CLOSE("mdi-close", '\uf156'),
    MDI_CROP_LANDSCAPE("mdi-crop-landscape", '\uf1a0'),
    MDI_CROSSHAIRS("mdi-crosshairs", '\uf1a3'),
    MDI_CROSSHAIRS_GPS("mdi-crosshairs-gps", '\uf1a4'),
    MDI_CUBE_OUTLINE("mdi-cube-outline", '\uf1a7'),
    MDI_CURSOR_MOVE("mdi-cursor-move", '\uf1b6'),
    MDI_DOWNLOAD("mdi-download", '\uf1da'),
    MDI_INFORMATION_VARIANT("mdi-information-variant", '\uf64e'),
    MDI_INVERT_COLORS("mdi-invert-colors", '\uf301'),
    MDI_LOCK("mdi-lock", '\uf33e'),
    MDI_LOCK_OPEN("mdi-lock-open", '\uf33f'),
    MDI_MAGNIFY_MINUS("mdi-magnify-minus", '\uf34a'),
    MDI_MAGNIFY_PLUS("mdi-magnify-plus", '\uf34b'),
    MDI_PAUSE("mdi-pause", '\uf3e4'),
    MDI_PLAY("mdi-play", '\uf40a'),
    MDI_PLUS_CIRCLE("mdi-plus-circle", '\uf417'),
    MDI_PLUS_ONE("mdi-plus-one", '\uf41b'),
    MDI_RECORD("mdi-record", '\uf44a'),
    MDI_ROTATE_3D("mdi-rotate-3d", '\uf464'),
    MDI_SHAPE_POLYGON_PLUS("mdi-shape-polygon-plus", '\uf65e'),
    MDI_SHARE_VARIANT("mdi-share-variant", '\uf497'),
    MDI_SKIP_BACKWARD("mdi-skip-backward", '\uf4ab'),
    MDI_SKIP_FORWARD("mdi-skip-forward", '\uf4ac'),
    // MDI_SLACK("mdi-slack", '\uf4b1'),
    MDI_STEP_BACKWARD("mdi-step-backward", '\uf4d5'),
    MDI_STEP_FORWARD("mdi-step-forward", '\uf4d7'),
    MDI_SYNC("mdi-sync", '\uf4e6'),
    // MDI_UPLOAD("mdi-upload", '\uf552'),
    MDI_VECTOR_CIRCLE_VARIANT("mdi-vector-circle-variant", '\uf557'),
    MDI_WEATHER_SUNNY("mdi-weather-sunny", '\uf599'),
    MDI_WHITE_BALANCE_SUNNY("mdi-white-balance-sunny", '\uf5a8');

    public final char code;

    MaterialDesign(String _description, char _code) {
        code = _code;
    }

}
