package org.helioviewer.jhv.imagedata;

class FilterWOW2 extends FilterWOW {

    @Override
    public float[] filter(float[] data, int width, int height) {
        return filter(data, width, height, true);
    }

}
