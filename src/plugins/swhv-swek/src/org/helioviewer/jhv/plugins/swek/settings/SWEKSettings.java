package org.helioviewer.jhv.plugins.swek.settings;

import java.io.File;

import org.helioviewer.jhv.JHVDirectory;

public class SWEKSettings {

    /** Home directory of plugin */
    public static final String SWEK_HOME = JHVDirectory.PLUGINS.getPath() + "swek_plugin" + File.separator;

    /** Directpry with downloader jar containing the sources */
    public static final String SWEK_SOURCES = JHVDirectory.PLUGIN_RESOURCES.getPath() + "swek_plugin" + File.separator;

}
