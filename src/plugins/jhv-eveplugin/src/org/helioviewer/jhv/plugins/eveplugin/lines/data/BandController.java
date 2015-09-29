package org.helioviewer.jhv.plugins.eveplugin.lines.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.settings.BandGroup;
import org.helioviewer.jhv.plugins.eveplugin.settings.BandType;
import org.helioviewer.jhv.plugins.eveplugin.settings.BandTypeAPI;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;

/**
 *
 * @author Stephan Pagel
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * */
public class BandController {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    /** the sole instance of this class */
    private static final BandController singletonInstance = new BandController();

    /** List holds references to all listeners */
    private final ArrayList<BandControllerListener> bandControllerListeners = new ArrayList<BandControllerListener>();

    private final HashMap<BandGroup, BandManager> bandManagerMap = new HashMap<BandGroup, BandManager>();

    private final LineDataSelectorModel selectorModel;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    /**
     * The private constructor to support the singleton pattern.
     * */
    private BandController() {
        selectorModel = LineDataSelectorModel.getSingletonInstance();
    }

    /**
     * Method returns the sole instance of this class.
     *
     * @return the only instance of this class.
     * */
    public static BandController getSingletonInstance() {
        return singletonInstance;
    }

    /**
     *
     * */
    public void addBand(final BandType bandType) {
        if (bandType == null) {
            return;
        }

        if (!bandManagerMap.containsKey(bandType.getGroup())) {
            bandManagerMap.put(bandType.getGroup(), new BandManager());
        }

        final Band band = bandManagerMap.get(bandType.getGroup()).addBand(bandType);

        if (band != null) {
            for (final BandControllerListener listener : bandControllerListeners) {
                listener.bandAdded(band);
            }
            selectorModel.addLineData(band);
            DownloadController.getSingletonInstance().updateBand(band, DrawController.getSingletonInstance().getAvailableInterval(), DrawController.getSingletonInstance().getSelectedInterval());
        }
    }

    /**
     *
     * */
    public void removeBand(final Band band) {
        if (band == null) {
            return;
        }

        if (!bandManagerMap.get(band.getBandType().getGroup()).removeBand(band)) {
            return;
        }

        for (final BandControllerListener listener : bandControllerListeners) {
            listener.bandRemoved(band);
        }
        selectorModel.removeLineData(band);
        // stop download (if it is running) if no instance is available anymore
        final BandType[] all = getAllAvailableBandTypes();
        boolean available = false;

        for (final BandType value : all) {
            if (value.equals(band.getBandType())) {
                available = true;
                break;
            }
        }

        if (!available) {
            DownloadController.getSingletonInstance().stopDownloads(band);
        }
    }

    public void removeAllBands() {

        for (BandManager bm : bandManagerMap.values()) {
            final Band[] bands = bm.getBands().toArray(new Band[0]);

            for (final Band band : bands) {
                removeBand(band);
            }
        }
    }

    /***/
    public void setBandVisibility(final BandType bandType, final boolean visible) {
        if (bandType == null) {
            return;
        }

        final Band band = bandManagerMap.get(bandType.getGroup()).getBand(bandType);

        if (band == null) {
            return;
        }

        band.setVisible(visible);

        for (final BandControllerListener listener : bandControllerListeners) {
            listener.bandUpdated(band);
        }
        selectorModel.lineDataElementUpdated(band);
    }

    public void selectBandGroup(final BandGroup group) {
        if (group == null) {
            return;
        }
        if (!bandManagerMap.containsKey(group)) {
            bandManagerMap.put(group, new BandManager());
        }
        bandManagerMap.get(group).selectBandGroup(group);

        for (BandControllerListener listener : bandControllerListeners) {
            listener.bandGroupChanged();
        }
    }

    /***/
    public int getNumberOfAvailableBands() {
        int totalBands = 0;
        for (BandManager bm : bandManagerMap.values()) {
            totalBands += bm.getBands().size();
        }
        return totalBands;
    }

    public boolean isBandAvailable(final Band band) {
        if (band == null) {
            return false;
        }
        for (BandManager bm : bandManagerMap.values()) {
            if (bm.containsBandType(band.getBandType())) {
                return true;
            }
        }
        return false;
    }

    public Band getBand(final BandType bandType) {
        if (bandType == null || !bandManagerMap.containsKey(bandType.getGroup())) {
            return null;
        }

        return bandManagerMap.get(bandType.getGroup()).getBand(bandType);
    }

    public Band[] getBands() {

        final ArrayList<Band> availableBands = new ArrayList<Band>();
        for (BandManager bm : bandManagerMap.values()) {
            availableBands.addAll(bm.getBands());
        }
        if (availableBands.size() == 0) {
            return new Band[0];
        }

        return availableBands.toArray(new Band[0]);
    }

    public Band[] getBands(final BandGroup group) {
        if (group == null) {
            return new Band[0];
        }

        final ArrayList<Band> availableBands = bandManagerMap.get(group).getBands(group);

        if (availableBands == null) {
            return new Band[0];
        }

        return availableBands.toArray(new Band[0]);
    }

    /**
     *
     * */
    public void addBandControllerListener(BandControllerListener listener) {
        bandControllerListeners.add(listener);
    }

    /**
     *
     * */
    public void removeBandControllerListener(BandControllerListener listener) {
        bandControllerListeners.remove(listener);
    }

    public BandType[] getAllAvailableBandTypes() {
        final HashSet<BandType> result = new HashSet<BandType>();
        for (BandManager bm : bandManagerMap.values()) {
            for (final Band band : bm.getBands()) {
                result.add(band.getBandType());
            }
        }
        return result.toArray(new BandType[0]);
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Band Manager
    // //////////////////////////////////////////////////////////////////////////////

    private class BandManager {

        // //////////////////////////////////////////////////////////////////////////
        // Definitions
        // //////////////////////////////////////////////////////////////////////////

        private BandGroup selectedBandGroup;

        private final HashMap<BandType, Band> lookupAvailableBandTypes = new HashMap<BandType, Band>();
        private final HashMap<BandGroup, ArrayList<Band>> availableBandsInGroupMap = new HashMap<BandGroup, ArrayList<Band>>();

        // //////////////////////////////////////////////////////////////////////////
        // Methods
        // //////////////////////////////////////////////////////////////////////////

        public BandManager() {
            selectedBandGroup = BandTypeAPI.getSingletonInstance().getGroups()[0];
            for (final BandGroup type : BandTypeAPI.getSingletonInstance().getGroups()) {
                availableBandsInGroupMap.put(type, new ArrayList<Band>());
            }
        }

        public Band addBand(final BandType bandType) {
            if (lookupAvailableBandTypes.containsKey(bandType)) {
                return null;
            }

            final Band band = new Band(bandType);

            lookupAvailableBandTypes.put(bandType, band);

            ArrayList<Band> availableBandTypes = availableBandsInGroupMap.get(bandType.getGroup());
            if (availableBandTypes == null) {
                availableBandsInGroupMap.put(bandType.getGroup(), new ArrayList<Band>());
                availableBandTypes = availableBandsInGroupMap.get(bandType.getGroup());
            }
            availableBandTypes.add(band);
            return band;
        }

        public boolean removeBand(final Band band) {
            if (lookupAvailableBandTypes.remove(band.getBandType()) != null) {
                final ArrayList<Band> availableBandTypes = availableBandsInGroupMap.get(band.getBandType().getGroup());
                availableBandTypes.remove(band);

                return true;
            }

            return false;
        }

        public boolean containsBandType(final BandType bandType) {
            return lookupAvailableBandTypes.containsKey(bandType);
        }

        public Band getBand(final BandType bandType) {
            return lookupAvailableBandTypes.get(bandType);
        }

        public ArrayList<Band> getBands() {
            return getBands(selectedBandGroup);
        }

        public ArrayList<Band> getBands(final BandGroup group) {
            final ArrayList<Band> availableBands = availableBandsInGroupMap.get(group);

            if (availableBands == null) {
                return new ArrayList<Band>();
            }

            return availableBands;
        }

        public void selectBandGroup(final BandGroup group) {
            // set current group visible = false
            /*
             * final ArrayList<Band> bands = getBands();
             *
             * for (final Band band : bands) { band.setVisible(false); }
             */

            // set selected group visible = true
            selectedBandGroup = group;

            final ArrayList<Band> newBands = getBands();

            for (final Band band : newBands) {
                band.setVisible(true);
            }
        }
    }

}
