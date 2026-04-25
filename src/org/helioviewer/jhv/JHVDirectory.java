package org.helioviewer.jhv;

import java.io.File;
import java.nio.charset.StandardCharsets;

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
            return transientRoot() + "Cache" + File.separator;
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
    // The SPICE kernels directory
    KERNELS {
        @Override
        public String getPath() {
            return HOME.getPath() + "kernels" + File.separator;
        }
    },
    // The downloads directory
    DOWNLOADS {
        @Override
        public String getPath() {
            return transientRoot() + "Downloads" + File.separator;
        }
    };

    // A String representation of the path of the directory
    public abstract String getPath();

    // A File representation of the path of the directory
    public File getFile() {
        return new File(getPath());
    }

    private static String transientRoot() {
        if (!Platform.isWindows())
            return HOME.getPath();

        String tmp = System.getProperty("java.io.tmpdir");
        String root = appendJHV(tmp);
        if (isUsableAsciiDirectory(root))
            return root;

        String systemRoot = System.getenv("SystemRoot");
        if (systemRoot != null) {
            root = appendJHV(systemRoot + File.separator + "Temp");
            if (isUsableAsciiDirectory(root))
                return root;
        }

        String programData = System.getenv("ProgramData");
        root = appendJHV(programData);
        if (isUsableAsciiDirectory(root))
            return root;

        throw new IllegalStateException("No writable ASCII temporary directory found. Set java.io.tmpdir to an ASCII path.");
    }

    private static boolean isUsableAsciiDirectory(String path) {
        if (path == null || !StandardCharsets.US_ASCII.newEncoder().canEncode(path))
            return false;

        File dir = new File(path);
        return (dir.isDirectory() || dir.mkdirs()) && dir.canWrite();
    }

    private static String appendJHV(String path) {
        if (path == null)
            return null;
        return path + File.separator + "JHelioviewer-SWHV" + File.separator;
    }

}
