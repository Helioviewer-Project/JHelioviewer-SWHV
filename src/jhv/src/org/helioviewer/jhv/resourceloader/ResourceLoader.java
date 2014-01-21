package org.helioviewer.jhv.resourceloader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.helioviewer.base.DownloadStream;
import org.helioviewer.base.FileUtils;
import org.helioviewer.base.UploadStream;
import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.JHVGlobals;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * This class tries to download necessary resource files such as platform
 * dependent libraries and executables from a server. XML files are used to
 * specify which files are needed for a certain platform. The loader will try to
 * use custom XML config files first. If they are not present or the load
 * process fails the default XML config file on the default server is used. If
 * this fails too, the system tries to use the last known working configuration
 * if possible.
 * 
 * @author Andre Dau
 * 
 */
public class ResourceLoader {
    public static ResourceLoader instance = new ResourceLoader();

    private ResourceLoader() {
    }

    /**
     * Method returns the sole instance of this class.
     * 
     * @return the only instance of this class.
     * */
    public static ResourceLoader getSingletonInstance() {
        return instance;
    }

    /**
     * Tries to load resources from a server using XML configuration files.
     * First a custom XML config file will be used to load the resources. It
     * this fails a default XML config file will be used. If this fails too the
     * last known configuration will be loaded.
     * 
     * @param resourceName
     *            Identifier of the resource package. The XML config file for
     *            this resource package must have the name resourceName.xml
     * @param defaultResourceDefinitionDirectory
     *            Location of the default XML config file. Source paths within
     *            the XML config file are resolved against the path to the XML
     *            config file.
     * @param customResourceDefinitionDirectory
     *            Location of the custom XML config file. Source paths within
     *            the XML config file are resolved against the path to the XML
     *            config file.
     * @param installDirectory
     *            Base download directory for the resources. Destination paths
     *            in the XML file are resolved against the base download
     *            directory.
     * @param configBackupDirectory
     *            Location of the last known configuratoin XML config file.
     *            Source paths within the XML config file are resolved against
     *            the path to the XML config file.
     * @param systemProperties
     *            Properties file containing platform specific properties in
     *            order to determine the appropriate resource configuration.
     * @return The successfully loaded resource configuration or null if the
     *         resource loader failed.
     */
    public ResourceConfiguration loadResource(String resourceName, URI defaultResourceDefinitionDirectory, URI customResourceDefinitionDirectory, URI installDirectory, URI configBackupDirectory, Properties systemProperties) {
        String logHeader = ">> ResourceLoader.loadResource(" + resourceName + ", " + defaultResourceDefinitionDirectory + ", " + customResourceDefinitionDirectory + ", " + installDirectory + ", " + configBackupDirectory + ", Properties systemProperties) > ";
        try {
            ResourceConfiguration config = null;
            String resFile = resourceName + ".xml";
        /*    if (customResourceDefinitionDirectory != null) {
                config = loadResource(resourceName, customResourceDefinitionDirectory.resolve(resFile), installDirectory, systemProperties);
                if (config != null) {
                    updateLastConfig(resourceName, config, configBackupDirectory);
                    return config;
                } else {
                    Log.debug(logHeader + "Failed to load resources with custom resource definition file. Try default definiton file.");
                }
            }
            if (defaultResourceDefinitionDirectory != null) {
                config = loadResource(resourceName, defaultResourceDefinitionDirectory.resolve(resFile), installDirectory, systemProperties);
                if (config != null) {
                    updateLastConfig(resourceName, config, configBackupDirectory);
                    return config;
                } else {
                    Log.error(logHeader + "Failed to load resources with default resource definition file. Try to use last known configuration.");
                }
            }
            if (configBackupDirectory != null) {*/
                config = loadResource(resourceName, configBackupDirectory.resolve(resFile), installDirectory, systemProperties);
            /*}
            if (config == null) {
                Log.error(logHeader + "Failed to load last known configuration!");
            }*/
            return config;
        } catch (Throwable t) {
            Log.error(logHeader + "Could not load resources", t);
            return null;
        }
    }

    /**
     * Store the given resource configuration in an xml file.
     * 
     * @param resourceName
     *            Name of the resource package
     * @param configuration
     *            The resource configuration to save
     * @param backupDir
     *            The directory in which to store the configuration
     */
    private void updateLastConfig(String resourceName, ResourceConfiguration configuration, URI backupDir) {
        String xml = getXmlHeader() + System.getProperty("line.separator") + "<resourceDefinition>" + System.getProperty("line.separator") + configuration.toXml("    ") + "</resourceDefinition>" + System.getProperty("line.separator");
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new UploadStream(backupDir.resolve(resourceName + ".xml")).getOutput());
            writer.write(xml);
        } catch (Throwable t) {
            Log.error(">> ResourceLoader.updateLastConfig(" + configuration.getName() + ", " + backupDir + ") > Could not save current configuration", t);
        } finally {
            try {
                writer.close();
            } catch (Throwable t) {
                Log.error(">> ResourceLoader.updateLastConfig(" + configuration.getName() + ", " + backupDir + ") > Could not close stream", t);
            }
        }
    }

    /**
     * Tries to load a compatible configuration for a specific platform using a
     * resource definition. Resource definitions contain multiple
     * configurations.
     * 
     * @param resourceDefinition
     *            The resource definition containing the different
     *            configurations
     * @param installDirectory
     *            The directory to which the resources are saved
     * @param systemProperties
     *            Properties file containing platform specific properties in
     *            order to determine the appropriate resource configuration.
     * @return The successfully loaded resource configuration or null if the
     *         resource loader failed.
     **/
    private ResourceConfiguration loadResource(ResourceDefinition resourceDefinition, URI installDirectory, Properties systemProperties) {
        List<ResourceConfiguration> configurations = resourceDefinition.getCompatibleConfigurations(systemProperties);
        for (ResourceConfiguration configuration : configurations) {
            if (loadConfiguration(configuration, resourceDefinition.getLocation(), installDirectory)) {
                return configuration;
            }
        }
        return null;
    }

    /**
     * Tries to load all files from a single configuration
     * 
     * @param configuration
     *            The configuration to load
     * @param resourceDefinitonFileUri
     *            The base URI of the resource definition file. Source paths are
     *            resolved against this URI.
     * @param installDirectory
     *            The directory to which the resources are saved.
     * @return True, if the configuration was loaded successfully, false
     *         otherwise.
     */
    private boolean loadConfiguration(ResourceConfiguration configuration, URI resourceDefinitonFileUri, URI installDirectory) {
        for (ResourceFile resourceFile : configuration.getFiles()) {
            if (!loadFile(resourceFile, resourceDefinitonFileUri, installDirectory)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Loads a single file from a resource configuration. The file is first
     * downloaded and then initialized if necessary. Initalization can be action
     * such as loading a library or registering an executable. Initialization
     * parameters are given in the XML config file for each resource file.
     * 
     * @param file
     *            The file which should be loaded
     * @param resourceDefinitonFileUri
     *            The base URI of the resource definition file. Source paths are
     *            resolved against this URI.
     * @param installDirectory
     *            The directory to which the resources are saved.
     * @return True, if the file was loaded successfully, false otherwise.
     */
    private boolean loadFile(ResourceFile file, URI resourceDefinitonFileUri, URI installDirectory) {
        URI srcResolved = FileUtils.getWorkingDirectory().toURI().resolve(resourceDefinitonFileUri.resolve(file.getSrcDir()).resolve(file.getSrcName()));
        URI destResolved = FileUtils.getWorkingDirectory().toURI().resolve(installDirectory.resolve(file.getDestDir()).resolve(file.getDestName()));
        if (!srcResolved.equals(destResolved)) {
            if (!downloadFile(file, srcResolved, destResolved)) {
                return false;
            }
        }
        return initializeFile(file, destResolved);
    }

    /**
     * Downloads a resource file from a source to a destination if the correct
     * file is not already present. MD5 hashes are used to compare files. The
     * MD5 hash of the file which should be downloaded is read from the XML
     * config file. The MD5 hash of the destination file if it already exists is
     * calculated at runtime. The hash is also used to verify the successful
     * download of the file.
     * 
     * @param file
     *            The resource definition file
     * @param src
     *            The URI of the source file
     * @param dest
     *            The URI of the destination file
     * @return True if the file could be downloaded successfully.
     */
    private boolean downloadFile(ResourceFile file, URI src, URI dest) {
        String logHeader = ">> ResourceLoader.downloadFile(ResourceFile, " + src + ", " + dest + ") > ";
        if (src.equals(dest)) {
            return true;
        }

        OutputStream destStream = null;
        InputStream srcStream = null;
        try {
            byte[] md5Dest = null;
            if (file.getMd5() != null) {
                try {
                    md5Dest = FileUtils.calculateMd5(dest);
                    if (md5Dest != null && MessageDigest.isEqual(md5Dest, file.getMd5())) {
                        Log.debug(logHeader + "Correct destination file already present. Skip download.");
                        return true;
                    }
                } catch (FileNotFoundException e) {
                    Log.debug(logHeader + "Destination does not exist yet.");
                }
                md5Dest = null;
            }
            Log.info(logHeader + "Begin download of file.");
            destStream = new BufferedOutputStream(new UploadStream(dest).getOutput());
            srcStream = new BufferedInputStream(new DownloadStream(src, JHVGlobals.getStdConnectTimeout(), JHVGlobals.getStdReadTimeout()).getInput());
            int c;
            MessageDigest md5Algo = MessageDigest.getInstance("MD5");
            md5Algo.reset();
            while ((c = srcStream.read()) != -1) {
                destStream.write(c);
                md5Algo.update((byte) c);
            }
            md5Dest = md5Algo.digest();
            Log.debug(logHeader + "Calculate MD5 sum of downloaded file");
            if (file.getMd5() != null) {
                if (!MessageDigest.isEqual(file.getMd5(), md5Dest)) {
                    Log.error(">> ResourceFile.downloadFile(ResourceFile, " + src + ", " + dest + ") > MD5 hash sum error. File was not downloaded correctly.");
                    return false;
                }
            } else {
                file.setMd5(md5Dest);
            }
            Log.debug(logHeader + "Successfully downloaded file.");
            return true;
        } catch (Throwable t) {
            Log.error(">> ResourceLoader.downloadFile(" + src + ", " + dest + ") > Could not download file.", t);
        } finally {
            try {
                if (srcStream != null) {
                    srcStream.close();
                }
            } catch (Throwable t) {
                Log.error(">> ResourceLoader.downloadFile(" + src + ", " + dest + ") > Could not close src stream", t);
            }
            try {
                if (destStream != null) {
                    destStream.close();
                }
            } catch (Throwable t) {
                Log.error(">> ResourceLoader.downloadFile(" + src + ", " + dest + ") > Could not close dest stream", t);
            }
        }
        return false;
    }

    /**
     * Performs file initialization after the file has been downloaded
     * successfully. This can be for example loading the library or registering
     * an executable.
     * 
     * @param file
     *            Resource file to initialize
     * @param filePath
     *            URI to the downloaded file
     * @return True if the file was initialized successfully, false otherwise
     */
    private boolean initializeFile(ResourceFile file, URI filePath) {
        String logHeader = ">> initializeFile(" + file.getDestName() + ", " + filePath + ") > ";
        if (file.getLoadLibrary()) {
            try {
                if (!filePath.getScheme().equals("file")) {
                    Log.error(logHeader + "Only files which can be converted an absoulte path can be loaded as libraries.");
                    return false;
                }
                File lib = new File(filePath);
                Log.debug(logHeader + "Load library: " + lib.getAbsolutePath());
                System.load(lib.getAbsolutePath());
                return true;
            } catch (Throwable t) {
                Log.error(">> ResourceLoader.initializeFile > Error could not load library " + filePath, t);
                return false;
            }
        }
        if (file.getRegisterExecutable() != null) {
            try {
                if (!filePath.getScheme().equals("file")) {
                    Log.error(logHeader + "Only files which can be converted into an absolute path can be registered as executables.");
                    return false;
                }
                File exe = new File(filePath);
                Log.debug(logHeader + "Register executable: " + file.getDestName());
                FileUtils.registerExecutable(file.getRegisterExecutable(), exe.getAbsolutePath());
                return true;
            } catch (Throwable t) {
                Log.error(">> ResourceLoader.initializeFile > Error could not register executable " + filePath, t);
                return false;
            }
        }
        return true;
    }

    /**
     * Tries to find and load a compatible resource configuration within a
     * resource definition file.
     * 
     * @param resourceName
     *            The name of the resource package
     * @param resourceDefiniton
     *            The URI of the resource definition file
     * @param installDirectory
     *            The directory to which the resources are saved
     * @param systemProperties
     *            Properties file containing platform specific properties in
     *            order to determine the appropriate resource configuration.
     * @return The successfully loaded resource configuration or null if the
     *         resource loader failed.
     */
    private ResourceConfiguration loadResource(String resourceName, URI resourceDefiniton, URI installDirectory, Properties systemProperties) {
        String logHeader = ">> ResourceLoader.loadResource(" + resourceName + ", " + resourceDefiniton + ", Properties systemProperties) > ";

        InputStream resourceDefinitionStream = null;

        try {
            try {
                resourceDefinitionStream = new DownloadStream(resourceDefiniton, JHVGlobals.getStdConnectTimeout(), JHVGlobals.getStdReadTimeout()).getInput();
            } catch (IOException e) {
                Log.debug(logHeader + "Could not open resource definition file: " + resourceDefiniton + " (" + e.getClass().getName() + ")");
                return null;
            }
            resourceDefinitionStream = new BufferedInputStream(resourceDefinitionStream);
            InputSource resourceDefinitionSource = new InputSource(resourceDefinitionStream);
            ResourceDefinition resourceDefinition = new ResourceDefinition(resourceDefiniton, resourceDefinitionSource);
            return loadResource(resourceDefinition, installDirectory, systemProperties);

        } catch (Throwable t) {
            Log.error(logHeader + "Could not load resources.", t);
        } finally {
            if (resourceDefinitionStream != null) {
                try {
                    resourceDefinitionStream.close();
                } catch (Throwable t) {
                    Log.error(logHeader + "Could not close stream to " + resourceDefiniton, t);
                }
            }
        }
        return null;
    }

    /**
     * Struct representing a resource file entry in a resource definition XML
     * file
     * 
     * @author Andre Dau
     * 
     */
    public class ResourceFile {
        private URI srcDir;
        private URI destDir;
        private String srcName;
        private String destName;
        private boolean loadLibrary;
        private String registerExecutable;
        private byte[] md5;

        /**
         * Constructor
         * 
         * @param srcName
         *            Name of the resource file
         * @param srcDir
         *            Directory of the resource file (relative to the resource
         *            definition file)
         * @param destName
         *            Name of the destination file to which this resource file
         *            should be downloaded
         * @param destDir
         *            Name of the destination directtory to which this resource
         *            file should be downloaded (relative to the base donwload
         *            directory)
         * @param loadLibrary
         *            true, if the resource file should be loaded as a native
         *            JNI library after the download
         * @param registerExecutable
         *            Identifier under which the file should be registered as an
         *            executable or null if it should not be registered
         * @param md5String
         *            MD5 hash of the source file or null if unknown
         */
        public ResourceFile(String srcName, URI srcDir, String destName, URI destDir, boolean loadLibrary, String registerExecutable, String md5String) {
            this.srcDir = srcDir;
            this.destDir = destDir;
            this.srcName = srcName;
            this.destName = destName;
            this.loadLibrary = loadLibrary;
            this.registerExecutable = registerExecutable;
            if (md5String != null && md5String.length() > 0) {
                md5 = FileUtils.hexStringToByteArray(md5String);
            } else {
                md5 = null;
            }
        }

        /**
         * Get the XML representation of the resource file
         * 
         * @param indent
         *            The indentation which should be appended before each line.
         * @return The XML representation of this class
         */
        public String toXml(String indent) {
            String sep = System.getProperty("line.separator");
            String res = indent + "<file srcName=\"" + getSrcName() + "\" srcDir=\"" + getSrcDir() + "\" destName=\"" + getDestName() + "\" destDir=\"" + getDestDir() + "\" loadLibrary=\"" + getLoadLibrary() + "\"";
            if (getRegisterExecutable() != null) {
                res += " registerExecutable=\"" + getRegisterExecutable() + "\"";
            }
            if (getMd5() != null) {
                res += " md5=\"" + FileUtils.byteArrayToHexString(md5) + "\"";
            }
            res += "/>" + sep;
            return res;
        }

        /**
         * {@inheritDoc}
         */
        public String toString() {
            return "[" + srcName + ", " + srcDir + ", " + destName + ", " + destDir + "]";
        }

        /**
         * Getter method for the source directory
         * 
         * @return source directory
         */
        public URI getSrcDir() {
            return srcDir;
        }

        /**
         * Getter method for the destination directory
         * 
         * @return destination directory
         */
        public URI getDestDir() {
            return destDir;
        }

        /**
         * Getter method for the source file name
         * 
         * @return source file name
         */
        public String getSrcName() {
            return srcName;
        }

        /**
         * Getter method for the destination file name
         * 
         * @return destination file name
         */
        public String getDestName() {
            return destName;
        }

        /**
         * Getter method for loadLibrary parameter
         * 
         * @return true, if the file should be loaded as a native library
         */
        public boolean getLoadLibrary() {
            return loadLibrary;
        }

        /**
         * Getter method for name under which the file should be registered as
         * an executable
         * 
         * @return name under which the file should be registered as an
         *         executable or null
         */
        public String getRegisterExecutable() {
            return registerExecutable;
        }

        /**
         * Getter method for the MD5 hash
         * 
         * @return MD5 hash or null
         */
        public byte[] getMd5() {
            return md5;
        }

        /**
         * Setter method for the MD5 hash
         * 
         * @param md5Hash
         *            new md5 hash
         */
        public void setMd5(byte[] md5Hash) {
            md5 = md5Hash;
        }
    }

    /**
     * First line of a xml document specifying the encoding and xml version
     * 
     * @return XML header
     */
    private String getXmlHeader() {
        return "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>";
    }

    /**
     * Struct representing a resource configuration entry in a resource
     * definition XML file. Resource configurations can be compared based on
     * their priority.
     * 
     * @author Andre Dau
     * 
     */
    public class ResourceConfiguration implements Comparable<ResourceConfiguration> {
        private int priority;
        private List<ResourceRequirement> requirements;
        private List<ResourceFile> files;
        private String name;

        /**
         * Constructor
         * 
         * @param name
         *            of the resource configuration
         */
        public ResourceConfiguration(String name) {
            requirements = new LinkedList<ResourceRequirement>();
            files = new LinkedList<ResourceFile>();
            this.name = name;
        }

        /**
         * Get the XML representation of the resource configuration
         * 
         * @param indent
         *            The indentation which should be appended before each line.
         * @return The XML representation of this class
         */
        public String toXml(String indent) {
            String sep = System.getProperty("line.separator");
            String res = indent + "<configuration name=\"" + name + "\">" + sep;
            res += indent + "    <priority value=\"" + priority + "\"/>" + sep;
            res += indent + "    <requirements>" + sep;
            for (ResourceRequirement req : requirements) {
                res += req.toXml(indent + "      ");
            }
            res += indent + "    </requirements>" + sep;
            res += indent + "    <files>" + sep;
            for (ResourceFile file : files) {
                res += file.toXml(indent + "        ");
            }
            res += indent + "    </files>" + sep;
            res += indent + "</configuration>" + sep;
            return res;
        }

        /**
         * Set the priority of this conifguration
         * 
         * @param priority
         *            Priority
         */
        public void setPriority(int priority) {
            this.priority = priority;
        }

        /**
         * Add a requirement to the list
         * 
         * @param requirement
         *            The new requirement
         */
        public void addRequirement(ResourceRequirement requirement) {
            requirements.add(requirement);
        }

        /**
         * Add a file to the list
         * 
         * @param file
         *            The new file
         */
        public void addFile(ResourceFile file) {
            files.add(file);
        }

        /**
         * Get the last added requirement
         * 
         * @return The last added requirement
         */
        public ResourceRequirement getLastRequirement() {
            return requirements.get(requirements.size() - 1);
        }

        /**
         * Get the name of the configuration
         * 
         * @return The name of the configuration
         */
        public String getName() {
            return name;
        }

        /**
         * Get the file list of the configuration
         * 
         * @return File list of the configuration
         */
        public List<ResourceFile> getFiles() {
            return files;
        }

        /**
         * {@inheritDoc}
         */
        public int compareTo(ResourceConfiguration conf) {
            if (priority < conf.priority) {
                return -1;
            } else if (priority > conf.priority) {
                return +1;
            } else {
                return 0;
            }
        }

        /**
         * Tests if the given set of system properties does not satisfy the
         * requirements. In case the system properties do not contain all
         * properties needed to determine if the requirement is met the method
         * returns false.
         * 
         * @param systemProperties
         *            Set of system properties defining the platform environment
         * @return True, if the system does not meet the requirement; false if
         *         the system does meet the requirements or if it cannot be
         *         decided.
         * 
         */
        public boolean requirementsNotSatisfied(Properties systemProperties) {
            for (ResourceRequirement requirement : requirements) {
                if (requirement.isNotSatisfied(systemProperties)) {
                    Log.warn("> ResourceLoader.requirementsNotSatisfied() >> Requirement " + requirement.getName() + " not satisfied. Current value: " + systemProperties.getProperty(requirement.getName()));
                    return true;
                }
            }
            return false;
        }

    }

    /**
     * Struct representing a resource definition file . A resource definition
     * can contain several resource configurations for different platforms.
     * 
     * @author Andre Dau
     * 
     */
    public class ResourceDefinition extends DefaultHandler {
        private ResourceConfiguration currentConfig;
        private List<ResourceConfiguration> configurations;
        private boolean isValid;
        private URI locationUri;

        /**
         * Constructor. Parses a xml file to generate the resource definition
         * struct.
         * 
         * @param definitionFileUri
         *            URI for the reource definition XML file
         * @param xmlSource
         *            InputSource of the XML file
         * @throws SAXException
         * @throws IOException
         */
        public ResourceDefinition(URI definitionFileUri, InputSource xmlSource) throws SAXException, IOException {
            locationUri = definitionFileUri;
            XMLReader parser = XMLReaderFactory.createXMLReader("com.sun.org.apache.xerces.internal.parsers.SAXParser");
            parser.setContentHandler(this);
            parser.parse(xmlSource);
        }

        /**
         * Returns a list of configurations after filtering out all
         * configurations which are definitely not compatible with the current
         * platform. However a configuration within this list is NOT guaranteed
         * to be compatible with the current platform.
         * 
         * @param systemProperties
         *            The system properties defining the current platform
         * @return a list of possibly compatible resource configurations
         */
        public List<ResourceConfiguration> getCompatibleConfigurations(Properties systemProperties) {
            List<ResourceConfiguration> compatibleConfigurations = new LinkedList<ResourceConfiguration>();
            for (ResourceConfiguration configuration : configurations) {
                if (!configuration.requirementsNotSatisfied(systemProperties)) {
                    compatibleConfigurations.add(configuration);
                }
            }
            return compatibleConfigurations;
        }

        /**
         * Returns a list of all configurations defined in this resource
         * definition file.
         * 
         * @return A list of all resource configurations in this resource
         *         definition file
         */
        public List<ResourceConfiguration> getConfigurations() {
            return configurations;
        }

        /**
         * Returns the location of the resource definition file.
         * 
         * @return Location of the resource definition file
         */
        public URI getLocation() {
            return locationUri;
        }

        /**
         * This method is responsible for parsing the XML resource definition
         * file and building the corresponding resource definition struct.
         * 
         * {@inheritDoc}
         */
        public void startElement(String namespaceURI, String localName, String qualifiedName, Attributes atts) throws SAXException {
            try {
                if (localName.equals("resourceDefinition")) {
                    configurations = new LinkedList<ResourceConfiguration>();
                } else if (localName.equals("configuration")) {
                    String name = atts.getValue("name");
                    if (name == null) {
                        throw new SAXException("Required attribute 'name' in element 'configuration' missing.");
                    }
                    currentConfig = new ResourceConfiguration(name);
                    isValid = true;
                } else if (isValid) {
                    if (localName.equals("requirements")) {

                    } else if (localName.equals("requirement")) {

                        String name = atts.getValue("name");
                        if (name == null) {
                            throw new SAXException("Required attribute 'name' in element 'requirement' missing.");
                        }

                        String type = atts.getValue("type");
                        ResourceRequirement requirement = null;
                        requirement = new ResourceRequirement(name, type);
                        String value = atts.getValue("value");
                        if (value != null) {
                            requirement.addValue(value);
                        }
                        currentConfig.addRequirement(requirement);
                    } else if (localName.equals("requirementValue")) {
                        String value = atts.getValue("value");
                        if (value == null) {
                            throw new SAXException("Required attribute 'value' in element 'requirementValue' missing.");
                        }
                        currentConfig.getLastRequirement().addValue(value);
                    } else if (localName.equals("files")) {
                    } else if (localName.equals("file")) {
                        String srcDir = atts.getValue("srcDir");
                        String srcName = atts.getValue("srcName");
                        String destDir = atts.getValue("destDir");
                        String destName = atts.getValue("destName");
                        String dir = atts.getValue("dir");
                        String name = atts.getValue("name");

                        if (name != null && (srcName != null || destName != null)) {
                            throw new SAXException("Cant specify attributes 'srcName' or 'destName' when 'name' is specified in element 'file'");
                        }
                        if (srcName != null && destName == null || srcName == null && destName != null) {
                            throw new SAXException("Must specify all or none of the attributes 'srcName' and 'destName' in element 'file'");
                        }
                        if (srcName == null && destName == null && name == null) {
                            throw new SAXException("Must specify either 'name' or both 'srcName' and 'destName' in element 'file'");
                        }
                        if (name != null) {
                            srcName = destName = name;
                        }

                        if (dir != null && (srcDir != null || destDir != null)) {
                            throw new SAXException("Cant specify attributes 'srcDir' or 'destDir' when 'dir' is specified in element 'file'");
                        }
                        if (srcDir != null && destDir == null || srcDir == null && destDir != null) {
                            throw new SAXException("Must specify all or none of the attributes 'srcDir' and 'destDir' in element 'file'");
                        }
                        if (srcDir == null && destDir == null && dir == null) {
                            throw new SAXException("Must specify either 'dir' or both 'srcDir' and 'destDir' in element 'file'");
                        }
                        if (dir != null) {
                            srcDir = destDir = dir;
                        }

                        if (!destDir.endsWith("/")) {
                            destDir += "/";
                        }

                        if (!srcDir.endsWith("/")) {
                            srcDir += "/";
                        }
                        String loadLibrary = atts.getValue("loadLibrary");
                        String registerExecutable = atts.getValue("registerExecutable");
                        String md5 = atts.getValue("md5");

                        URI src = new URI(srcDir);
                        URI dest = new URI(destDir);
                        currentConfig.addFile(new ResourceFile(srcName, src, destName, dest, Boolean.parseBoolean(loadLibrary), registerExecutable, md5));
                    } else if (localName.equals("priority")) {
                        int priority = Integer.parseInt(atts.getValue("value"));
                        currentConfig.setPriority(priority);
                    }
                }
            } catch (Throwable t) {
                Log.error(">> ResourcePackageDefintionHandler.startElement(String,String,String,Attributes) > Error while parsing resource configuration: " + currentConfig.getName() + "." + "The resource alternative will be skipped", t);
                isValid = false;
            }

        }

        /**
         * After parsing the resource definition end tag the configurations are
         * sorted by priority. After parsing a configuration end tag the current
         * configuration is added to the list of all configurations.
         * 
         * {@inheritDoc}
         */
        public void endElement(String namespaceURI, String localName, String qualifiedName) throws SAXException {
            if (localName.equals("configuration")) {
                if (isValid) {
                    configurations.add(currentConfig);
                }
                currentConfig = null;
            } else if (localName.equals("resourceDefinition")) {
                Collections.sort(configurations);
            }
        }

    }

    private enum REQUIREMENT_TYPES {
        STRING, VERSION
    };

    private enum VERSION_COMP_MODE {
        LESS, LESSEQUAL, EQUAL, GREATEREQUAL, GREATER
    };

    /**
     * Struct representing a resource requirement entry in a resource definition
     * file . A requirement can have different types. Currently there are two
     * types: versions and strings. To meet a string requirement the
     * corresponding system property must be equal to the requirement string.
     * Versions are numbers separated by dots. The numbers are compared from
     * left to right. A version string start with a comparator, for example
     * '>=2.0.0' which means, that the system version must be greater or equal
     * to 2.0.0 . Each requirement can have multiple values. These values are
     * alternatives. The system does not satisfy the requirement if it does not
     * satisfy any of the possible values.
     * 
     * @author Andre Dau
     * 
     */
    public class ResourceRequirement {
        private String name;
        private REQUIREMENT_TYPES type;
        private List<String> values;

        /**
         * Constructor
         * 
         * @param name
         *            The name of the requirement (not the value)
         * @param typeString
         *            The type of the requirement
         * @throws SAXException
         */
        public ResourceRequirement(String name, String typeString) throws SAXException {
            this.name = name;
            if (typeString == null) {
                typeString = "string";
            }
            if (typeString.equals("string")) {
                type = REQUIREMENT_TYPES.STRING;
            } else if (typeString.equals("version")) {
                type = REQUIREMENT_TYPES.VERSION;
            } else {
                throw new SAXException("Unknown type for requirement '" + name + "'");
            }
            values = new LinkedList<String>();
        }

        /**
         * Get the XML representation of the resource requirement
         * 
         * @param indent
         *            The indentation which should be appended before each line.
         * @return The XML representation of this class
         */
        public String toXml(String indent) {
            String sep = System.getProperty("line.separator");
            String res = indent + "<requirement name=\"" + getName() + "\" type=\"" + getType() + "\">" + sep;
            for (String val : getValues()) {
                res += indent + "    <requirementValue value=\"" + val + "\"/>" + sep;
            }
            res += indent + "</requirement>" + sep;
            return res;
        }

        /**
         * Get the type of the resource requirement
         * 
         * @return Type of the resource requirement
         */
        public String getType() {

            switch (type) {
            case STRING:
                return "string";
            case VERSION:
                return "version";
            }
            return "unknown";
        }

        /**
         * Add value to the requirement. To satisfy a requirement the system
         * property must satisfy at least one value.
         * 
         * @param value
         */
        public void addValue(String value) {
            values.add(value);
        }

        /**
         * 
         * @return
         * 
         *         The name of the requirement
         */
        public String getName() {
            return name;
        }

        /**
         * Get all possible values for this requirement
         * 
         * @return All valid values for this requirement
         */
        public List<String> getValues() {
            return values;
        }

        /**
         * This function checks if a given set of system properties does not
         * satisfy the requirement. The properties does not satisfy the
         * requirement they do not satisfy any of the possible values. If it can
         * not be determined if this requirement is met the function returns
         * false.
         * 
         * @param systemProperties
         *            System properties defining the current platform
         * @return True if and only if it is certain that none of the possible
         *         values are satisfied by the system properties, false
         *         otherwise
         */
        public boolean isNotSatisfied(Properties systemProperties) {
            String sysProp = systemProperties.getProperty(name);
            if (sysProp == null) {
                return true;
            }
            if (type == REQUIREMENT_TYPES.STRING) {
                for (String value : values) {
                    if (value.equals(sysProp)) {
                        return false;
                    }
                }
            } else if (type == REQUIREMENT_TYPES.VERSION) {
                for (String value : values) {
                    if (!invalidVersion(sysProp, value)) {
                        return false;
                    }
                }
            } else {
                throw new IllegalStateException(">> ResourceLoader.Requirement.isSatsified(Properties) > Unhandled type " + type);
            }
            return true;
        }

        /**
         * Tests if a a given system version string represents a valid version
         * given a requirement string. A requirement string can start with one
         * of the comparators <, <=, =, >= or > . If no comparator is specified
         * = is assumed.
         * 
         * @param systemProperty
         *            The platform specific version string
         * @param requirement
         *            The requirement version string
         * @return True, if the system version string is NOT valid, false
         *         otherwise
         */
        public boolean invalidVersion(String systemProperty, String requirement) {
            Pattern p = Pattern.compile("(\\d(\\.\\d)*)-(\\d(\\.\\d)*)");
            Matcher m = p.matcher(requirement);
            if (m.find()) {
                if (m.end() == requirement.length()) {
                    return invalidVersion(systemProperty, ">=" + m.group(1)) || invalidVersion(systemProperty, "<=" + m.group(3));
                } else {
                    throw new IllegalArgumentException("Invalid required version: " + requirement);
                }
            }
            VERSION_COMP_MODE mode;

            if (requirement == null || systemProperty == null) {
                return false;
            } else {
                if (requirement.startsWith("=")) {
                    mode = VERSION_COMP_MODE.EQUAL;
                    requirement = requirement.substring(1, requirement.length());
                } else if (requirement.startsWith("<=")) {
                    mode = VERSION_COMP_MODE.LESSEQUAL;
                    requirement = requirement.substring(2, requirement.length());
                } else if (requirement.startsWith("<")) {
                    mode = VERSION_COMP_MODE.LESS;
                    requirement = requirement.substring(1, requirement.length());
                } else if (requirement.startsWith(">=")) {
                    mode = VERSION_COMP_MODE.GREATEREQUAL;
                    requirement = requirement.substring(2, requirement.length());
                } else if (requirement.startsWith(">")) {
                    mode = VERSION_COMP_MODE.GREATER;
                    requirement = requirement.substring(1, requirement.length());
                } else if (Character.isDigit(requirement.charAt(0))) {
                    mode = VERSION_COMP_MODE.EQUAL;
                } else {
                    throw new IllegalArgumentException("Invalid required version: " + requirement);
                }

                String reqParts[] = requirement.split("\\.");
                String sysParts[] = systemProperty.split("\\.");
                int commonLength = Math.min(reqParts.length, sysParts.length);
                for (int i = 0; i < commonLength; ++i) {
                    int rVersion = Integer.parseInt(reqParts[i]);
                    int sVersion = Integer.parseInt(sysParts[i]);

                    switch (mode) {
                    case EQUAL:
                        if (sVersion != rVersion)
                            return true;
                        break;
                    case LESS:
                    case LESSEQUAL:
                        if (sVersion < rVersion)
                            return false;
                        else if (rVersion < sVersion)
                            return true;
                        break;
                    case GREATER:
                    case GREATEREQUAL:
                        if (sVersion > rVersion) {
                            return false;
                        } else if (rVersion > sVersion) {
                            return true;
                        }
                        break;
                    }
                }
                if (reqParts.length > sysParts.length) {
                    if (mode == VERSION_COMP_MODE.GREATER) {
                        return true;
                    }
                    for (int i = commonLength; i < reqParts.length; ++i) {
                        if (Integer.parseInt(reqParts[i]) != 0) {
                            if (mode == VERSION_COMP_MODE.EQUAL || mode == VERSION_COMP_MODE.GREATEREQUAL) {
                                return true;
                            } else {
                                return false;
                            }
                        }
                    }
                    if (mode == VERSION_COMP_MODE.LESS) {
                        return true;
                    } else {
                        return false;
                    }
                } else if (reqParts.length < sysParts.length) {
                    if (mode == VERSION_COMP_MODE.LESS) {
                        return true;
                    }
                    for (int i = commonLength; i < sysParts.length; ++i) {
                        if (Integer.parseInt(sysParts[i]) != 0) {
                            if (mode == VERSION_COMP_MODE.EQUAL || mode == VERSION_COMP_MODE.LESSEQUAL) {
                                return true;
                            } else {
                                return false;
                            }
                        }
                    }
                    if (mode == VERSION_COMP_MODE.GREATER) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    if (mode == VERSION_COMP_MODE.LESS || mode == VERSION_COMP_MODE.GREATER) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }
    }
}
