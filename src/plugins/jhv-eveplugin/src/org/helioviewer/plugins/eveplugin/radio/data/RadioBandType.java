package org.helioviewer.plugins.eveplugin.radio.data;

import java.net.URL;
import java.util.Date;
import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.settings.BandType;

public class RadioBandType extends BandType {

    public RadioBandType() {}

    public URL buildUrl(Interval<Date> interval) {
        return null;
    }

    public String toStringAlt() {
        return "Alt string";
    }

}
