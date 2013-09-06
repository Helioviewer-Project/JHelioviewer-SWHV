package org.helioviewer.viewmodel.view;

import java.util.Date;
import java.util.GregorianCalendar;

import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.base.physics.DifferentialRotation;
import org.helioviewer.base.physics.HeliocentricCartesianCoordinatesFromEarth;
import org.helioviewer.base.physics.StonyhurstHeliographicCoordinates;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.TimestampChangedReason;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

/**
 * Implementation of View to pan automatically according to the solar rotation.
 * 
 * <p>
 * This plugin uses the differential rotation tracking to keep the center of the
 * screen over a certain point of the surface of the sun.
 * 
 * @author Markus Langenberg
 */
public class StandardSolarRotationTrackingView extends AbstractBasicView implements RegionView {

    private RegionView regionView;
    private Date currentDate;
    private Date startDate;
    private StonyhurstHeliographicCoordinates startPosition;
    private boolean startPositionIsInsideDisc = true;
    private boolean enabled = false;

    /**
     * Enables or disabled the solar rotatin tracking
     * 
     * @param enabled
     *            If true, the solar rotation tracking is enabled, false
     *            otherwise
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            resetStartPosition();
        }
    }

    /**
     * Toggles the solar rotatin tracking on or off
     */
    public void toggleEnabled() {
        enabled = !enabled;
        if (enabled) {
            resetStartPosition();
        }
    }

    /**
     * Returns whether the solar rotation tracking is enabled or not.
     * 
     * @return True, if the solar rotation tracking is enabled, false otherwise
     */
    public boolean getEnabled() {
        return enabled;
    }

    /**
     * Sets the date of the starting point. Usually, it is unnecessary to call
     * this function because the start date is extracted from the
     * TimestampChangedReason during {@link #viewChanged(View, ChangeEvent)}.
     * 
     * @param start
     *            new start date
     */
    public void setStartDate(Date startDate) {
        resetStartPosition(regionView.getRegion(), startDate);
    }

    /**
     * {@inheritDoc}
     */
    public void viewChanged(View sender, ChangeEvent aEvent) {

        synchronized (this) {
            TimestampChangedReason timestampReason = aEvent.getLastChangedReasonByType(TimestampChangedReason.class);

            if ((timestampReason != null) && (timestampReason.getView() instanceof TimedMovieView) && LinkedMovieManager.getActiveInstance().isMaster((TimedMovieView) timestampReason.getView())) {
                currentDate = timestampReason.getNewDateTime().getTime();

                if (!enabled || !startPositionIsInsideDisc) {
                    notifyViewListeners(aEvent);
                    return;
                }

                GregorianCalendar currentCalendarDate = new GregorianCalendar();
                currentCalendarDate.setTime(currentDate);

                if (startDate != null) {
                    long timeDiff = (currentDate.getTime() - startDate.getTime()) / 1000;

                    if (timeDiff == 0) {
                        notifyViewListeners(aEvent);
                        return;
                    }

                    StonyhurstHeliographicCoordinates currentPosition = DifferentialRotation.calculateNextPosition(startPosition, timeDiff);

                    // Move to "parking position" while on the back side of the
                    // sun
                    if ((currentPosition.phi > 90 && currentPosition.phi < 180) || (currentPosition.phi > -270 && currentPosition.phi < -180)) {

                        currentPosition = new StonyhurstHeliographicCoordinates(currentPosition.theta, 90, currentPosition.r);

                    } else if ((currentPosition.phi >= 180 && currentPosition.phi < 270) || (currentPosition.phi >= -180 && currentPosition.phi < -90)) {

                        currentPosition = new StonyhurstHeliographicCoordinates(currentPosition.theta, 270, currentPosition.r);
                    }

                    HeliocentricCartesianCoordinatesFromEarth currentHCC = new HeliocentricCartesianCoordinatesFromEarth(currentPosition, currentCalendarDate);

                    Region currentRegion = regionView.getRegion();
                    Vector2dDouble currentCenter = currentHCC.getCartesianCoordinatesOnDisc();
                    Region newRegion = StaticRegion.createAdaptedRegion(currentCenter.subtract(currentRegion.getSize().scale(0.5)), currentRegion.getSize());
                    regionView.setRegion(newRegion, new ChangeEvent());
                } else {
                    resetStartPosition();
                }
            }
        }

        notifyViewListeners(aEvent);
    }

    /**
     * {@inheritDoc}
     */
    public boolean setRegion(Region r, ChangeEvent event) {
        if (regionView != null) {
            boolean returnValue = regionView.setRegion(r, event);
            resetStartPosition();
            return returnValue;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Region getRegion() {
        if (regionView != null)
            return regionView.getRegion();

        return null;
    }

    /**
     * {@inheritDoc}
     */
    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
        if (newView != null)
            regionView = newView.getAdapter(RegionView.class);
    }

    private void resetStartPosition() {
        if (regionView == null) {
            return;
        }
        Region region = regionView.getRegion();
        if (region == null) {
            return;
        }
        if (LinkedMovieManager.getActiveInstance() == null) {
            return;
        }
        TimedMovieView masterView = LinkedMovieManager.getActiveInstance().getMasterMovie();
        if (masterView == null) {
            return;
        }
        ImmutableDateTime idt = masterView.getCurrentFrameDateTime();
        if (idt == null) {
            return;
        }
        resetStartPosition(region, idt.getTime());
    }

    private synchronized void resetStartPosition(Region newRegion, Date startDate) {
        if (startDate == null || newRegion == null) {
            return;
        }
        this.startDate = startDate;
        GregorianCalendar startCalendarDate = new GregorianCalendar();
        startCalendarDate.setTime(startDate);

        Vector2dDouble center = newRegion.getLowerLeftCorner().add(newRegion.getSize().scale(0.5));
        // don't rotate anything if the center is not in the sun
        startPositionIsInsideDisc = (center.lengthSq() <= Constants.SunRadius * Constants.SunRadius);
        if (!startPositionIsInsideDisc) {
            return;
        }

        HeliocentricCartesianCoordinatesFromEarth hcc = new HeliocentricCartesianCoordinatesFromEarth(center, startCalendarDate);
        startPosition = new StonyhurstHeliographicCoordinates(hcc);
    }
}
