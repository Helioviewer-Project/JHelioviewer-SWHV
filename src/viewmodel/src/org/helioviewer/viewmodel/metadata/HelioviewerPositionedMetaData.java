package org.helioviewer.viewmodel.metadata;

public class HelioviewerPositionedMetaData extends HelioviewerMetaData implements PositionedMetaData {
    private double heeqX;
    private double heeqY;
    private double heeqZ;
    private boolean heeqAvailable = false;

    private double heeX;
    private double heeY;
    private double heeZ;
    private boolean heeAvailable = false;

    private double crlt;
    private double crln;
    private double dobs;
    private boolean carringtonAvailable = false;

    private double stonyhurstLongitude;
    private double stonyhurstLatitude;
    private boolean stonyhurstAvailable = false;

    public HelioviewerPositionedMetaData(MetaDataContainer mdc) {
        super(mdc);

        this.heeqX = mdc.tryGetDouble("HEQX_OBS");
        this.heeqY = mdc.tryGetDouble("HEQY_OBS");
        this.heeqZ = mdc.tryGetDouble("HEQZ_OBS");
        this.heeqAvailable = this.heeqX != 0.0 || this.heeqY != 0.0 || this.heeqZ != 0.0;

        this.heeX = mdc.tryGetDouble("HEEX_OBS");
        this.heeY = mdc.tryGetDouble("HEEY_OBS");
        this.heeZ = mdc.tryGetDouble("HEEZ_OBS");
        this.heeAvailable = this.heeX != 0.0 || this.heeY != 0.0 || this.heeZ != 0.0;

        this.crlt = mdc.tryGetDouble("CRLT_OBS");
        this.crln = mdc.tryGetDouble("CRLN_OBS");
        this.dobs = mdc.tryGetDouble("DSUN_OBS");
        this.carringtonAvailable = this.crlt != 0.0 || this.crln != 0.0;

        this.stonyhurstLatitude = mdc.tryGetDouble("HGLT_OBS");
        this.stonyhurstLongitude = mdc.tryGetDouble("HGLN_OBS");
        this.stonyhurstAvailable = this.stonyhurstLatitude != 0.0 || this.stonyhurstLongitude != 0.0;
    }

    public double getHEEX() {
        return heeX;
    }

    public double getHEEY() {
        return heeqY;
    }

    public double getHEEZ() {
        return heeZ;
    }

    public boolean isHEEProvided() {
        return heeAvailable;
    }

    public double getHEEQX() {
        return this.heeqX;
    }

    public double getHEEQY() {
        return this.heeqY;
    }

    public double getHEEQZ() {
        return this.heeqZ;
    }

    public boolean isHEEQProvided() {
        return this.heeqAvailable;
    }

    public double getCrln() {
        return crln;
    }

    public double getCrlt() {
        return crlt;
    }

    public double getDobs() {
        return dobs;
    }

    public boolean isCarringtonProvided() {
        return carringtonAvailable;
    }

    public boolean isStonyhurstProvided() {
        return stonyhurstAvailable;
    }

    public double getStonyhurstLatitude() {
        return stonyhurstLatitude;
    }

    public double getStonyhurstLongitude() {
        return stonyhurstLongitude;
    }
}