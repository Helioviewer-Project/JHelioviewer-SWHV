package org.helioviewer.jhv;

import java.io.File;

/**
 * An enum containing all the directories mapped in a system independent way. If
 * a new directory is required, just add it here and it will be created at
 * startup.
 * 
 * @author caplins
 * 
 */
public enum JHVDirectory {
    /** The home directory. */
    HOME {
        private final String path = System.getProperty("user.home");

        public String getPath() {
            return path + FILE_SEP + "JHelioviewer" + FILE_SEP;
        }

        public File getFile() {
            return new File(getPath());
        }
    },
    /** The image cache directory. */
    CACHE {
        public String getPath() {
            return HOME.getPath() + "Cache" + FILE_SEP;
        }

        public File getFile() {
            return new File(getPath());
        }
    },
    /** The shared library directory. */
    LIBS {
        public String getPath() {
            return HOME.getPath() + "Libs" + FILE_SEP;
        }

        public File getFile() {
            return new File(getPath());
        }
    },
    /** The shared library directory. */
    LIBS_LAST_CONFIG {
        public String getPath() {
            return HOME.getPath() + "Libs" + FILE_SEP + "LastConfig" + FILE_SEP;
        }

        public File getFile() {
            return new File(getPath());
        }
    },
    /** The JHV state directory. */
    STATES {
        public String getPath() {
            return HOME.getPath() + "States" + FILE_SEP;
        }

        public File getFile() {
            return new File(getPath());
        }
    },
    /** The exports directory (movies, screenshots, meta data). */
    EXPORTS {
        public String getPath() {
            return HOME.getPath() + "Exports" + FILE_SEP;
        }

        public File getFile() {
            return new File(getPath());
        }
    },
    /** The log directory. */
    LOGS {
        public String getPath() {
            return HOME.getPath() + "Logs" + FILE_SEP;
        }

        public File getFile() {
            return new File(getPath());
        }
    },
    /** The log settings directory. */
    SETTINGS {
        public String getPath() {
            return HOME.getPath() + "Settings" + FILE_SEP;
        }

        public File getFile() {
            return new File(getPath());
        }
    },
    /** The remote files directory. */
    REMOTEFILES {
        public String getPath() {
            return HOME.getPath() + "Downloads" + FILE_SEP;
        }

        public File getFile() {
            return new File(getPath());
        }
    },
    /** The plugins directory. */
    PLUGINS {
        public String getPath() {
            return HOME.getPath() + "Plugins" + FILE_SEP;
        }

        public File getFile() {
            return new File(getPath());
        }
    },
    /** The plugins directory. */
    PLUGINS_LAST_CONFIG {
        public String getPath() {
            return HOME.getPath() + "Plugins" + FILE_SEP + "LastConfig" + FILE_SEP;
        }

        public File getFile() {
            return new File(getPath());
        }
    },
    /** Directory of automatically loaded GIMP gradient files. */
    COLOR_PLUGINS {
        public String getPath() {
            return HOME.getPath() + "Colortables" + FILE_SEP;
        }

        public File getFile() {
            return new File(getPath());
        }
    },
    /** temp directory for the Cg-compiler */
    TEMP {
        public String getPath() {
            return HOME.getPath() + "Temp" + FILE_SEP;
        }

        public File getFile() {
            return new File(getPath());
        }
    },
    /** The remote files directory. */
    VSO_DOWNLOAD {
        public String getPath() {
            return HOME.getPath() + "VSOData" + FILE_SEP;
        }

        public File getFile() {
            return new File(getPath());
        }
    };
    /**
     * The system dependent file separator.
     * <p>
     * 
     * @see File#separator
     * @see File#separatorChar
     **/
    @Deprecated
    public static final String FILE_SEP = System.getProperty("file.separator");

    /** A String representation of the path of the directory. */
    abstract public String getPath();

    /** A File representation of the path of the directory. */
    abstract public File getFile();

};