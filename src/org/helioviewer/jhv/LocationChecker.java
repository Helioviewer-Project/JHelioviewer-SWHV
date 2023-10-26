package org.helioviewer.jhv;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;

import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.NetClient;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CountryResponse;
import okio.BufferedSource;

class LocationChecker {

    static void setProximityServer() {
        new Thread(() -> {
            try (NetClient nc = NetClient.of(new URI("https://api.ipify.org")); BufferedSource source = nc.getSource()) {
                String address = source.readUtf8Line();
                if (address == null || address.isEmpty()) {
                    throw new Exception("Location Checker: Empty IP");
                }
                // Log.info(address);
                try (InputStream is = FileUtils.getResource("/geoip/GeoLite2-Country.mmdb")) {
                    DatabaseReader reader = new DatabaseReader.Builder(is).build();
                    CountryResponse response = reader.country(InetAddress.getByName(address));
                    // Log.info(response.getCountry().getIsoCode());
                    String continent = response.getContinent().getCode();
                    if ("EU".equals(continent))
                        Settings.setProperty("default.server", "IAS");
                    else
                        Settings.setProperty("default.server", "GSFC");
                    Log.info("Location: " + continent + " -> default server: " + Settings.getProperty("default.server"));
                }
            } catch (Exception e) {
                Settings.setProperty("default.server", "GSFC");
                Log.warn(e);
            }
        }, "Location Checker").start();
    }

}
