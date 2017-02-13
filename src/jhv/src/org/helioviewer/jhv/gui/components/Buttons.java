package org.helioviewer.jhv.gui.components;

import org.apache.commons.lang3.StringEscapeUtils;

public class Buttons {

    public static final String optionsDown = "<html>Options&nbsp;<font face=Ionicons>" + StringEscapeUtils.escapeHtml4(String.valueOf(Ionicons.ION_CHEVRON_DOWN.getCode()));
    public static final String optionsUp = "<html>Options&nbsp;<font face=Ionicons>" + StringEscapeUtils.escapeHtml4(String.valueOf(Ionicons.ION_CHEVRON_UP.getCode()));

    public static final String play = "<html>&nbsp;<font size=5 face=Ionicons>" + StringEscapeUtils.escapeHtml4(String.valueOf(Ionicons.ION_PLAY.getCode())) + "</font>&nbsp;";
    public static final String pause = "<html>&nbsp;<font size=5 face=Ionicons>" + StringEscapeUtils.escapeHtml4(String.valueOf(Ionicons.ION_PAUSE.getCode())) + "</font>&nbsp;";
    public static final String prev = "<html>&nbsp;<font size=5 face=Ionicons>" + StringEscapeUtils.escapeHtml4(String.valueOf(Ionicons.ION_CHEVRON_LEFT.getCode())) + "</font>&nbsp;";
    public static final String next = "<html>&nbsp;<font size=5 face=Ionicons>" + StringEscapeUtils.escapeHtml4(String.valueOf(Ionicons.ION_CHEVRON_RIGHT.getCode())) + "</font>&nbsp;";
    public static final String record = "<html>&nbsp;<font size=5 color=#f2363a face=Ionicons>" + StringEscapeUtils.escapeHtml4(String.valueOf(Ionicons.ION_RECORD.getCode())) + "</font>&nbsp;";

    public static final String newLayer = "<html><font face=Ionicons>" + StringEscapeUtils.escapeHtml4(String.valueOf(Ionicons.ION_PLUS.getCode())) + "</font>&nbsp;New Layer";
    public static final String syncLayers = "<html><font face=Ionicons>" + StringEscapeUtils.escapeHtml4(String.valueOf(Ionicons.ION_ANDROID_SYNC.getCode()));

    public static final String lock = "<html><font face=Ionicons>" + StringEscapeUtils.escapeHtml4(String.valueOf(Ionicons.ION_LOCKED.getCode()));
    public static final String unlock = "<html><font face=Ionicons>" + StringEscapeUtils.escapeHtml4(String.valueOf(Ionicons.ION_UNLOCKED.getCode()));

}
