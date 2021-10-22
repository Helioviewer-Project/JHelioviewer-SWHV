package org.helioviewer.jhv.gui.components;

enum MaterialDesign {

    // https://github.com/aalmiray/ikonli
    BACKUP_RESTORE("mdi-backup-restore", '\uf06f'),
    BORDER_ALL("mdi-border-all", '\uf0c7'),
    // BRIGHTNESS_AUTO("mdi-brightness-auto", '\uf0e1'),
    CALENDAR("mdi-calendar", '\uf0ed'),
    CHART_GANTT("mdi-chart-gantt", '\uf66c'),
    CHECK("mdi-check", '\uf12c'),
    CHEVRON_DOWN("mdi-chevron-down", '\uf140'),
    CHEVRON_RIGHT("mdi-chevron-right", '\uf142'),
    CLOSE("mdi-close", '\uf156'),
    CROP_LANDSCAPE("mdi-crop-landscape", '\uf1a0'),
    CROSSHAIRS_GPS("mdi-crosshairs-gps", '\uf1a4'),
    CUBE_OUTLINE("mdi-cube-outline", '\uf1a7'),
    CURSOR_MOVE("mdi-cursor-move", '\uf1b6'),
    DEBUG_STEP_OUT("mdi-debug-step-out", '\uf1bc'),
    DOWNLOAD("mdi-download", '\uf1da'),
    IMAGE_FILTER_CENTER_FOCUS("mdi-image-filter-center-focus", '\uf2f1'),
    IMAGE_FILTER_HDR("mdi-image-filter-hdr", '\uf2f5'),
    INFORMATION_VARIANT("mdi-information-variant", '\uf64e'),
    INVERT_COLORS("mdi-invert-colors", '\uf301'),
    LOCK("mdi-lock", '\uf33e'),
    LOCK_OPEN("mdi-lock-open", '\uf33f'),
    MAGNIFY_MINUS("mdi-magnify-minus", '\uf34a'),
    MAGNIFY_PLUS("mdi-magnify-plus", '\uf34b'),
    PAUSE("mdi-pause", '\uf3e4'),
    PLAY("mdi-play", '\uf40a'),
    PLUS_CIRCLE("mdi-plus-circle", '\uf417'),
    PLUS_ONE("mdi-plus-one", '\uf41b'),
    RECORD("mdi-record", '\uf44a'),
    ROTATE_3D("mdi-rotate-3d", '\uf464'),
    SHAPE_POLYGON_PLUS("mdi-shape-polygon-plus", '\uf65e'),
    SHARE_VARIANT("mdi-share-variant", '\uf497'),
    SKIP_BACKWARD("mdi-skip-backward", '\uf4ab'),
    SKIP_FORWARD("mdi-skip-forward", '\uf4ac'),
    // SLACK("mdi-slack", '\uf4b1'),
    STEP_BACKWARD("mdi-step-backward", '\uf4d5'),
    STEP_FORWARD("mdi-step-forward", '\uf4d7'),
    SYNC("mdi-sync", '\uf4e6'),
    // UPLOAD("mdi-upload", '\uf552'),
    VECTOR_CIRCLE_VARIANT("mdi-vector-circle-variant", '\uf557'),
    WEATHER_SUNNY("mdi-weather-sunny", '\uf599'),
    WHITE_BALANCE_SUNNY("mdi-white-balance-sunny", '\uf5a8');

    private final char code;

    MaterialDesign(String _description, char _code) {
        code = _code;
    }

    @Override
    public String toString() {
        return /*code <= '\uffff' ?*/ String.valueOf(code) /*: new String(Character.toChars(code))*/;
    }

}
