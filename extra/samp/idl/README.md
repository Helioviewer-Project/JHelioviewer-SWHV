# README
This folder contains examples on how to send and receive notifications using SAMP in IDL. In order to receive notifications we use the SAMP hub client *JSAMP* which can be accessed using the *IDL-Java bridge*.

Contents:  
**idljava.pro**: Functions for setting up and using JSAMP and VSO  
**start.pro**: Example usage of *idljava.pro*  
**utils.pro**: General help functions e.g. for `datetime` operations  
**IDL_JSAMP_Bridge.jar**: Helper class for accessing received notifications, *compiled with JDK 1.8.0_181*  
**IDL_JSAMP_Bridge/src/ch/fhnw/jsamp/IDL_MessageHandler.java**: Source code for helper class  
**samp.pro**: Example on how to send notifications to the SAMP hub using pure IDL

### Setup IDL-Java Bridge
1. Download *JSAMP* from their [Official website](http://www.star.bristol.ac.uk/~mbt/jsamp) or from [GitHub](https://github.com/mbtaylor/jsamp)
1. If your IDL is using JRE 1.8 or higher you can use the distributed *IDL_JSAMP_Bridge.jar*. Otherwise you have to tell IDL to use a different JRE (see [IDL-Java Bridge configuration](#idl-java-bridge-configuration)), or you have to compile the class manually using the JDK that matches your IDL's JRE (or lower).  
   *note: prior to IDL 8.7.1 the default JRE distributed was 1.7*
1. Make sure IDL knows where to find the two .jar files:
   
   Option 1: Set the environment variable `CLASSPATH` **before** IDL initializes the IDL-Java Bridge (currently done in `initialize_IDL_Java_Bridge`)
   
   Option 2: Adjust your IDL-Java Bridge configuration file (see [IDL-Java Bridge configuration](#idl-java-bridge-configuration))

For more informations see [Official reference](https://www.harrisgeospatial.com/docs/initializingtheidl-javabridge.html).

### IDL-Java Bridge configuration
*The configuration file for the IDL-Java Bridge is located in <IDL_DEFAULT>/resource/bridges/import/java* and is called *.idljavabrc* on linux and *idljavabrc* on Windows. If you're using 32-bit IDL on a 64-bit Windows, the file is called *idljavabrc.32*.

The configuration file contains:
* The **JVM Classpath** which should contain all the .jar files you wish to use
* The **JVM LibLocation** which defines the path to the JRE that IDL uses.

These variables can also be overwriten using `SETENV` as it is currently donein `initialize_IDL_Java_Bridge`
For more information see [Official reference](https://www.harrisgeospatial.com/docs/initializingtheidl-javabridge.html) as well as [Changing the Java version used with IDL](https://www.harrisgeospatial.com/Support/Self-Help-Tools/Help-Articles/Help-Articles-Detail/ArtMID/10220/ArticleID/16290/Changing-the-Java-version-used-with-the-IDL-8-Workbench) 
