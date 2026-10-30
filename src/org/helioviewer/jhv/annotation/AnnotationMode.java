package org.helioviewer.jhv.annotation;

import org.helioviewer.jhv.app.Log;

import org.json.JSONObject;

public enum AnnotationMode {
    Rectangle, Circle, Cross, FOV, Line, Loop;

    public Annotateable generate(JSONObject jo) {
        return switch (this) {
            case Rectangle -> new AnnotateRectangle(jo);
            case Circle -> new AnnotateCircle(jo);
            case Cross -> new AnnotateCross(jo);
            case FOV -> new AnnotateFOV(jo);
            case Line -> new AnnotateLine(jo);
            case Loop -> new AnnotateLoop(jo);
        };
    }

    static Annotateable fromJson(JSONObject jo) {
        String type = jo.optString("type", Rectangle.toString());
        try {
            return valueOf(type).generate(jo);
        } catch (IllegalArgumentException e) {
            Log.warn("Unknown annotation type: " + type, e);
            return Rectangle.generate(jo);
        }
    }
}
