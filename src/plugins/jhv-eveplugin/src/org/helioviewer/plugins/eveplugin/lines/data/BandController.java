package org.helioviewer.plugins.eveplugin.lines.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import org.helioviewer.plugins.eveplugin.controller.ZoomController;
import org.helioviewer.plugins.eveplugin.settings.BandGroup;
import org.helioviewer.plugins.eveplugin.settings.BandType;
import org.helioviewer.plugins.eveplugin.settings.BandTypeAPI;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;

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
    private final LinkedList<BandControllerListener> bandControllerListeners = new LinkedList<BandControllerListener>();

    private final HashMap<String, Map<BandGroup, BandManager>> bandManagerMap = new HashMap<String, Map<BandGroup, BandManager>>();

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

    public void registerBandManager(final String identifier) {
        if (!bandManagerMap.containsKey(identifier)) {
            bandManagerMap.put(identifier, new HashMap<BandGroup, BandManager>());
        }
    }

    /**
     * 
     * */
    public void addBand(final String identifier, final BandType bandType) {
        if (identifier == null || bandType == null || !bandManagerMap.containsKey(identifier)) {
            return;
        }

        if (!bandManagerMap.get(identifier).containsKey(bandType.getGroup())) {
            bandManagerMap.get(identifier).put(bandType.getGroup(), new BandManager());
        }

        final Band band = bandManagerMap.get(identifier).get(bandType.getGroup()).addBand(bandType);

        if (band != null) {
            for (final BandControllerListener listener : bandControllerListeners) {
                listener.bandAdded(band, identifier);
            }
            band.setPlotIndentifier(identifier);
            selectorModel.addLineData(band);
            DownloadController.getSingletonInstance().updateBand(band, ZoomController.getSingletonInstance().getAvailableInterval(),
                    ZoomController.getSingletonInstance().getSelectedInterval());
        }
    }

    /**
     * 
     * */
    public void removeBand(final String identifier, final Band band) {
        if (identifier == null || band == null || !bandManagerMap.containsKey(identifier)) {
            return;
        }

        if (!bandManagerMap.get(identifier).get(band.getBandType().getGroup()).removeBand(band)) {
            return;
        }

        for (final BandControllerListener listener : bandControllerListeners) {
            listener.bandRemoved(band, identifier);
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

    public void removeAllBands(final String identifier) {
        if (identifier == null || !bandManagerMap.containsKey(identifier)) {
            return;
        }
        for (BandManager bm : bandManagerMap.get(identifier).values()) {
            final Band[] bands = bm.getBands().toArray(new Band[0]);

            for (final Band band : bands) {
                removeBand(identifier, band);
            }
        }
    }

    /***/
    public void setBandVisibility(final String identifier, final BandType bandType, final boolean visible) {
        if (identifier == null || bandType == null || !bandManagerMap.containsKey(identifier)) {
            return;
        }

        final Band band = bandManagerMap.get(identifier).get(bandType.getGroup()).getBand(bandType);

        if (band == null) {
            return;
        }

        band.setVisible(visible);

        for (final BandControllerListener listener : bandControllerListeners) {
            listener.bandUpdated(band, identifier);
        }
        selectorModel.lineDataElementUpdated(band);
    }

    public void selectBandGroup(final String identifier, final BandGroup group) {
        if (identifier == null || group == null || !bandManagerMap.containsKey(identifier)) {
            return;
        }
        if (!bandManagerMap.get(identifier).containsKey(group)) {
            bandManagerMap.get(identifier).put(group, new BandManager());
        }
        bandManagerMap.get(identifier).get(group).selectBandGroup(group);

        for (BandControllerListener listener : bandControllerListeners) {
            listener.bandGroupChanged(identifier);
        }
        // TODO Check if I really need a method in LineDataSelectorModel for
        // handling this.
    }

    /*
     * public BandGroup getSelectedGroup(final String identifier) { if
     * (identifier == null || !bandManagerMap.containsKey(identifier)) { return
     * null; }
     * 
     * return bandManagerMap.get(identifier).getSelectedGroup(); }
     */

    /***/
    public int getNumberOfAvailableBands(final String identifier) {
        if (identifier == null || !bandManagerMap.containsKey(identifier)) {
            return -1;
        }
        int totalBands = 0;
        for (BandManager bm : bandManagerMap.get(identifier).values()) {
            totalBands += bm.getBands().size();
        }
        return totalBands;
    }

    public boolean isBandAvailable(final String identifier, final Band band) {
        if (identifier == null || band == null || !bandManagerMap.containsKey(identifier)) {
            return false;
        }
        for (BandManager bm : bandManagerMap.get(identifier).values()) {
            if (bm.containsBandType(band.getBandType())) {
                return true;
            }
        }
        return false;
    }

    /***/
    /*
     * public int indexOfBand(final String identifier, final BandType bandType)
     * { if (identifier == null || bandType == null ||
     * !bandManagerMap.containsKey(identifier)) { return -1; }
     * 
     * final LinkedList<Band> availableBands =
     * bandManagerMap.get(identifier).getBands(bandType.getGroup()); return
     * availableBands.indexOf(bandType); }
     */

    /***/
    /*
     * public int indexOfBand(final String identifier, final Band band) { if
     * (identifier == null || band == null ||
     * !bandManagerMap.containsKey(identifier)) { return -1; }
     * 
     * final LinkedList<Band> availableBands =
     * bandManagerMap.get(identifier).getBands(band.getBandType().getGroup());
     * return availableBands.indexOf(band); }
     */

    /***/
    /*
     * public Band getBand(final String identifier, final int index) { if
     * (identifier == null || !bandManagerMap.containsKey(identifier)) { return
     * null; }
     * 
     * final LinkedList<Band> availableBands =
     * bandManagerMap.get(identifier).getBands();
     * 
     * if (availableBands == null) { return null; }
     * 
     * return availableBands.get(index); }
     */

    public Band getBand(final String identifier, final BandType bandType) {
        if (identifier == null || bandType == null || !bandManagerMap.containsKey(identifier)
                || !bandManagerMap.get(identifier).containsKey(bandType.getGroup())) {
            return null;
        }

        return bandManagerMap.get(identifier).get(bandType.getGroup()).getBand(bandType);
    }

    public Band[] getBands(final String identifier) {
        if (identifier == null) {
            return new Band[0];
        }

        final LinkedList<Band> availableBands = new LinkedList<Band>();
        for (BandManager bm : bandManagerMap.get(identifier).values()) {
            availableBands.addAll(bm.getBands());
        }
        if (availableBands.size() == 0) {
            return new Band[0];
        }

        return availableBands.toArray(new Band[0]);
    }

    public Band[] getBands(final String identifier, final BandGroup group) {
        if (identifier == null || group == null) {
            return new Band[0];
        }

        final LinkedList<Band> availableBands = bandManagerMap.get(identifier).get(group).getBands(group);

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
        for (final String key : bandManagerMap.keySet()) {
            for (BandManager bm : bandManagerMap.get(key).values()) {
                for (final Band band : bm.getBands()) {
                    result.add(band.getBandType());
                }
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
        private final HashMap<BandGroup, LinkedList<Band>> availableBandsInGroupMap = new HashMap<BandGroup, LinkedList<Band>>();

        // //////////////////////////////////////////////////////////////////////////
        // Methods
        // //////////////////////////////////////////////////////////////////////////

        public BandManager() {
            selectedBandGroup = BandTypeAPI.getSingletonInstance().getGroups()[0];
            for (final BandGroup type : BandTypeAPI.getSingletonInstance().getGroups()) {
                availableBandsInGroupMap.put(type, new LinkedList<Band>());
            }
        }

        public Band addBand(final BandType bandType) {
            if (lookupAvailableBandTypes.containsKey(bandType)) {
                return null;
            }

            final Band band = new Band(bandType);

            lookupAvailableBandTypes.put(bandType, band);

            LinkedList<Band> availableBandTypes = availableBandsInGroupMap.get(bandType.getGroup());
            if (availableBandTypes == null) {
                availableBandsInGroupMap.put(bandType.getGroup(), new LinkedList<Band>());
                availableBandTypes = availableBandsInGroupMap.get(bandType.getGroup());
            }
            availableBandTypes.add(band);
            return band;
        }

        public boolean removeBand(final Band band) {
            if (lookupAvailableBandTypes.remove(band.getBandType()) != null) {
                final LinkedList<Band> availableBandTypes = availableBandsInGroupMap.get(band.getBandType().getGroup());
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

        public LinkedList<Band> getBands() {
            return getBands(selectedBandGroup);
        }

        public LinkedList<Band> getBands(final BandGroup group) {
            final LinkedList<Band> availableBands = availableBandsInGroupMap.get(group);

            if (availableBands == null) {
                return new LinkedList<Band>();
            }

            return availableBands;
        }

        public void selectBandGroup(final BandGroup group) {
            // set current group visible = false
            /*
             * final LinkedList<Band> bands = getBands();
             * 
             * for (final Band band : bands) { band.setVisible(false); }
             */

            // set selected group visible = true
            selectedBandGroup = group;

            final LinkedList<Band> newBands = getBands();

            for (final Band band : newBands) {
                band.setVisible(true);
            }
        }

        public BandGroup getSelectedGroup() {
            return selectedBandGroup;
        }
    }
}
