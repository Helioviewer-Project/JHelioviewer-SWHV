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
            // transient files, access problem for non-ASCII username on Windows
            String cacheRoot = Platform.isWindows() ? System.getProperty("java.io.tmpdir") + File.separator + "JHelioviewer-SWHV" + File.separator : HOME.getPath();
            return cacheRoot + "Cache" + File.separator;
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
    // The settings directory
    SETTINGS {
        @Override
        public String getPath() {
            return HOME.getPath() + "Settings" + File.separator;
        }
    },
    // The downloads directory
    DOWNLOADS {
        @Override
        public String getPath() {
            // access problem for non-ASCII username on Windows
            String downRoot = Platform.isWindows() ? System.getProperty("java.io.tmpdir") + File.separator + "JHelioviewer-SWHV" + File.separator : HOME.getPath();
            return downRoot + "Downloads" + File.separator;
        }
    };

    // A String representation of the path of the directory
    public abstract String getPath();

    // A File representation of the path of the directory
    public File getFile() {
        return new File(getPath());
    }

}
