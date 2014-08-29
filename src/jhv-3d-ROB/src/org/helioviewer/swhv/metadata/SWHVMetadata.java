package org.helioviewer.swhv.metadata;

import java.util.Date;

import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;

public class SWHVMetadata implements Comparable {
    private final Date date;
    private final double hgltobs;
    private final double hglnobs;
    private final JHVJPXView view;
    private final int frameNumber;
    private final double solarRadiusPixels;
    private final double solarRadiusoffsetXPixels;
    private final double solarRadiusoffsetYPixels;
    private final double widthPixels;
    private final double heightPixels;
    private final double widthSolarUnits;
    private final double heightSolarUnits;

    public SWHVMetadata(Date date, double hgltobs, double hglnobs, double crpix1, double crpix2, double cdelt1, double cdelt2, JHVJPXView view, int frameNumber) {
        super();
        this.date = date;
        this.hgltobs = hgltobs;
        this.hglnobs = hglnobs;
        this.view = view;
        this.frameNumber = frameNumber;
        this.solarRadiusoffsetXPixels = 0;
        this.solarRadiusoffsetYPixels = 0;
        this.solarRadiusPixels = 0.;
        this.widthPixels = 0.;
        this.heightPixels = 0.;
        this.widthSolarUnits = 0.;
        this.heightSolarUnits = 0.;
    }

    public Date getDate() {
        return date;
    }

    public double getHgltobs() {
        return hgltobs;
    }

    public double getHglnobs() {
        return hglnobs;
    }

    public JHVJPXView getView() {
        return view;
    }

    public int getFrameNumber() {
        return frameNumber;
    }

    @Override
    public String toString() {
        return date.toString();
    }

    @Override
    public int compareTo(Object o) {
        return this.getDate().compareTo(((SWHVMetadata) o).getDate());
    }

    public double getSolarRadiusPixels() {
        return solarRadiusPixels;
    }

    public double getSolarRadiusoffsetXPixels() {
        return solarRadiusoffsetXPixels;
    }

    public double getSolarRadiusoffsetYPixels() {
        return solarRadiusoffsetYPixels;
    }
}
