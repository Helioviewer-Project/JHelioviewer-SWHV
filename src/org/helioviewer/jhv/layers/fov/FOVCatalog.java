package org.helioviewer.jhv.layers.fov;

import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.Viewport;

import org.json.JSONObject;

public final class FOVCatalog {

    private final List<FOVPlatform> platforms = new ArrayList<>();

    public FOVCatalog(JSONObject jo) {
        JSONObject empty = new JSONObject();
        if (jo == null)
            jo = empty;
        buildCatalog(jo, empty);
    }

    List<FOVPlatform> platforms() {
        return platforms;
    }

    private void buildCatalog(JSONObject jo, JSONObject empty) {
        addSolarOrbiter(jo, empty);
        addStereoAhead(jo, empty);
        addEarthOrbit(jo, empty);
    }

    private void addSolarOrbiter(JSONObject jo, JSONObject empty) {
        String uiName = "Solar Orbiter";
        JSONObject jpo = jo.optJSONObject(uiName, empty);
        FOVPlatform plat = new FOVPlatform(uiName, "SOLO", SpaceObject.SOLO.getColor(), jpo);
        plat.add(new FOVInstrument("EUI/HRI", FOVInstrument.FOVType.RECTANGULAR, 0, 16.6 / 60., 16.6 / 60., jpo));
        plat.add(new FOVInstrument("EUI/FSI", FOVInstrument.FOVType.RECTANGULAR, 0, 228 / 60., 228 / 60., jpo));
        plat.add(new FOVInstrument("Metis", FOVInstrument.FOVType.CIRCULAR, 3.2, 6.8, 6.8, jpo));
        plat.add(new FOVInstrument("PHI/HRT", FOVInstrument.FOVType.RECTANGULAR, 0, 0.28, 0.28, jpo));
        plat.add(new FOVInstrument("PHI/FDT", FOVInstrument.FOVType.RECTANGULAR, 0, 2, 2, jpo));
        plat.add(new FOVInstrument("SPICE", FOVInstrument.FOVType.RECTANGULAR, 0, 16 / 60., 11 / 60., jpo));
        plat.add(new FOVInstrument("STIX", FOVInstrument.FOVType.RECTANGULAR, 0, 2, 2, jpo));
        addPlatform(plat);
    }

    private void addStereoAhead(JSONObject jo, JSONObject empty) {
        String uiName = "STEREO Ahead";
        JSONObject jpo = jo.optJSONObject(uiName, empty);
        FOVPlatform plat = new FOVPlatform(uiName, "STEREO AHEAD", SpaceObject.STA.getColor(), jpo);
        plat.add(new FOVInstrument("EUVI", FOVInstrument.FOVType.RECTANGULAR, 0, 1.5877740 * 2048 / 3600., 1.5877740 * 2048 / 3600., jpo));
        plat.add(new FOVInstrument("COR1", FOVInstrument.FOVType.RECTANGULAR, 0, 15.008600 * 512 / 3600., 15.008600 * 512 / 3600., jpo));
        plat.add(new FOVInstrument("COR2", FOVInstrument.FOVType.CIRCULAR, 0, 14.700000 * 2048 / 3600., 14.700000 * 2048 / 3600., jpo));
        addPlatform(plat);
    }

    private void addEarthOrbit(JSONObject jo, JSONObject empty) {
        String uiName = "Earth orbit";
        JSONObject jpo = jo.optJSONObject(uiName, empty);
        FOVPlatform plat = new FOVPlatform(uiName, "EARTH", Colors.Blue, jpo); // Earth approximate
        plat.add(new FOVInstrument("AIA", FOVInstrument.FOVType.RECTANGULAR, 0, (0.6 * 4096) / 3600., (0.6 * 4096) / 3600., jpo));
        plat.add(new FOVInstrument("HMI", FOVInstrument.FOVType.RECTANGULAR, 0, (0.6 * 4096) / 3600., (0.6 * 4096) / 3600., jpo));
        plat.add(new FOVInstrument("SWAP", FOVInstrument.FOVType.RECTANGULAR, 0, (3.1646941 * 1024) / 3600., (3.1646941 * 1024) / 3600., jpo));
        plat.add(new FOVInstrument("ASPIICS", FOVInstrument.FOVType.RECTANGULAR, .5850334, 1.6, 1.6, jpo));
        addPlatform(plat);
    }

    private void addPlatform(FOVPlatform platform) {
        platforms.add(platform);
    }

    public void init() {
        platforms.forEach(FOVPlatform::init);
    }

    public void dispose() {
        platforms.forEach(FOVPlatform::dispose);
    }

    public void render(MapView mv, Viewport vp, MapScale scale) {
        platforms.forEach(platform -> platform.render(mv, vp, scale));
    }

    public void serialize(JSONObject jo) {
        platforms.forEach(platform -> jo.put(platform.toString(), platform.toJson()));
    }

}
