package org.helioviewer.jhv.annotation;

import org.helioviewer.jhv.Log;

import org.json.JSONObject;

public enum AnnotationMode {
    Rectangle {
        @Override
        public Annotateable generate(JSONObject jo) {
            return new AnnotateRectangle(jo);
        }
    },
    Circle {
        @Override
        public Annotateable generate(JSONObject jo) {
            return new AnnotateCircle(jo);
        }
    },
    Cross {
        @Override
        public Annotateable generate(JSONObject jo) {
            return new AnnotateCross(jo);
        }
    },
    FOV {
        @Override
        public Annotateable generate(JSONObject jo) {
            return new AnnotateFOV(jo);
        }
    },
    Line {
        @Override
        public Annotateable generate(JSONObject jo) {
            return new AnnotateLine(jo);
        }
    },
    Loop {
        @Override
        public Annotateable generate(JSONObject jo) {
            return new AnnotateLoop(jo);
        }
    };

    public abstract Annotateable generate(JSONObject jo);

    static Annotateable generate(String type, JSONObject jo) {
        try {
            return valueOf(type).generate(jo);
        } catch (IllegalArgumentException e) {
            Log.warn("Unknown annotation type: " + type, e);
            return Rectangle.generate(jo);
        }
    }
}
