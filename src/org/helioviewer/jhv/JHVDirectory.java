package org.helioviewer.jhv;

import java.io.File;

// An enum containing all the directories mapped in a system independent way. If
// a new directory is required, just add it here, and it will be created at startup.
public enum JHVDirectory {
    // The home directory
    HOME {
        private final String path = System.getProperty("user.home");

        @Override
        public String getPath() {
            return path + File.separator + "JHelioviewer-SWHV" + File.separator;
        }
    },
    CACHE {
        @Override
        public String getPath() {
            return HOME.getPath() + "Cache" + File.separator;
        }
    },
    // The JHV state directory
    STATES {
        @Override
        public String getPath() {
            return HOME.getPath() + "States" + File.separator;
        }
    },
    // The exports directory (movies, screenshots, meta data)
    EXPORTS {
        @Override
        public String getPath() {
            return HOME.getPath() + "Exports" + File.separator;
        }
    },
    // The log directory
    LOGS {
        @Override
        public String getPath() {
            return HOME.getPath() + "Logs" + File.separator;
        }
    },
    // The log settings directory
    SETTINGS {
        @Override
        public String getPath() {
            return HOME.getPath() + "Settings" + File.separator;
        }
    },
    // The remote files directory
    REMOTEFILES {
        @Override
        public String getPath() {
            return HOME.getPath() + "Downloads" + File.separator;
        }
    },
    // Directory of automatically loaded GIMP gradient files
    COLOR_PLUGINS {
        @Override
        public String getPath() {
            return HOME.getPath() + "Colortables" + File.separator;
        }
    },
    TEMP {
        @Override
        public String getPath() {
            return HOME.getPath() + "Temp" + File.separator;
        }
    },
    EVENTS {
        @Override
        public String getPath() {
            return HOME.getPath() + "Events" + File.separator;
        }
    };

    // A String representation of the path of the directory
    public abstract String getPath();

    // A File representation of the path of the directory
    public File getFile() {
        return new File(getPath());
    }

}
