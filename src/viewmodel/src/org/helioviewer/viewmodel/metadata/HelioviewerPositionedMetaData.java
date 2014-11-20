package org.helioviewer.viewmodel.metadata;

import org.helioviewer.base.physics.Astronomy;

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
    private double refb0;
    private double refl0;
    private boolean refAvailable;

    public HelioviewerPositionedMetaData(MetaDataContainer mdc) {
        super(mdc);
        updatePosition();
    }

    @Override
    protected boolean updatePixelParameters() {
        updatePosition();
        return super.updatePixelParameters();
    }

    private void updatePosition() {
        this.heeqX = metaDataContainer.tryGetDouble("HEQX_OBS");
        this.heeqY = metaDataContainer.tryGetDouble("HEQY_OBS");
        this.heeqZ = metaDataContainer.tryGetDouble("HEQZ_OBS");
        this.heeqAvailable = this.heeqX != 0.0 || this.heeqY != 0.0 || this.heeqZ != 0.0;

        this.heeX = metaDataContainer.tryGetDouble("HEEX_OBS");
        this.heeY = metaDataContainer.tryGetDouble("HEEY_OBS");
        this.heeZ = metaDataContainer.tryGetDouble("HEEZ_OBS");
        this.heeAvailable = this.heeX != 0.0 || this.heeY != 0.0 || this.heeZ != 0.0;

        this.crlt = metaDataContainer.tryGetDouble("CRLT_OBS");
        this.crln = metaDataContainer.tryGetDouble("CRLN_OBS");
        this.dobs = metaDataContainer.tryGetDouble("DSUN_OBS");
        this.carringtonAvailable = this.crlt != 0.0 || this.crln != 0.0;

        this.refb0 = metaDataContainer.tryGetDouble("REF_B0");
        this.refl0 = metaDataContainer.tryGetDouble("REF_L0");
        this.refAvailable = this.refb0 != 0.0 || this.refl0 != 0.0;

        this.stonyhurstLatitude = metaDataContainer.tryGetDouble("HGLT_OBS");
        if (this.stonyhurstLatitude == 0) {
            this.stonyhurstLatitude = this.crlt;
            if (this.stonyhurstLatitude == 0) {
                this.stonyhurstLatitude = this.refb0;
            }
        }
        this.stonyhurstLongitude = metaDataContainer.tryGetDouble("HGLN_OBS");
        if (this.refl0 != 0.) {
            this.stonyhurstLongitude = this.refl0 - Astronomy.getL0Degree(this.getDateTime().getTime());
        }

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

    @Override
    public double getHEEQX() {
        return this.heeqX;
    }

    @Override
    public double getHEEQY() {
        return this.heeqY;
    }

    @Override
    public double getHEEQZ() {
        return this.heeqZ;
    }

    @Override
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